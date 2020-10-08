package com.waterloo.wit

import android.content.Context
import androidx.multidex.MultiDex
import androidx.multidex.MultiDexApplication
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.squareup.picasso.OkHttp3Downloader
import com.squareup.picasso.Picasso
import com.waterloo.wit.db.DBHelper
import com.waterloo.wit.prefs.PrefsManager
import es.situm.sdk.SitumSdk

class MainApplication : MultiDexApplication() {
//    lateinit var dbHelper: DBHelper
    lateinit var prefs: PrefsManager
    var mDatabase: FirebaseFirestore? = null
    companion object{
        lateinit var instance: MainApplication
            private set
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        MultiDex.install(this)
    }
    fun enableOfflineCache(){
        mDatabase = FirebaseFirestore.getInstance()
        val settings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .build()
        mDatabase?.setFirestoreSettings(settings)
    }
    override fun onCreate() {
        super.onCreate()
        instance = this
        enableOfflineCache()

        prefs = PrefsManager(this)

        val built = Picasso.Builder(this).downloader(OkHttp3Downloader(this)).build()
        built.setIndicatorsEnabled(false)
        built.isLoggingEnabled = true
        Picasso.setSingletonInstance(built)

        /*
        // Local Storage
        dbHelper = DBHelper(this)

        dbHelper.readAllOrganization()
        val dbState = prefs.getDatabaseState()
        if(!dbState){
            dbHelper.initDatabaseWithInitialData()
            prefs.setDatabaseState(true)
        }

         */
        SitumSdk.init(this);
        SitumSdk.configuration().setApiKey("chris.gibson@publicsectorpartners.ca", "3b5c857e9803073d93dd99106f209162bbc24e9075e708bee75b4d31db447066");
//        SitumSdk.configuration().setApiKey("hymtmk@hotmail.com", "ba50d476a636438c0d215ff510ae365f6ebec8d4666c14b2c2ae1681c1e2d7d6");

    }

}