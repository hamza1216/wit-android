package com.waterloo.wit.fragment

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.NonNull
import androidx.core.content.ContextCompat
import androidx.core.os.HandlerCompat
import androidx.core.os.HandlerCompat.postDelayed
import androidx.fragment.app.Fragment
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.kunzisoft.switchdatetime.SwitchDateTimeDialogFragment
import com.kunzisoft.switchdatetime.SwitchDateTimeDialogFragment.OnButtonWithNeutralClickListener
import com.kunzisoft.switchdatetime.SwitchDateTimeDialogFragment.SimpleDateMonthAndDayFormatException
import com.waterloo.wit.MainApplication
import com.waterloo.wit.R
import com.waterloo.wit.data.BuildingItem
import com.waterloo.wit.data.UserItem
import com.waterloo.wit.data.WorkItem
import com.waterloo.wit.helpers.WitHelper
import com.waterloo.wit.ui.GalleryActivity
import com.waterloo.wit.ui.MainActivity
import com.waterloo.wit.ui.NavigationActivity2
import com.waterloo.wit.ui.RealTimeNavigationActivity
import com.waterloo.wit.utils.LogUtils
import es.situm.sdk.SitumSdk
import es.situm.sdk.error.Error
import es.situm.sdk.location.LocationListener
import es.situm.sdk.location.LocationManager
import es.situm.sdk.location.LocationRequest
import es.situm.sdk.location.LocationStatus
import es.situm.sdk.model.cartography.Building
import es.situm.sdk.model.cartography.Floor
import es.situm.sdk.model.location.Location
import es.situm.sdk.utils.Handler
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [CreateWorkItemFragment.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [CreateWorkItemFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class CreateWorkItemFragment : Fragment(), OnMapReadyCallback {
    // TODO: Rename and change types of parameters


    var initial_latitude  = 0.0
    var initial_longitude = 0.0

    var marker: Marker? = null
    private var param1: String? = null
    private var param2: String? = null

    private val TAG = CreateWorkItemFragment::class.java.name
    private val TAG_DATETIME_FRAGMENT = "TAG_DATETIME_FRAGMENT"

    private var dateTimeFragment: SwitchDateTimeDialogFragment? = null

    private var mapFragment: SupportMapFragment? = null
    private lateinit var mMap: GoogleMap

    private var locationManager: LocationManager? = null
    private var locationListener: LocationListener? = null

    private val buildingList: ArrayList<Building> = ArrayList()
    private val buildingNameList: ArrayList<String> = ArrayList()
    private val floorNameList: ArrayList<String> = ArrayList()
    private var floorList: ArrayList<Floor> = ArrayList()
    private var building: Building? = null

    private var userInfo: UserItem? = null
    private var currentWorkItem: WorkItem? = null
    private var buildingItem: BuildingItem? = null

    private lateinit var nav_navigation: LinearLayout
    private lateinit var nav_edit: LinearLayout
    private lateinit var nav_gallery: LinearLayout
    private lateinit var nav_save: LinearLayout
    private lateinit var nav_delete: LinearLayout

    private lateinit var workTitleEditText: EditText
    private lateinit var areaEditText: EditText
    private lateinit var workNoteEditText: EditText

    private lateinit var editDateTimeImageView: ImageView
    private var workIdTextView: TextView? = null
    private var dateTimeTextView: TextView? = null
    private lateinit var buildingSpinner: Spinner
    private lateinit var floorSpinner: Spinner
    private lateinit var progressBar: ProgressBar

    // Ground Overlay
    private var buildingGroundOverlay: GroundOverlay? = null

    private lateinit var mFirebaseFirestore: FirebaseFirestore
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val rootView = inflater.inflate(R.layout.fragment_create_work_item, container, false)

        mFirebaseFirestore = FirebaseFirestore.getInstance()
        readUserInfo()

        nav_navigation = rootView.findViewById(R.id.nav_navigation)
        nav_edit = rootView.findViewById(R.id.nav_edit)
        nav_gallery = rootView.findViewById(R.id.nav_gallery)
        nav_save = rootView.findViewById(R.id.nav_save)
        nav_delete = rootView.findViewById(R.id.nav_delete)

        workTitleEditText = rootView.findViewById(R.id.workTitleEditText)
        areaEditText = rootView.findViewById(R.id.areaEditText)
        workNoteEditText = rootView.findViewById(R.id.workNoteEditText)


        editDateTimeImageView = rootView.findViewById(R.id.editDateTimeImageView)
        workIdTextView = rootView.findViewById(R.id.workIdTextView)
        dateTimeTextView = rootView.findViewById(R.id.dateTimeTextView)
        buildingSpinner = rootView.findViewById(R.id.buildingSpinner)
        floorSpinner = rootView.findViewById(R.id.floorSpinner)
        //progressBar = rootView.findViewById(R.id.progressBar)

        mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment?.getMapAsync(this)

        locationManager = SitumSdk.locationManager()
        loadBuildings()
        return rootView
    }
    override fun onStart() {
        super.onStart()
        val mainActivity = activity as MainActivity
        mainActivity.supportActionBar?.title = "Work Item"
        setupUIControl()
        // init work item
        initWorkItem()
        if(checkContentChanged()){
            enableEditMode(true)
        }
        else{
            enableEditMode(false)
        }
    }
    fun readUserInfo(){
        val mainActivity = (activity as MainActivity)
        userInfo = mainActivity.getUserInfo()
    }
    fun setupUIControl(){
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val min = calendar.get(Calendar.MINUTE)
        editDateTimeImageView.setOnClickListener{
            dateTimeFragment?.startAtCalendarView()
            dateTimeFragment?.setDefaultDateTime(
                GregorianCalendar(
                    year,
                    month,
                    day,
                    hour,
                    min
                ).time
            )
            dateTimeFragment?.show(activity!!.supportFragmentManager, TAG_DATETIME_FRAGMENT)
        }

        nav_navigation.setOnClickListener{
            if(building == null)
                return@setOnClickListener
            if(!checkContentChanged()){
                val workTitle = workTitleEditText.text.toString()
                val intent = Intent(context, RealTimeNavigationActivity::class.java)
                intent.putExtra(RealTimeNavigationActivity.EXTRA_WORKITEM_TITLE, workTitle)
                intent.putExtra(RealTimeNavigationActivity.EXTRA_BUILDING, building)
                intent.putExtra(RealTimeNavigationActivity.EXTRA_DESTINATION_LATI, initial_latitude)
                intent.putExtra(RealTimeNavigationActivity.EXTRA_DESTINATION_LONGI, initial_longitude)

                val selectedFloor = floorList.get(floorSpinner.selectedItemPosition)
                intent.putExtra(RealTimeNavigationActivity.EXTRA_DESTINATION_FLOOR_ID, selectedFloor.identifier)
                intent.putExtra(RealTimeNavigationActivity.EXTRA_DESTINATION_FLOOR_LEVEL, selectedFloor.floor)
                startActivity(intent)
            }
            else{
                WitHelper.showYesNowAlert(context!!, "Save changes?", DialogInterface.OnClickListener { dialog, which ->
                    saveWorkItem()
                }, DialogInterface.OnClickListener { dialog, which ->

                })
            }
        }
        nav_edit.setOnClickListener{
            enableEditMode(true)
        }
        nav_gallery.setOnClickListener{
            if(!checkContentChanged()){
                val intent = Intent(context, GalleryActivity::class.java)
                intent.putExtra(GalleryActivity.EXTRA_WORKITEM_ID, currentWorkItem!!.UID)
                startActivity(intent)
            }
            else{
                WitHelper.showYesNowAlert(context!!, "Save changes?", DialogInterface.OnClickListener { dialog, which ->
                    saveWorkItem()
                }, DialogInterface.OnClickListener { dialog, which ->

                })
            }
        }
        nav_save.setOnClickListener{
            saveWorkItem()
        }
        nav_delete.setOnClickListener{
            /*
            if(checkWorkItem(currentWorkItem!!.UID)){
                alertDeleteItem()
            }
            else{
                WitHelper.showAlert(context!!, "No WorkItem to delete.")
            }
             */
            mFirebaseFirestore.collection("workitems").document(currentWorkItem!!.UID).get().addOnSuccessListener {
                if(it.exists()) {
                    alertDeleteItem()
                }
                else{
                    WitHelper.showAlert(context!!, "No WorkItem to delete.")
                }
            }.addOnFailureListener {
                WitHelper.showAlert(context!!, "No WorkItem to delete.")
            }
        }
        initSwitchDateTimePicker()
    }
    fun saveWorkItem(){
        val workTitle = workTitleEditText.text.toString()
        val workTime = dateTimeTextView?.text.toString()
        val area = areaEditText.text.toString()
        val notes = workNoteEditText.text.toString()
        val gps_latitude = initial_latitude.toString()
        val gps_longitude = initial_longitude.toString()

//            currentWorkItem = WorkItem(workItemId!!, buildingItem!!.UID, workTitle, workTime, area, notes, gps_location, userId!!)
        currentWorkItem?.WorkTitle = workTitle
        currentWorkItem?.Building_UID = building?.identifier!!
        currentWorkItem?.Date_Time = workTime
        currentWorkItem?.Area = area
        currentWorkItem?.Notes = notes
        currentWorkItem?.GPS_Latitude = gps_latitude
        currentWorkItem?.GPS_Longitude = gps_longitude
        currentWorkItem?.FloorLevel = floorSpinner.selectedItemPosition + 1
        currentWorkItem?.UserId = userInfo!!.UID

        if(workTitle.isEmpty() || area.isEmpty() || notes.isEmpty()){
            WitHelper.showAlert(context!!, "Enter the empty fields")
            return
        }
        val hashMap = HashMap<String, String>()
        hashMap.put("worktitle", currentWorkItem!!.WorkTitle)
        hashMap.put("buildingid", currentWorkItem!!.Building_UID)
        hashMap.put("datetime", currentWorkItem!!.Date_Time)
        hashMap.put("area", currentWorkItem!!.Area)
        hashMap.put("note", currentWorkItem!!.Notes)
        hashMap.put("latitude", currentWorkItem!!.GPS_Latitude)
        hashMap.put("longitude", currentWorkItem!!.GPS_Longitude)
        hashMap.put("floorlevel", currentWorkItem!!.FloorLevel.toString())
        hashMap.put("userid", currentWorkItem!!.UserId)

        mFirebaseFirestore.collection("workitems").document(currentWorkItem!!.UID).set(hashMap, SetOptions.merge())
        enableEditMode(false)

        /*
        //Local Storage
        val dbHelper = MainApplication.instance.dbHelper
        if(checkWorkItem(currentWorkItem!!.UID)){
            if(dbHelper.updateWorkItem(currentWorkItem!!)){
                WitHelper.showAlert(context!!, "WorkItem saved")
                enableEditMode(false)
            }
            else{
                WitHelper.showAlert(context!!, "Unable to update your workitem")
            }
        }
        else{
            if(dbHelper.insertWorkItem(currentWorkItem!!)){
                WitHelper.showAlert(context!!, "WorkItem saved")
                enableEditMode(false)
            }
            else{
                WitHelper.showAlert(context!!, "Unable to update your workitem")
            }
        }
         */

    }
    fun initWorkItem(){
        this.workIdTextView?.text = currentWorkItem?.UID
        this.workTitleEditText?.setText(currentWorkItem?.WorkTitle)
        this.dateTimeTextView?.setText(currentWorkItem?.Date_Time)
        this.areaEditText?.setText(currentWorkItem?.Area)
        this.workNoteEditText?.setText(currentWorkItem?.Notes)
        initial_latitude = currentWorkItem?.GPS_Latitude!!.toDouble()
        initial_longitude = currentWorkItem?.GPS_Longitude!!.toDouble()

    }
    fun createNewWorkItem(){
        val workItemId = WitHelper.generateUniqId()
        val dateTime = WitHelper.getDateString(Calendar.getInstance().time)
        currentWorkItem = WorkItem(workItemId, "", "", dateTime, "", "", "0.0", "0.0", 0, "")

    }
    fun loadWorkItem(workItem: WorkItem){
        currentWorkItem = workItem
    }
    override fun onMapReady(map: GoogleMap?) {
        System.err.println("OnMapReady start")
        mMap = map as GoogleMap;

    }
    private fun setupLocationManager() {
        startLocation()
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
                var centerLatLang = LatLng(initial_latitude, initial_longitude)

                if(latLngBounds.contains(latLng)) {
                    initial_latitude = latLng.latitude
                    initial_longitude = latLng.longitude
                    centerLatLang = LatLng(initial_latitude, initial_longitude)
                    drawDestinationMarker(centerLatLang, true)
                }
                else{
                    initial_latitude = coordinateSW.latitude + (coordinateNE.latitude - coordinateSW.latitude) / 2
                    initial_longitude = coordinateSW.longitude +(coordinateNE.longitude - coordinateSW.longitude) / 2
                    centerLatLang = LatLng(initial_latitude, initial_longitude)
                    drawDestinationMarker(centerLatLang, false)
                }
//                mMap?.animateCamera(CameraUpdateFactory.newLatLngBounds(latLngBounds, 100))
                mMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(centerLatLang, 100.0f))
                // add new marker
                stopLocation()
            }

            override fun onStatusChanged(@NonNull locationStatus: LocationStatus) {
                //statusTV.setText("Status: " + locationStatus.name())
            }

            override fun onError(@NonNull error: Error) {
                Toast.makeText(context, error.message, Toast.LENGTH_LONG)
                    .show()
            }
        }
        val locationRequest = LocationRequest.Builder()
            .useWifi(true)
            .useBle(true)
            .useForegroundService(true)
            .build()
        locationManager?.requestLocationUpdates(locationRequest, locationListener!!)
    }
    private fun stopLocation() {
        if (!locationManager!!.isRunning) {
            marker?.remove()
            return
        }
        locationManager!!.removeUpdates(locationListener!!)

    }
    fun drawDestinationMarker(latLng: LatLng, realPos: Boolean){
        marker?.remove()
        if(context != null) {
            marker = mMap.addMarker(
                MarkerOptions().position(latLng).icon(
                    BitmapDescriptorFactory.fromBitmap(WitHelper.getRedMarkerBitmap(context!!))
                ).rotation(180.0f)
            )
            marker?.isDraggable = buildingSpinner.isEnabled
        }
        if(!realPos)
            marker?.setAnchor(0.5f, 0.5f)
    }
    fun drawBuilding(bitmap: Bitmap) {
        val drawBounds = building!!.bounds
        val coordinateNE = drawBounds.northEast
        val coordinateSW = drawBounds.southWest
        val latLngBounds = LatLngBounds(
            LatLng(coordinateSW.latitude, coordinateSW.longitude),
            LatLng(coordinateNE.latitude, coordinateNE.longitude)
        )
        mMap.setOnMarkerDragListener(object : GoogleMap.OnMarkerDragListener {
            override fun onMarkerDragEnd(p0: Marker?) {
                if(p0 != null){
                    initial_latitude =  p0.position.latitude
                    initial_longitude = p0.position.longitude
                }
            }

            override fun onMarkerDragStart(p0: Marker?) {
                marker?.setAnchor(0.5f, 0.5f)
            }

            override fun onMarkerDrag(p0: Marker?) {
                val dragPos = p0?.position

            }
        })

        buildingGroundOverlay?.remove()
        buildingGroundOverlay = mMap?.addGroundOverlay(
            GroundOverlayOptions()
                .image(BitmapDescriptorFactory.fromBitmap(bitmap))
                .bearing(building!!.rotation.degrees().toFloat())
                .positionFromBounds(latLngBounds)
        )
        // check if saved position data exist
        if(initial_latitude == 0.0 && initial_longitude == 0.0){
            // Setup Location Manager
            setupLocationManager()
        }
        else{
            val currnetPos = LatLng(initial_latitude, initial_longitude);

            if(latLngBounds.contains(currnetPos)) {
//                mMap?.animateCamera(CameraUpdateFactory.newLatLngBounds(latLngBounds, 100))
                mMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(currnetPos, 100.0f))
                drawDestinationMarker(currnetPos, false)
            }
            else{
                stopLocation()
                setupLocationManager()
            }
        }

    }
    fun fetchFirstFloorImage(building: Building, callback: Callback) {
        SitumSdk.communicationManager().fetchFloorsFromBuilding(
            building,
            object : Handler<Collection<Floor>> {
                override fun onSuccess(floorsCollection: Collection<Floor>) {
                    floorList = ArrayList(floorsCollection)
                    floorNameList.clear()
                    var floorIndex = 0
                    for (item in floorList){
                        floorIndex++
                        floorNameList.add(floorIndex.toString())
                    }

                    val adapter = ArrayAdapter(
                        context!!,
                        R.layout.support_simple_spinner_dropdown_item,
                        floorNameList
                    )
                    floorSpinner.adapter = adapter
                    floorSpinner.setSelection(currentWorkItem?.FloorLevel!!-1)
                    floorSpinner?.setOnItemSelectedListener(object : AdapterView.OnItemSelectedListener {
                        override fun onNothingSelected(parent: AdapterView<*>?) {
                            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
                        }

                        override fun onItemSelected(
                            parent: AdapterView<*>?,
                            view: View?,
                            position: Int,
                            id: Long
                        ) {
                            // fetch building
                            val selectedFloor = floorList.get(position)

                            SitumSdk.communicationManager().fetchMapFromFloor(
                                selectedFloor,
                                object : Handler<Bitmap> {
                                    override fun onSuccess(bitmap: Bitmap) {
                                        callback.onSuccess(bitmap)
                                    }

                                    override fun onFailure(error: Error) {
                                        callback.onError(error)
                                    }
                                })
                        }

                    })


                }

                override fun onFailure(error: Error) {
                    callback.onError(error)
                }
            })
    }

    private fun alertDeleteItem(){
        lateinit var dialog: AlertDialog
        // Initialize a new instance of alert dialog builder object
        val builder = AlertDialog.Builder(context)
        // Set a title for alert dialog
        builder.setTitle(this.getString(R.string.app_name))
        // Set a message for alert dialog
        builder.setMessage(this.getString(R.string.text_confirm_delete))
        // On click listener for dialog buttons
        val dialogClickListener = DialogInterface.OnClickListener{ _, which ->
            when(which){
                DialogInterface.BUTTON_POSITIVE -> {
                    /*
                    // Local Storage
                    val dbHelper = MainApplication.instance.dbHelper
                    // get all photo files
                    val allPhotoList = dbHelper.readWorkItemPhoto(currentWorkItem!!.UID)
                    if(allPhotoList.count()>0){
                        for(item in allPhotoList){
                            WitHelper.deleteFile(context!!, item.Path)
                        }
                    }
                    // delete photo from database
                    dbHelper.deleteAllPhotohWorkItemId(currentWorkItem!!.UID)
                    // delete workitem
                    dbHelper.deleteWorkItem(currentWorkItem!!.UID)
                    // new workitem
                    val activity = activity as MainActivity
                    activity.switchToCreateWorkItem()
                     */
                    mFirebaseFirestore.collection("workitems").document(currentWorkItem!!.UID).delete().addOnSuccessListener {

                    }
                }
                DialogInterface.BUTTON_NEGATIVE -> {}
            }
        }
        // Set the alert dialog positive/yes button
        builder.setPositiveButton("YES",dialogClickListener)
        // Set the alert dialog negative/no button
        builder.setNegativeButton("NO",dialogClickListener)
        // Initialize the AlertDialog using builder object
        dialog = builder.create()
        // Finally, display the alert dialog
        dialog.show()
    }

    private fun checkWorkItem(workId: String): Boolean{
/*        val dbHelper = MainApplication.instance.dbHelper
        val workItemList = dbHelper.readWorkItem(currentWorkItem!!.UID ?: "")
        if(workItemList.count()>0)
            return true
 */
        return false
    }
    /*
    private fun saveBuildingToDatabase(building: Building){
        val dbHelper = MainApplication.instance.dbHelper
        val buildingItem = BuildingItem(building.identifier, userInfo?.Organization_UID!!, building.name, building.address)
        if(!dbHelper.insertBuilding(buildingItem)){
            dbHelper.updateBuildingItem(buildingItem)
        }
    }
     */
    private fun loadBuildings() {
        buildingSpinner.visibility = View.GONE
        floorSpinner.visibility = View.GONE
        //progressBar.visibility = View.VISIBLE
        val mainActivity = activity as MainActivity
        mainActivity.showProgressDialog("Loading...")
        // Get Building info from database
        val buildingIdList = (activity as MainActivity).getBuildingIdList()

        SitumSdk.communicationManager().fetchBuildings(object :
            Handler<Collection<Building>> {
            override fun onSuccess(buildings: Collection<Building>) {
                buildingSpinner.visibility = View.VISIBLE
                floorSpinner.visibility = View.VISIBLE
                //progressBar.setVisibility(View.GONE)
                mainActivity.hideProgressDialog()
                //building list
                buildingList.clear()
                for (building in buildings){
                    for(buildingId in buildingIdList){
                        if (building.identifier == buildingId){
                            buildingList.add(building)
                            break
                        }
                    }
                }
                // bulding name list
                buildingNameList.clear()
                // saved building pos
                var buildingPos = 0
                for((index, item) in buildingList.withIndex()){
                    //saveBuildingToDatabase(item)
                    buildingNameList.add(item.name)
                    if(!currentWorkItem?.Building_UID!!.isEmpty()){
                        if(item.identifier == currentWorkItem?.Building_UID!!){
                            buildingPos = index
                        }
                    }
                }
                if(context != null){
                    val adapter = ArrayAdapter(
                        context!!,
                        R.layout.support_simple_spinner_dropdown_item,
                        buildingNameList
                    )
                    buildingSpinner.adapter = adapter
                    buildingSpinner.setSelection(buildingPos)
                    buildingSpinner?.setOnItemSelectedListener(object : AdapterView.OnItemSelectedListener {
                        override fun onNothingSelected(parent: AdapterView<*>?) {
                            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
                        }
                        override fun onItemSelected(
                            parent: AdapterView<*>?,
                            view: View?,
                            position: Int,
                            id: Long
                        ) {
                            // fetch building
                            building = buildingList.get(position)
                            fetchFirstFloorImage(building!!, object : Callback {
                                override fun onSuccess(floorImage: Bitmap) {
                                    drawBuilding(floorImage)
                                }
                                override fun onError(error: Error) {

                                }
                            })
                        }

                    })
                }

            }

            override fun onFailure(error: Error) {
                //progressBar.visibility = View.GONE
                mainActivity.hideProgressDialog()
                Toast.makeText(context,"Failed to load buildings", Toast.LENGTH_SHORT).show()

            }
        })

    }
    fun initSwitchDateTimePicker(){
//        dateTimeFragment = activity?.getSupportFragmentManager()?.findFragmentByTag(TAG_DATETIME_FRAGMENT) as SwitchDateTimeDialogFragment
        if(dateTimeFragment == null){
            dateTimeFragment = SwitchDateTimeDialogFragment.newInstance(
                getString(R.string.label_datetime_dialog),
                getString(android.R.string.ok),
                getString(android.R.string.cancel))
        }
        dateTimeFragment?.setTimeZone(TimeZone.getDefault())

        // Assign unmodifiable values
        dateTimeFragment?.set24HoursMode(false)
        dateTimeFragment?.setHighlightAMPMSelection(false)
        dateTimeFragment?.minimumDateTime = GregorianCalendar(2015, Calendar.JANUARY, 1).time
        dateTimeFragment?.maximumDateTime = GregorianCalendar(2025, Calendar.DECEMBER,31).time

        // Define new day and month format
        try {
            dateTimeFragment!!.simpleDateMonthAndDayFormat = SimpleDateFormat(
                "MMMM dd",
                Locale.getDefault()
            )
        } catch (e: SimpleDateMonthAndDayFormatException) {
            LogUtils.e(TAG, e.message)
        }

        dateTimeFragment!!.setOnButtonClickListener(object : OnButtonWithNeutralClickListener {
            override fun onPositiveButtonClick(date: Date?) {
                dateTimeTextView?.setText(WitHelper.getDateString(date!!))
            }

            override fun onNegativeButtonClick(date: Date?) {
                // Do nothing
            }

            override fun onNeutralButtonClick(date: Date?) {
                // Optional if neutral button does'nt exists
                //dateTimeTextView.setText("")
            }
        })
    }
    fun checkContentChanged(): Boolean{
        val workTitle = workTitleEditText.text.toString()
        val workTime = dateTimeTextView?.text.toString()
        val area = areaEditText.text.toString()
        val notes = workNoteEditText.text.toString()
        val gps_latitude = initial_latitude.toString()
        val gps_longitude = initial_longitude.toString()
        if(workTitle.isEmpty() && area.isEmpty() && notes.isEmpty())
            return true
        if(currentWorkItem?.WorkTitle == workTitle && currentWorkItem?.Date_Time == workTime && currentWorkItem?.Area == area && currentWorkItem?.Notes == notes && currentWorkItem?.GPS_Latitude == gps_latitude && currentWorkItem?.GPS_Longitude == gps_longitude)
            return false
        return true
    }
    fun enableEditMode(enable: Boolean){
        if(enable){
            workTitleEditText.isEnabled = true
            areaEditText.isEnabled = true
            editDateTimeImageView.visibility = View.VISIBLE
            workNoteEditText.isEnabled = true
            nav_edit.setBackgroundColor(ContextCompat.getColor(context!!, R.color.colorPrimaryDark))
        }
        else{
            workTitleEditText.isEnabled = false
            areaEditText.isEnabled = false
            editDateTimeImageView.visibility = View.INVISIBLE
            workNoteEditText.isEnabled = false
            nav_edit.setBackgroundColor(ContextCompat.getColor(context!!, R.color.colorPrimary))
        }
        buildingSpinner.isEnabled = enable
        floorSpinner.isEnabled = enable
        marker?.isDraggable = enable

    }
    interface Callback {
        fun onSuccess(floorImage: Bitmap)
        fun onError(error: Error)
    }
    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment CreateWorkItemFragment.
         */
        // TODO: Rename and change types and number of parameters
        var instance: CreateWorkItemFragment? = null
        @JvmStatic
        fun newInstance(param1: String, param2: String): CreateWorkItemFragment{
            if(instance == null){
                instance = CreateWorkItemFragment()
                    instance?.apply {
                    arguments = Bundle().apply {
                        putString(ARG_PARAM1, param1)
                        putString(ARG_PARAM2, param2)
                    }
                }

            }
            return instance!!
        }
    }
}
