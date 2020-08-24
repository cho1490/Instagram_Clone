package com.instagram_clone.navigation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.instagram_clone.R
import com.instagram_clone.navigation.model.AlarmDTO
import kotlinx.android.synthetic.main.fragment_alarm.view.*
import kotlinx.android.synthetic.main.item_comment.view.*

class AlarmFragment : Fragment(){

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        var view = LayoutInflater.from(activity).inflate(R.layout.fragment_alarm , container, false)
        view.alarmfragment_recyclerview.adapter = AlarmRecyclerView()
        view.alarmfragment_recyclerview.layoutManager = LinearLayoutManager(activity)
        return view
    }

    inner class AlarmRecyclerView : RecyclerView.Adapter<RecyclerView.ViewHolder>(){

        var alarmDTOs : ArrayList<AlarmDTO> = arrayListOf()

        init {
            val uid = FirebaseAuth.getInstance().currentUser?.uid

            FirebaseFirestore.getInstance()
                .collection("alarms")
                .whereEqualTo("destinationUid", uid)
                .addSnapshotListener { value, error ->
                    alarmDTOs.clear()

                    if(value == null) return@addSnapshotListener

                    for(snapshot in value.documents){
                        alarmDTOs.add(snapshot.toObject(AlarmDTO::class.java)!!)
                    }
                    notifyDataSetChanged()
                }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            var view = LayoutInflater.from(parent.context).inflate(R.layout.item_comment, parent, false)
            return CustomViewHolder(view)
        }

        inner class CustomViewHolder(view : View) : RecyclerView.ViewHolder(view){

        }

        override fun getItemCount(): Int {
            return alarmDTOs.size
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {

            var view = holder.itemView

            FirebaseFirestore.getInstance().collection("profileImages").document(alarmDTOs[position].uid!!).get().addOnCompleteListener { task ->
                if(task.isSuccessful){
                    val url = task.result!!["image"]
                    Glide.with(view.context).load(url).apply(RequestOptions.circleCropTransform()).into(view.commentview_imageview_profile)
                }
            }

            when(alarmDTOs[position].kind){
                0 ->{
                    var str0 = alarmDTOs[position].userId + " " + getString(R.string.alarm_favorite)
                    view.commentview_textview_comment.text = str0
                }
                1 -> {
                    var str0 = alarmDTOs[position].userId + " " + getString(R.string.alarm_comment) + " -> \"" + alarmDTOs[position].message + "\""
                    view.commentview_textview_comment.text = str0
                }
                2 -> {
                    var str0 = alarmDTOs[position].userId + " " + getString(R.string.alarm_follow)
                    view.commentview_textview_comment.text = str0
                }
            }

        }
    }

}