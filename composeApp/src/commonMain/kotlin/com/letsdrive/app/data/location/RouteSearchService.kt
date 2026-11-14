package com.letsdrive.app.data.location

import com.letsdrive.app.model.LatLng
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.serialization.json.*
import kotlin.math.roundToInt

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
    val destinationCoordinates: LatLng
)

/**
 * Serviço de Geocodificação (OpenStreetMap Nominatim) e Roteamento Viário (OSRM API)
 * com catálogo rápido de destinos e fallback geodésico offline.
 */
class RouteSearchService {

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
                PlaceSuggestion(
                    title = title,
                    subtitle = subtitle,
                    fullAddress = title,
                    coordinates = LatLng(lat, lon)
                )
            }
        }.getOrDefault(emptyList())

        return (localMatches + remoteMatches).distinctBy { it.title }.take(5)
    }

    /**
     * Resolve coordenadas de um endereço digitado e calcula a rota real de carro via OSRM API.
     */
    suspend fun calculateRoute(originText: String, destinationText: String): RouteMetrics {
        val originCoords = resolveCoordinates(originText, default = LatLng(-9.6498, -35.7089))
        val destCoords = resolveCoordinates(destinationText, default = LatLng(-9.5120, -35.6810))

        // Consulta a API pública do OSRM (Open Source Routing Machine) para distância e tempo reais de condução
        val osrmResult = runCatching {
            val url = "https://router.project-osrm.org/route/v1/driving/" +
                "${originCoords.longitude},${originCoords.latitude};${destCoords.longitude},${destCoords.latitude}" +
                "?overview=false"
            val body = httpClient.get(url).bodyAsText()
            val root = json.parseToJsonElement(body).jsonObject
            val routes = root["routes"]?.jsonArray
            val firstRoute = routes?.firstOrNull()?.jsonObject
            val distMeters = firstRoute?.get("distance")?.jsonPrimitive?.doubleOrNull
            val durationSeconds = firstRoute?.get("duration")?.jsonPrimitive?.doubleOrNull

            if (distMeters != null && durationSeconds != null) {
                val distKm = ((distMeters / 1000.0) * 10.0).roundToInt() / 10.0
                val durMin = (durationSeconds / 60.0).roundToInt().coerceAtLeast(1)
                RouteMetrics(
                    distanceKm = distKm,
                    durationMinutes = durMin,
                    mainHighwayName = "BR-101 / AL-101",
                    destinationCoordinates = destCoords
                )
            } else null
        }.getOrNull()

        if (osrmResult != null) return osrmResult

        // Fallback geodésico (Haversine * fator viário 1.22x)
        val directKm = GeoSpatialCalculator.distanceKm(originCoords, destCoords)
        val roadKm = ((directKm * 1.22) * 10.0).roundToInt() / 10.0
        val etaMin = GeoSpatialCalculator.estimateEtaMinutes(roadKm, speedKmh = 75)
        return RouteMetrics(
            distanceKm = roadKm.coerceAtLeast(1.5),
            durationMinutes = etaMin.coerceAtLeast(3),
            mainHighwayName = "BR-101 • Rota Principal",
            destinationCoordinates = destCoords
        )
    }

    fun resolveCoordinates(address: String, default: LatLng): LatLng {
        val match = presetPlaces.firstOrNull {
            address.contains(it.title.substringBefore(","), ignoreCase = true) ||
                it.title.contains(address.trim(), ignoreCase = true)
        }
        return match?.coordinates ?: default
    }
}

