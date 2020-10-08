package com.waterloo.wit.ui

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.waterloo.wit.helpers.WitHelper
import com.waterloo.wit.utils.DialogUtil


open class BaseActivity : AppCompatActivity() {
    private var dlgProgress: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onStart() {
        super.onStart()

        if(!WitHelper.isNetworkAvailable(this)){
            Toast.makeText(this, "You are offline. Data will be synced with the cloud the next time you are connected to the internet.", Toast.LENGTH_LONG).show()
        }
    }
    fun showProgressDialog(message: String? = null) {
        dlgProgress?.dismiss()
        dlgProgress = this?.let { DialogUtil.showProgressDialog(it, message ?: "") }
    }

    fun hideProgressDialog() {
        dlgProgress?.dismiss()
    }
}