package com.tutorial.blooddonationapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class MyDonationsActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyStateLayout: LinearLayout
    private lateinit var recordCountText: TextView
    private lateinit var recordCountBadge: com.google.android.material.card.MaterialCardView
    private lateinit var progressBar: View
    private lateinit var donorRecordAdapter: DonorRecordAdapter

    private val donorRecords = mutableListOf<DonorRecord>()

    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()
    private val recordsRef = database.getReference("userRecords")
    private val usersRef = database.getReference("users")

    private var currentDonorPuId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_donations)

        // Initialize views
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        recyclerView = findViewById(R.id.myRecordsRecyclerView)
        emptyStateLayout = findViewById(R.id.emptyStateLayout)
        recordCountText = findViewById(R.id.recordCountText)
        recordCountBadge = findViewById(R.id.recordCountBadge)
        progressBar = findViewById(R.id.progressBar)

        // Setup Toolbar
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        // Setup RecyclerView
        setupRecyclerView()

        // 1. Get the current user's puID first
        fetchCurrentUserPuId()
    }

    private fun showLoading(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    private fun fetchCurrentUserPuId() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "You must be logged in to view history.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        showLoading(true)
        // Read the puId from the user's profile
        usersRef.child(currentUser.uid).child("puId").get().addOnSuccessListener { snapshot ->
            currentDonorPuId = snapshot.getValue(String::class.java)?.uppercase()

            if (currentDonorPuId.isNullOrEmpty()) {
                showLoading(false)
                Toast.makeText(this, "Donor profile incomplete (Missing PU ID).", Toast.LENGTH_LONG).show()
                updateUI()
            } else {
                // 2. Once the puID is known, load the filtered records
                loadRecordsByPuId(currentDonorPuId!!)
            }
        }.addOnFailureListener {
            showLoading(false)
            Toast.makeText(this, "Failed to fetch user data.", Toast.LENGTH_LONG).show()
            updateUI()
        }
    }

    // 🌟 Filter records using the current user's puID
    private fun loadRecordsByPuId(puId: String) {
        // Query the /userRecords node where the nested 'donorId' matches the current user's puId
        recordsRef.orderByChild("donorId").equalTo(puId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                donorRecords.clear()

                // Fetch records in reverse order (newest first)
                for (recordSnapshot in snapshot.children.reversed()) {
                    try {
                        val record = recordSnapshot.getValue(DonorRecord::class.java)
                        if (record != null) {
                            donorRecords.add(record)
                        }
                    } catch (e: Exception) {
                        Log.e("MyDonationsActivity", "Failed to deserialize record: ${e.message}")
                    }
                }

                donorRecordAdapter.notifyDataSetChanged()
                showLoading(false)
                updateUI()
            }

            override fun onCancelled(error: DatabaseError) {
                showLoading(false)
                Log.e("MyDonationsActivity", "Firebase query cancelled: ${error.message}")
                Toast.makeText(this@MyDonationsActivity, "Failed to load history: ${error.message}", Toast.LENGTH_LONG).show()
                updateUI()
            }
        })
    }

    private fun setupRecyclerView() {
        donorRecordAdapter = DonorRecordAdapter(donorRecords) { record ->
            // Handle item click - open details activity
            val intent = Intent(this, DonorDetailsActivity::class.java)
            intent.putExtra("DONOR_RECORD", record)
            startActivity(intent)
        }

        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@MyDonationsActivity)
            adapter = donorRecordAdapter
        }
    }

    private fun updateUI() {
        if (donorRecords.isEmpty()) {
            recyclerView.visibility = View.GONE
            emptyStateLayout.visibility = View.VISIBLE
            recordCountBadge.visibility = View.GONE
        } else {
            recyclerView.visibility = View.VISIBLE
            emptyStateLayout.visibility = View.GONE
            recordCountBadge.visibility = View.VISIBLE
            recordCountText.text = "${donorRecords.size} Donations"
        }
    }
}