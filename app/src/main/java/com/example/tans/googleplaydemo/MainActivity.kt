package com.example.tans.googleplaydemo

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.util.AttributeSet
import android.view.View
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.analytics.FirebaseAnalytics
import java.security.Permission

class MainActivity : AppCompatActivity(), OnMapReadyCallback{

    private lateinit var mapView: MapView
    private lateinit var googleMap: GoogleMap
    private lateinit var client: GoogleApiClient
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private var isPermissionAllow = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mapView = findViewById(R.id.map_view)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
    }

    override fun onMapReady(p0: GoogleMap?) {
        googleMap = p0!!

        requestPermission()

        updateCurrentPosition()

    }

    private fun initGoogleClient() {
        client = GoogleApiClient.Builder(this)
                .addConnectionCallbacks(object: GoogleApiClient.ConnectionCallbacks {
                    override fun onConnected(p0: Bundle?) {

                    }

                    override fun onConnectionSuspended(p0: Int) {

                    }

                })
                .addApi(LocationServices.API)
                .build()
        client.connect()
    }

    private fun requestPermission() {
        isPermissionAllow = if (ContextCompat.checkSelfPermission(applicationContext, android.Manifest.permission.ACCESS_FINE_LOCATION)
        == PackageManager.PERMISSION_GRANTED) {
            true
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), 0)
            false
        }
    }

    @SuppressLint("MissingPermission")
    private fun updateCurrentPosition() {
        if(isPermissionAllow) {
            val lastLocation = fusedLocationProviderClient.lastLocation
            lastLocation.addOnCompleteListener { lastLocation ->
                if(lastLocation.isSuccessful) {
                    val result = lastLocation.result
                    val cameraUpdate = CameraUpdateFactory.newCameraPosition(
                            CameraPosition.builder()
                                    .zoom(12f)
                                    .target(LatLng(result.latitude, result.longitude))
                                    .build()
                    )
                    val eventParams = Bundle()
                    eventParams.putString("user_location", "lat:${result.latitude},long:${result.longitude}")
                    val firebaseAnalytics = (application as AppApplication).getFireBaseAnalytics()
                    firebaseAnalytics.logEvent("location", eventParams)
                    googleMap.uiSettings.isMyLocationButtonEnabled = true
                    googleMap.isMyLocationEnabled = true
                    googleMap.animateCamera(cameraUpdate)
                }
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        isPermissionAllow = false
        when (requestCode) {
            0 -> {
                if(grantResults.size >= 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    isPermissionAllow = true
                    updateCurrentPosition()
                }
            }
        }
    }
}
