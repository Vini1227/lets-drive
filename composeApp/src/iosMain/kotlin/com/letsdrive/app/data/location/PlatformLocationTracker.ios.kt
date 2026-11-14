package com.letsdrive.app.data.location

import com.letsdrive.app.model.LatLng
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLLocationAccuracyBestForNavigation
import platform.darwin.NSObject
import kotlin.math.roundToInt

@OptIn(ExperimentalForeignApi::class)
actual class PlatformLocationTracker actual constructor() {

    actual fun observeLocationUpdates(intervalMs: Long): Flow<LocationSample> = callbackFlow {
        val manager = CLLocationManager()
        manager.desiredAccuracy = kCLLocationAccuracyBestForNavigation
        manager.distanceFilter = 2.0

        var receivedRealGps = false

        val delegate = object : NSObject(), CLLocationManagerDelegateProtocol {
            override fun locationManager(manager: CLLocationManager, didUpdateLocations: List<*>) {
                val lastLoc = didUpdateLocations.lastOrNull() as? CLLocation ?: return
                receivedRealGps = true
                val coords = lastLoc.coordinate.useContents {
                    LatLng(latitude = latitude, longitude = longitude)
                }
                val rawSpeedMps = lastLoc.speed
                val speedKmh = if (rawSpeedMps >= 0.0) {
                    (rawSpeedMps * 3.6).roundToInt()
                } else 75

                trySend(
                    LocationSample(
                        coordinates = coords,
                        speedKmh = speedKmh,
                        accuracyMeters = lastLoc.horizontalAccuracy.toFloat().coerceAtLeast(3f),
                        isHardwareGps = true
                    )
                )
            }
        }

        manager.delegate = delegate
        manager.requestWhenInUseAuthorization()
        manager.startUpdatingLocation()

        val fallbackJob = launch {
            var step = 0
            while (isActive) {
                delay(intervalMs)
                if (!receivedRealGps) {
                    step++
                    val progress = (step % 40) / 40.0
                    val lat = -9.6498 + (-9.5120 - (-9.6498)) * progress * 0.35
                    val lon = -35.7089 + (-35.6810 - (-35.7089)) * progress * 0.35
                    val simulatedSpeed = 84 + ((step % 5) - 2) * 2
                    trySend(
                        LocationSample(
                            coordinates = LatLng(lat, lon),
                            speedKmh = simulatedSpeed,
                            accuracyMeters = 6.0f,
                            isHardwareGps = false
                        )
                    )
                }
            }
        }

        awaitClose {
            fallbackJob.cancel()
            manager.stopUpdatingLocation()
            manager.delegate = null
        }
    }
}

