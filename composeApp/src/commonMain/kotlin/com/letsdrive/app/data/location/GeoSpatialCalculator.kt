package com.letsdrive.app.data.location

import com.letsdrive.app.model.LatLng
import kotlin.math.*

/**
 * Motor de cálculo geoespacial multiplataforma (equivalente cliente ao PostGIS geography(Point, 4326)).
 * Calcula distâncias geodésicas reais em metros/quilômetros na superfície terrestre (WGS-84),
 * converte coordenadas para o formato EWKT do PostGIS e estima tempos de chegada (ETA).
 */
object GeoSpatialCalculator {

    private const val EARTH_RADIUS_KM = 6371.0088

    /**
     * Equivalente ao `ST_Distance(a::geography, b::geography) / 1000.0` do PostGIS.
     */
    fun distanceKm(a: LatLng, b: LatLng): Double {
        val dLat = toRadians(b.latitude - a.latitude)
        val dLon = toRadians(b.longitude - a.longitude)
        val lat1 = toRadians(a.latitude)
        val lat2 = toRadians(b.latitude)

        val sinDLat = sin(dLat / 2.0)
        val sinDLon = sin(dLon / 2.0)
        val h = sinDLat * sinDLat + cos(lat1) * cos(lat2) * sinDLon * sinDLon
        val c = 2.0 * atan2(sqrt(h.coerceIn(0.0, 1.0)), sqrt((1.0 - h).coerceIn(0.0, 1.0)))
        val rawKm = EARTH_RADIUS_KM * c
        return (rawKm * 10.0).roundToInt() / 10.0
    }

    /**
     * Equivalente ao `ST_Distance(a::geography, b::geography)` em metros no PostGIS.
     */
    fun distanceMeters(a: LatLng, b: LatLng): Double {
        return distanceKm(a, b) * 1000.0
    }

    /**
     * Equivalente ao `NOT ST_DWithin(a::geography, b::geography, threshold_meters)` do PostGIS.
     * Retorna true se a distância entre os dois veículos em movimento ultrapassou o limite configurado.
     */
    fun isSeparatedBeyondThreshold(a: LatLng, b: LatLng, thresholdKm: Double): Boolean {
        return distanceKm(a, b) > thresholdKm
    }

    /**
     * Formata uma coordenada para o padrão PostGIS EWKT: `SRID=4326;POINT(longitude latitude)`.
     */
    fun toPostGisEwkt(point: LatLng): String {
        return "SRID=4326;POINT(${point.longitude} ${point.latitude})"
    }

    /**
     * Faz o parse de uma coordenada retornada pelo PostGIS em qualquer formato comum do Supabase/PostgREST:
     * 1. WKT / EWKT: `SRID=4326;POINT(-35.7089 -9.6498)` ou `POINT(-35.7089 -9.6498)`
     * 2. GeoJSON: `{"type":"Point","coordinates":[-35.7089,-9.6498]}`
     * 3. EWKB Hex: string hexadecimal binária retornada por padrão em colunas geography/geometry
     */
    fun parsePostGisEwkt(wkt: String): LatLng? {
        val trimmed = wkt.trim()
        if (trimmed.isEmpty()) return null

        // 1. Formato EWKT / WKT
        if (trimmed.contains("POINT(", ignoreCase = true)) {
            val clean = trimmed.substringAfter("(", "").substringBefore(")", "").trim()
            val parts = clean.split(Regex("\\s+"))
            if (parts.size >= 2) {
                val lon = parts[0].toDoubleOrNull()
                val lat = parts[1].toDoubleOrNull()
                if (lat != null && lon != null && isValidCoordinate(lat, lon)) {
                    return LatLng(latitude = lat, longitude = lon)
                }
            }
        }

        // 2. Formato GeoJSON
        if (trimmed.startsWith("{") && trimmed.contains("coordinates", ignoreCase = true)) {
            val coordsRaw = trimmed
                .substringAfter("coordinates", "")
                .substringAfter("[", "")
                .substringBefore("]", "")
            val parts = coordsRaw.split(",")
            if (parts.size >= 2) {
                val lon = parts[0].trim().toDoubleOrNull()
                val lat = parts[1].trim().toDoubleOrNull()
                if (lat != null && lon != null && isValidCoordinate(lat, lon)) {
                    return LatLng(latitude = lat, longitude = lon)
                }
            }
        }

        // 3. Formato EWKB / WKB Hex (últimos 16 bytes = 32 hex chars para X=lon e Y=lat IEEE-754 float64)
        if (trimmed.length >= 42 && trimmed.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }) {
            val isLittleEndian = when {
                trimmed.startsWith("01") -> true
                trimmed.startsWith("00") -> false
                else -> return null
            }
            val xHex = trimmed.substring(trimmed.length - 32, trimmed.length - 16)
            val yHex = trimmed.substring(trimmed.length - 16)
            val lon = parseHexDouble(xHex, isLittleEndian) ?: return null
            val lat = parseHexDouble(yHex, isLittleEndian) ?: return null
            if (isValidCoordinate(lat, lon)) {
                return LatLng(latitude = lat, longitude = lon)
            }
        }

        return null
    }

    private fun isValidCoordinate(lat: Double, lon: Double): Boolean {
        return !lat.isNaN() && !lon.isNaN() && lat in -90.0..90.0 && lon in -180.0..180.0
    }

    private fun parseHexDouble(hex16: String, littleEndian: Boolean): Double? {
        if (hex16.length != 16) return null
        var bits = 0L
        for (byteIdx in 0 until 8) {
            val byteVal = hex16.substring(byteIdx * 2, byteIdx * 2 + 2).toIntOrNull(16) ?: return null
            bits = if (littleEndian) {
                bits or ((byteVal.toLong() and 0xFFL) shl (byteIdx * 8))
            } else {
                (bits shl 8) or (byteVal.toLong() and 0xFFL)
            }
        }
        return Double.fromBits(bits)
    }

    /**
     * Calcula um ponto de convergência (Ponto de Reencontro) na direção do destino final
     * entre o líder do comboio e o veículo que se afastou.
     */
    fun computeConvergenceMeetingPoint(
        leader: LatLng,
        separated: LatLng,
        destination: LatLng
    ): LatLng {
        val midLat = (leader.latitude + separated.latitude) / 2.0
        val midLon = (leader.longitude + separated.longitude) / 2.0
        // Avança 45% em direção ao destino a partir do ponto médio entre os dois veículos
        val convLat = midLat + (destination.latitude - midLat) * 0.45
        val convLon = midLon + (destination.longitude - midLon) * 0.45
        return LatLng(convLat, convLon)
    }

    /**
     * Estima o tempo de chegada (ETA em minutos) com base na distância geodésica e velocidade atual.
     */
    fun estimateEtaMinutes(distanceKm: Double, speedKmh: Int): Int {
        if (distanceKm <= 0.15) return 0
        val effectiveSpeed = speedKmh.coerceAtLeast(50)
        return ((distanceKm / effectiveSpeed.toDouble()) * 60.0).roundToInt().coerceAtLeast(1)
    }

    private fun toRadians(deg: Double): Double = deg * (PI / 180.0)
}

