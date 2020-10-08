package com.waterloo.wit.ui

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.*
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.mancj.materialsearchbar.MaterialSearchBar
import com.waterloo.wit.R
import com.waterloo.wit.adapter.FloorItemClickListener
import com.waterloo.wit.adapter.FloorLevelAdapter
import com.waterloo.wit.adapter.SitumSearchAdapter
import com.waterloo.wit.adapter.SpinnerItemAdapter
import com.waterloo.wit.data.SitumBuildingItem
import com.waterloo.wit.data.SitumFloorItem
import com.waterloo.wit.data.WitDestWorkItem
import com.waterloo.wit.fragment.MapStateListener
import com.waterloo.wit.fragment.TouchableMapFragment
import com.waterloo.wit.helpers.WitHelper
import com.waterloo.wit.utils.GetPoiCategoryIconUseCase
import com.waterloo.wit.utils.LogUtils
import com.waterloo.wit.utils.PositionAnimator
import es.situm.maps.library.ui.view.info.InfoView
import es.situm.sdk.SitumSdk
import es.situm.sdk.directions.DirectionsRequest
import es.situm.sdk.error.Error
import es.situm.sdk.location.*
import es.situm.sdk.location.util.CoordinateConverter
import es.situm.sdk.model.cartography.*
import es.situm.sdk.model.directions.Indication
import es.situm.sdk.model.directions.Route
import es.situm.sdk.model.location.Angle
import es.situm.sdk.model.location.Coordinate
import es.situm.sdk.model.location.Location
import es.situm.sdk.model.navigation.NavigationProgress
import es.situm.sdk.navigation.NavigationListener
import es.situm.sdk.navigation.NavigationRequest
import es.situm.sdk.utils.Handler
import kotlinx.android.synthetic.main.activity_realtime_navigation.*
import kotlinx.android.synthetic.main.situm_fragment_level_list.*
import kotlinx.android.synthetic.main.situm_fragment_viewitem_list.*
import kotlinx.android.synthetic.main.situm_info_view.*
import kotlinx.android.synthetic.main.situm_poi_summary_view.*
import kotlinx.android.synthetic.main.situm_route_view.*
import org.locationtech.jts.geom.GeometryFactory
import java.util.*
import kotlin.collections.ArrayList


class RealTimeNavigationActivity : BaseActivity(), OnMapReadyCallback, MaterialSearchBar.OnSearchActionListener, FloorItemClickListener {
    private val TAG = RealTimeNavigationActivity::class.java.simpleName
    private val UPDATE_LOCATION_ANIMATION_TIME = 300
    private val MIN_CHANGE_IN_BEARING_TO_ANIMATE_CAMERA = 5

    private lateinit var context: Context
    private var mapFragment: TouchableMapFragment? = null
    private lateinit var mMap: GoogleMap
    private var building: Building? = null

    private lateinit var searchBar: MaterialSearchBar
    private lateinit var adapter: SitumSearchAdapter
    private var floorLevelAdapter: FloorLevelAdapter? = null

    private var situmSearchItemList = ArrayList<SitumBuildingItem>()
    private var floorItemList: ArrayList<SitumFloorItem> = ArrayList()
    private var selectedFloor: Floor? = null
    private var selectedMode: Int = 0
    // User Location
    private var markerWithOrientation = false
    private var lastCameraLatLng: LatLng? = null
    private var lastCameraBearing = 0f
    private var groundOverlay: GroundOverlay? = null
    private var positionAnimator: PositionAnimator = PositionAnimator()
    var markerCurrent: Marker? = null
    var srcLocation: Location? = null
    var oldUserFloor: Floor? = null
    // POIs
    var markerPoiList: ArrayList<Marker> = ArrayList()
    private val getPoiCategoryIconUseCase = GetPoiCategoryIconUseCase()
    // GeoFence
    private var geofencePolygonMap = HashMap<Geofence, org.locationtech.jts.geom.Polygon>()
    private val geometryFactory = GeometryFactory()
    private var geoPolygonList = ArrayList<Polygon>()
    // Navigation
    private var navigation = false
    private var isRouteSetup: Boolean = false
    private var pointDest: Point? = null
    private var navigationRequest: NavigationRequest? = null
    private var currentRoute: Route? = null
    private var locationParametersUpdate: LocationParametersUpdate? = null
    private var routeId = 0
    private val polylines: ArrayList<Polyline> = ArrayList()
    // Ground Overlay
    private var buildingGroundOverlay: GroundOverlay? = null
    // Current Screen State
    private var situmScreenState = SITUM_STATE.INIT

    private var locationManager: LocationManager? = null

    private var isCenterButtonClicked = false

