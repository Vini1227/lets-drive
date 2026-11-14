package com.letsdrive.app.data.location

import com.letsdrive.app.model.LatLng
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.serialization.json.*
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

data class PlaceSuggestion(
    val title: String,
    val subtitle: String,
    val fullAddress: String,
    val coordinates: LatLng
)

data class RouteMetrics(
    val distanceKm: Double,
    val durationMinutes: Int,
    val mainHighwayName: String,
    val destinationCoordinates: LatLng,
    val originCoordinates: LatLng = LatLng(-9.6498, -35.7089),
    val routeGeometry: List<LatLng> = emptyList(),
    val intermediateStopAddress: String? = null,
    val intermediateStopCoordinates: LatLng? = null
)

/**
 * Serviço de Geocodificação (OpenStreetMap Nominatim) e Roteamento Viário (OSRM API)
 * com catálogo rápido de destinos e fallback geodésico offline.
 */
class RouteSearchService {

    private companion object {
        val geocodedCache = mutableMapOf<String, LatLng>()
        val reverseCache = mutableMapOf<String, String>()
    }

    private val httpClient = HttpClient()
    private val json = Json { ignoreUnknownKeys = true }

    private val presetPlaces = listOf(
        PlaceSuggestion(
            title = "Praia da Palma, Alagoas",
            subtitle = "Litoral Norte • AL-101 Norte",
            fullAddress = "Praia da Palma, Alagoas",
            coordinates = LatLng(-9.5120, -35.6810)
        ),
        PlaceSuggestion(
            title = "Praia do Francês, AL",
            subtitle = "Marechal Deodoro • AL-101 Sul",
            fullAddress = "Praia do Francês, AL",
            coordinates = LatLng(-9.7708, -35.8406)
        ),
        PlaceSuggestion(
            title = "Barra de São Miguel, AL",
            subtitle = "Litoral Sul • AL-101 Sul",
            fullAddress = "Barra de São Miguel, AL",
            coordinates = LatLng(-9.8402, -35.9080)
        ),
        PlaceSuggestion(
            title = "Maragogi, Alagoas",
            subtitle = "Costa dos Corais • AL-101 Norte",
            fullAddress = "Maragogi, Alagoas",
            coordinates = LatLng(-9.0122, -35.2225)
        ),
        PlaceSuggestion(
            title = "Pajuçara, Maceió",
            subtitle = "Orla Marítima • Maceió - AL",
            fullAddress = "Pajuçara, Maceió",
            coordinates = LatLng(-9.6691, -35.7145)
        ),
        PlaceSuggestion(
            title = "Aeroporto Zumbi dos Palmares",
            subtitle = "Rio Largo • BR-104",
            fullAddress = "Aeroporto Zumbi dos Palmares",
            coordinates = LatLng(-9.5108, -35.7917)
        )
    )

