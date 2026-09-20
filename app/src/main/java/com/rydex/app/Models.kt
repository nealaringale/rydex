package com.rydex.app

import kotlinx.serialization.Serializable

@Serializable
data class LatLngDto(val lat: Double, val lng: Double)

@Serializable
data class TripPreferences(
    val needsFuel: Boolean,
    val fuelPriority: String,
    val needsFood: Boolean,
    val foodTime: String?,
    val needsWeather: Boolean,
)

@Serializable
data class PlanRequest(
    val origin: LatLngDto,
    val destinationQuery: String,
    val departureTime: String,
    val preferences: TripPreferences,
)

@Serializable
data class Stop(
    val type: String,
    val name: String,
    val address: String? = null,
    val lat: Double,
    val lng: Double,
    val rating: Double? = null,
    val distanceFromRouteMeters: Double? = null,
    val reason: String? = null,
)

@Serializable
data class WeatherCheckpoint(
    val kmFromStart: Double,
    val arrivalTime: String,
    val weatherCode: Int? = null,
    val precipitationProbability: Int? = null,
    val rainMm: Double? = null,
    val recommendation: String,
)

@Serializable
data class NavigationStep(
    val instruction: String,
    val maneuver: String? = null,
    val lat: Double,
    val lng: Double,
)

@Serializable
data class TripPlan(
    val destinationName: String,
    val destinationAddress: String? = null,
    val distanceMeters: Int,
    val durationSeconds: Int,
    val encodedPolyline: String,
    val stops: List<Stop> = emptyList(),
    val weather: List<WeatherCheckpoint> = emptyList(),
    val steps: List<NavigationStep> = emptyList(),
)
