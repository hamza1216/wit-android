package com.waterloo.wit.ui

import android.os.Bundle
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentActivity
import com.google.firebase.firestore.FirebaseFirestore
import com.waterloo.wit.R
import com.waterloo.wit.adapter.RoomInfoAdapter
import com.waterloo.wit.data.RoomInfoItem
import com.waterloo.wit.helpers.WitHelper
import com.waterloo.wit.utils.LogUtils
import es.situm.sdk.model.cartography.Building
import es.situm.sdk.model.cartography.Geofence
import org.apache.poi.hssf.usermodel.HSSFCell
import org.apache.poi.hssf.usermodel.HSSFRow
import org.apache.poi.hssf.usermodel.HSSFSheet
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import org.apache.poi.poifs.filesystem.POIFSFileSystem
import org.apache.poi.ss.usermodel.Row
import java.io.InputStream


class RoomInformationActivity : AppCompatActivity() {
    private val TAG = RoomInformationActivity::class.java.simpleName

    private lateinit var roomInfoListView: ListView
    private lateinit var roomInfoTextView: TextView

    private lateinit var geofence: Geofence
    private lateinit var building: Building

    private var roomInfoList = ArrayList<RoomInfoItem>()
    private var adapter: RoomInfoAdapter? = null

    private lateinit var mFirebaseFirestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_room_information)

        roomInfoListView = findViewById(R.id.roomInfoListView)
        roomInfoTextView = findViewById(R.id.roomInfoTextView)

        building = intent.getParcelableExtra(EXTRA_BUILDING)
        geofence = intent.getParcelableExtra(EXTRA_GEOFENCEE)

        roomInfoTextView.text = building.name + " / " + geofence.name
        //readExcelFileFromAssets()
        mFirebaseFirestore = FirebaseFirestore.getInstance()
        readRoomInfo()


    }
    fun readRoomInfo(){
        if(WitHelper.isNetworkAvailable(this)) {
            mFirebaseFirestore.collection("rooms").whereEqualTo("room", geofence.name).get()
                .addOnSuccessListener { documents ->
                    for (document in documents) {
                        val description1 = document.data.get("description_level2").toString()
                        val description2 = document.data.get("description_level3").toString()
                        val uom = document.data.get("uom").toString()
                        val amount = document.data.get("quantity").toString()
                        roomInfoList.add(
                            RoomInfoItem(
                                building.name,
                                geofence.name,
                                description1,
                                description2,
                                uom,
                                amount
                            )
                        )
                    }
                    adapter = RoomInfoAdapter(this, roomInfoList)
                    roomInfoListView.adapter = adapter
                    adapter?.notifyDataSetChanged()
                }
        }
        else{
            mFirebaseFirestore.collection("rooms").whereEqualTo("room", geofence.name)
                .addSnapshotListener{ querySnapshot, firebaseFirestoreException ->
                    if(querySnapshot != null){
                        for (document in querySnapshot!!.documents) {
                            val description1 = document.data?.get("description_level2").toString()
                            val description2 = document.data?.get("description_level3").toString()
                            val uom = document.data?.get("uom").toString()
                            val amount = document.data?.get("quantity").toString()
                            roomInfoList.add(
                                RoomInfoItem(
                                    building.name,
                                    geofence.name,
                                    description1,
                                    description2,
                                    uom,
                                    amount
                                )
                            )
                        }
                        adapter = RoomInfoAdapter(this, roomInfoList)
                        roomInfoListView.adapter = adapter
                        adapter?.notifyDataSetChanged()
                    }
                }
        }
    }
    fun readExcelFileFromAssets() {
        try {
            val myInput: InputStream
            // initialize asset manager
            val assetManager = assets
            //  open excel sheet
            myInput = assetManager.open("fenceData.xls")
            // Create a POI File System object
            val myFileSystem = POIFSFileSystem(myInput)
            // Create a workbook using the File System
            val myWorkBook = HSSFWorkbook(myFileSystem)
            // Get the first sheet from workbook
            val mySheet: HSSFSheet = myWorkBook.getSheetAt(0)
            // We now need something to iterate through the cells.
            val rowIter: Iterator<Row> = mySheet.rowIterator()
            var rowno = 0

            while (rowIter.hasNext()) {
                LogUtils.e(TAG, " row no $rowno")
                val myRow = rowIter.next() as HSSFRow
                if (rowno != 0) {
                    val cellIter =
                        myRow.cellIterator()
                    var colno = 0
                    var building = ""
                    var room = ""
                    var description1 = ""
                    var description2 = ""
                    var uom = ""
                    var amount = ""
                    while (cellIter.hasNext()) {
                        val myCell = cellIter.next() as HSSFCell
                        if (colno == 0) {
                            building = myCell.toString()
                        } else if (colno == 1) {
                            room = myCell.toString()
                        } else if (colno == 2) {
                            description1 = myCell.toString()
                        }
                        else if (colno == 3) {
                            description2 = myCell.toString()
                        }
                        else if (colno == 4) {
                            uom = myCell.toString()
                        }
                        else if (colno == 5) {
                            amount = myCell.toString()
                        }
                        colno++
                        LogUtils.e(
                            TAG,
                            " Index :" + myCell.columnIndex + " -- " + myCell.toString()
                        )
                    }
                    if(geofence.name.equals(room))
                        roomInfoList.add(RoomInfoItem(building, room, description1, description2, uom, amount))
                }
                rowno++
            }
        } catch (e: Exception) {
            LogUtils.e(TAG, "error $e")
        }
    }
    companion object{
        val EXTRA_BUILDING = "Building"
        val EXTRA_GEOFENCEE = "Geofence"
    }
}
