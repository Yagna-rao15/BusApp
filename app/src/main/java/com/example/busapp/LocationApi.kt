package com.example.busapp

import retrofit2.http.Body
import retrofit2.http.POST

interface LocationApi {
    @POST("update-location")
    suspend fun sendLocation(@Body locationData: LocationData)
}