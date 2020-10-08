package com.waterloo.wit.ui

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.transition.Transition
import android.view.View
import android.view.ViewGroup
import androidx.viewpager.widget.PagerAdapter

import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.target.CustomTarget
import com.ortiz.touchview.TouchImageView
import com.waterloo.wit.R
import com.waterloo.wit.adapter.ExtendedViewPager
import com.waterloo.wit.helpers.WitHelper
import kotlinx.android.synthetic.main.item_gallery_image.view.*


class TouchImageViewActivity : Activity() {
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_touchimage)
        val mViewPager =
            findViewById<View>(R.id.view_pager) as ExtendedViewPager
        setContentView(mViewPager)
        mViewPager.adapter = TouchImageAdapter()
    }

    internal class TouchImageAdapter : PagerAdapter() {
        override fun getCount(): Int {
            return WitHelper.imageList.count()
        }

        override fun instantiateItem(
            container: ViewGroup,
            position: Int
        ): View {
            val img = TouchImageView(container.context)
            val imageItem = WitHelper.imageList.get(position)
            img.setImageURI(Uri.parse(imageItem.imageUrl))
            // load image
            Glide.with(container.context)
                .asBitmap()
                .load(imageItem.imageUrl)
                .centerCrop()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(object : CustomTarget<Bitmap>() {
                    override fun onResourceReady(
                        resource: Bitmap,
                        transition: com.bumptech.glide.request.transition.Transition<in Bitmap>?
                    ) {
                        img.setImageBitmap(resource)
                    }

                    override fun onLoadCleared(placeholder: Drawable?) {
                        // this is called when imageView is cleared on lifecycle call or for
                        // some other reason.
                        // if you are referencing the bitmap somewhere else too other than this imageView
                        // clear it here as you can no longer have the bitmap
                    }
                })

            Glide.with(container.context)
                .asBitmap()
                .load(imageItem.storeUrl)
                .centerCrop()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(object : CustomTarget<Bitmap>() {
                    override fun onResourceReady(
                        resource: Bitmap,
                        transition: com.bumptech.glide.request.transition.Transition<in Bitmap>?
                    ) {
                        img.setImageBitmap(resource)
                    }

                    override fun onLoadCleared(placeholder: Drawable?) {
                        // this is called when imageView is cleared on lifecycle call or for
                        // some other reason.
                        // if you are referencing the bitmap somewhere else too other than this imageView
                        // clear it here as you can no longer have the bitmap
                    }
                })
/*
            Glide.with(container.context)
                .load(imageItem.imageUrl)
                .centerCrop()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(img );

 */
            container.addView(
                img,
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            return img
        }

        override fun destroyItem(
            container: ViewGroup,
            position: Int,
            `object`: Any
        ) {
            container.removeView(`object` as View)
        }

        override fun isViewFromObject(
            view: View,
            `object`: Any
        ): Boolean {
            return view === `object`
        }
    }
}