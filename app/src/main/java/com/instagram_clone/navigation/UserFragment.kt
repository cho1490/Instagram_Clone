package com.instagram_clone.navigation

import android.content.Intent
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.instagram_clone.LoginActivity
import com.instagram_clone.MainActivity
import com.instagram_clone.R
import com.instagram_clone.navigation.model.AlarmDTO
import com.instagram_clone.navigation.model.ContentDTO
import com.instagram_clone.navigation.model.FollowDTO
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_user.view.*

class UserFragment : Fragment() {

    var fragmentView : View? = null
    var firestore : FirebaseFirestore? = null
    var uid : String? = null
    var auth : FirebaseAuth? = null
    var currentUserUid : String? = null

    companion object{
        var PICK_PROFILE_FROM_ALBUM = 10
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        fragmentView = LayoutInflater.from(activity).inflate(R.layout.fragment_user , container, false)
        uid = arguments?.getString("destinationUid")
        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        currentUserUid = auth?.currentUser?.uid

        if(uid == currentUserUid){
            // my page
            fragmentView?.account_btn_follow_signout?.text = getString(R.string.signout)
            fragmentView?.account_btn_follow_signout?.setOnClickListener {
                activity?.finish()
                startActivity(Intent(activity, LoginActivity::class.java))
                auth?.signOut()
            }
        }else{
            // other page
            fragmentView?.account_btn_follow_signout?.text = getString(R.string.follow)
            var mainActivity = (activity as MainActivity)
            mainActivity?.toolbar_username?.text = arguments?.getString("userId")
            mainActivity?.toolbar_btn_back.setOnClickListener {
                mainActivity.bottom_navigation.selectedItemId = R.id.action_home
            }
            mainActivity?.toolbar_title_image?.visibility = View.GONE
            mainActivity?.toolbar_username?.visibility = View.VISIBLE
            mainActivity?.toolbar_btn_back?.visibility = View.VISIBLE
            fragmentView?.account_btn_follow_signout?.setOnClickListener {
                requestFollow()
            }
        }

        fragmentView?.account_recyclerview?.adapter = UserFragmentRecyclerViewAdapter()
        fragmentView?.account_recyclerview?.layoutManager = GridLayoutManager(activity, 3)

        // profile image click
        fragmentView?.account_iv_profile?.setOnClickListener {
            var photoPickerIntent = Intent(Intent.ACTION_PICK)
            photoPickerIntent.type = "image/*"
            activity?.startActivityForResult(photoPickerIntent, PICK_PROFILE_FROM_ALBUM)
        }

        getProfileImage()
        getCountOfFollowerAndFollowing()

        return fragmentView
    }

    fun getProfileImage(){
        firestore?.collection("profileImages")?.document(uid!!)?.addSnapshotListener { value, error ->
            if(value == null) return@addSnapshotListener
            if(value.data != null){
                var url = value?.data!!["image"]
                Glide.with(activity!!).load(url).apply(RequestOptions.circleCropTransform()).into(fragmentView?.account_iv_profile!!)
            }
        }
    }

