package com.waterloo.wit.ui

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log

import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast

import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserInfo
import com.google.firebase.firestore.FirebaseFirestore
import com.waterloo.wit.MainApplication
import com.waterloo.wit.R
import com.waterloo.wit.data.UserItem
import com.waterloo.wit.data.WorkItem
import com.waterloo.wit.fragment.CreateWorkItemFragment
import com.waterloo.wit.fragment.SearchWorkItemFragment
import com.waterloo.wit.helpers.WitHelper
import com.waterloo.wit.utils.LogUtils

class MainActivity : BaseActivity(), NavigationView.OnNavigationItemSelectedListener {
    private val TAG = MainActivity::class.java.simpleName

    companion object {
        private const val REQUEST_LOCATION_PERMISSION = 1
    }
    private val locationPermission = ACCESS_FINE_LOCATION

    private lateinit var toolbar: Toolbar
    private lateinit var drawer_layout: DrawerLayout
    private lateinit var navView: NavigationView

    private lateinit var txt_username: TextView
    private lateinit var txt_email: TextView

    private lateinit var userInfo: UserItem

    private lateinit var mAuth: FirebaseAuth
    private lateinit var mFirebaseFirestore: FirebaseFirestore

    private var buildingIdList = ArrayList<String>()
    //creating fragment object
    var fragment: Fragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        drawer_layout = findViewById(R.id.drawer_layout)
        navView = findViewById(R.id.nav_view)

        val toggle = ActionBarDrawerToggle(
            this, drawer_layout, toolbar, 0, 0
        )
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()
        navView.setNavigationItemSelectedListener(this)

        // select first menu
        val indexPage: Int = 0
        //navView.getMenu().getItem(indexPage).setChecked(true);
        onNavigationItemSelected(navView.getMenu().getItem(0));

        // Get Navigation Header
        val headerView: View = navView.getHeaderView(0)
        txt_username = headerView?.findViewById(R.id.txt_username)
        txt_email = headerView?.findViewById(R.id.txt_email)

