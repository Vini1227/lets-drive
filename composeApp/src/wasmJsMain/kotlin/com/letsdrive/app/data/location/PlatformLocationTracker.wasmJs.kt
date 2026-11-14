package com.letsdrive.app.data.location

import com.letsdrive.app.model.LatLng
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

actual class PlatformLocationTracker actual constructor() {

    actual fun observeLocationUpdates(intervalMs: Long): Flow<LocationSample> = flow {
        var step = 0
        while (true) {
            delay(intervalMs)
            step++
            val progress = (step % 40) / 40.0
            val lat = -9.6498 + (-9.5120 - (-9.6498)) * progress * 0.35
            val lon = -35.7089 + (-35.6810 - (-35.7089)) * progress * 0.35
            val simulatedSpeed = 84 + ((step % 5) - 2) * 2
            emit(
                LocationSample(
                    coordinates = LatLng(lat, lon),
                    speedKmh = simulatedSpeed,
                    accuracyMeters = 5.0f,
                    isHardwareGps = false
                )
            )
        }
    }
}

