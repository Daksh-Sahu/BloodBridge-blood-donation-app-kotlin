package com.tutorial.blooddonationapp

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.tutorial.blooddonationapp.databinding.ActivityHistoryBinding

class HistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHistoryBinding
    private lateinit var adapter: PatientRequestAdapter
    private val historyList = mutableListOf<PatientRequest>()

    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()
    private val currentUserId = auth.currentUser?.uid

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup Toolbar
        binding.toolBar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        setupRecyclerView()
        fetchTotalDonations()
        fetchHistoryList()
    }

    private fun setupRecyclerView() {
        // We reuse the same adapter from Doner_page
        adapter = PatientRequestAdapter(historyList) { selectedRequest ->
            Toast.makeText(this, "You clicked on ${selectedRequest.patientName}", Toast.LENGTH_SHORT).show()
        }
        binding.historyRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.historyRecyclerView.adapter = adapter
    }

    /**
     * Fetches the user's total donation count from their profile.
     */
    private fun fetchTotalDonations() {
        if (currentUserId == null) return

        val userStatsRef = database.reference.child("users").child(currentUserId)

        userStatsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val donationsMade = snapshot.child("donationsMade").getValue(Long::class.java) ?: 0L
                    binding.tvTotalDonationsCount.text = donationsMade.toString()
                } else {
                    binding.tvTotalDonationsCount.text = "0"
                }
            }
            override fun onCancelled(error: DatabaseError) {
                binding.tvTotalDonationsCount.text = "0"
            }
        })
    }

    /**
     * Fetches the user's list of completed donations (both given and received)
     * from the /history node.
     */
    private fun fetchHistoryList() {
        if (currentUserId == null) return

        // This is the path we set up in RequestDetailsActivity
        val historyRef = database.reference.child("history").child(currentUserId)

        historyRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                historyList.clear()
                if (snapshot.exists()) {
                    for (requestSnapshot in snapshot.children) {
                        val request = requestSnapshot.getValue(PatientRequest::class.java)
                        if (request != null) {
                            historyList.add(request)
                        }
                    }
                }

                // Show newest first
                historyList.reverse()
                adapter.notifyDataSetChanged()

                // Toggle visibility of the "No History" message
                if (historyList.isEmpty()) {
                    binding.tvNoHistory.visibility = View.VISIBLE
                    binding.historyRecyclerView.visibility = View.GONE
                } else {
                    binding.tvNoHistory.visibility = View.GONE
                    binding.historyRecyclerView.visibility = View.VISIBLE
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@HistoryActivity, "Failed to load history", Toast.LENGTH_SHORT).show()
                binding.tvNoHistory.visibility = View.VISIBLE
                binding.historyRecyclerView.visibility = View.GONE
            }
        })
    }
}
