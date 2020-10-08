package com.waterloo.wit.adapter

import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.waterloo.wit.R
import com.waterloo.wit.data.SitumFloorItem
import com.waterloo.wit.ui.inflate
import kotlinx.android.synthetic.main.situm_item_level.view.*


class FloorLevelAdapter(
    private val list: ArrayList<SitumFloorItem>,
    private val shouldShowFullList: Boolean
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    var listener: FloorItemClickListener? = null
    var selected_position = 0
    var active_position = -1
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflatedView = parent.inflate( R.layout.situm_item_level, false)
        return FloorItemCellViewHolder(inflatedView)
    }

    override fun getItemCount(): Int {
        if (!shouldShowFullList && list.size > 20) {
            return 20
        }
        return list.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        var topHolder = holder as FloorItemCellViewHolder
        topHolder.updateWithTransactionItem(list[position], position)
    }
    inner
    class FloorItemCellViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        private var view: View = v

        fun updateWithTransactionItem(item: SitumFloorItem, position: Int) {
            if(position == selected_position){
                view.isSelected = true
                view.setBackgroundColor(ContextCompat.getColor(view.context, R.color.gray_second))
            }
            else{
                view.isSelected = false
                view.setBackgroundColor(ContextCompat.getColor(view.context, R.color.rowOddColor))

            }
            if(position == active_position){
                view.setBackgroundColor(ContextCompat.getColor(view.context, R.color.situm_primary_color))
            }
            view.setOnClickListener{
                selected_position = adapterPosition
                listener?.onFloorItemClick(adapterPosition)
            }
            view.txt_level_number.text = item.level.toString()
        }
    }

}
