package com.tutorial.blooddonationapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.tutorial.blooddonationapp.databinding.ActivityDonerPageBinding
import java.util.concurrent.atomic.AtomicInteger

class Doner_page: AppCompatActivity() {
    private lateinit var binding: ActivityDonerPageBinding
    private lateinit var adapter: PatientRequestAdapter

    private val allPatientRequests = mutableListOf<PatientRequest>()
    private val database = FirebaseDatabase.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var currentUserBloodType: String? = null

    private val standardRef = database.reference.child("patients")
    private val emergencyRef = database.reference.child("emergency_requests")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDonerPageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolBar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        setupRecyclerView()
        fetchCurrentUserBloodType()

        binding.cbMyBloodType.setOnCheckedChangeListener { _, _ ->
            applyFilterAndRefreshList()
        }

        loadAllRequests()
    }

    private fun loadAllRequests() {
        binding.progressBar.visibility = View.VISIBLE
        binding.tvNoRequests.visibility = View.GONE

        allPatientRequests.clear()

        val pendingFetches = AtomicInteger(2)

        val completionCallback = {
            if (pendingFetches.decrementAndGet() == 0) {
                binding.progressBar.visibility = View.GONE
                applyFilterAndRefreshList()
            }
        }

        // 1. Fetch STANDARD Requests (/patients)
        standardRef.addValueEventListener(createDataListener(false, completionCallback))

        // 2. Fetch EMERGENCY Requests (/emergency_requests)
        emergencyRef.addValueEventListener(createDataListener(true, completionCallback))
    }

    private fun createDataListener(isEmergencySource: Boolean, completionCallback: () -> Unit): ValueEventListener {
        return object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val newRequests = mutableListOf<PatientRequest>()

                for (requestSnapshot in snapshot.children) {
                    val request = try {
                        requestSnapshot.getValue(PatientRequest::class.java)
                    } catch (e: Exception) {
                        Log.e("DonerPage", "Mapping error from source (Emergency: $isEmergencySource): ${e.message}")
                        null
                    }

                    if (request != null) {
                        request.isEmergency = isEmergencySource
                        request.requestId = requestSnapshot.key
                        newRequests.add(request)
                    }
                }

                // Remove old requests from this source before adding new ones
                allPatientRequests.removeAll { it.isEmergency == isEmergencySource }
                allPatientRequests.addAll(newRequests)

                completionCallback()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("DonerPage", "Data fetch failed from source (Emergency: $isEmergencySource): ${error.message}")
                completionCallback()
            }
        }
    }


    private fun setupRecyclerView() {
        adapter = PatientRequestAdapter(emptyList()) { selectedRequest ->
            val intent = Intent(this, RequestDetailsActivity::class.java)
            intent.putExtra("EXTRA_PATIENT_REQUEST", selectedRequest)
            startActivity(intent)
        }
        binding.patientRequestsRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.patientRequestsRecyclerView.adapter = adapter
    }

    private fun fetchCurrentUserBloodType() {
        val currentUserId = auth.currentUser?.uid ?: return

        val userRef = database.reference.child("users").child(currentUserId)
        userRef.get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                val bloodGroup = snapshot.child("bloodGroup").getValue(String::class.java)
                if (!bloodGroup.isNullOrEmpty()) {
                    currentUserBloodType = bloodGroup
                    binding.cbMyBloodType.text = "Show only my blood type ($currentUserBloodType)"
                    binding.cbMyBloodType.isEnabled = true
                } else {
                    binding.cbMyBloodType.text = "Set your blood type in profile"
                    binding.cbMyBloodType.isEnabled = false
                }
            }
        }
    }

    private fun applyFilterAndRefreshList() {
        // 1. Apply any blood type filters first (returns the list relevant to the user)
        val unfilteredList = if (binding.cbMyBloodType.isChecked && currentUserBloodType != null) {
            allPatientRequests.filter { it.bloodType == currentUserBloodType }
        } else {
            allPatientRequests
        }

        // 🌟 STEP A: SEGREGATE THE LIST 🌟
        // Partition: This splits the list into two based on the isEmergency flag.
        val (emergencyRequests, standardRequests) = unfilteredList.partition { it.isEmergency }

        // 🌟 STEP B: SORT AND MERGE 🌟
        // 1. Sort requests (using descending requestId as a proxy for newest first).
        val sortedStandardRequests = standardRequests.sortedByDescending { it.requestId }
        val sortedEmergencyRequests = emergencyRequests.sortedByDescending { it.requestId }

        // 2. Combine: Emergency requests must be added first.
        val finalSortedList = mutableListOf<PatientRequest>()
        finalSortedList.addAll(sortedEmergencyRequests) // Emergency on top
        finalSortedList.addAll(sortedStandardRequests) // Standard below

        // 3. Update UI and Adapter
        if (finalSortedList.isEmpty()) {
            binding.tvNoRequests.visibility = View.VISIBLE
        } else {
            binding.tvNoRequests.visibility = View.GONE
        }

        adapter.updateList(finalSortedList) // Adapter receives the final, sorted list.
    }
}