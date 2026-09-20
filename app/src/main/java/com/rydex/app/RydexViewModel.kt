package com.rydex.app

import android.app.Application
import android.location.Location
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

class RydexViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = RydexRepository()
    private val locationManager = RydexLocationManager(app)
    private var locationJob: Job? = null

    var destination by mutableStateOf("")
    var locationLabel by mutableStateOf("Waiting for GPS…")
    var currentLocation by mutableStateOf<LatLngDto?>(null)
    var plan by mutableStateOf<TripPlan?>(null)
    var planning by mutableStateOf(false)
    var needsFuel by mutableStateOf(false)
    var fuelPriority by mutableStateOf("HIGH")
    var needsFood by mutableStateOf(false)
    var foodTime by mutableStateOf("13:30")
    var needsWeather by mutableStateOf(true)
    var currentSpeedKmh by mutableStateOf("0 km/h")
    var nextInstruction by mutableStateOf<String?>(null)
    var hasLocationPermission by mutableStateOf(false)
        private set

    private var stepIndex = 0

    fun backendUrl() = repo.backendUrl()

    fun startLocationUpdates() {
        locationJob?.cancel()
        locationJob = viewModelScope.launch {
            locationManager.updates().catch { }.collect { loc ->
                hasLocationPermission = true
                updateLocation(loc)
            }
        }
    }

    private fun updateLocation(loc: Location) {
        currentLocation = LatLngDto(loc.latitude, loc.longitude)
        locationLabel = "${"%.5f".format(loc.latitude)}, ${"%.5f".format(loc.longitude)}"
        currentSpeedKmh = "${(loc.speed * 3.6f).roundToInt()} km/h"
        updateInstruction(loc)
    }

    fun planTrip(onDone: () -> Unit) {
        val origin = currentLocation ?: return
        planning = true
        viewModelScope.launch {
            val request = PlanRequest(
                origin = origin,
                destinationQuery = destination,
                departureTime = ZonedDateTime.now(ZoneId.systemDefault())
                    .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                preferences = TripPreferences(
                    needsFuel,
                    fuelPriority,
                    needsFood,
                    foodTime.ifBlank { null },
                    needsWeather,
                ),
            )

            runCatching { repo.planTrip(request) }
                .onSuccess {
                    plan = it
                    onDone()
                }
                .onFailure {
                    plan = DemoPlanFactory.create(origin, destination, request.preferences)
                    onDone()
                }

            planning = false
        }
    }

    private fun updateInstruction(loc: Location) {
        val steps = plan?.steps.orEmpty()
        if (steps.isEmpty()) return

        if (stepIndex >= steps.size) stepIndex = steps.lastIndex

        val step = steps[stepIndex]
        val d = distanceMeters(loc.latitude, loc.longitude, step.lat, step.lng)

        if (d < 120 && stepIndex < steps.lastIndex) {
            stepIndex++
        }

        nextInstruction =
            if (stepIndex < steps.size) {
                steps[stepIndex].instruction
            } else {
                "Continue to ${plan?.destinationName ?: "destination"}"
            }
    }

    fun nextFuel() = plan?.stops?.firstOrNull { it.type == "fuel" }

    fun nextFood() = plan?.stops?.firstOrNull { it.type == "food" }

    fun currentWeatherRecommendation() =
        plan?.weather?.firstOrNull()?.recommendation

    private fun distanceMeters(
        aLat: Double,
        aLng: Double,
        bLat: Double,
        bLng: Double,
    ): Double {
        val result = FloatArray(1)
        Location.distanceBetween(aLat, aLng, bLat, bLng, result)
        return result[0].toDouble()
    }
}
