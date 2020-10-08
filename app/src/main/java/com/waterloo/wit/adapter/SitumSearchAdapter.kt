package com.waterloo.wit.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import com.waterloo.wit.R
import com.waterloo.wit.data.SitumBuildingItem

class SitumSearchAdapter(private val context: Context,
                         private val dataSource: ArrayList<SitumBuildingItem>) : BaseAdapter() {
    private var filterList = ArrayList<SitumBuildingItem>()
    private val inflater: LayoutInflater
            = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    override fun getCount(): Int {
        return filterList.count()
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getItem(position: Int): Any {
        return filterList[position]
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        var rootView = convertView
        val viewHolder: SearchItemViewHolder
        if(convertView == null){
            rootView = inflater.inflate(R.layout.adapter_search_item, parent, false)

            viewHolder = SearchItemViewHolder()
            viewHolder.buildingImageView = rootView?.findViewById(R.id.buildingImageView)
            viewHolder.buildingTextView = rootView?.findViewById(R.id.buildingTextView)
            viewHolder.floorLevelTextView = rootView?.findViewById(R.id.floorLevelTextView)

        }
        else{
            viewHolder = rootView?.tag as SearchItemViewHolder
        }
        val searchItem = filterList.get(position)
        if(searchItem.itemType == 0){
            viewHolder.buildingTextView?.text = searchItem.building?.name
            viewHolder.floorLevelTextView?.visibility = View.GONE
        }
        else{

        }
        rootView?.tag = viewHolder
        return rootView!!
    }
    fun getFilters(): ArrayList<SitumBuildingItem>{
        return filterList
    }
    fun filter(charText: String){
        filterList.clear()
        var charText = charText.toLowerCase()
        if(charText.isEmpty()){
            filterList.addAll(dataSource)
        }
        else{
            for (item in dataSource){
                if(item.itemType == 0){
                    if(item.building!!.name.toLowerCase().contains(charText)){
                        filterList.add(item)
                    }
                }
            }
        }
        notifyDataSetChanged()
    }
    private class SearchItemViewHolder {
        internal var buildingImageView: ImageView? = null
        internal var buildingTextView: TextView? = null
        internal var floorLevelTextView: TextView? = null
    }

}