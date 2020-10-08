package com.waterloo.wit.ui

import android.content.DialogInterface
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.annotation.NonNull
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.OnMapClickListener
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.waterloo.wit.R
import com.waterloo.wit.helpers.WitHelper
import com.waterloo.wit.utils.GetPoiCategoryIconUseCase
import com.waterloo.wit.utils.GetPoisUseCase
import com.waterloo.wit.utils.PositionAnimator
import es.situm.sdk.SitumSdk
import es.situm.sdk.directions.DirectionsRequest
import es.situm.sdk.error.Error
import es.situm.sdk.location.*
import es.situm.sdk.location.util.CoordinateConverter
import es.situm.sdk.model.cartography.Building
import es.situm.sdk.model.cartography.Floor
import es.situm.sdk.model.cartography.Poi
import es.situm.sdk.model.cartography.Point
import es.situm.sdk.model.directions.Route
import es.situm.sdk.model.location.Angle
import es.situm.sdk.model.location.Coordinate
import es.situm.sdk.model.location.Location
import es.situm.sdk.model.navigation.NavigationProgress
import es.situm.sdk.navigation.NavigationListener
import es.situm.sdk.navigation.NavigationRequest
import es.situm.sdk.utils.Handler


class NavigationActivity2 : AppCompatActivity(), OnMapReadyCallback {
    private val TAG = NavigationActivity2::class.java.simpleName
    private val UPDATE_LOCATION_ANIMATION_TIME = 600
    private val MIN_CHANGE_IN_BEARING_TO_ANIMATE_CAMERA = 10

    private var mapFragment: SupportMapFragment? = null
    private lateinit var mMap: GoogleMap
    private lateinit var building: Building

    var markerCurrent: Marker? = null
    var markerDestination: Marker? = null
    var markerPoiList: ArrayList<Marker> = ArrayList()
    var poisList: ArrayList<Poi> = ArrayList()
    private var locationManager: LocationManager? = null
    private var locationListener: LocationListener? = null

    private val getPoisUseCase = GetPoisUseCase()
    private val getPoiCategoryIconUseCase = GetPoiCategoryIconUseCase()

    private var navigationRequest: NavigationRequest? = null
    
    private lateinit var progressBar: ProgressBar
    private lateinit var navigation_layout: LinearLayout
    private lateinit var tv_indication: TextView
    private lateinit var tv_indication_bottom: TextView

    private var navigation = false

    private var srcLocation: Location? = null
    private var destination_lati: Double? = null
    private var destination_longi: Double? = null

    private val polylines: ArrayList<Polyline> = ArrayList()
    private var coordinateConverter: CoordinateConverter? = null
    private var locationParametersUpdate: LocationParametersUpdate? = null
    private var routeId = 0

    private var floorList: ArrayList<Floor> = ArrayList()

    private var currentRoute: Route? = null
    private var currentFloorIdentifier: String? = null
    private var destFloorIdentifier: String? = null
    private var pointSrc: Point? = null
    private var pointDest: Point? = null
    private var isRouteSetup: Boolean = false

    private var isFirstLoad: Boolean = false

    private var markerWithOrientation = false
    private var lastCameraLatLng: LatLng? = null
    private var lastCameraBearing = 0f
    private var groundOverlay: GroundOverlay? = null
    private var positionAnimator: PositionAnimator = PositionAnimator()

    private var buildingOverlay: GroundOverlay? = null
    private lateinit var radio_group: RadioGroup
    private lateinit var radio_button4: RadioButton
    private lateinit var radio_button3: RadioButton
    private lateinit var radio_button2: RadioButton
    private lateinit var radio_button1: RadioButton

