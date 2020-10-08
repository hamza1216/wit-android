package com.waterloo.wit.utils
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.waterloo.wit.R


object DialogUtil {
    @SuppressLint("InflateParams")
    fun showProgressDialog(context: Context, message: String) : AlertDialog? {
        try {
            val view = LayoutInflater.from(context).inflate(R.layout.layout_progress_bar_with_text, null)
            val textView = view.findViewById<TextView>(R.id.pbText)
            textView.text = message
            val dlg = AlertDialog.Builder(context)
                .setView(view)
                .setCancelable(false)
                .show()

            dlg.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            return dlg
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return null
    }
}