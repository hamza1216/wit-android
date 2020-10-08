package com.waterloo.wit.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView.OnEditorActionListener
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.waterloo.wit.MainApplication
import com.waterloo.wit.R
import com.waterloo.wit.adapter.WorkItemCellAdapter
import com.waterloo.wit.adapter.WorkItemClickListener
import com.waterloo.wit.data.WorkItem
import com.waterloo.wit.helpers.WitHelper
import com.waterloo.wit.ui.MainActivity
import kotlinx.android.synthetic.main.fragment_search_work_item.*


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [SearchWorkItemFragment.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [SearchWorkItemFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class SearchWorkItemFragment : Fragment(), WorkItemClickListener {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    private lateinit var recyclerView: RecyclerView
    private lateinit var editSearch: EditText

    var adapter: WorkItemCellAdapter? = null
    var workItemList: ArrayList<WorkItem> = ArrayList()
    var searchItemList: ArrayList<WorkItem> = ArrayList()

    private lateinit var firebaseFirestore: FirebaseFirestore
    private lateinit var mAuth: FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val rootView = inflater.inflate(R.layout.fragment_search_work_item, container, false)
        recyclerView = rootView.findViewById(R.id.recyclerView)
        editSearch = rootView.findViewById(R.id.editSearch)

        mAuth = FirebaseAuth.getInstance()
        firebaseFirestore = FirebaseFirestore.getInstance()
        return rootView
    }

    override fun onStart() {
        super.onStart()
        val mainActivity = activity as MainActivity
        mainActivity.supportActionBar?.title = "Search for past work items"
        WitHelper.SetLinearLayoutManagerAndHairlineDividerFor(recyclerView, this.context!!, false)
        refreshListView()
        editSearch.setOnEditorActionListener(OnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val searchText = editSearch.text.toString()
                if(searchText.isEmpty()){
                   refreshListView()
                }
                else{
                    refreshListView(searchText)
                }
                val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(v.windowToken, 0)
                true
            }
            false
        })
    }

    override fun onClick(position: Int) {
        val workItem = workItemList.get(position)
        val activity = activity as MainActivity
        activity.switchToWorkItem(workItem)
    }
    fun refreshListView(text: String){
        /*
        val dbHelper = MainApplication.instance.dbHelper
        workItemList = dbHelper.readWorkItemWithKeyword(text)
         */
        val userId = MainApplication.instance.prefs.getUserId()
        val mainActivity = activity as MainActivity
        mainActivity.showProgressDialog()
        if(WitHelper.isNetworkAvailable(context!!)){
            firebaseFirestore.collection("workitems").whereEqualTo("userid", userId).get().addOnSuccessListener { documents->
                mainActivity.hideProgressDialog()
                workItemList.clear()
                for (document in documents){
                    val buildingId = document.data.get("buildingid").toString()
                    val worktitle = document.data.get("worktitle").toString()
                    val datetime = document.data.get("datetime").toString()
                    val area = document.data.get("area").toString()
                    val note = document.data.get("note").toString()
                    val latitude = document.data.get("latitude").toString()
                    val longitude = document.data.get("longitude").toString()
                    val floorlevel = document.data.get("floorlevel").toString().toInt()
                    workItemList.add(WorkItem(document.id, buildingId, worktitle, datetime, area, note, latitude, longitude, floorlevel, userId!!))
                }
                // search item
                searchItemList.clear()
                for (item in workItemList){
                    if(text.isEmpty() || item.WorkTitle.toLowerCase().contains(text.toLowerCase()) || item.Area.toLowerCase().contains(text.toLowerCase()) || item.Notes.toLowerCase().contains(text.toLowerCase()))
                    {
                        searchItemList.add(item)
                    }
                }
                adapter = WorkItemCellAdapter(searchItemList, true)
                adapter?.listener = this
                recyclerView.adapter = adapter

            }
        }
        else{
            firebaseFirestore.collection("workitems").whereEqualTo("userid", userId).addSnapshotListener{ querySnapshot, firebaseFirestoreException ->
                mainActivity.hideProgressDialog()
                workItemList.clear()
                if(querySnapshot != null){
                    for (document in querySnapshot!!.documents){
                        val buildingId = document.data?.get("buildingid").toString()
                        val worktitle = document.data?.get("worktitle").toString()
                        val datetime = document.data?.get("datetime").toString()
                        val area = document.data?.get("area").toString()
                        val note = document.data?.get("note").toString()
                        val latitude = document.data?.get("latitude").toString()
                        val longitude = document.data?.get("longitude").toString()
                        val floorlevel = document.data?.get("floorlevel").toString().toInt()
                        workItemList.add(WorkItem(document.id, buildingId, worktitle, datetime, area, note, latitude, longitude, floorlevel, userId!!))
                    }
                    // search item
                    searchItemList.clear()
                    for (item in workItemList){
                        if(text.isEmpty() || item.WorkTitle.toLowerCase().contains(text.toLowerCase()) || item.Area.toLowerCase().contains(text.toLowerCase()) || item.Notes.toLowerCase().contains(text.toLowerCase()))
                        {
                            searchItemList.add(item)
                        }
                    }
                    adapter = WorkItemCellAdapter(searchItemList, true)
                    adapter?.listener = this
                    recyclerView.adapter = adapter

                }
            }
        }

    }

    fun refreshListView(){
        /*
        val dbHelper = MainApplication.instance.dbHelper
        workItemList = dbHelper.readAllWorkItems()
         */
        val userId = MainApplication.instance.prefs.getUserId()
        val mainActivity = activity as MainActivity
        mainActivity.showProgressDialog()
        if(WitHelper.isNetworkAvailable(context!!)){
            firebaseFirestore.collection("workitems").whereEqualTo("userid", userId).get().addOnSuccessListener { documents->
                mainActivity.hideProgressDialog()
                workItemList.clear()
                for (document in documents){
                    val buildingId = document.data.get("buildingid").toString()
                    val worktitle = document.data.get("worktitle").toString()
                    val datetime = document.data.get("datetime").toString()
                    val area = document.data.get("area").toString()
                    val note = document.data.get("note").toString()
                    val latitude = document.data.get("latitude").toString()
                    val longitude = document.data.get("longitude").toString()
                    val floorlevel = document.data.get("floorlevel").toString().toInt()
                    workItemList.add(WorkItem(document.id, buildingId, worktitle, datetime, area, note, latitude, longitude, floorlevel, userId!!))
                    adapter = WorkItemCellAdapter(workItemList, true)
                    adapter?.listener = this
                    recyclerView.adapter = adapter
                }
            }
        }
        else{
            firebaseFirestore.collection("workitems").whereEqualTo("userid", userId).addSnapshotListener{ querySnapshot, firebaseFirestoreException ->
                mainActivity.hideProgressDialog()
                workItemList.clear()
                if(querySnapshot != null){
                    for (document in querySnapshot!!.documents){
                        val buildingId = document.data?.get("buildingid").toString()
                        val worktitle = document.data?.get("worktitle").toString()
                        val datetime = document.data?.get("datetime").toString()
                        val area = document.data?.get("area").toString()
                        val note = document.data?.get("note").toString()
                        val latitude = document.data?.get("latitude").toString()
                        val longitude = document.data?.get("longitude").toString()
                        val floorlevel = document.data?.get("floorlevel").toString().toInt()
                        workItemList.add(WorkItem(document.id, buildingId, worktitle, datetime, area, note, latitude, longitude, floorlevel, userId!!))
                        adapter = WorkItemCellAdapter(workItemList, true)
                        adapter?.listener = this
                        recyclerView.adapter = adapter
                    }
                }
            }
        }

    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment SearchWorkItemFragment.
         */
        // TODO: Rename and change types and number of parameters
        var instance: SearchWorkItemFragment? = null
        @JvmStatic
        fun newInstance(param1: String, param2: String):SearchWorkItemFragment{
            if(instance == null){
                instance = SearchWorkItemFragment()
                    instance!!.apply {
                    arguments = Bundle().apply {
                        putString(ARG_PARAM1, param1)
                        putString(ARG_PARAM2, param2)
                    }
                }
            }
            return instance!!
        }
    }
}