    private lateinit var closeImageView: ImageView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_navigation2)

        progressBar = findViewById(R.id.progressBar)
        navigation_layout = findViewById(R.id.navigation_layout)

        closeImageView = findViewById(R.id.closeImageView)

        radio_group = findViewById(R.id.radio_group)
        radio_button1 = findViewById(R.id.radio_button1)
        radio_button2 = findViewById(R.id.radio_button2)
        radio_button3 = findViewById(R.id.radio_button3)
        radio_button4 = findViewById(R.id.radio_button4)

        closeImageView.setOnClickListener{
            onBackPressed()
        }
        tv_indication = findViewById(R.id.tv_indication)
        tv_indication_bottom = findViewById(R.id.tv_indication_bottom)
        
        building = intent.getParcelableExtra(EXTRA_BUILDING)
        destination_lati = intent.getDoubleExtra(EXTRA_DESTINATION_LATI, 0.0)
        destination_longi = intent.getDoubleExtra(EXTRA_DESTINATION_LONGI, 0.0)
        destFloorIdentifier = intent.getStringExtra(EXTRA_DESTINATION_FLOOR_ID)

        radio_group.setOnCheckedChangeListener { group, checkedId ->
            var destFloor = floorList.get(0)
            if(checkedId == R.id.radio_button1){
                destFloor = floorList.get(0)
            }
            else if(checkedId == R.id.radio_button2){
                destFloor = floorList.get(1)
            }
            else if(checkedId == R.id.radio_button3){
                destFloor = floorList.get(2)
            }
            else if(checkedId == R.id.radio_button4){
                destFloor = floorList.get(3)
            }
            currentFloorIdentifier = destFloor.identifier
            SitumSdk.communicationManager().fetchMapFromFloor(
                destFloor!!,
                object : Handler<Bitmap> {
                    override fun onSuccess(bitmap: Bitmap) {
                        onMapLoaded(bitmap)
                        updateObjects()
                    }
                    override fun onFailure(error: Error) {

                    }
                })
        }
        mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment?.getMapAsync(this)

        navigation_layout.visibility = View.GONE
        tv_indication_bottom.text = building.name
        locationManager = SitumSdk.locationManager()

        progressBar.visibility = View.VISIBLE

    }

    override fun onDestroy() {
        super.onDestroy()
        stopLocation()
    }
    fun updateObjects(){
        if (markerDestination != null) {
            markerDestination!!.remove()
        }
        if(destFloorIdentifier == currentFloorIdentifier) {
            val destLatLng = LatLng(destination_lati!!, destination_longi!!)
            markerDestination = mMap.addMarker(
                MarkerOptions().position(destLatLng).title("Work Item")
            )
        }
        updatePois()
        if(currentRoute != null) {
            removePolylines()
            drawRoute(currentRoute!!)
        }
        if(srcLocation != null && srcLocation?.floorIdentifier != currentFloorIdentifier){
            markerCurrent?.remove()
            markerCurrent = null
        }

    }
    fun updatePois(){
        for(poiItem in markerPoiList){
            poiItem.remove()
        }
        markerPoiList.clear()
        for (poi in poisList) {
            getPoiCategoryIconUseCase.getUnselectedIcon(poi, object : GetPoiCategoryIconUseCase.Callback {
                override fun onSuccess(bitmap: Bitmap?) {
                    if(poi.floorIdentifier == currentFloorIdentifier) {
                        val marker = drawPoi(poi, bitmap)
                        markerPoiList.add(marker)
                    }
                }
                override fun onError(error: String?) {
                    Log.d("Error fetching poi icon", error)
                }
            })
        }
    }
    private fun initializeMarker(latLng: LatLng) {
        val bitmapArrow = BitmapFactory.decodeResource(resources, R.drawable.position)
        val arrowScaled = Bitmap.createScaledBitmap(
            bitmapArrow,
            bitmapArrow.width / 4,
            bitmapArrow.height / 4,
            false
        )
        markerCurrent = mMap.addMarker(
            MarkerOptions()
                .position(latLng)
                .zIndex(100f)
                .flat(true)
                .anchor(0.5f, 0.5f)
                .icon(BitmapDescriptorFactory.fromBitmap(arrowScaled))
        )
    }

    private fun initializeGroundOverlay() {
        val opts = BitmapFactory.Options()
        opts.inSampleSize = 1
        val bitmapPosbg =
            BitmapFactory.decodeResource(resources, R.drawable.situm_posbg, opts)
        val groundOverlayOptions = GroundOverlayOptions()
            .image(BitmapDescriptorFactory.fromBitmap(bitmapPosbg))
            .anchor(0.5f, 0.5f)
            .position(LatLng(0.0, 0.0), 0.1f)
            .zIndex(2f)
        groundOverlay = mMap.addGroundOverlay(groundOverlayOptions)
    }
    private fun updateMarkerIcon(location: Location) {
        val newLocationHasOrientation =
            location.hasBearing() && location.isIndoor
        if (markerWithOrientation == newLocationHasOrientation) {
            return
        }
        markerWithOrientation = newLocationHasOrientation
        val bitmapDescriptor: BitmapDescriptor
        val bitmapScaled: Bitmap
        bitmapScaled = if (markerWithOrientation) {
            val bitmapArrow = BitmapFactory.decodeResource(resources, R.drawable.pose)
            Bitmap.createScaledBitmap(
                bitmapArrow,
                bitmapArrow.width / 4,
                bitmapArrow.height / 4,
                false
            )
        } else {
            val bitmapCircle =
                BitmapFactory.decodeResource(resources, R.drawable.position)
            Bitmap.createScaledBitmap(
                bitmapCircle,
                bitmapCircle.width / 4,
                bitmapCircle.height / 4,
                false
            )
        }
        bitmapDescriptor = BitmapDescriptorFactory.fromBitmap(bitmapScaled)
        markerCurrent?.setIcon(bitmapDescriptor)
    }
    private fun centerInUser(location: Location) {
        val tilt = 40f
        val bearing =
            if (location.hasBearing() && location.isIndoor) location.bearing.degrees().toFloat() else mMap.getCameraPosition().bearing
        val latLng =
            LatLng(location.coordinate.latitude, location.coordinate.longitude)
        //Skip if no change in location and little bearing change
        val skipAnimation =
            lastCameraLatLng != null && lastCameraLatLng == latLng && Math.abs(
                bearing - lastCameraBearing
            ) < MIN_CHANGE_IN_BEARING_TO_ANIMATE_CAMERA
        lastCameraLatLng = latLng
        lastCameraBearing = bearing
        if (!skipAnimation) {
            val cameraPosition = CameraPosition.Builder(mMap.getCameraPosition())
                .target(latLng)
                .bearing(bearing)
                .tilt(tilt)
                .build()
            mMap.animateCamera(
                CameraUpdateFactory.newCameraPosition(cameraPosition),
                UPDATE_LOCATION_ANIMATION_TIME,
                null
            )
        }
    }
    private fun startLocation() {
        if (locationManager!!.isRunning) {
            return
        }
        locationListener = object : LocationListener {
            override fun onLocationChanged(@NonNull location: Location) {
                //resultTV.setText(location.toString())
                val latLng = LatLng(
                    location.coordinate.latitude,
                    location.coordinate.longitude
                )
                val drawBounds = building!!.bounds
                val coordinateNE = drawBounds.northEast
                val coordinateSW = drawBounds.southWest

                val latLngBounds = LatLngBounds(
                    LatLng(coordinateSW.latitude, coordinateSW.longitude),
                    LatLng(coordinateNE.latitude, coordinateNE.longitude)
                )

                if (markerCurrent == null) {
                    initializeMarker(latLng)
                }
                if (groundOverlay == null) {
                    initializeGroundOverlay()
                }

                srcLocation = location

                updateMarkerIcon(location)
                positionAnimator.animate(markerCurrent, groundOverlay, location)
                //centerInUser(location)

                if(!isRouteSetup){
                    pointSrc = createPoint(latLng)

                    val destLatLang = LatLng(destination_lati!!, destination_longi!!)
                    showNavigation(mMap, destLatLang)

                }

                if (pointDest != null) {
                    if (navigation) {
                        SitumSdk.navigationManager().updateWithLocation(location)
                    }

                }

            }

            override fun onStatusChanged(@NonNull locationStatus: LocationStatus) {
                //statusTV.setText("Status: " + locationStatus.name())
            }

            override fun onError(@NonNull error: Error) {
                Toast.makeText(this@NavigationActivity2, error.message, Toast.LENGTH_LONG)
                    .show()
            }
        }
        val locationRequest = LocationRequest.Builder()
            //.buildingIdentifier(building.getIdentifier())
            .useWifi(true)
            .useBle(false)
            .useGps(true)
            .useForegroundService(true)
            .build()
        locationManager?.requestLocationUpdates(locationRequest!!, locationListener!!)
    }

    private fun stopLocation() {
        if (!locationManager!!.isRunning) {
            return
        }
        locationManager!!.removeUpdates(locationListener!!)
        stopNavigation()
    }
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
    interface Callback {
        fun onSuccess(floorImage: Bitmap)
        fun onError(error: Error)
    }

    override fun onMapReady(map: GoogleMap?) {
        System.err.println("OnMapReady start")
        mMap = map as GoogleMap;
        fetchCurrentFloorImage(building, object : Callback {
            override fun onSuccess(floorImage: Bitmap) {

                drawBuilding(floorImage)
            }
            override fun onError(error: Error) {

            }
        })
        // get pois list
        getPois(mMap)
        mMap.getUiSettings().setMapToolbarEnabled(false);
        mMap.getUiSettings().setMyLocationButtonEnabled(false);
        /*
        mMap.setOnMapClickListener(OnMapClickListener { latLng: LatLng ->
            getPoint(
                mMap,
                latLng
            )
        })
         */
    }
    private fun getPoint(googleMap: GoogleMap, latLng: LatLng) {
        removePolylines()
        if (markerDestination != null) {
            markerDestination!!.remove()
        }
        pointDest = createPoint(latLng)
        if (pointDest == null) {
            return
        }
        navigation = true
        calculateRoute()
        markerDestination =
            googleMap.addMarker(MarkerOptions().position(latLng).title("destination"))
    }
    private fun hideProgress(){
        progressBar.visibility = View.GONE
//        mapFragment?.view?.visibility = View.VISIBLE
    }
    private fun getPois(googleMap: GoogleMap) {
        getPoisUseCase.get(object : GetPoisUseCase.Callback {
            override fun onSuccess(building: Building, pois: Collection<Poi>) {
                poisList.addAll(pois)
            }
            override fun onError(error: String?) {
                Toast.makeText(this@NavigationActivity2, error, Toast.LENGTH_LONG).show()
            }
        })
    }
    private fun drawPoi(poi: Poi, bitmap: Bitmap? = null): Marker {
        val builder = LatLngBounds.Builder()
        val latLng = LatLng(
            poi.coordinate.latitude,
            poi.coordinate.longitude
        )
        val markerOptions = MarkerOptions()
            .position(latLng)
            .title(poi.name)
        if (bitmap != null) {
            markerOptions.icon(BitmapDescriptorFactory.fromBitmap(bitmap))
        }
        val poiMarker = mMap.addMarker(markerOptions)
        builder.include(latLng)
        return poiMarker
    }
    fun onMapLoaded(bitmap: Bitmap){
        coordinateConverter = CoordinateConverter(
            building.dimensions,
            building.center,
            building.rotation
        )
        drawBuilding(bitmap)
        if(isFirstLoad == false) {
            startLocation()
            isFirstLoad = true
        }

    }
    fun drawBuilding(bitmap: Bitmap) {
        val drawBounds = building.bounds
        val coordinateNE = drawBounds.northEast
        val coordinateSW = drawBounds.southWest
        val latLngBounds = LatLngBounds(
            LatLng(coordinateSW.latitude, coordinateSW.longitude),
            LatLng(coordinateNE.latitude, coordinateNE.longitude)
        )

        if(buildingOverlay == null) {
            buildingOverlay = mMap.addGroundOverlay(
                GroundOverlayOptions()
                    .image(BitmapDescriptorFactory.fromBitmap(bitmap))
                    .bearing(building.rotation.degrees().toFloat())
                    .positionFromBounds(latLngBounds)
            )
        }
        else{
            buildingOverlay?.setImage(BitmapDescriptorFactory.fromBitmap(bitmap))
        }
        mMap?.animateCamera(CameraUpdateFactory.newLatLngBounds(latLngBounds, 100))
    }
    fun updateFloorButton(count: Int){
        if(count > 3)
            radio_button4.visibility = View.VISIBLE
        else
            radio_button4.visibility = View.GONE

        if(count > 2)
            radio_button3.visibility = View.VISIBLE
        else
            radio_button3.visibility = View.GONE

        if(count > 1)
            radio_button2.visibility = View.VISIBLE
        else
            radio_button2.visibility = View.GONE

        if(count > 0)
            radio_button1.visibility = View.VISIBLE
        else
            radio_button1.visibility = View.GONE
    }
    fun changeFloor(floor_level: Int?){
        if(floor_level == 0)
            radio_group.check(R.id.radio_button1)
        else if(floor_level == 1)
            radio_group.check(R.id.radio_button2)
        else if(floor_level == 2)
            radio_group.check(R.id.radio_button3)
        else if(floor_level == 3)
            radio_group.check(R.id.radio_button4)
    }
    fun changeFloor(floorIdentifier: String?){
        for((index, item) in floorList.withIndex()){
            if(item.identifier == floorIdentifier){
                changeFloor(index)
                break
            }
        }
    }
    fun fetchCurrentFloorImage(building: Building, callback: Callback) {
        SitumSdk.communicationManager().fetchFloorsFromBuilding(
            building,
            object : Handler<Collection<Floor>> {
                override fun onSuccess(floorsCollection: Collection<Floor>) {
                    floorList = ArrayList(floorsCollection)
                    updateFloorButton(floorList.count())
                    changeFloor(destFloorIdentifier)

                }

                override fun onFailure(error: Error) {
                    callback.onError(error)
                }
            })
    }
    private fun centerCamera(route: Route) {
        val from = route.from.coordinate
        val to = route.to.coordinate
        val builder = LatLngBounds.Builder()
            .include(LatLng(from.latitude, from.longitude))
            .include(LatLng(to.latitude, to.longitude))
        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 100))
    }
    // routes
    /**
     * END DRAWING
     *
     */
    /**
     * ROUTE
     */
    /**
     * Update location parameters on the fly
     */
    private fun updateLocationParams(points: List<Point>) {
        locationParametersUpdate = LocationParametersUpdate.Builder()
            //.buildingIdentifier(building.identifier)
            .addRoutePoints(points)
            .locationDelimitedByRoute(true)
            .routeId(++routeId) //Must be increased (different) every time the params are updated so that the routes can be differentiated
            .build()
        locationManager?.updateLocationParameters(locationParametersUpdate!!, object :
            LocationParametersUpdateListener {
            override fun onApplied(locationParametersUpdate: LocationParametersUpdate) {
                Log.d(TAG, "Update successful"
                )
            }

            override fun onError(error: Error) {
                Log.d(TAG, "Error on update: $error"
                )
            }
        })
    }

    /**
     * Stop updating location parameters if an error occurs
     */
    private fun stopUpdateParams() {
        locationParametersUpdate = LocationParametersUpdate.Builder()
            .locationDelimitedByRoute(false)
            .routeId(++routeId)
            .build()
        locationManager!!.updateLocationParameters(locationParametersUpdate!!, object :
            LocationParametersUpdateListener {
            override fun onApplied(locationParametersUpdate: LocationParametersUpdate) {
                Log.d(TAG, "Location params update stopped")
            }

            override fun onError(error: Error) { Log.d(TAG, "Error stopping location params update: $error")
            }
        })
    }
    fun setUpNavigation() {
        Log.d(TAG, "setUpNavigation: ")
        SitumSdk.navigationManager().requestNavigationUpdates(navigationRequest!!, object :
            NavigationListener {
            override fun onDestinationReached() {
                Log.d(TAG, "onDestinationReached: ")
                tv_indication.text = "Destination Arrived"
                WitHelper.showDismissAlert(this@NavigationActivity2, "You have arrived at your destination.", DialogInterface.OnClickListener { dialog, which ->
                    onBackPressed()
                })
                removePolylines()
            }

            override fun onProgress(navigationProgress: NavigationProgress) {
                val context = applicationContext
                val progressText = navigationProgress.currentIndication.toText(context)
                Log.d(TAG, "onProgress: " + progressText)
                tv_indication.text = progressText
                calculateRoute()
            }

            override fun onUserOutsideRoute() {
                Log.d(TAG, "onUserOutsideRoute: ")
                tv_indication.text = "Outside of the route"
                //startNavigation()
            }
        })
    }
    private fun calculateRoute(){
        val directionsRequest: DirectionsRequest = DirectionsRequest.Builder()
            .from(srcLocation!!.position, Angle.EMPTY)
            .to(pointDest!!)
            .build()
        changeFloor(srcLocation?.floorIdentifier)
        SitumSdk.directionsManager().requestDirections(
            directionsRequest,
            object : Handler<Route> {
                override fun onSuccess(route: Route) {
                    currentRoute = route
                    updateLocationParams(route.points())
                    removePolylines()
                    drawRoute(route)
                    centerCamera(route)
                    navigationRequest = NavigationRequest.Builder()
                        .route(route)
                        .distanceToGoalThreshold(3.0)
                        .outsideRouteThreshold(50.0)
                        .build()
                    setUpNavigation()
                }

                override fun onFailure(error: Error) {
                    stopUpdateParams()
                }
            })
    }

    private fun clearMap() {
        markerCurrent?.remove()
        markerDestination?.remove()
        removePolylines()
    }
    private fun showNavigation(googleMap: GoogleMap, latLng: LatLng) {
        removePolylines()

        pointDest = createPoint(latLng)
        if (srcLocation == null || pointDest == null) {
            return
        }
        if (srcLocation!!.position.floorIdentifier == "-1")
            return
        navigation = true
        startNavigation()
        // add destination marker
        isRouteSetup = true
    }
    private fun createPoint(latLng: LatLng): Point {
        val coordinate = Coordinate(latLng.latitude, latLng.longitude)
        val cartesianCoordinate =
            coordinateConverter!!.toCartesianCoordinate(coordinate)
        return Point(
            building.identifier,
            destFloorIdentifier!!,
            coordinate,
            cartesianCoordinate
        )
    }

    private fun removePolylines() {
        for (polyline in polylines) {
            polyline.remove()
        }
        polylines.clear()
    }
    private fun drawRoute(route: Route): Unit {
        for (segment in route.segments) { //For each segment you must draw a polyline
//Add an if to filter and draw only the current selected floor
            if(segment.floorIdentifier != currentFloorIdentifier)
                continue
            val latLngs: MutableList<LatLng> = java.util.ArrayList()
            for (point in segment.points) {
                latLngs.add(
                    LatLng(
                        point.coordinate.latitude,
                        point.coordinate.longitude
                    )
                )
            }
            val polyLineOptions = PolylineOptions()
                .color(Color.GREEN)
                .width(18f)
                .zIndex(3f)
                .addAll(latLngs)
            val polyline: Polyline = mMap.addPolyline(polyLineOptions)
            polylines.add(polyline)
        }
    }


    fun startNavigation() {
        Log.d(TAG, "startNavigation: ")
        navigation_layout.visibility = View.VISIBLE
        hideProgress()
        calculateRoute()
    }

    fun stopNavigation() {
        removePolylines()
        pointDest = null
        navigationRequest = null
        navigation = false
        tv_indication.setText("Navigation")
    }
    companion object{
        val EXTRA_BUILDING = "Building"
        val EXTRA_DESTINATION_LATI = "DestinationLati"
        val EXTRA_DESTINATION_LONGI = "DestinationLongti"
        val EXTRA_DESTINATION_FLOOR_ID = "FloorIdentifier"
    }
}
