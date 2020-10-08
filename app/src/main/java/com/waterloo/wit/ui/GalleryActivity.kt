package com.waterloo.wit.ui

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.ContentValues
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import com.waterloo.wit.R
import com.waterloo.wit.adapter.GalleryImageAdapter
import com.waterloo.wit.adapter.GalleryImageClickListener
import com.waterloo.wit.adapter.Image
import com.waterloo.wit.data.WorkItemPhoto
import com.waterloo.wit.helpers.WitHelper
import com.waterloo.wit.utils.LogUtils
import kotlinx.android.synthetic.main.activity_gallery.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.URL


class GalleryActivity : BaseActivity(), GalleryImageClickListener {
    private val TAG = RealTimeNavigationActivity::class.java.simpleName
    // gallery column count
    private val SPAN_COUNT = 3

    private val PERMISSION_CODE = 1000;
    private val IMAGE_CAPTURE_CODE = 1001
    var image_uri: Uri? = null

    lateinit var galleryAdapter: GalleryImageAdapter
    private var  workItemId: String? = null


    private lateinit var mFirebaseFirestore: FirebaseFirestore
    private lateinit var mFirebaseStorage: FirebaseStorage
    private lateinit var mStorageRef: StorageReference
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gallery)

        //action bar
        val actionbar = supportActionBar
        actionbar!!.title = getString(R.string.text_gallery)
        actionbar.setDisplayHomeAsUpEnabled(true)

        mFirebaseFirestore = FirebaseFirestore.getInstance()
        mFirebaseStorage = FirebaseStorage.getInstance()
        mStorageRef = mFirebaseStorage.reference

        workItemId = intent.getStringExtra(EXTRA_WORKITEM_ID)
        // init adapter
        galleryAdapter = GalleryImageAdapter(WitHelper.imageList)
        galleryAdapter.listener = this
        // init recyclerview
        recyclerView.layoutManager = GridLayoutManager(this, SPAN_COUNT)
        recyclerView.adapter = galleryAdapter

        refreshGallery()
    }
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu to use in the action bar
        val inflater = menuInflater
        inflater.inflate(R.menu.activity_gallery_menu, menu)
        applyTintColor(menu, R.id.nav_takephoto)
        applyTintColor(menu, R.id.nav_delete)
        return super.onCreateOptionsMenu(menu)
    }
    fun applyTintColor(menu: Menu, id: Int){
        var drawable =
            menu.findItem(id).icon

        drawable = DrawableCompat.wrap(drawable!!)
        DrawableCompat.setTint(drawable, ContextCompat.getColor(this, R.color.white_color))
        menu.findItem(id).icon = drawable
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle presses on the action bar menu items
        when (item.itemId) {
            R.id.nav_takephoto -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                    if (checkSelfPermission(Manifest.permission.CAMERA)
                        == PackageManager.PERMISSION_DENIED ||
                        checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        == PackageManager.PERMISSION_DENIED){
                        //permission was not enabled
                        val permission = arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        //show popup to request permission
                        requestPermissions(permission, PERMISSION_CODE)
                    }
                    else{
                        //permission already granted
                        openCamera()
                    }
                }
                else{
                    //system os is < marshmallow
                    openCamera()
                }
                return true
            }
            R.id.nav_delete -> {
                val itemList = galleryAdapter.selectedItems
                if(itemList.count() == 0){
                    WitHelper.showAlert(this, "Select images to delete.")
                }
                else{
                    lateinit var dialog: AlertDialog
                    // Initialize a new instance of alert dialog builder object
                    val builder = AlertDialog.Builder(this)
                    // Set a title for alert dialog
                    builder.setTitle(this.getString(R.string.app_name))
                    // Set a message for alert dialog
                    builder.setMessage(this.getString(R.string.text_confirm_delete))
                    // On click listener for dialog buttons
                    val dialogClickListener = DialogInterface.OnClickListener{ _, which ->
                        when(which){
                            DialogInterface.BUTTON_POSITIVE -> {
                                /*
                                val dbHelper = MainApplication.instance.dbHelper
                                for (item in itemList){
                                    WitHelper.deleteFile(this, item.imageUrl)
                                    /*
                                    // Local Storage
                                    dbHelper.deleteWorkItemPhoto(item.photoId)
                                     */
                                    WitHelper.imageList.remove(item)
                                }
                                 */
                                for (item in itemList) {
                                    // delete local file
                                    WitHelper.deleteFile(this, item.imageUrl)
                                    // delete server side file
                                    if(WitHelper.isValidURL(item.storeUrl)) {
                                        val photoRef =
                                            mFirebaseStorage.getReferenceFromUrl(item.storeUrl)
                                        if (photoRef != null) {
                                            photoRef.delete().addOnSuccessListener {

                                            }.addOnFailureListener {

                                            }
                                        }
                                    }
                                    mFirebaseFirestore.collection("workitem_photos").document(item.photoId).delete()
                                    WitHelper.imageList.remove(item)
                                }
                                galleryAdapter.notifyDataSetChanged()

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
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }
    fun getExtension(uri: Uri): String?{
        return MimeTypeMap.getSingleton().getExtensionFromMimeType(contentResolver.getType(uri))
    }

    private fun openCamera() {
        val values = ContentValues()
        values.put(MediaStore.Images.Media.TITLE, "New Picture")
        values.put(MediaStore.Images.Media.DESCRIPTION, "From the Camera")
        image_uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        //camera intent
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, image_uri)
        startActivityForResult(cameraIntent, IMAGE_CAPTURE_CODE)
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        //called when user presses ALLOW or DENY from Permission Request Popup
        when(requestCode){
            PERMISSION_CODE -> {
                if (grantResults.size > 0 && grantResults[0] ==
                    PackageManager.PERMISSION_GRANTED){
                    //permission from popup was granted
                    openCamera()
                }
                else{
                    //permission from popup was denied
                    Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        //called when image was captured from camera intent
        if (resultCode == Activity.RESULT_OK){
            //set image captured to image view
            val workItem = WorkItemPhoto(WitHelper.generateUniqId(), workItemId!!, image_uri.toString())
            /*
            // Local Storage
            val dbHelper = MainApplication.instance.dbHelper
            if(dbHelper.insertWorkItemPhoto(workItem)){
                refreshGallery()
            }
             */

            val bitmap = getCapturedImage(image_uri!!);
            //val rotatedBitmap = rotateImageIfRequired(this, bitmap, image_uri!!);
            val boas = ByteArrayOutputStream()
            //rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, boas)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, boas)

            //var file = Uri.fromFile(File(image_uri.toString()))
            val fileName = System.currentTimeMillis().toString() + "." + getExtension(image_uri!!)
            val riversRef = mStorageRef.child("images/" + fileName)
            //val uploadTask = riversRef.putFile(image_uri!!)
            val uploadTask = riversRef.putBytes(boas.toByteArray())

            if(WitHelper.isNetworkAvailable(this))
                showProgressDialog()
            // Register observers to listen for when the download is done or if it fails
            uploadTask.addOnFailureListener {
                // Handle unsuccessful uploads
                hideProgressDialog()
                Log.e(TAG, it.message)
            }.addOnSuccessListener {
                hideProgressDialog()
                riversRef.downloadUrl.addOnSuccessListener {
                    Log.e(TAG, "Upload path: " + it.toString())
                    var urlPath = it.toString()
                    urlPath = urlPath.replace("%2F", "/")
                    val quoteMarkIndex = urlPath.indexOf('?')
                    val fileName = urlPath.substring(urlPath.lastIndexOf('/') + 1, quoteMarkIndex);
                    // check if it is registered before
                    mFirebaseFirestore.collection("workitem_photos").whereEqualTo("photo_path", fileName).get().addOnSuccessListener { documents ->
                        if(documents.size()>0){
                            for (document in documents){
                                val hashMap = HashMap<String, String>()
                                hashMap.put("photo_path", it.toString())
                                mFirebaseFirestore.collection("workitem_photos").document(document.id).set(hashMap, SetOptions.merge())
                                break
                            }
                        }
                        else{
                            // taskSnapshot.metadata contains file metadata such as size, content-type, etc.
                            val hashMap = HashMap<String, String>()
                            hashMap.put("workitem_id", workItemId!!)
                            hashMap.put("photo_path", it.toString())
                            hashMap.put("local_path", image_uri.toString())
                            mFirebaseFirestore.collection("workitem_photos").document(System.currentTimeMillis().toString()).set(hashMap)
                        }
                        refreshGallery()

                    }

                }
            }
            if(!WitHelper.isNetworkAvailable(this)){
                val hashMap = HashMap<String, String>()
                hashMap.put("workitem_id", workItemId!!)
                hashMap.put("photo_path", fileName)
                hashMap.put("local_path", image_uri.toString())
                mFirebaseFirestore.collection("workitem_photos").document().set(hashMap)
                refreshGallery()
            }
        }
    }
    private fun getCapturedImage(selectedPhotoUri: Uri): Bitmap {
        if (Build.VERSION.SDK_INT < 28)
            return MediaStore.Images.Media.getBitmap(
                this.contentResolver,
                selectedPhotoUri
            )
        else {
            val source = ImageDecoder.createSource(this.contentResolver, selectedPhotoUri)
            return ImageDecoder.decodeBitmap(source)
        }
    }

    fun rotateImage(img: Bitmap, degree: Int): Bitmap {
        val matrix = Matrix();
        matrix.postRotate(degree.toFloat());
        val rotatedImg = Bitmap.createBitmap(img, 0, 0, img.getWidth(), img.getHeight(), matrix, true);
        img.recycle();
        return rotatedImg;
    }
    fun rotateImageIfRequired(context: Context, img: Bitmap, selectedImage: Uri) : Bitmap {
        val input = context.getContentResolver().openInputStream(selectedImage);
        var ei: ExifInterface? = null
        if (Build.VERSION.SDK_INT > 23)
            ei = ExifInterface(input)
        else
            ei = ExifInterface(selectedImage.getPath());

        val orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 ->
                return rotateImage(img, 90);
            ExifInterface.ORIENTATION_ROTATE_180->
                return rotateImage(img, 180);
            ExifInterface.ORIENTATION_ROTATE_270->
                return rotateImage(img, 270);
            else->
                return img;
        }
    }

    fun refreshGallery(){
        /*
        // Local Storage
        val dbHelper = MainApplication.instance.dbHelper
        val workItemPhotos = dbHelper.readWorkItemPhoto(workItemId!!)
        for (item in workItemPhotos){
            val image = Image(item.Path, "", item.UID)
            WitHelper.imageList.add(image)
        }
        galleryAdapter.notifyDataSetChanged()

         */
        showProgressDialog()
        if(WitHelper.isNetworkAvailable(this)){
            mFirebaseFirestore.collection("workitem_photos").whereEqualTo("workitem_id", workItemId).get().addOnSuccessListener { documents->
                WitHelper.imageList.clear()
                for (document in documents) {
                    val path = document.data.get("local_path").toString()
                    val storepath = document.data.get("photo_path").toString()
                    val photo_uid = document.id
                    val image = Image(storepath, path, "", photo_uid)
                    WitHelper.imageList.add(image)
                }
                galleryAdapter.notifyDataSetChanged()
                hideProgressDialog()
            }
        }
        else{
            mFirebaseFirestore.collection("workitem_photos").whereEqualTo("workitem_id", workItemId).addSnapshotListener{ querySnapshot, firebaseFirestoreException ->
                WitHelper.imageList.clear()
                if(querySnapshot != null){
                    for (document in querySnapshot!!.documents) {
                        val path = document.data?.get("local_path").toString()
                        val storepath = document.data?.get("photo_path").toString()
                        val photo_uid = document.id
                        val image = Image(storepath, path, "", photo_uid)
                        WitHelper.imageList.add(image)
                    }
                    galleryAdapter.notifyDataSetChanged()
                }
                hideProgressDialog()
            }

        }
    }

    override fun onClick(position: Int) {
        // handle click of image
    }
    companion object{
        val EXTRA_WORKITEM_ID = "WorkItemId"
    }

}
