package com.waterloo.wit.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import com.waterloo.wit.R
import com.waterloo.wit.data.RoomInfoItem
import com.waterloo.wit.data.SitumBuildingItem

class RoomInfoAdapter(private val context: Context,
                      private val dataSource: ArrayList<RoomInfoItem>) : BaseAdapter() {
    private val inflater: LayoutInflater
            = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    override fun getCount(): Int {
        return dataSource.count()
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getItem(position: Int): Any {
        return dataSource[position]
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        var rootView = convertView
        val viewHolder: RoomItemViewHolder
        if(convertView == null){
            rootView = inflater.inflate(R.layout.adapter_room_info_item, parent, false)

            viewHolder = RoomItemViewHolder()
            viewHolder.description1TextView = rootView?.findViewById(R.id.description1TextView)
            viewHolder.description2TextView = rootView?.findViewById(R.id.description2TextView)
            viewHolder.uomTextView = rootView?.findViewById(R.id.uomTextView)
            viewHolder.amountTextView = rootView?.findViewById(R.id.amountTextView)

        }
        else{
            viewHolder = rootView?.tag as RoomItemViewHolder
        }
        val roomInfoItem = dataSource.get(position)

        viewHolder.description1TextView?.text = roomInfoItem.description1
        viewHolder.description2TextView?.text = roomInfoItem.description2
        viewHolder.uomTextView?.text = roomInfoItem.uom
        viewHolder.amountTextView?.text = roomInfoItem.amount.toString()

        rootView?.tag = viewHolder
        return rootView!!
    }


    private class RoomItemViewHolder {
        internal var description1TextView: TextView? = null
        internal var description2TextView: TextView? = null
        internal var uomTextView: TextView? = null
        internal var amountTextView: TextView? = null
    }

}