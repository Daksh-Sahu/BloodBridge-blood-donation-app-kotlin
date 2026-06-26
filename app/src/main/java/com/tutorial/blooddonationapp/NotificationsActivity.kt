package com.tutorial.blooddonationapp

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.tutorial.blooddonationapp.databinding.ActivityNotificationsBinding

class NotificationsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityNotificationsBinding
    private lateinit var adapter: NotificationAdapter
    private val notificationList = mutableListOf<Notification>()
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()
    private val currentUserId = auth.currentUser?.uid

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotificationsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolBar)

        // Setup Toolbar
        binding.toolBar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        setupRecyclerView()
        fetchNotifications()

        // --- **NEW: Add Swipe-to-Delete** ---
        val swipeToDeleteCallback = object : ItemTouchHelper.SimpleCallback(
            0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false // We don't need drag & drop
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val notificationToDelete = notificationList[position]

                // Call the delete function
                deleteNotification(notificationToDelete)
            }
        }

        val itemTouchHelper = ItemTouchHelper(swipeToDeleteCallback)
        itemTouchHelper.attachToRecyclerView(binding.notificationsRecyclerView)
        // --- **END OF NEW** ---
    }

    private fun setupRecyclerView() {
        adapter = NotificationAdapter(notificationList)
        binding.notificationsRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.notificationsRecyclerView.adapter = adapter
    }

    // --- **NEW: Function to delete from Firebase** ---
    private fun deleteNotification(notification: Notification) {
        if (currentUserId.isNullOrEmpty() || notification.notificationId.isEmpty()) {
            Toast.makeText(this, "Could not delete notification", Toast.LENGTH_SHORT).show()
            return
        }

        val notificationRef = database.reference
            .child("notifications")
            .child(currentUserId)
            .child(notification.notificationId)

        notificationRef.removeValue()
            .addOnSuccessListener {
                Toast.makeText(this, "Notification deleted", Toast.LENGTH_SHORT).show()
                // Note: The ValueEventListener will automatically update the list
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to delete: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }


    private fun fetchNotifications() {
        if (currentUserId == null) return

        val notificationsRef = database.reference.child("notifications").child(currentUserId)

        notificationsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                notificationList.clear()
                if (snapshot.exists()) {
                    for (notificationSnapshot in snapshot.children) {
                        val notification = notificationSnapshot.getValue(Notification::class.java)
                        if (notification != null) {
                            // --- **NEW: Store the Firebase Key** ---
                            notification.notificationId = notificationSnapshot.key ?: ""
                            notificationList.add(notification)
                        }
                    }
                }

                // Show newest notifications first
                notificationList.reverse()
                adapter.notifyDataSetChanged()

                // --- **NEW: Show/Hide "No Notifications" message** ---
                if (notificationList.isEmpty()) {
                    binding.tvNoNotifications.visibility = View.VISIBLE
                    binding.notificationsRecyclerView.visibility = View.GONE
                } else {
                    binding.tvNoNotifications.visibility = View.GONE
                    binding.notificationsRecyclerView.visibility = View.VISIBLE
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@NotificationsActivity, "Failed to load notifications", Toast.LENGTH_SHORT).show()
            }
        })
    }
}