    private lateinit var searchListView: ListView
    private lateinit var layout_search_view: LinearLayout
    private lateinit var layout_info_view: InfoView
    private lateinit var poi_summary_view: RelativeLayout
    private lateinit var navigationFloatingButton: FloatingActionButton
    private lateinit var navigation_center_view: Button
    private lateinit var positionFloatingButton: FloatingActionButton
    private lateinit var geofenceFloatingButton: FloatingActionButton
    private lateinit var layout_floor_level_list: RelativeLayout
    private lateinit var layout_viewitem_list: RelativeLayout
    private lateinit var viewModeSpinner: Spinner

    // Destination information
    private var destination_building: Building? = null
    private var destination_workitem: WitDestWorkItem? = null
    private var messageUser = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_realtime_navigation)

        context = applicationContext

        searchBar = findViewById(R.id.searchBar)
        searchBar.setCardViewElevation(20)
        searchBar.setOnSearchActionListener(this)
        searchBar.addTextChangeListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun afterTextChanged(s: Editable) {

            }
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                LogUtils.e(TAG, "SearchText Changed: " + s)
                if(adapter != null)
                    adapter.filter(s.toString())
            }
        })
        searchListView = findViewById(R.id.searchListView)
        layout_search_view = findViewById(R.id.layout_search_view)
        layout_info_view = findViewById(R.id.layout_info_view)
        poi_summary_view = findViewById(R.id.poi_summary_view)
        navigationFloatingButton = findViewById(R.id.navigationFloatingButton)
        geofenceFloatingButton = findViewById(R.id.geofenceFloatingButton)
        navigation_center_view = findViewById(R.id.navigation_center_view)
        positionFloatingButton = findViewById(R.id.positionFloatingButton)
        layout_floor_level_list = findViewById(R.id.layout_floor_level_list)
        layout_viewitem_list = findViewById(R.id.layout_viewitem_list)

        viewModeSpinner = findViewById(R.id.viewModeSpinner)

