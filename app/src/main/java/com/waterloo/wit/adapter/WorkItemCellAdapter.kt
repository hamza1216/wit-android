package com.waterloo.wit.adapter

import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.waterloo.wit.MainApplication
import com.waterloo.wit.R
import com.waterloo.wit.data.WorkItem
import com.waterloo.wit.ui.inflate
import kotlinx.android.synthetic.main.adapter_workitem.view.*

class WorkItemCellAdapter(
    private val list: ArrayList<WorkItem>,
    private val shouldShowFullList: Boolean
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    var listener: WorkItemClickListener? = null
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflatedView = parent.inflate( R.layout.adapter_workitem, false)
        return WorkItemCellViewHolder(inflatedView)
    }

    override fun getItemCount(): Int {
        if (!shouldShowFullList && list.size > 20) {
            return 20
        }
        return list.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        var topHolder = holder as WorkItemCellViewHolder
        topHolder.updateWithTransactionItem(list[position], position)
    }
    inner
    class WorkItemCellViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        private var view: View = v

        fun updateWithTransactionItem(item: WorkItem, position: Int) {
            /*
            val buildingList = MainApplication.instance.dbHelper.readBuilding(item.Building_UID)
            var buildingName = ""
            if(buildingList.count()>0) {
                val buildingItem = buildingList.get(0)
                buildingName = buildingItem.Name
            }
             */
            val firebaseFirestore = FirebaseFirestore.getInstance()
            firebaseFirestore.collection("buildings").document(item.Building_UID).get().addOnSuccessListener {
                view.buildingTextView.text = it.get("name").toString()

            }
            view.setOnClickListener{
                listener?.onClick(adapterPosition)
            }
            view.workTitleTextView.text = item.WorkTitle
            view.dateTimeTextView.text = item.Date_Time
            view.areaTextView.text = item.Area
            val mod = position % 2
            if (mod != 0) {
                view.setBackgroundColor(ContextCompat.getColor(view.context, R.color.rowOddColor))
            } else {
                view.setBackgroundColor(ContextCompat.getColor(view.context, R.color.rowEvenColor))
            }
        }
    }

}