    /**
     * Busca sugestões de lugares combinando catálogo instantâneo + API Nominatim OpenStreetMap.
     */
    suspend fun searchPlaces(query: String): List<PlaceSuggestion> {
        val clean = query.trim()
        val localMatches = if (clean.isEmpty()) {
            presetPlaces.take(4)
        } else {
            presetPlaces.filter {
                it.title.contains(clean, ignoreCase = true) ||
                    it.subtitle.contains(clean, ignoreCase = true)
            }
        }

        if (clean.length < 3) {
            return localMatches.ifEmpty { presetPlaces.take(4) }
        }

        val remoteMatches = runCatching {
            val responseText = httpClient.get("https://nominatim.openstreetmap.org/search") {
                parameter("q", "$clean, Brasil")
                parameter("format", "json")
                parameter("limit", "4")
            }.bodyAsText()

            val array = json.parseToJsonElement(responseText).jsonArray
            array.mapNotNull { elem ->
                val obj = elem.jsonObject
                val displayName = obj["display_name"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                val lat = obj["lat"]?.jsonPrimitive?.doubleOrNull ?: return@mapNotNull null
                val lon = obj["lon"]?.jsonPrimitive?.doubleOrNull ?: return@mapNotNull null
                val parts = displayName.split(",").map { it.trim() }
                val title = parts.take(2).joinToString(", ")
                val subtitle = parts.drop(2).take(2).joinToString(", ").ifBlank { "Brasil" }
                val coords = LatLng(lat, lon)
                cachePlace(title, coords)
                cachePlace(displayName, coords)
                PlaceSuggestion(
                    title = title,
                    subtitle = subtitle,
                    fullAddress = title,
                    coordinates = coords
                )
            }
        }.getOrDefault(emptyList())

        return (localMatches + remoteMatches).distinctBy { it.title }.take(5)
    }

    private fun cachePlace(address: String, coords: LatLng) {
        val normalized = address.trim().lowercase()
        if (normalized.isNotBlank()) {
            geocodedCache[normalized] = coords
        }
    }

    /**
     * Resolve coordenadas de forma assíncrona consultando o catálogo local, cache Nominatim
     * e, quando necessário, a API pública de geocodificação OpenStreetMap Nominatim.
     */
    suspend fun resolveCoordinatesAsync(address: String, default: LatLng): LatLng {
        val clean = address.trim()
        if (clean.isEmpty()) return default

        resolveFromLocalOrCache(clean)?.let { return it }

        if (clean.length >= 3) {
            val remote = runCatching {
                val responseText = httpClient.get("https://nominatim.openstreetmap.org/search") {
                    parameter("q", "$clean, Brasil")
                    parameter("format", "json")
                    parameter("limit", "1")
                }.bodyAsText()
                val first = json.parseToJsonElement(responseText).jsonArray.firstOrNull()?.jsonObject
                val lat = first?.get("lat")?.jsonPrimitive?.doubleOrNull
                val lon = first?.get("lon")?.jsonPrimitive?.doubleOrNull
                if (lat != null && lon != null) {
                    val coords = LatLng(lat, lon)
                    cachePlace(clean, coords)
                    first["display_name"]?.jsonPrimitive?.contentOrNull?.let { full ->
                        cachePlace(full, coords)
                        val shortTitle = full.split(",").take(2).joinToString(",").trim()
                        cachePlace(shortTitle, coords)
                    }
                    coords
                } else null
            }.getOrNull()

            if (remote != null) return remote
        }

        return default
    }

    /**
     * Geocodificação reversa: converte coordenadas GPS (LatLng) em endereço legível via Nominatim.
     */
    suspend fun reverseGeocode(coords: LatLng): String {
        val cacheKey = "${(coords.latitude * 10000).roundToInt()},${(coords.longitude * 10000).roundToInt()}"
        reverseCache[cacheKey]?.let { cached ->
            cachePlace(cached, coords)
            return cached
        }

        val resolved = runCatching {
            val body = httpClient.get("https://nominatim.openstreetmap.org/reverse") {
                parameter("lat", coords.latitude.toString())
                parameter("lon", coords.longitude.toString())
                parameter("format", "json")
                parameter("zoom", "18")
                parameter("addressdetails", "1")
            }.bodyAsText()
            val obj = json.parseToJsonElement(body).jsonObject
            val addr = obj["address"]?.jsonObject
            val road = addr?.get("road")?.jsonPrimitive?.contentOrNull
                ?: addr?.get("pedestrian")?.jsonPrimitive?.contentOrNull
                ?: addr?.get("suburb")?.jsonPrimitive?.contentOrNull
                ?: addr?.get("neighbourhood")?.jsonPrimitive?.contentOrNull
            val houseNumber = addr?.get("house_number")?.jsonPrimitive?.contentOrNull
            val city = addr?.get("city")?.jsonPrimitive?.contentOrNull
                ?: addr?.get("town")?.jsonPrimitive?.contentOrNull
                ?: addr?.get("municipality")?.jsonPrimitive?.contentOrNull

            when {
                road != null && houseNumber != null -> "$road, $houseNumber"
                road != null && city != null -> "$road, $city"
                road != null -> road
                else -> obj["display_name"]?.jsonPrimitive?.contentOrNull
                    ?.split(",")?.take(2)?.joinToString(",")?.trim()
            }?.takeIf { it.isNotBlank() }
        }.getOrNull()

        val finalAddress = resolved ?: run {
            val nearestPreset = presetPlaces.minByOrNull { GeoSpatialCalculator.distanceKm(coords, it.coordinates) }
            if (nearestPreset != null && GeoSpatialCalculator.distanceKm(coords, nearestPreset.coordinates) <= 2.5) {
                nearestPreset.title
            } else {
                val latShort = (coords.latitude * 1000.0).roundToInt() / 1000.0
                val lonShort = (coords.longitude * 1000.0).roundToInt() / 1000.0
                "Localização GPS ($latShort, $lonShort)"
            }
        }

        reverseCache[cacheKey] = finalAddress
        cachePlace(finalAddress, coords)
        return finalAddress
    }

    /**
     * Resolve coordenadas de um endereço digitado (inclui parada opcional) e calcula a rota real via OSRM.
     */
    suspend fun calculateRoute(
        originText: String,
        destinationText: String,
        extraStopText: String? = null
    ): RouteMetrics {
        val originCoords = resolveCoordinatesAsync(originText, default = LatLng(-9.6498, -35.7089))
        val destCoords = resolveCoordinatesAsync(destinationText, default = LatLng(-9.5120, -35.6810))
        val cleanStop = extraStopText?.trim()?.takeIf { it.isNotBlank() }
        val stopCoords = cleanStop?.let { stopQuery ->
            val midFallback = LatLng(
                latitude = (originCoords.latitude + destCoords.latitude) / 2.0,
                longitude = (originCoords.longitude + destCoords.longitude) / 2.0
            )
            resolveCoordinatesAsync(stopQuery, default = midFallback)
        }
        return calculateRouteByCoordinates(
            originCoords = originCoords,
            destCoords = destCoords,
            stopCoords = stopCoords,
            stopAddress = cleanStop
        )
    }

    /**
     * Calcula rota real de condução entre duas (ou três, com parada) coordenadas via API OSRM.
     */
    suspend fun calculateRouteByCoordinates(
        originCoords: LatLng,
        destCoords: LatLng,
        stopCoords: LatLng? = null,
        stopAddress: String? = null
    ): RouteMetrics {
        val waypoints = buildString {
            append("${originCoords.longitude},${originCoords.latitude}")
            if (stopCoords != null) {
                append(";${stopCoords.longitude},${stopCoords.latitude}")
            }
            append(";${destCoords.longitude},${destCoords.latitude}")
        }

        val osrmResult = runCatching {
            val url = "https://router.project-osrm.org/route/v1/driving/$waypoints?overview=full&geometries=geojson"
            val body = httpClient.get(url).bodyAsText()
            val root = json.parseToJsonElement(body).jsonObject
            val firstRoute = root["routes"]?.jsonArray?.firstOrNull()?.jsonObject
            val distMeters = firstRoute?.get("distance")?.jsonPrimitive?.doubleOrNull
            val durationSeconds = firstRoute?.get("duration")?.jsonPrimitive?.doubleOrNull

            val geometry = firstRoute?.get("geometry")
                ?.jsonObject
                ?.get("coordinates")
                ?.jsonArray
                ?.mapNotNull { elem ->
                    val arr = elem.jsonArray
                    val lon = arr.getOrNull(0)?.jsonPrimitive?.doubleOrNull
                    val lat = arr.getOrNull(1)?.jsonPrimitive?.doubleOrNull
                    if (lat != null && lon != null) LatLng(lat, lon) else null
                }
                .orEmpty()

            val legSummary = firstRoute?.get("legs")
                ?.jsonArray
                ?.mapNotNull { leg ->
                    leg.jsonObject["summary"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
                }
                ?.joinToString(" -> ")
                ?.takeIf { it.isNotBlank() }

            if (distMeters != null && durationSeconds != null) {
                val distKm = ((distMeters / 1000.0) * 10.0).roundToInt() / 10.0
                val durMin = (durationSeconds / 60.0).roundToInt().coerceAtLeast(1)
                RouteMetrics(
                    distanceKm = distKm,
                    durationMinutes = durMin,
                    mainHighwayName = legSummary ?: "BR-101 / AL-101",
                    destinationCoordinates = destCoords,
                    originCoordinates = originCoords,
                    routeGeometry = geometry.ifEmpty { buildFallbackRouteGeometry(originCoords, destCoords, stopCoords) },
                    intermediateStopAddress = stopAddress,
                    intermediateStopCoordinates = stopCoords
                )
            } else null
        }.getOrNull()

        if (osrmResult != null) return osrmResult

        // Fallback geodésico (Haversine * fator viário 1.22x + curva suavizada)
        val directKm = if (stopCoords != null) {
            GeoSpatialCalculator.distanceKm(originCoords, stopCoords) +
                GeoSpatialCalculator.distanceKm(stopCoords, destCoords)
        } else {
            GeoSpatialCalculator.distanceKm(originCoords, destCoords)
        }
        val roadKm = ((directKm * 1.22) * 10.0).roundToInt() / 10.0
        val etaMin = GeoSpatialCalculator.estimateEtaMinutes(roadKm, speedKmh = 75)
        return RouteMetrics(
            distanceKm = roadKm.coerceAtLeast(1.5),
            durationMinutes = etaMin.coerceAtLeast(3),
            mainHighwayName = if (stopCoords != null) "Rota com parada intermediária" else "BR-101 • Rota Principal",
            destinationCoordinates = destCoords,
            originCoordinates = originCoords,
            routeGeometry = buildFallbackRouteGeometry(originCoords, destCoords, stopCoords),
            intermediateStopAddress = stopAddress,
            intermediateStopCoordinates = stopCoords
        )
    }

    /**
     * Gera uma polilinha suavizada entre origem, parada opcional e destino como fallback offline.
     */
    fun buildFallbackRouteGeometry(
        origin: LatLng,
        dest: LatLng,
        stop: LatLng? = null,
        segments: Int = 16
    ): List<LatLng> {
        if (stop == null) return interpolateLeg(origin, dest, segments)
        val half = (segments / 2).coerceAtLeast(4)
        val firstLeg = interpolateLeg(origin, stop, half)
        val secondLeg = interpolateLeg(stop, dest, half).drop(1)
        return firstLeg + secondLeg
    }

    private fun interpolateLeg(start: LatLng, end: LatLng, segments: Int): List<LatLng> {
        val dLat = end.latitude - start.latitude
        val dLon = end.longitude - start.longitude
        return (0..segments).map { step ->
            val t = step.toDouble() / segments.toDouble()
            val bend = sin(t * PI) * 0.08
            LatLng(
                latitude = start.latitude + dLat * t - dLon * bend * 0.35,
                longitude = start.longitude + dLon * t + dLat * bend * 0.35
            )
        }
    }

    private fun resolveFromLocalOrCache(address: String): LatLng? {
        val clean = address.trim()
        val normalized = clean.lowercase()
        geocodedCache[normalized]?.let { return it }
        val presetMatch = presetPlaces.firstOrNull {
            clean.contains(it.title.substringBefore(","), ignoreCase = true) ||
                it.title.contains(clean, ignoreCase = true)
        }
        if (presetMatch != null) return presetMatch.coordinates
        return geocodedCache.entries.firstOrNull { (key, _) ->
            normalized.contains(key) || key.contains(normalized)
        }?.value
    }

    fun resolveCoordinates(address: String, default: LatLng): LatLng {
        return resolveFromLocalOrCache(address) ?: default
    }
}

