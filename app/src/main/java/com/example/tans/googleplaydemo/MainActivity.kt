package com.example.tans.googleplaydemo

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.places.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.messaging.FirebaseMessaging
import io.reactivex.*
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subscribers.DisposableSubscriber
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription
import java.util.function.Consumer
import kotlin.collections.ArrayList

class MainActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    private lateinit var mapView: MapView
    private lateinit var googleMap: GoogleMap
    private lateinit var client: GoogleApiClient
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var placesClient: PlaceDetectionClient
    private val placeLike = ArrayList<Place>()
    private var isPermissionAllow = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mapView = findViewById(R.id.map_view)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        placesClient = Places.getPlaceDetectionClient(this)
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
        googleMap.setOnMarkerClickListener(this)
        googleMap.setInfoWindowAdapter(MapInfoAdapter(placeLike, this))
        requestPermission()

        updateCurrentPosition()

    }

    override fun onMarkerClick(p0: Marker?): Boolean {
        if(p0 != null) {
            if (!p0.isInfoWindowShown) {
                p0.showInfoWindow()
            } else {
                p0.hideInfoWindow()
            }
        }
        return true
    }

//    private fun initGoogleClient() {
//        client = GoogleApiClient.Builder(this)
//                .addConnectionCallbacks(object: GoogleApiClient.ConnectionCallbacks {
//                    override fun onConnected(p0: Bundle?) {
//
//                    }
//
//                    override fun onConnectionSuspended(p0: Int) {
//
//                    }
//
//                })
//                .addApi(LocationServices.API)
//                .build()
//        client.connect()
//    }

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
        if (isPermissionAllow) {
            val lastLocation = fusedLocationProviderClient.lastLocation
            lastLocation.addOnCompleteListener { lastLocation ->
                if (lastLocation.isSuccessful) {
                    val result = lastLocation.result
                    val cameraUpdate = CameraUpdateFactory.newCameraPosition(
                            CameraPosition.builder()
                                    .zoom(12f)
                                    .target(LatLng(result.latitude, result.longitude))
                                    .build()
                    )


                    (application as AppApplication).sendFirebaseEvent(
                            eventType = "device_position",
                            event = "lat:${result.latitude},long:${result.longitude}"
                    )
                    (application as AppApplication).sendGoogleAnalyticsEvent(
                            eventType = "device_position",
                            event = "lat:${result.latitude},long:${result.longitude}")

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
                if (grantResults.size >= 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    isPermissionAllow = true
                    updateCurrentPosition()
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_activity_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when(item!!.itemId) {
            R.id.menu_get_places -> {
                requestCurrentPlace()
                true
            }
            R.id.menu_get_places_with_rx -> {
                requestCurrentPlaceDetection()
                true
            }
            R.id.menu_subscribe_news_message -> {
                changeNewsMessageSubscriptionState()
                true
            }
            else -> false
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestCurrentPlace() {
        placesClient.let {
            it.getCurrentPlace(null)
                    .addOnCompleteListener {
                        if (it.isSuccessful) {
                            placeLike.clear()
                            for( a in it.result) {
                                placeLike.add(a.let {
                                    Place(name = it.place.name.toString(),
                                            address = it.place.address.toString(),
                                            attribution = it.place.attributions.toString(),
                                            position =  it.place.latLng)
                                })
                            }
                            showLikePlacesDialog()
                        }
                        it.result.release()
                    }
        }
    }



    @SuppressLint("MissingPermission")
    private fun requestCurrentPlaceDetection() {
        Flowable.create<Place>({ emitter ->
            if (isPermissionAllow) {
                placesClient.let {
                    it.getCurrentPlace(null)
                            .addOnCompleteListener {
                                if (it.isSuccessful) {
                                    it.result.forEach {
                                        emitter.onNext(it.let {
                                            Place(name = it.place.name.toString(),
                                                    address = it.place.address.toString(),
                                                    attribution = it.place.attributions.toString(),
                                                    position = it.place.latLng)
                                        })
                                    }
                                    emitter.onComplete()
                                } else {
                                    emitter.onComplete()
                                }
                                it.result.release()
                            }
                }
            } else {
                emitter.onError(Throwable("Need location permission"))
            }
        }, BackpressureStrategy.ERROR)
                .compose(rxTransformerThread(128))
                .compose(rxTransformerLoadingDialog())
                .subscribe(object : DisposableSubscriber<Place>() {
                    override fun onComplete() {
                        showLikePlacesDialog()
                    }

                    override fun onStart() {
                        request(MAX_PLACES_SIZE.toLong())
                        placeLike.clear()
                    }

                    override fun onNext(t: Place) {
                        placeLike.add(t)
                        if(placeLike.size >= MAX_PLACES_SIZE) {
                            cancel()
                            showLikePlacesDialog()
                        }
                    }

                    override fun onError(t: Throwable?) {
                    }

                })


    }

    @SuppressLint("MissingPermission")
    private fun requestCurrentPlacesWithObservable() {
        Observable.create<Place> { emitter ->
            if (isPermissionAllow) {
                placesClient.let {
                    it.getCurrentPlace(null)
                            .addOnCompleteListener {
                                if (it.isSuccessful) {
                                    it.result.forEach {
                                        emitter.onNext(it.let {
                                            Place(name = it.place.name.toString(),
                                                    address = it.place.address.toString(),
                                                    attribution = it.place.attributions.toString(),
                                                    position = it.place.latLng)
                                        })
                                    }
                                    emitter.onComplete()
                                } else {
                                    emitter.onComplete()
                                }
                                it.result.release()
                            }
                }
            } else {
                emitter.onError(Throwable("Need location permission"))
            }
        }
                .compose(rxObservableTransformerThread())
                .compose(rxObservableTransformerLoading())
                .subscribe(object: Observer<Place> {
                    override fun onSubscribe(d: Disposable) {
                        placeLike.clear()
                    }

                    override fun onNext(t: Place) {
                        placeLike.add(t)
                    }

                    override fun onError(e: Throwable) {

                    }

                    override fun onComplete() {
                        showLikePlacesDialog()
                    }

                })
    }

    private fun <T> rxTransformerThread(bufferSize: Int = 128): FlowableTransformer<T, T> = FlowableTransformer {
        it.subscribeOn(Schedulers.io(), true)
                .observeOn(AndroidSchedulers.mainThread(), false, bufferSize)
    }

    private fun <T> rxObservableTransformerThread(): ObservableTransformer<T, T> = ObservableTransformer {
        it.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
    }

    private fun <T> rxObservableTransformerLoading(): ObservableTransformer<T, T> = ObservableTransformer {
        val loadingDialog = AlertDialog.Builder(this)
                .setView(R.layout.layout_loading_dialog)
                .setCancelable(false)
                .create()
        loadingDialog.window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        it.doOnSubscribe {
            loadingDialog.show()
        }
                .doOnComplete {
                    loadingDialog.dismiss()
                }
                .doOnError {
                    loadingDialog.dismiss()
                }
    }

    private fun <T> rxTransformerLoadingDialog(): FlowableTransformer<T, T> = FlowableTransformer {
        val loadingDialog = AlertDialog.Builder(this)
                .setView(R.layout.layout_loading_dialog)
                .setCancelable(false)
                .create()
        loadingDialog.window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        it.doOnSubscribe {
            loadingDialog.show()
        }
                .doOnComplete {
                    loadingDialog.dismiss()
                }
                .doOnError {
                    loadingDialog.dismiss()
                }
                .doOnCancel {
                    loadingDialog.dismiss()
                }
    }

    private fun showLikePlacesDialog() {

        AlertDialog.Builder(this)
                .setTitle("Like Places")
                .setItems(placeLike.map {
                    it.name
                }.toTypedArray()) { dialog, which ->
                    dialog.dismiss()
                }.show()
    }
    private fun flowableTest() {
        Flowable.create<Int>(FlowableOnSubscribe<Int> {
            for(i in 1 .. 10) {
                it.onNext(i)
//                Thread.sleep(1000)
            }
            it.onComplete()
        }, BackpressureStrategy.ERROR)
//        Flowable.fromIterable(arrayListOf(1,2,3,4,5,6))
                .compose(rxTransformerThread())
                .subscribe(object: DisposableSubscriber<Int>() {
                    override fun onComplete() {
                        Log.i(MainActivity::class.java.simpleName, "completed-------------------------------------")
                    }

                    override fun onStart() {
                        request(5)
                        Log.i(MainActivity::class.java.simpleName, "subscribed-------------------------------------")
                    }

                    override fun onNext(t: Int?) {
                        Log.i(MainActivity::class.java.simpleName, "onNext: ${t.toString()}")
                        cancel()
                    }

                    override fun onError(t: Throwable?) {
                        Log.i(MainActivity::class.java.simpleName, "!!!!!!!!!!!!!! onError: ${t.toString()}")
                    }

                })
    }

    private fun addMark(place: Place) {
        googleMap.addMarker(MarkerOptions().let {
            it.title(place.name)
            it.position(place.position)
            it
        })
    }
    private fun changeNewsMessageSubscriptionState() {
        FirebaseMessaging.getInstance().subscribeToTopic("news").addOnCompleteListener {
            if(it.isSuccessful) {
                Log.i(this::class.java.simpleName, "add topic news success")
            }
        }
    }

    companion object {
        const val MAX_PLACES_SIZE = 5
        data class Place (val name: String = "",
                          val address: String = "",
                          val attribution: String = "",
                          val position: LatLng)

        class MapInfoAdapter(val data: List<Place>, context: Context) : GoogleMap.InfoWindowAdapter {

            private var itemView: View = LayoutInflater.from(context).inflate(R.layout.map_info_window_layout, null, false)

            override fun getInfoContents(p0: Marker?): View? = null

            override fun getInfoWindow(p0: Marker?): View {
                val itemData = data.find { p0!!.title == it.name }
                itemView.findViewById<TextView>(R.id.info_window_attributes).text = itemData?.attribution ?: ""
                itemView.findViewById<TextView>(R.id.info_window_address).text = itemData?.address ?: ""
                return itemView
            }

        }
    }

}