    // follow click
    fun requestFollow(){
        var tsDocFollowing = firestore?.collection("users")?.document(currentUserUid!!)
        firestore?.runTransaction { transaction ->
            var followDTO = transaction.get(tsDocFollowing!!).toObject(FollowDTO::class.java)
            if(followDTO == null){
                followDTO = FollowDTO()
                followDTO!!.followingCount = 1
                followDTO!!.followings[uid!!] = true

                transaction.set(tsDocFollowing, followDTO)
                return@runTransaction
            }

            if(followDTO.followings.containsKey(uid)){
                // remove following
                followDTO?.followingCount = followDTO?.followingCount - 1
                followDTO?.followings?.remove(uid)
            }else{
                // add following
                followDTO?.followingCount = followDTO?.followingCount + 1
                followDTO?.followings[uid!!] = true
            }

            transaction.set(tsDocFollowing, followDTO)
            return@runTransaction
        }

        var tsDocFollower = firestore?.collection("users")?.document(uid!!)
        firestore?.runTransaction { transaction ->
            var followDTO = transaction.get(tsDocFollower!!).toObject(FollowDTO::class.java)
            if(followDTO == null){
                followDTO = FollowDTO()
                followDTO!!.followerCount = 1
                followDTO!!.followers[currentUserUid!!] = true
                followerAlarm(uid!!)

                transaction.set(tsDocFollower, followDTO!!)
                return@runTransaction
            }

            if(followDTO!!.followers.containsKey(currentUserUid)){
                // cancel
                followDTO!!.followerCount =   followDTO!!.followerCount - 1
                followDTO!!.followers.remove(currentUserUid!!)
            }else{
                // follow
                followDTO!!.followerCount =   followDTO!!.followerCount + 1
                followDTO!!.followers[currentUserUid!!] = true
                followerAlarm(uid!!)
            }

            transaction.set(tsDocFollower, followDTO!!)
            return@runTransaction
        }
    }

    fun getCountOfFollowerAndFollowing(){
        firestore?.collection("users")?.document(uid!!)?.addSnapshotListener { value, error ->
            if(value == null) return@addSnapshotListener

            var followDTO = value.toObject(FollowDTO::class.java)
            if(followDTO?.followingCount != null){
                 fragmentView?.account_tv_following_count?.text =  followDTO?.followingCount?.toString()
            }
            if(followDTO?.followerCount != null){
                fragmentView?.account_tv_follower_count?.text = followDTO?.followerCount?.toString()
                if(followDTO?.followers?.containsKey(currentUserUid)){
                    fragmentView?.account_btn_follow_signout?.text = getString(R.string.follow_cancel)
                    fragmentView?.account_btn_follow_signout?.background?.setColorFilter(ContextCompat.getColor(activity!!, R.color.colorLightGray), PorterDuff.Mode.MULTIPLY)
                }else{
                    if(uid != currentUserUid){
                        fragmentView?.account_btn_follow_signout?.text = getString(R.string.follow)
                        fragmentView?.account_btn_follow_signout?.background?.colorFilter = null
                    }
                }
            }
        }
    }

    fun followerAlarm(destinationUid : String){
        var alarmDTO = AlarmDTO()
        alarmDTO.destinationUid = destinationUid
        alarmDTO.userId = auth?.currentUser?.email
        alarmDTO.uid = auth?.currentUser?.uid
        alarmDTO.kind = 2
        alarmDTO.timestamp = System.currentTimeMillis()
        FirebaseFirestore.getInstance().collection("alarms").document().set(alarmDTO)
    }

    inner class UserFragmentRecyclerViewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        var contentDTOs : ArrayList<ContentDTO> = arrayListOf()

        init {
            firestore?.collection("images")?.whereEqualTo("uid",uid)?.addSnapshotListener { value, error ->
                // not login -> return null
                if(value == null) return@addSnapshotListener

                // login -> get data
                for(snapshot in value.documents){
                    contentDTOs.add(snapshot.toObject(ContentDTO::class.java)!!)
                }

                fragmentView?.account_tv_post_count?.text = contentDTOs.size.toString()
                notifyDataSetChanged()
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            var width = resources.displayMetrics.widthPixels / 3
            var imageview = ImageView(parent.context)
            imageview.layoutParams = LinearLayoutCompat.LayoutParams(width,width)

            return CustomViewHolder(imageview)
        }

        inner class CustomViewHolder(var imageview: ImageView) : RecyclerView.ViewHolder(imageview) {

        }

        override fun getItemCount(): Int {
            return contentDTOs.size
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            var imageview = (holder as CustomViewHolder).imageview
            Glide.with(holder.itemView.context).load(contentDTOs[position].imageUrl).apply(RequestOptions().centerCrop()).into(imageview)
        }

    }

}