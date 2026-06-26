package com.tutorial.blooddonationapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth // 🌟 Needed for current user UID
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class DonationsActivity : AppCompatActivity() {

    private lateinit var backButton: ImageButton
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyStateLayout: LinearLayout
    private lateinit var recordCountText: TextView
    private lateinit var recordCountBadge: LinearLayout
    private lateinit var donorRecordAdapter: DonorRecordAdapter
    private lateinit var recordsListContainer: LinearLayout

    private val donorRecords = mutableListOf<DonorRecord>()

    // 🌟 FIREBASE SETUP
    private val database = FirebaseDatabase.getInstance()
    // This node stores ALL records, we will filter the data based on its content.
    private val recordsRef = database.getReference("userRecords")
    private val hospitalUserRef = database.getReference("hospital_users")
    private val auth = FirebaseAuth.getInstance()
    // END FIREBASE SETUP

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContentView(R.layout.activity_donations)

        // Initialize views
        backButton = findViewById(R.id.backButton)
        recyclerView = findViewById(R.id.donorRecyclerView)
        emptyStateLayout = findViewById(R.id.emptyStateLayout)
        recordCountText = findViewById(R.id.recordCountText)
        recordCountBadge = findViewById(R.id.recordCountBadge)
        recordsListContainer = findViewById(R.id.recordsListContainer)

        // Setup back button
        backButton.setOnClickListener {
            finish()
        }

        // Setup RecyclerView (must be done before loading data)
        setupRecyclerView()

        // 🌟 START THE PROCESS: Fetch the hospital name first
        fetchHospitalNameAndLoadRecords()
    }

    override fun onResume() {
        super.onResume()
        // Reload records if the activity comes back to the foreground
        fetchHospitalNameAndLoadRecords()
    }

    // 🌟 NEW FUNCTION: Get Hospital Name before loading records
    private fun fetchHospitalNameAndLoadRecords() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Authentication error. Please log in.", Toast.LENGTH_LONG).show()
            updateUI(true) // Show empty state
            return
        }

        hospitalUserRef.child(currentUser.uid).child("hospitalName")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val hospitalName = snapshot.getValue(String::class.java)
                    if (!hospitalName.isNullOrEmpty()) {
                        // Success: Found hospital name, now load the filtered records
                        loadRecordsFromFirebase(hospitalName)
                    } else {
                        // User is logged in but has no hospital name saved
                        Toast.makeText(this@DonationsActivity, "Hospital name not found in profile.", Toast.LENGTH_LONG).show()
                        updateUI(true)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@DonationsActivity, "Failed to fetch user hospital: ${error.message}", Toast.LENGTH_LONG).show()
                    updateUI(true)
                }
            })
    }


    // 🌟 MODIFIED: Now takes hospitalName to filter the query
    private fun loadRecordsFromFirebase(hospitalName: String) {
        // 🚨 CRITICAL FIX: Query the userRecords node, ordering by the 'hospitalName' field
        // and matching it exactly to the current user's hospital.
        recordsRef.orderByChild("hospitalName").equalTo(hospitalName)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    donorRecords.clear()

                    // Note: If you want records sorted newest-first, you must load them
                    // and then sort them in Kotlin, as Firebase queries cannot sort
                    // by two properties (hospitalName and createdAt) simultaneously.

                    val tempRecords = mutableListOf<DonorRecord>()

                    if (snapshot.exists()) {
                        for (recordSnapshot in snapshot.children) {
                            try {
                                val record = recordSnapshot.getValue(DonorRecord::class.java)
                                if (record != null) {
                                    tempRecords.add(record)
                                }
                            } catch (e: Exception) {
                                Log.e("DonationsActivity", "Failed to deserialize record: ${e.message}")
                            }
                        }
                    }

                    // Sort records by createdAt (assuming this field holds a Date or Long timestamp)
                    // If createdAt holds a Date object, you might need to change it to Long (timestamp) for reliable sorting.
                    val sortedRecords = tempRecords.sortedByDescending { it.createdAt }

                    donorRecords.addAll(sortedRecords)

                    donorRecordAdapter.notifyDataSetChanged()
                    updateUI(false)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("DonationsActivity", "Firebase data load cancelled: ${error.message}")
                    Toast.makeText(this@DonationsActivity, "Failed to load records: ${error.message}", Toast.LENGTH_LONG).show()
                    updateUI(true)
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
            layoutManager = LinearLayoutManager(this@DonationsActivity)
            adapter = donorRecordAdapter
        }
    }

    // Modified updateUI to take a boolean to force empty state/hide list
    private fun updateUI(forceEmpty: Boolean) {
        if (donorRecords.isEmpty() || forceEmpty) {
            // Show empty state
            emptyStateLayout.visibility = View.VISIBLE
            recordsListContainer.visibility = View.GONE
            recordCountBadge.visibility = View.GONE
        } else {
            // Show records
            emptyStateLayout.visibility = View.GONE
            recordsListContainer.visibility = View.VISIBLE
            recordCountBadge.visibility = View.VISIBLE
            recordCountText.text = "${donorRecords.size} Records"
        }
    }
}