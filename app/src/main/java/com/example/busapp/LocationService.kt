package com.example.busapp

import android.app.*
import android.content.Intent
import android.os.Build
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import kotlinx.coroutines.*
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class LocationService : Service() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private val api: LocationApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://node-server-bus.vercel.app/") // Replace with your server URL
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(LocationApi::class.java)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate() {
        super.onCreate()
        Log.d("LocationService", "Service created")

        val channelId = "location_service"
        val channel = NotificationChannel(
            channelId,
            "Location Service",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Location Service")
            .setContentText("Tracking location...")
            .setSmallIcon(R.drawable.baseline_location_on_24)
            .build()

        startForeground(1, notification)
        Log.d("LocationService", "Foreground notification started")

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    Log.d("LocationService", "Location received: ${location.latitude}, ${location.longitude}")
                    serviceScope.launch {
                        try {
                            api.sendLocation(
                                LocationData(
                                    latitude = location.latitude,
                                    longitude = location.longitude,
                                )
                            )
                            Log.d("LocationService", "Location sent to server: ${location.latitude}, ${location.longitude}")
                        } catch (e: Exception) {
                            Log.e("LocationService", "Error sending location to server", e)
                        }
                    }
                }
            }
        }

        startLocationUpdates()
    }

    private fun startLocationUpdates() {
        Log.d("LocationService", "Starting location updates")
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            interval = 5000 // 5 seconds
        }

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            Log.e("LocationService", "Permission denied", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
        serviceScope.cancel()
        Log.d("LocationService", "Service destroyed, location updates stopped")
    }

    override fun onBind(intent: Intent?) = null
}
