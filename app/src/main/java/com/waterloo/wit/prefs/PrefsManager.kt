package com.waterloo.wit.prefs

import android.content.Context
import android.content.SharedPreferences

class PrefsManager(context: Context){
    private var PRIVATE_MODE = 0
    private val PREF_NAME = "wit-welcome"

    private val KEY_STARTDATE = "start_date"
    private val KEY_ENDDATE = "end_date"

    private val KEY_USERID = "userid"
    private val KEY_EMAIL = "email_address"
    private val KEY_PASSWORD = "userpassword"
    private val KEY_DATABASE_REGISTERED = "database_registered"

    private val preferences: SharedPreferences = context.getSharedPreferences(PREF_NAME, PRIVATE_MODE)

    fun setDatabaseState(value: Boolean){
        val  editor: SharedPreferences.Editor = preferences.edit()
        editor.putBoolean(KEY_DATABASE_REGISTERED, value)
        editor.commit()
    }
    fun getDatabaseState(): Boolean{
        return preferences.getBoolean(KEY_DATABASE_REGISTERED, false)
    }
    fun setEmail(value: String) {
        val editor: SharedPreferences.Editor = preferences.edit()
        editor.putString(KEY_EMAIL, value)
        editor.commit()
    }
    fun getEmail(): String? {
        return preferences.getString(KEY_EMAIL, "")
    }
    fun setPassword(value: String) {
        val editor: SharedPreferences.Editor = preferences.edit()
        editor.putString(KEY_PASSWORD, value)
        editor.commit()
    }
    fun getPassword(): String? {
        return preferences.getString(KEY_PASSWORD, "")
    }
    fun setUserId(value: String) {
        val editor: SharedPreferences.Editor = preferences.edit()
        editor.putString(KEY_USERID, value)
        editor.commit()
    }
    fun getUserId(): String? {
        return preferences.getString(KEY_USERID, "")
    }
    fun logout(){
        val editor: SharedPreferences.Editor = preferences.edit()
        editor.putString(KEY_USERID, "")
        editor.putString(KEY_EMAIL, "")
        editor.commit()

    }
    fun clear() {
        val editor: SharedPreferences.Editor = preferences.edit()
        //sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        editor.clear()
        editor.commit()
    }
}