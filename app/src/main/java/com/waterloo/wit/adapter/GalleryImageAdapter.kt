package com.waterloo.wit.adapter

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.squareup.picasso.Callback
import com.squareup.picasso.NetworkPolicy
import com.squareup.picasso.Picasso
import com.waterloo.wit.R
import com.waterloo.wit.helpers.WitHelper
import com.waterloo.wit.ui.LoginActivity
import com.waterloo.wit.ui.TouchImageViewActivity
import kotlinx.android.synthetic.main.item_gallery_image.view.*
import java.io.File
import java.lang.Exception

class GalleryImageAdapter(private val itemList: List<Image>) : RecyclerView.Adapter<GalleryImageAdapter.ViewHolder>() {
    private var context: Context? = null
    var listener: GalleryImageClickListener? = null
    val selectedItems = ArrayList<Image>()
    var selectMode = false
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GalleryImageAdapter.ViewHolder {
        context = parent.context
        val view = LayoutInflater.from(parent.context).inflate(
            R.layout.item_gallery_image, parent,
            false)
        return ViewHolder(view)
    }
    override fun getItemCount(): Int {
        return itemList.size
    }
    override fun onBindViewHolder(holder: GalleryImageAdapter.ViewHolder, position: Int) {
        holder.bind(itemList.get(position))
    }
    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun selectItem(image: Image){
            if(selectedItems.contains(image)){
                itemView.ivCheckImage.visibility = View.GONE
                selectedItems.remove(image)
            }
            else{
                itemView.ivCheckImage.visibility = View.VISIBLE
                selectedItems.add(image)
            }
            if(selectedItems.isEmpty())
                selectMode = false
        }
        fun bind(image: Image) {
            if(selectedItems.contains(image)){
                itemView.ivCheckImage.visibility = View.VISIBLE
            }
            else{
                itemView.ivCheckImage.visibility = View.GONE
            }
            // load image
            Glide.with(context!!)
                .asBitmap()
                .load(image.imageUrl)
                .centerCrop()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(object : CustomTarget<Bitmap>() {
                    override fun onResourceReady(
                        resource: Bitmap,
                        transition: Transition<in Bitmap>?
                    ) {
                        itemView.ivGalleryImage.setImageBitmap(resource)
                    }

                    override fun onLoadCleared(placeholder: Drawable?) {
                        // this is called when imageView is cleared on lifecycle call or for
                        // some other reason.
                        // if you are referencing the bitmap somewhere else too other than this imageView
                        // clear it here as you can no longer have the bitmap
                    }
                })

            Glide.with(context!!)
                .asBitmap()
                .load(image.storeUrl)
                .centerCrop()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(object : CustomTarget<Bitmap>() {
                    override fun onResourceReady(
                        resource: Bitmap,
                        transition: Transition<in Bitmap>?
                    ) {
                        itemView.ivGalleryImage.setImageBitmap(resource)
                    }

                    override fun onLoadCleared(placeholder: Drawable?) {
                        // this is called when imageView is cleared on lifecycle call or for
                        // some other reason.
                        // if you are referencing the bitmap somewhere else too other than this imageView
                        // clear it here as you can no longer have the bitmap
                    }
                })

            /*
            Picasso.get().load(image.storeUrl).networkPolicy(NetworkPolicy.OFFLINE).into(itemView.ivGalleryImage, object: com.squareup.picasso.Callback {
                override fun onSuccess() {
                    //set animations here

                }
                override fun onError(e: Exception?) {
                    Glide.with(context!!)
                        .load(image.imageUrl)
                        .centerCrop()
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .into(itemView.ivGalleryImage)

                }
            })
             */
            // adding click or tap handler for our image layout
            itemView.container.setOnClickListener {
                listener?.onClick(adapterPosition)
                if(selectMode)
                    selectItem(image)
                else{
                    var intent = Intent(context, TouchImageViewActivity::class.java)
                    context!!.startActivity(intent)
                }
            }
            itemView.container.setOnLongClickListener {
                selectItem(image)
                selectMode = true
                true
            }
        }
    }
}
data class Image (
    val storeUrl: String,
    val imageUrl: String,
    val title: String,
    val photoId: String
)