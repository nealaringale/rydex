package com.rydex.app

import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.math.round

class RydexRepository {
    private val json = Json { ignoreUnknownKeys = true }

    private val api: RydexApi by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.BACKEND_URL.ensureTrailingSlash())
            .client(OkHttpClient.Builder().build())
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(RydexApi::class.java)
    }

    suspend fun planTrip(request: PlanRequest): TripPlan = api.planTrip(request)

    fun backendUrl(): String = BuildConfig.BACKEND_URL

    private fun String.ensureTrailingSlash() = if (endsWith('/')) this else "$this/"
}

object DemoPlanFactory {
    fun create(origin: LatLngDto, destination: String, prefs: TripPreferences): TripPlan {
        val route = listOf(
            LatLngDto(origin.lat, origin.lng),
            LatLngDto(origin.lat + 0.18, origin.lng + 0.04),
            LatLngDto(origin.lat + 0.35, origin.lng + 0.08),
            LatLngDto(origin.lat + 0.50, origin.lng + 0.12),
        )

        val points = PolylineEncoder.encode(route)
        val stops = buildList {
            if (prefs.needsFuel) {
                add(
                    Stop(
                        "fuel",
                        "RYDEX Demo Fuel Stop",
                        "Demo mode — connect backend",
                        route[1].lat,
                        route[1].lng,
                        4.2,
                        400.0,
                        "Fuel priority: ${prefs.fuelPriority}",
                    ),
                )
            }

            if (prefs.needsFood) {
                add(
                    Stop(
                        "food",
                        "RYDEX Demo Food Stop",
                        "Demo mode — connect backend",
                        route[2].lat,
                        route[2].lng,
                        4.4,
                        700.0,
                        "Food time: ${prefs.foodTime ?: "flexible"}",
                    ),
                )
            }
        }

        val weather = if (prefs.needsWeather) {
            val now = ZonedDateTime.now(ZoneId.systemDefault())
            listOf(
                WeatherCheckpoint(
                    30.0,
                    now.plusMinutes(40).toString(),
                    61,
                    55,
                    1.6,
                    "Rain possible — keep raincoat accessible.",
                ),
                WeatherCheckpoint(
                    60.0,
                    now.plusMinutes(85).toString(),
                    1,
                    5,
                    0.0,
                    "No significant rain expected.",
                ),
            )
        } else {
            emptyList()
        }

        return TripPlan(
            destinationName = destination,
            distanceMeters = 92_000,
            durationSeconds = 7_200,
            encodedPolyline = points,
            stops = stops,
            weather = weather,
        )
    }
}

private object PolylineEncoder {
    fun encode(points: List<LatLngDto>): String {
        var prevLat = 0
        var prevLng = 0
        val result = StringBuilder()

        for (point in points) {
            val lat = round(point.lat * 1e5).toInt()
            val lng = round(point.lng * 1e5).toInt()
            encodeValue(lat - prevLat, result)
            encodeValue(lng - prevLng, result)
            prevLat = lat
            prevLng = lng
        }

        return result.toString()
    }

    private fun encodeValue(delta: Int, out: StringBuilder) {
        var value = delta shl 1
        if (delta < 0) value = value.inv()

        while (value >= 0x20) {
            out.append(((0x20 or (value and 0x1f)) + 63).toChar())
            value = value shr 5
        }

        out.append((value + 63).toChar())
    }
}
