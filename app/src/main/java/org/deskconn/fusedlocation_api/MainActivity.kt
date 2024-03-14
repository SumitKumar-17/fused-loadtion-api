package org.deskconn.fusedlocation_api

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*

class MainActivity : AppCompatActivity() {

    private lateinit var mServiceIntent: Intent
    private lateinit var mActivity: Activity

    private val LOCATION_PERMISSION_REQ_CODE = 1000
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var latitudeTextView: TextView
    private lateinit var longitudeTextView: TextView
    private lateinit var currentLocationButton: Button
    private lateinit var serviceButton: Button

    private lateinit var latitudeEditText: EditText
    private lateinit var longitudeEditText: EditText
    private lateinit var calculateDistanceButton: Button
    private lateinit var distanceTextView: TextView

    private var currentLatitude: Double = 0.0
    private var currentLongitude: Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mActivity = this

        latitudeTextView = findViewById(R.id.latitudeTextView)
        longitudeTextView = findViewById(R.id.longitudeTextView)
        currentLocationButton = findViewById(R.id.currentLocationButton)
        serviceButton = findViewById(R.id.serviceButton)

        latitudeEditText = findViewById(R.id.latitudeEditText)
        longitudeEditText = findViewById(R.id.longitudeEditText)
        calculateDistanceButton = findViewById(R.id.calculateDistanceButton)
        distanceTextView= findViewById(R.id.distanceTextView)

        currentLatitude = 37.7749 // Initialize with default value
        currentLongitude = -122.4194 // Initialize with default value

        currentLocationButton.setOnClickListener {
            if (PermissionUtils.isLocationEnabled(this)) {
                setLocationListener()
            } else {
                PermissionUtils.showGPSNotEnabledDialogue(this)
            }
        }

        serviceButton.setOnClickListener {
            mServiceIntent = Intent(this, LocationService::class.java)
            if (!PermissionUtils.isMyServiceRunning(LocationService::class.java, this)) {
                startService(mServiceIntent)
                Toast.makeText(
                    this,
                    getString(R.string.service_start_successfully),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(
                    this,
                    getString(R.string.service_already_running),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        calculateDistanceButton.setOnClickListener {
            val inputLatitude = latitudeEditText.text.toString().toDoubleOrNull()
            val inputLongitude = longitudeEditText.text.toString().toDoubleOrNull()

            if (inputLatitude != null && inputLongitude != null) {
                val results = FloatArray(1)
                Location.distanceBetween(
                    currentLatitude,
                    currentLongitude,
                    inputLatitude,
                    inputLongitude,
                    results
                )

                val distance = results[0] / 1000 // Convert to kilometers
                distanceTextView.text = "Distance from current location: $distance km"
            } else {
                distanceTextView.text = "Invalid input. Please enter valid latitude and longitude."
            }
        }

        when {
            PermissionUtils.checkAccessFineLocationGranted(this) -> {
                when {
                    PermissionUtils.isLocationEnabled(this) -> {
                        setLocationListener()
                    }
                    else -> {
                        PermissionUtils.showGPSNotEnabledDialogue(mActivity)
                    }
                }
            }
            else -> {
                PermissionUtils.askToAccessFineLocationPermission(
                    this,
                    LOCATION_PERMISSION_REQ_CODE
                )
            }
        }
    }

    private fun setLocationListener() {
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        val locationRequest = LocationRequest().setInterval(30000).setFastestInterval(15000)
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        fusedLocationProviderClient.requestLocationUpdates(
            locationRequest,
            object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    super.onLocationResult(locationResult)
                    for (location in locationResult.locations) {
                        latitudeTextView.text = "Latitude is: ${location.latitude}"
                        longitudeTextView.text = "Longitude is: ${location.longitude}"
                    }
                }
            },
            Looper.getMainLooper()
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            LOCATION_PERMISSION_REQ_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    when {
                        PermissionUtils.isLocationEnabled(this) -> {
                            // Setting things up
                            setLocationListener()
                        }
                        else -> {
                            PermissionUtils.showGPSNotEnabledDialogue(this)
                        }
                    }
                } else {
                    Toast.makeText(
                        this,
                        getString(R.string.common_google_play_services_enable_text),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    override fun onDestroy() {
        if (::mServiceIntent.isInitialized) {
            stopService(mServiceIntent)
        }
        super.onDestroy()
    }
}
