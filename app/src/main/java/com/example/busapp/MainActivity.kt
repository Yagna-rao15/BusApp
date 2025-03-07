package com.example.busapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import android.provider.Settings
import androidx.appcompat.app.AlertDialog

class MainActivity : AppCompatActivity() {
    private var isLocationServiceRunning by mutableStateOf(false)

    private lateinit var requestPermissionsLauncher: androidx.activity.result.ActivityResultLauncher<Array<String>>

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("MainActivity", "onCreate called")

        // Initialize permission launcher inside onCreate
        requestPermissionsLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val allGranted = permissions.all { it.value }
            if (allGranted) {
                Log.d("MainActivity", "All permissions granted")
                startLocationService()
                isLocationServiceRunning = true
            } else {
                Log.e("MainActivity", "Permissions denied")
                showPermissionDeniedDialog()
            }
        }

        setContent {
            var serviceRunning by remember { mutableStateOf(isLocationServiceRunning) }

            // Observe changes
            LaunchedEffect(isLocationServiceRunning) {
                serviceRunning = isLocationServiceRunning
            }

            MainScreen(
                isServiceRunning = serviceRunning,
                onStartStopClick = { toggleLocationService() }
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun toggleLocationService() {
        Log.d("MainActivity", "Toggle button clicked. Service running: $isLocationServiceRunning")

        if (!isLocationServiceRunning) {
            if (checkPermissions()) {
                startLocationService()
                isLocationServiceRunning = true
                Log.d("MainActivity", "Location service started")
            }
        } else {
            stopLocationService()
            isLocationServiceRunning = false
            Log.d("MainActivity", "Location service stopped")
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun checkPermissions(): Boolean {
        Log.d("MainActivity", "Checking permissions")
        val fineLocationGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val backgroundLocationGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        } else true

        return if (!fineLocationGranted || !backgroundLocationGranted) {
            val permissions = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                permissions.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            }
            Log.d("MainActivity", "Requesting permissions")
            requestPermissionsLauncher.launch(permissions.toTypedArray())
            false
        } else {
            Log.d("MainActivity", "Permissions already granted")
            true
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun startLocationService() {
        val intent = Intent(this, LocationService::class.java)
        startForegroundService(intent)
        Log.d("MainActivity", "Foreground service started")
    }

    private fun stopLocationService() {
        val intent = Intent(this, LocationService::class.java)
        stopService(intent)
        Log.d("MainActivity", "Service stopped")
    }

    private fun showPermissionDeniedDialog() {
        AlertDialog.Builder(this)
            .setTitle("Permissions Required")
            .setMessage("This app needs location permissions to function properly. Please grant the necessary permissions in settings.")
            .setPositiveButton("Go to Settings") { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = android.net.Uri.fromParts("package", packageName, null)
                startActivity(intent)
            }
            .setNegativeButton("Exit") { _, _ ->
                finishAffinity()
            }
            .setCancelable(false)
            .show()
    }

    @Composable
    fun MainScreen(
        isServiceRunning: Boolean,
        onStartStopClick: () -> Unit
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = if (isServiceRunning) "Service is Running" else "Service is Stopped")
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { onStartStopClick() }) {
                Text(text = if (isServiceRunning) "Stop Service" else "Start Service")
            }
        }
    }
}