//        progressBar = findViewById(R.id.progressBar)


        destination_building = intent.getParcelableExtra(EXTRA_BUILDING)
        if(destination_building != null) {
            val workitem_title = intent.getStringExtra(EXTRA_WORKITEM_TITLE)
            val destination_lati = intent.getDoubleExtra(EXTRA_DESTINATION_LATI, 0.0)
            val destination_longi = intent.getDoubleExtra(EXTRA_DESTINATION_LONGI, 0.0)
            val destination_floor_id = intent.getStringExtra(EXTRA_DESTINATION_FLOOR_ID)
            val destination_floor_level = intent.getIntExtra(EXTRA_DESTINATION_FLOOR_LEVEL, 0)
            destination_workitem = WitDestWorkItem(workitem_title, destination_lati, destination_longi, destination_floor_id, destination_floor_level, destination_building!!.identifier)
        }
        // item view mode
        WitHelper.SetLinearLayoutManagerAndHairlineDividerFor(recycler_level_list, this.context, true)
        val viewItemList = arrayListOf<String>("POIs", "Room", "None")
        val adapter = SpinnerItemAdapter(
            context!!,
            R.layout.spinner_item_layout,
            viewItemList
        )
        viewModeSpinner.adapter = adapter
        //viewModeSpinner.setSelection(0)
        viewModeSpinner?.setOnItemSelectedListener(object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                selectedMode = position
                updateViewMode()
            }
        })
        // map fragment
        mapFragment = supportFragmentManager.findFragmentById(R.id.map) as TouchableMapFragment
        mapFragment?.getMapAsync(this)
        locationManager = SitumSdk.locationManager()
        refreshScreen(SITUM_STATE.INIT)
    }

    override fun onStart() {
        super.onStart()

        // positioning button
        positionFloatingButton.setOnClickListener(View.OnClickListener {
            if(locationManager!!.isRunning){
                stopPositioning()
                positionFloatingButton.setBackgroundTintList(
                    ContextCompat.getColorStateList(
                        getApplicationContext(),
                        R.color.white
                    ));
            }
            else
                startPositioning()
            isCenterButtonClicked = false
        })
        // center button
        navigation_center_view.setOnClickListener(View.OnClickListener {
            isCenterButtonClicked = true

            if(srcLocation != null) {
                val latLng = LatLng(srcLocation!!.coordinate.latitude, srcLocation!!.coordinate.longitude);
                // Showing the current location in Google Map
                mMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));
                val floorPos = getFloorPos(srcLocation!!.floorIdentifier)
                onFloorItemClick(floorPos)
            }

            navigation_center_view.visibility = View.GONE
            positionFloatingButton.visibility = View.VISIBLE
        })
        // navigation button
        navigationFloatingButton.setOnClickListener(View.OnClickListener {
            refreshScreen(SITUM_STATE.BUILDING)
            navigation = true
            layout_search_view.visibility = View.GONE
            navigationFloatingButton.visibility = View.GONE
            navigation_center_view.visibility = View.GONE
            positionFloatingButton.visibility = View.GONE
            startPositioning()
        })
        // cancel navigation
        cancel_route_visualization_button.setOnClickListener(View.OnClickListener {
            stopNavigation()
        })
        // geofence button
        geofenceFloatingButton.setOnClickListener(View.OnClickListener {
            if(srcLocation != null) {
                val latLng =
                    LatLng(srcLocation!!.coordinate.latitude, srcLocation!!.coordinate.longitude);
                launchGeofenceInfoScreen(latLng)
            }
        })
    }
    override fun onDestroy() {
        super.onDestroy()
        stopPositioning()
    }


    override fun onButtonClicked(buttonCode: Int) {
        LogUtils.e(TAG, "onButtonClicked " + buttonCode)
    }

    override fun onSearchConfirmed(text: CharSequence?) {
        LogUtils.e(TAG, "onSearchConfirmed " + text)
    }

    override fun onSearchStateChanged(enabled: Boolean) {
        LogUtils.e(TAG, "State Changed " + enabled)
        if(enabled){
            searchListView.visibility = View.VISIBLE
            adapter?.filter("")
        }
        else{
            searchListView.visibility = View.GONE
        }
    }
    private fun refreshScreen(state: SITUM_STATE){
        if(navigation)
            return
        layout_search_view.visibility = View.VISIBLE

        if(state == SITUM_STATE.INIT) {
            searchListView.visibility = View.GONE
            layout_info_view.visibility = View.INVISIBLE
            poi_summary_view.visibility = View.GONE
            navigationFloatingButton.visibility = View.GONE
            navigation_center_view.visibility = View.GONE
            positionFloatingButton.visibility = View.GONE
            geofenceFloatingButton.visibility = View.GONE
            layout_floor_level_list.visibility = View.GONE
            layout_viewitem_list.visibility = View.GONE
        }
        else if(state == SITUM_STATE.OUTDOOR){
            layout_info_view.visibility = View.INVISIBLE
            positionFloatingButton.visibility = View.VISIBLE
            poi_summary_view.visibility = View.GONE
            navigationFloatingButton.visibility = View.GONE
            layout_floor_level_list.visibility = View.GONE
            layout_viewitem_list.visibility = View.GONE
        }
        else if(state == SITUM_STATE.BUILDING){
            layout_info_view.visibility = View.VISIBLE
            positionFloatingButton.visibility = View.VISIBLE
            poi_summary_view.visibility = View.GONE
            navigationFloatingButton.visibility = View.GONE
            layout_floor_level_list.visibility = View.VISIBLE
            layout_viewitem_list.visibility = View.VISIBLE
        }
        else if(state == SITUM_STATE.POI) {
            layout_info_view.visibility = View.INVISIBLE
            positionFloatingButton.visibility = View.VISIBLE
            poi_summary_view.visibility = View.VISIBLE
            navigationFloatingButton.visibility = View.VISIBLE
        }
        situmScreenState = state
    }
    fun updateViewMode(){
        if(selectedFloor == null)
            return
        hidePois()
        clearAllGeoFence()
        if(selectedMode == 0) {
            // Update Poi
            updatePois(selectedFloor!!)
        }
        else if(selectedMode == 1){
            // Draw Geo Fence
            updateGeoFence(selectedFloor!!)
        }
    }

    // FloorItemClick listener
    override fun onFloorItemClick(position: Int) {
        if(position == -1)
            return
        val selectedFloorItem = floorItemList.get(position)
        selectedFloor = selectedFloorItem.floor

        SitumSdk.communicationManager().fetchMapFromFloor(
            selectedFloor!!,
            object : Handler<Bitmap> {
                override fun onSuccess(bitmap: Bitmap) {
                    drawBuilding(bitmap)
                    updateViewMode()
                    if(currentRoute != null) {
                        removePolylines()
                        drawRoute(currentRoute!!)
                    }
                }
                override fun onFailure(error: Error) {
                }
            })
        // check location floor
        if(srcLocation != null && !srcLocation!!.isOutdoor){
            val floor = getFloor(srcLocation!!.floorIdentifier)
            if(floor?.identifier == selectedFloor?.identifier){
                markerCurrent?.isVisible = true
            }
            else{
                markerCurrent?.isVisible = false
            }
        }
        floorLevelAdapter?.selected_position = position
        floorLevelAdapter?.notifyDataSetChanged()

        if(locationManager!!.isRunning) {
            navigation_center_view.visibility = View.VISIBLE
            positionFloatingButton.visibility = View.GONE
        }

        if(messageUser == false && destination_workitem != null){
            messageUser = true
            WitHelper.showAlert(this, "Tap the Red marker to begin navigation");
        }
    }
    private fun launchGeofenceInfoScreen(it: LatLng){
        val geofence = checkIfPointIsInsideGeofence(
            geometryFactory.createPoint(
                org.locationtech.jts.geom.Coordinate(
                    it.latitude,
                    it.longitude
                )
            )
        )
        if(geofence != null){
            val intent = Intent(context, RoomInformationActivity::class.java)
            intent.putExtra(RoomInformationActivity.EXTRA_BUILDING, building)
            intent.putExtra(RoomInformationActivity.EXTRA_GEOFENCEE, geofence)
            startActivity(intent)
        }
    }
    private fun checkIfPointIsInsideGeofence(point: org.locationtech.jts.geom.Point): Geofence? {
        if (geofencePolygonMap.isEmpty()) {
            return null
        }
        var geofence: Geofence? = null
        for ((key, value) in geofencePolygonMap) {
            if (key.floorIdentifier != selectedFloor?.getIdentifier()) {
                continue
            }
            if (point.within(value)) {
                geofence = key

            }
        }
        return geofence
    }
    private fun getGeoFence(building: Building) {
        SitumSdk.communicationManager().fetchGeofencesFromBuilding(
            building,
            object :
                Handler<List<Geofence>> {
                override fun onSuccess(geofences: List<Geofence>) {
                    for (geofence in geofences) {
                        //Log.e(TAG, "GeoFence 111 Name ----- " + geofence.name)
                        val jtsCoordinates: MutableList<org.locationtech.jts.geom.Coordinate> = ArrayList()
                        for (point in geofence.polygonPoints) {
                            val coordinate =
                                org.locationtech.jts.geom.Coordinate(
                                    point.coordinate.latitude,
                                    point.coordinate.longitude
                                )
                            jtsCoordinates.add(coordinate)
                        }
                        if (!jtsCoordinates.isEmpty()) {
                            val polygon =
                                geometryFactory.createPolygon(jtsCoordinates.toTypedArray())
                            geofencePolygonMap.put(geofence, polygon)
                        }
                    }
                }
                override fun onFailure(error: Error) {

                }
            })
    }
    private fun clearAllGeoFence(){
        for(polygon in geoPolygonList)
            polygon.remove()
        geoPolygonList.clear()
    }
    private fun updateGeoFence(floor: Floor){
        for (geofence in geofencePolygonMap.keys){
            if(!geofence.floorIdentifier.equals(floor.identifier))
                continue
            val polygon = createPolygonAndDrawInTheMap(geofence)
            geoPolygonList.add(polygon!!)
        }
    }
    private fun createPolygonAndDrawInTheMap(geofence: Geofence): Polygon? {
        var polygon: Polygon? = null
        val polygonOptions = PolygonOptions()
        val latLngs: MutableList<LatLng> = java.util.ArrayList()
        for (point in geofence.polygonPoints) {
            latLngs.add(
                LatLng(
                    point.coordinate.latitude,
                    point.coordinate.longitude
                )
            )
        }
        polygonOptions.addAll(latLngs)
        polygonOptions.strokeColor(Color.BLACK)
        polygonOptions.strokeWidth(5f)
        polygonOptions.fillColor(0x7Ff3ff00)
        polygonOptions.zIndex(2f)
        if (!latLngs.isEmpty()) {
            polygon = mMap.addPolygon(polygonOptions)
        }
        return polygon
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

    fun drawDestinationMarker(latLng: LatLng, realPos: Boolean): Marker{
        val markerOptions = MarkerOptions().
            position(latLng)
            .icon(BitmapDescriptorFactory.fromBitmap(WitHelper.getRedMarkerBitmap(context!!)))
            .rotation(180.0f)
            .zIndex(100f)
            .title(destination_workitem?.name)
        val destMarker = mMap.addMarker(markerOptions)
        destMarker.setAnchor(0.5f, 0.5f)
        return destMarker
    }
    fun hidePois(){
        for (item in markerPoiList){
            item.isVisible = false
        }
    }
    fun updatePois(floor: Floor){
        for (item in markerPoiList){
            if(item.tag is WitDestWorkItem){
                val poi = item.tag as WitDestWorkItem
                if(poi.floorIdentifier == floor.identifier)
                    item.isVisible = true
                else
                    item.isVisible = false
            }
            else if(item.tag is Poi){
                val poi = item.tag as Poi
                if (poi.floorIdentifier == floor.identifier) {
                    item.isVisible = true
                } else {
                    item.isVisible = false
                }
            }
        }

    }
    private fun getPois(building: Building, callback: PoisCallback){
        for(poiItem in markerPoiList){
            poiItem.remove()
        }
        markerPoiList.clear()
        SitumSdk.communicationManager().fetchIndoorPOIsFromBuilding(
            building,
            object : Handler<Collection<Poi>> {
                override fun onSuccess(pois: Collection<Poi>) {
                    for (poi in pois) {
                        getPoiCategoryIconUseCase.getUnselectedIcon(poi, object : GetPoiCategoryIconUseCase.Callback {
                            override fun onSuccess(bitmap: Bitmap?) {
                                val marker = drawPoi(poi, bitmap)
                                marker.isVisible = false
                                marker.tag = poi
                                markerPoiList.add(marker)
                            }
                            override fun onError(error: String?) {
                                Log.d("Error fetching poi icon", error)
                            }
                        })
                    }
                    if(building == destination_building && destination_workitem != null){
                        val destLatLng = LatLng(destination_workitem!!.latitude, destination_workitem!!.longitude);
                        val destMarker = drawDestinationMarker(destLatLng, true)
                        destMarker.tag  = destination_workitem
                        markerPoiList.add(destMarker)
                    }
                    callback.onSuccess(pois)
                }

                override fun onFailure(error: Error) {
                }
            })
    }
    private fun loadBuildings() {
        //llProgressBar.visibility = View.VISIBLE
        showProgressDialog("Downloading building...")
        SitumSdk.communicationManager().fetchBuildings(object :
            Handler<Collection<Building>> {
            override fun onSuccess(buildings: Collection<Building>) {
                val buildingList = ArrayList<Building>(buildings)
                //building list
                for((index, item) in buildingList.withIndex()){
                    situmSearchItemList.add(SitumBuildingItem(item, null, 0))
                }
                adapter = SitumSearchAdapter(this@RealTimeNavigationActivity, situmSearchItemList)
                searchListView.adapter = adapter
                searchListView.setOnItemClickListener { parent, view, position, id ->
                    val searchItem = situmSearchItemList.get(position)
                    if( searchItem.itemType == 0){
                        selectBuilding(searchItem.building!!)
                    }
                    else{

                    }

                    searchBar.closeSearch()
                }

                // Select first building
                if (buildingList.count()>0) {
                    if(destination_building == null)
                        selectBuilding(buildingList.get(0))
                    else
                        selectBuilding(destination_building!!)
                }
                refreshScreen(SITUM_STATE.BUILDING)
                //llProgressBar.visibility = View.GONE
                hideProgressDialog()
            }

            override fun onFailure(error: Error) {
                //llProgressBar.visibility = View.GONE
                hideProgressDialog()
                Toast.makeText(context,"Failed to load buildings", Toast.LENGTH_SHORT).show()

            }
        })
    }
    fun selectBuilding(building: Building){
        // building
        if(this.building == null || this.building?.identifier != building.identifier || situmScreenState == SITUM_STATE.OUTDOOR) {
            this.building = building
            // geo fence
            getGeoFence(building)
            getPois(building, object : PoisCallback{
                override fun onSuccess(pois: Collection<Poi>) {
                    fetchFirstFloorImage(building)
                }

                override fun onError(error: Error) {
                    fetchFirstFloorImage(building)
                }
            })
            // fetch pois
            // display building name
            txt_active_building.text = this.building?.name
        }
    }
    fun drawBuilding(bitmap: Bitmap) {
        val drawBounds = building!!.bounds
        val coordinateNE = drawBounds.northEast
        val coordinateSW = drawBounds.southWest
        val latLngBounds = LatLngBounds(
            LatLng(coordinateSW.latitude, coordinateSW.longitude),
            LatLng(coordinateNE.latitude, coordinateNE.longitude)
        )
        buildingGroundOverlay?.remove()
        buildingGroundOverlay = mMap.addGroundOverlay(
            GroundOverlayOptions()
                .image(BitmapDescriptorFactory.fromBitmap(bitmap))
                .bearing(building!!.rotation.degrees().toFloat())
                .positionFromBounds(latLngBounds)
        )
        mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(latLngBounds, 100))
    }
    fun fetchFirstFloorImage(building: Building) {
        SitumSdk.communicationManager().fetchFloorsFromBuilding(
            building,
            object : Handler<Collection<Floor>> {
                override fun onSuccess(floorsCollection: Collection<Floor>) {
                    floorItemList.clear()
                    var floorIndex = 0
                    for ((index, item) in floorsCollection.withIndex()){
                        if(destination_workitem != null && item.identifier == destination_workitem!!.floorIdentifier)
                            floorIndex = index;
                        floorItemList.add(SitumFloorItem(item.floor, item))
                    }
                    floorLevelAdapter = FloorLevelAdapter(floorItemList, true)
                    floorLevelAdapter?.listener = this@RealTimeNavigationActivity
                    recycler_level_list.adapter = floorLevelAdapter
                    onFloorItemClick(floorIndex)
                }

                override fun onFailure(error: Error) {
               }
            })
    }
    fun getFloor(floorId: String): Floor?{
        var floor: Floor? = null
        for (item in  floorItemList){
            if(item.floor?.identifier == floorId){
                floor = item.floor
                break
            }
        }
        return floor
    }
    fun getFloorPos(floorId: String): Int{
        for((index, item) in floorItemList.withIndex()){
            if(item.floor?.identifier == floorId){
                return index
            }
        }
        return -1
    }
    override fun onMapReady(map: GoogleMap?) {
        System.err.println("OnMapReady start")
        mMap = map as GoogleMap;

        mMap.getUiSettings().setMapToolbarEnabled(false);
        mMap.getUiSettings().setMyLocationButtonEnabled(false);
        //mMap.setMyLocationEnabled(true);

        mMap.setOnMapLongClickListener {
            launchGeofenceInfoScreen(it)
        }
        mMap.setOnMapClickListener {
            if(building != null && isBuildingInMap())
                refreshScreen(SITUM_STATE.BUILDING)
            navigation_center_view.visibility = View.VISIBLE
            positionFloatingButton.visibility = View.GONE
        }
        mMap.setOnMarkerClickListener {
            if(navigation)
                false
            if(it.tag is Poi){
                val poi = it.tag as Poi

                val floorForPoi = getFloor(poi.floorIdentifier)
                txt_poi_name.text = poi.name
                txt_poi_detail.text = building?.name + "/ Floor " + floorForPoi?.floor.toString()
                refreshScreen(SITUM_STATE.POI)
                pointDest = poi.position
            }
            else if(it.tag is WitDestWorkItem){
                val poi = it.tag as WitDestWorkItem
                txt_poi_name.text = poi.name
                txt_poi_detail.text = building?.name + "/ Floor " + poi.floorLevel.toString()
                refreshScreen(SITUM_STATE.POI)

                val coordinateConverter = CoordinateConverter(
                    building!!.dimensions,
                    building!!.center,
                    building!!.rotation
                )
                val coordinate = Coordinate(poi.latitude, poi.longitude)
                val cartesianCoordinate =
                    coordinateConverter!!.toCartesianCoordinate(coordinate)
                pointDest = Point(
                    building!!.identifier,
                    poi.floorIdentifier,
                    coordinate,
                    cartesianCoordinate
                )
            }
            false
        }
        val mapListener = object : MapStateListener(mMap, mapFragment, this) {
            override fun onMapTouched() {

            }

            override fun onMapReleased() {
                if(locationManager!!.isRunning )
                   isCenterButtonClicked = false
            }

            override fun onMapSettled() {

            }

            override fun onMapUnsettled() {

            }
        }
        mMap.setOnCameraMoveListener {
            if(isBuildingInMap()){
                if(situmScreenState == SITUM_STATE.OUTDOOR)
                    refreshScreen(SITUM_STATE.BUILDING)
            }
            else{
                refreshScreen(SITUM_STATE.OUTDOOR)
            }
            if(locationManager!!.isRunning && isCenterButtonClicked == false) {
                navigation_center_view.visibility = View.VISIBLE
                positionFloatingButton.visibility = View.GONE
            }
        }
        loadBuildings()
    }
    private fun isMarkerInBuilding(latLng: LatLng): Boolean{
        val bounds: LatLngBounds = mMap.projection.visibleRegion.latLngBounds
        if (bounds.contains(latLng))
            return true
        return false
    }
    private fun isBuildingInMap(): Boolean {
        val bounds: LatLngBounds = mMap.projection.visibleRegion.latLngBounds
        val buildingBounds = building?.bounds
        val northEastLatLng =
            LatLng(buildingBounds!!.northEast.latitude, buildingBounds!!.northEast.longitude)
        val northWestLatLng =
            LatLng(buildingBounds!!.northWest.latitude, buildingBounds!!.northWest.longitude)
        val southEastLatLng =
            LatLng(buildingBounds!!.southEast.latitude, buildingBounds!!.southEast.longitude)
        val southWestLatLng =
            LatLng(buildingBounds!!.southWest.latitude, buildingBounds!!.southWest.longitude)
        if (bounds.contains(northEastLatLng) || bounds.contains(northWestLatLng) || bounds.contains(
                southEastLatLng
            ) || bounds.contains(southWestLatLng)
        ) {
            return true
        }
        return false
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
                //.tilt(tilt)
                .build()
            mMap.moveCamera(
                CameraUpdateFactory.newCameraPosition(cameraPosition)
            )
        }
    }
    private fun removePolylines() {
        for (polyline in polylines) {
            polyline.remove()
        }
        polylines.clear()
    }
    private fun drawRoute(route: Route): Unit {
        if(navigation == false)
            return
        for (segment in route.segments) { //For each segment you must draw a polyline
            //Add an if to filter and draw only the current selected floor
            if(segment.floorIdentifier != selectedFloor?.identifier)
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
                .color(Color.argb(100, 30, 144, 255))
                .width(18f)
                .zIndex(3f)

            val PATTERN_GAP_LENGTH_PX = 10
            val list = Arrays.asList(Dot(), Gap(PATTERN_GAP_LENGTH_PX.toFloat()))
            for (latLang in latLngs){
                polyLineOptions.add(latLang).jointType(2).endCap(RoundCap()).pattern(list)

            }
            val polyline: Polyline = mMap.addPolyline(polyLineOptions)
            polylines.add(polyline)
        }
    }
    private fun centerCamera(route: Route) {
        val from = route.from.coordinate
        val to = route.to.coordinate
        val builder = LatLngBounds.Builder()
            .include(LatLng(from.latitude, from.longitude))
            .include(LatLng(to.latitude, to.longitude))
        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 100))
    }
    /**
     * ROUTE
     */
    /**
     * Update location parameters on the fly
     */
    private fun updateLocationParams(points: List<Point>) {
        locationParametersUpdate = LocationParametersUpdate.Builder()
            .buildingIdentifier(building!!.identifier)
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
    fun getIndicationDrawable(indication: Indication): Int{
        var drawableResource = R.drawable.situm_direction_empty
        val indicationType = indication.indicationType
        val orientationType = indication.orientationType
        if(indicationType == Indication.Action.TURN){
            when(orientationType){
                Indication.Orientation.STRAIGHT -> {
                    drawableResource =  R.drawable.situm_direction_continue
                }
                Indication.Orientation.VEER_RIGHT, Indication.Orientation.RIGHT, Indication.Orientation.SHARP_RIGHT -> {
                    drawableResource =  R.drawable.situm_direction_turn_right
                }
                Indication.Orientation.VEER_LEFT, Indication.Orientation.LEFT, Indication.Orientation.SHARP_LEFT -> {
                    drawableResource =  R.drawable.situm_direction_turn_left
                }
                Indication.Orientation.BACKWARD -> {
                    drawableResource =  R.drawable.situm_direction_uturn
                }
                else -> {
                    drawableResource =  R.drawable.situm_direction_empty
                }
            }
        }
        else if(indicationType == Indication.Action.GO_AHEAD){
            drawableResource = R.drawable.situm_direction_continue
        }
        else if(indicationType == Indication.Action.CHANGE_FLOOR){
            if(indication.distanceToNextLevel > 0)
                drawableResource = R.drawable.situm_direction_stairs_up
            else
                drawableResource = R.drawable.situm_direction_stairs_down
        }
        else if(indicationType == Indication.Action.END){
            drawableResource = R.drawable.situm_direction_destination
        }
        else
            drawableResource = R.drawable.situm_direction_empty
        return drawableResource
    }
    fun setUpNavigation() {
        Log.d(TAG, "setUpNavigation: ")
        SitumSdk.navigationManager().requestNavigationUpdates(navigationRequest!!, object :
            NavigationListener {
            override fun onDestinationReached() {
                Log.d(TAG, "onDestinationReached: ")
                tv_indication.text = "Destination Arrived"
                stopNavigation()
                WitHelper.showDismissAlert(this@RealTimeNavigationActivity , "You have arrived at your destination.", DialogInterface.OnClickListener { dialog, which ->
                    if(destination_workitem != null){
                        onBackPressed()
                    }
                })
            }

            override fun onProgress(navigationProgress: NavigationProgress) {
                layout_route_view.visibility = View.VISIBLE
                txt_route_estimation.text = building?.name

                val indication = navigationProgress.currentIndication
                val nextIndication = navigationProgress.nextIndication
                val currentDrawable = getIndicationDrawable(indication)
                val nextDrawable = getIndicationDrawable(nextIndication)
                imageView_indication.setImageDrawable(ContextCompat.getDrawable(context, currentDrawable))
                imageView_next_indication.setImageDrawable(ContextCompat.getDrawable(context, nextDrawable))
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
    private fun showNavigation(googleMap: GoogleMap) {
        removePolylines()

        if (srcLocation == null || pointDest == null) {
            return
        }
        if (srcLocation!!.position.floorIdentifier == "-1")
            return
        startNavigation()
        // add destination marker
        isRouteSetup = true
    }
    fun startNavigation() {
        Log.d(TAG, "startNavigation: ")
        calculateRoute()
    }
    private fun calculateRoute(){
        val directionsRequest: DirectionsRequest = DirectionsRequest.Builder()
            .from(srcLocation!!.position, Angle.EMPTY)
            .to(pointDest!!)
            .build()
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
                        .distanceToChangeFloorThreshold(3.0)
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
    fun stopNavigation() {
        navigation = false
        pointDest = null
        navigationRequest = null
        currentRoute = null
        isRouteSetup = false
        SitumSdk.navigationManager().removeUpdates()
        removePolylines()
        layout_route_view.visibility = View.GONE
        navigation_center_view.visibility = View.GONE
        refreshScreen(SITUM_STATE.BUILDING)
    }

    private fun startPositioning() {
        if (locationManager!!.isRunning) {
            return
        }
        val locationRequest = LocationRequest.Builder()
            .buildingIdentifier(building!!.identifier)
            //.useGps(true)
            .useWifi(true)
            .useBle(true)
            .useForegroundService(true)
            .build()
        locationManager?.requestLocationUpdates(locationRequest, locationListener)
        positionFloatingButton.setBackgroundTintList(
            ContextCompat.getColorStateList(
            getApplicationContext(),
            R.color.situm_color_positioning_centered
        ));
        geofenceFloatingButton.visibility = View.VISIBLE
    }

    private fun stopPositioning() {
        locationManager?.removeUpdates(locationListener)
        markerCurrent?.remove()
        groundOverlay?.remove()
        markerCurrent = null
        groundOverlay = null
        pb_loading.visibility = View.GONE
        txt_active_building.text = building?.name

        geofenceFloatingButton.visibility = View.GONE

        floorLevelAdapter?.active_position = -1
        floorLevelAdapter?.notifyDataSetChanged()

        stopNavigation()
    }

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            Log.i(TAG, "onLocationChanged() called with: location = [$location]")
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

            srcLocation = location

            if (markerCurrent == null) {
                initializeMarker(latLng)
                centerInUser(location)
            }
            if (groundOverlay == null) {
                initializeGroundOverlay()
            }
            val userFloor = getFloor(location.floorIdentifier)
            if(userFloor?.identifier == selectedFloor?.identifier){
                markerCurrent?.isVisible = true
            }
            else{
                markerCurrent?.isVisible = false
            }
            val floorPos = getFloorPos(location.floorIdentifier)
            floorLevelAdapter?.active_position = floorPos
            floorLevelAdapter?.notifyDataSetChanged()

            if(oldUserFloor != null && oldUserFloor?.identifier != location.floorIdentifier){
                onFloorItemClick(floorPos)
            }
            oldUserFloor = userFloor

            updateMarkerIcon(location)
            positionAnimator.animate(markerCurrent, groundOverlay, location)
            //if(location.isOutdoor)
            //centerInUser(location)
            if (navigation) {
                if(!isRouteSetup)
                    showNavigation(mMap)
                SitumSdk.navigationManager().updateWithLocation(location)
            }

        }

        override fun onStatusChanged(status: LocationStatus) {
            Log.i(TAG, "onStatusChanged() called with: status = [$status]")
            if(status == LocationStatus.STARTING_POSITIONING){
                pb_loading.visibility = View.GONE
                txt_active_building.text = building?.name
            }
            else{
                pb_loading.visibility = View.VISIBLE
                txt_active_building.text = status.toString()
            }
        }

        override fun onError(error: Error) {
            Log.e(TAG, "onError() called with: error = [$error]")
            Toast.makeText(this@RealTimeNavigationActivity, error.message, Toast.LENGTH_LONG)
                .show()
        }
    }
    interface Callback {
        fun onSuccess(floorImage: Bitmap)
        fun onError(error: Error)
    }

    interface PoisCallback {
        fun onSuccess(pois: Collection<Poi>)
        fun onError(error: Error)
    }

    companion object{
        val EXTRA_WORKITEM_TITLE = "WorkItemTitle"
        val EXTRA_BUILDING = "Building"
        val EXTRA_DESTINATION_LATI = "DestinationLati"
        val EXTRA_DESTINATION_LONGI = "DestinationLongti"
        val EXTRA_DESTINATION_FLOOR_ID = "FloorIdentifier"
        var EXTRA_DESTINATION_FLOOR_LEVEL = "FloorLevel"
    }
}
enum class SITUM_STATE{
    INIT,
    OUTDOOR,
    BUILDING,
    POI,
    NAVIGATION

}
