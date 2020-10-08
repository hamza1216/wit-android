package com.waterloo.wit.adapter

import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.waterloo.wit.R
import com.waterloo.wit.data.SitumFloorItem
import com.waterloo.wit.ui.inflate
import kotlinx.android.synthetic.main.situm_item_level.view.*


class ViewItemAdapter(
    private val list: ArrayList<String>,
    private val shouldShowFullList: Boolean
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    var listener: ViewItemClickListener? = null
    var selected_position = 0
    var active_position = -1
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflatedView = parent.inflate( R.layout.situm_item_level, false)
        return ViewItemCellViewHolder(inflatedView)
    }

    override fun getItemCount(): Int {
        if (!shouldShowFullList && list.size > 20) {
            return 20
        }
        return list.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        var topHolder = holder as ViewItemCellViewHolder
        topHolder.updateWithTransactionItem(list[position], position)
    }
    inner
    class ViewItemCellViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        private var view: View = v

        fun updateWithTransactionItem(item: String, position: Int) {
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
                listener?.onViewItemClick(adapterPosition)
            }
            view.txt_level_number.text = item
        }
    }

}
