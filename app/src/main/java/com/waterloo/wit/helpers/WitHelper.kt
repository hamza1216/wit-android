package com.waterloo.wit.helpers

import android.app.AlertDialog
import android.content.ContentUris
import android.content.Context
import android.content.DialogInterface
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.api.Http
import com.waterloo.wit.R
import com.waterloo.wit.adapter.Image
import cz.msebera.android.httpclient.protocol.HTTP
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*


class WitHelper {
    companion object {
        var imageList = ArrayList<Image>()

        fun SetLinearLayoutManagerAndHairlineDividerFor(
            recyclerView: RecyclerView,
            context: Context,
            reversed: Boolean
        ) {
            if (recyclerView.itemDecorationCount > 0) {
                recyclerView.removeItemDecoration(recyclerView.getItemDecorationAt(0))
            }

            var linearLayoutManager = LinearLayoutManager(context)
            if(reversed)
                linearLayoutManager.reverseLayout = true
            recyclerView.layoutManager = linearLayoutManager

            var dividerItemDecoration = DividerItemDecoration(
                context,
                linearLayoutManager.getOrientation()
            )
            ContextCompat.getDrawable(
                context,
                R.drawable.cell_hairline_divider
            )?.let {
                dividerItemDecoration.setDrawable(it)
            }
            recyclerView.addItemDecoration(dividerItemDecoration)
        }
        fun getDayOfMonthSuffix(n: Int): String? {
            return if (n >= 11 && n <= 13) {
                "th"
            } else when (n % 10) {
                1 -> "st"
                2 -> "nd"
                3 -> "rd"
                else -> "th"
            }
        }
        fun getDateString(date: Date): String{
            var dateString = ""
            val sdf1 = SimpleDateFormat("EEE, MMM ")
            val sdf2 = SimpleDateFormat("dd")
            val sdf3 = SimpleDateFormat(" yyyy, HH:mm a")
            dateString = sdf1.format(date)
            dateString += sdf2.format(date)
            dateString += getDayOfMonthSuffix(sdf2.format(date).toInt())
            dateString += sdf3.format(date)
            return dateString
        }
        fun generateUniqId() : String{
            val charPool : List<Char> = ('0'..'4') + ('5'..'9')
            val randomString = (1..9)
                .map { i -> kotlin.random.Random.nextInt(0, charPool.size) }
                .map(charPool::get)
                .joinToString("");
            return randomString
        }
        fun getRedMarkerBitmap(context: Context): Bitmap{
            val height = 180
            val width = 120
            val bitmapdraw =
                ContextCompat.getDrawable(context, R.drawable.ic_red_marker) as BitmapDrawable
            val b = bitmapdraw.bitmap
            val smallMarker = Bitmap.createScaledBitmap(b, width, height, false)
            return smallMarker
        }
        fun showAlert(context: Context, message: String){
            val dialogBuilder = AlertDialog.Builder(context)

            // set message of alert dialog
            dialogBuilder.setMessage(message)
                // if the dialog is cancelable
                .setCancelable(false)
                // positive button text and action
                // negative button text and action
                .setNegativeButton("OK", DialogInterface.OnClickListener {
                        dialog, id -> dialog.cancel()
                })

            // create dialog box
            val alert = dialogBuilder.create()
            // set title for alert dialog box
            alert.setTitle(context.getString(R.string.app_name))
            // show alert dialog
            alert.show()
        }
        fun showDismissAlert(context: Context, message: String, listenerYes:DialogInterface.OnClickListener){
            val dialogBuilder = AlertDialog.Builder(context)

            // set message of alert dialog
            dialogBuilder.setMessage(message)
                // if the dialog is cancelable
                .setCancelable(false)
                // positive button text and action
                // negative button text and action
                .setNegativeButton("OK", listenerYes)

            // create dialog box
            val alert = dialogBuilder.create()
            // set title for alert dialog box
            alert.setTitle(context.getString(R.string.app_name))
            // show alert dialog
            alert.show()
        }
        fun showYesNowAlert(context: Context, message: String, listenerYes:DialogInterface.OnClickListener, listenerNo:DialogInterface.OnClickListener){
            val dialogBuilder = AlertDialog.Builder(context)

            // set message of alert dialog
            dialogBuilder.setMessage(message)
                // if the dialog is cancelable
                .setCancelable(false)
                // positive button text and action
                .setPositiveButton("Yes", listenerYes)
                // negative button text and action
                .setNegativeButton("No", listenerNo)

            // create dialog box
            val alert = dialogBuilder.create()
            // set title for alert dialog box
            alert.setTitle(context.getString(R.string.app_name))
            // show alert dialog
            alert.show()
        }
        fun deleteFile(context: Context, uri: String){
            val path = getPath(context, Uri.parse(uri))
            try {
                val file = File(path)
                //if (file.exists()) {
                    file.delete()
                //}
            }catch (e: Exception){
                e.printStackTrace()

            }
        }
        fun getPath(context: Context, uri: Uri): String? {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && DocumentsContract.isDocumentUri(context, uri)) { // DocumentProvider
                // ExternalStorageProvider
                if (isExternalStorageDocument(uri)) {
                    val docId = DocumentsContract.getDocumentId(uri)
                    val split = docId.split(":").toTypedArray()
                    val type = split[0]
                    if ("primary".equals(type, ignoreCase = true)) {
                        return Environment.getExternalStorageDirectory().toString() + "/" + split[1]
                    }
                    // TODO handle non-primary volumes
                } else if (isDownloadsDocument(uri)) {
                    var id = DocumentsContract.getDocumentId(uri)
                    if (id.startsWith("raw:")) {
                        id = id.replaceFirst("raw:".toRegex(), "")
                        return id
                    }
                    val contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"),
                        java.lang.Long.valueOf(id)
                    )
                    return getDataColumn(context, contentUri, null, null)
                } else if (isMediaDocument(uri)) {
                    val docId = DocumentsContract.getDocumentId(uri)
                    val split = docId.split(":").toTypedArray()
                    val type = split[0]
                    var contentUri: Uri? = null
                    if ("image" == type) {
                        contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    } else if ("video" == type) {
                        contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                    } else if ("audio" == type) {
                        contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                    }
                    val selection = "_id=?"
                    val selectionArgs =
                        arrayOf(split[1])
                    return getDataColumn(context, contentUri, selection, selectionArgs)
                }
            }
            else if ("content".equals(uri.scheme, ignoreCase = true))
            { // Return the remote address
                return if (isGooglePhotosUri(uri)) uri.lastPathSegment else getDataColumn(
                    context,
                    uri,
                    null,
                    null
                )
            } else if ("file".equals(uri.scheme, ignoreCase = true)) {
                return uri.path
            }
            return null
        }

        fun getDataColumn(
            context: Context,
            uri: Uri?,
            selection: String?,
            selectionArgs: Array<String>?
        ): String? {
            var cursor: Cursor? = null
            val column = "_data"
            val projection = arrayOf(column)
            try {
                cursor =
                    context.contentResolver.query(uri!!, projection, selection, selectionArgs, null)
                if (cursor != null && cursor.moveToFirst()) {
                    val index = cursor.getColumnIndexOrThrow(column)
                    return cursor.getString(index)
                }
            } finally {
                cursor?.close()
            }
            return null
        }

        fun isExternalStorageDocument(uri: Uri): Boolean {
            return "com.android.externalstorage.documents" == uri.authority
        }

        /**
         * @param uri The Uri to check.
         * @return Whether the Uri authority is DownloadsProvider.
         */
        fun isDownloadsDocument(uri: Uri): Boolean {
            return "com.android.providers.downloads.documents" == uri.authority
        }

        /**
         * @param uri The Uri to check.
         * @return Whether the Uri authority is MediaProvider.
         */
        fun isMediaDocument(uri: Uri): Boolean {
            return "com.android.providers.media.documents" == uri.authority
        }

        /**
         * @param uri The Uri to check.
         * @return Whether the Uri authority is Google Photos.
         */
        fun isGooglePhotosUri(uri: Uri): Boolean {
            return "com.google.android.apps.photos.content" == uri.authority
        }

        fun isValidURL(url: String): Boolean{
            try{
                URL(url).toURI()
                return true
            }
            catch (e: java.lang.Exception){

            }
            return false
        }
        fun isNetworkAvailable(context: Context): Boolean{
            var result = false
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                connectivityManager?.run {
                    connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)?.run {
                        result = when {
                            hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                            hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                            hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
                            else -> false
                        }
                    }
                }
            } else {
                connectivityManager?.run {
                    connectivityManager.activeNetworkInfo?.run {
                        if (type == ConnectivityManager.TYPE_WIFI) {
                            result = true
                        } else if (type == ConnectivityManager.TYPE_MOBILE) {
                            result = true
                        }
                    }
                }
            }
            return result
        }
        fun checkFirebaseConnection(context: Context): Boolean{
            if (isNetworkAvailable(context)) {
                val url = URL("http://firebase.google.com/")

                with(url.openConnection() as HttpURLConnection) {
                    requestMethod = "GET"  // optional default is GET

                    println("\nSent 'GET' request to URL : $url; Response Code : $responseCode")
                    return responseCode == 200
                }
            } else {
                Log.d("WitHelper", "No network available!")
            }
            return false
        }

    }
}