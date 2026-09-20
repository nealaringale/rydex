package com.rydex.app

import retrofit2.http.Body
import retrofit2.http.POST

interface RydexApi {
    @POST("api/plan")
    suspend fun planTrip(@Body request: PlanRequest): TripPlan
}
