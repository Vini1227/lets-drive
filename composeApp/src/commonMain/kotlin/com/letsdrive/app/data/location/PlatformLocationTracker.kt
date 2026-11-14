package com.letsdrive.app.data.location

import com.letsdrive.app.model.LatLng
import kotlinx.coroutines.flow.Flow

/**
 * Amostra de telemetria GPS emitida pelo rastreador nativo de cada plataforma.
 */
data class LocationSample(
    val coordinates: LatLng,
    val speedKmh: Int,
    val accuracyMeters: Float = 5.0f,
    val isHardwareGps: Boolean = false
)

/**
 * Rastreador de localização multiplataforma:
 * - Android (`androidMain`): `LocationManager` nativo (`GPS_PROVIDER` / `NETWORK_PROVIDER`).
 * - iOS (`iosMain`): `CLLocationManager` nativo do `CoreLocation`.
 * - Web (`wasmJsMain`): Telemetria contínua de rota no navegador.
 */
expect class PlatformLocationTracker() {
    fun observeLocationUpdates(intervalMs: Long = 2500L): Flow<LocationSample>
}

