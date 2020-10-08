package com.waterloo.wit.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import com.waterloo.wit.R


class SpinnerItemAdapter(
    context: Context,
    resource: Int,
    objects: List<String>
) :
    ArrayAdapter<String>(context, resource, objects!!) {
    override fun getView(
        position: Int,
        convertView: View?,
        parent: ViewGroup
    ): View { // TODO Auto-generated method stub
        var view: View? = convertView
        val viewHolder: SpinnerItemViewHolder
        if (view == null) {
            viewHolder = SpinnerItemViewHolder()
            val inflate = context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            view = inflate.inflate(R.layout.spinner_item_layout, null)
            viewHolder?.spinnerItemTextView = view?.findViewById(R.id.spinnerTextView) as TextView

        }
        else{
            viewHolder = view?.tag as SpinnerItemViewHolder
        }
        viewHolder?.spinnerItemTextView?.text = getItem(position)
        view.tag = viewHolder
        return view!!
    }
    private class SpinnerItemViewHolder {
        internal var spinnerItemTextView: TextView? = null
    }
}