        if(!checkLocationPermission()){
            requestLocationPermission()
        }

    }

    override fun onStart() {
        super.onStart()
        /*
        // Local Storage
        val userId = MainApplication.instance.prefs.getUserId()
        val userList = MainApplication.instance.dbHelper.readUser(userId!!)
        if(userList.count()>0){
            userInfo = userList.get(0)
            txt_username.text = userInfo.FirstName + " " + userInfo.LastName
            txt_email.text = userInfo.Email
        }
         */
        // Firebase
        mAuth = FirebaseAuth.getInstance()
        mFirebaseFirestore = FirebaseFirestore.getInstance()
        val userId = MainApplication.instance.prefs.getUserId()
        mFirebaseFirestore.collection("users").document(userId!!).get().addOnSuccessListener {
            val organization_uid = it.get("organization_id").toString()
            val email = it.get("email").toString()
            val firstname = it.get("firstname").toString()
            val lastname = it.get("lastname").toString()
            val username = firstname + " " + lastname
            txt_email.text = email
            txt_username.text = username

            val userId = MainApplication.instance.prefs.getUserId()
            userInfo = UserItem(userId!!, organization_uid, firstname, lastname, email, "")

            mFirebaseFirestore.collection("buildings").whereEqualTo("organization_id", organization_uid).get().addOnSuccessListener { documents ->
                buildingIdList.clear()
                for (document in documents){
                    buildingIdList.add(document.id)
                }
            }

        }.addOnFailureListener {

        }

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
//        menuInflater.inflate(R.menu.main, menu)
        return true
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return super.onOptionsItemSelected(item)
    }
    override fun onNavigationItemSelected(menuItem: MenuItem): Boolean {
        if(menuItem.itemId == R.id.nav_realtime_navigation){
            openRealTimeNavigationActivity()
        }
        else {
            displaySelectedScreen(menuItem.itemId)
        }
        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }
    override fun onBackPressed() {
        if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
            drawer_layout.closeDrawer(GravityCompat.START)
        } else {
            //super.onBackPressed()
        }
    }
    fun getUserInfo(): UserItem{
        return userInfo
    }

    fun getBuildingIdList(): ArrayList<String>{
        return buildingIdList
    }
    private fun checkLocationPermission(): Boolean {
        /*
        locationPermissions.forEach {
            if (ContextCompat.checkSelfPermission(context!!, it) != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        return true
         */
        return (ContextCompat.checkSelfPermission(this, locationPermission) == PackageManager.PERMISSION_GRANTED)
    }
    private fun requestLocationPermission() {
        // optional implementation of shouldShowRequestPermissionRationale
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, locationPermission)) {
                AlertDialog.Builder(this)
                    .setMessage("Need location permission to get current place")
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        // ActivityCompat.requestPermissions(activity!!, locationPermissions, REQUEST_LOCATION_PERMISSION)
                        requestPermissions(arrayOf(locationPermission), REQUEST_LOCATION_PERMISSION)
                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
            } else {
                // ActivityCompat.requestPermissions(activity!!, locationPermissions, REQUEST_LOCATION_PERMISSION)
                requestPermissions(arrayOf(locationPermission), REQUEST_LOCATION_PERMISSION)
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            } else {
                finish()
            }
        }
    }
    private fun displaySelectedScreen(itemId: Int) {
        //initializing the fragment object which is selected
        if(fragment != null && fragment is CreateWorkItemFragment){
            val createWorkItemFragment = fragment as CreateWorkItemFragment
            if(createWorkItemFragment.checkContentChanged()){
                WitHelper.showYesNowAlert(this, "Save changes?", DialogInterface.OnClickListener { dialog, which ->
                    createWorkItemFragment.saveWorkItem()
                }, DialogInterface.OnClickListener { dialog, which ->
                    when (itemId) {
                        R.id.nav_search_workitem -> fragment =
                            SearchWorkItemFragment.newInstance("", "")
                        R.id.nav_create_workitem -> {
                            fragment = CreateWorkItemFragment()
                            (fragment as CreateWorkItemFragment).createNewWorkItem()
                        }
                        R.id.nav_logout -> {
                            promptAlert()
                        }
                    }
                    transactFragment(fragment)
                })
                return
            }
        }
        when (itemId) {
            R.id.nav_search_workitem -> fragment =
                SearchWorkItemFragment.newInstance("", "")
            R.id.nav_create_workitem -> {
                fragment = CreateWorkItemFragment()
                (fragment as CreateWorkItemFragment).createNewWorkItem()
            }

            R.id.nav_logout -> {
                promptAlert()
            }
        }
        transactFragment(fragment)
    }
    fun switchToCreateWorkItem(){
        fragment = CreateWorkItemFragment()
        (fragment as CreateWorkItemFragment).createNewWorkItem()
        transactFragment(fragment)
    }
    fun switchToWorkItem(workItem: WorkItem){
        fragment = CreateWorkItemFragment()
        (fragment as CreateWorkItemFragment).loadWorkItem(workItem)
        transactFragment(fragment)
    }
    fun transactFragment(fragment: Fragment?){
        //replacing the fragment
        if (fragment != null) {
            val ft = supportFragmentManager.beginTransaction()
            ft.replace(R.id.content_frame, fragment)
            ft.commit()
        }
    }
    fun openRealTimeNavigationActivity(){
        val intent = Intent(this, RealTimeNavigationActivity::class.java)
        startActivity(intent)
    }

    fun promptAlert(){
        lateinit var dialog: AlertDialog
        // Initialize a new instance of alert dialog builder object
        val builder = AlertDialog.Builder(this)
        // Set a title for alert dialog
        builder.setTitle(this.getString(R.string.app_name))
        // Set a message for alert dialog
        builder.setMessage(this.getString(R.string.text_alert_confirm))
        // On click listener for dialog buttons
        val dialogClickListener = DialogInterface.OnClickListener{ _, which ->
            when(which){
                DialogInterface.BUTTON_POSITIVE -> { logoutActivity() }
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
    fun logoutActivity(){
        MainApplication.instance.prefs.logout()

        FirebaseAuth.getInstance().signOut()
        var intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }
}
