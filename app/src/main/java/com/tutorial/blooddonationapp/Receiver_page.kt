package com.tutorial.blooddonationapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.tutorial.blooddonationapp.databinding.ActivityReceiverPageBinding

data class Hospital(
    val id: String,
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double
)

class Receiver_page : AppCompatActivity() {

    private lateinit var binding: ActivityReceiverPageBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    // --- NEW: Hospital List ---
    private val hospitals = listOf(
        Hospital("1", "Sparsh Hospital", "RR Nagar, Bangalore, 560098", 12.926827799784602, 77.52129580801484),
        Hospital("2", "BGS Gleneagles Hospital", "Uttarahalli, Bangalore, 560060",12.902825442982833, 77.49764490325491),
        Hospital("3", "Rashtrothana Hospital", "RR Nagar, Bangalore, 560098",12.909071518718536, 77.513549998747),
        Hospital("4", "Manipal Hospital", "Old Airport Road, Kodihalli, Bangalore 560017",12.958568832612087, 77.64878275034528),
        Hospital("5", "Apollo Hospital", "Bannerghata road, Bangalore, 560076",12.896214526768329, 77.59858879146624),
        Hospital("6", "Fortis Hospital", "Bannerghata road, Bangalore, 560076",12.894886643539039, 77.59861874183741),
        Hospital("7", "Rajarajeshwari Hospital", "Mysore road, Bangalore, 560074",12.896476551512759, 77.46179338102488),
        Hospital("8", "Ramaiah Memorial Hospital", "MSR Nagar, Bangalore, 560054",13.028376623014996, 77.569776052987),
        Hospital("9", "People Tree Hospital", "Dasarahalli, Bangalore, 560057",13.042535075593632, 77.51557896608773),
        Hospital("10", "Sagar Hospital", "Jayanagar, Bangalore, 560041",12.928076828669106, 77.59942295034483)
    )
    // --- END NEW ---

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReceiverPageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        // 1. Setup Toolbar
        setSupportActionBar(binding.toolBar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        binding.toolBar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // 2. Setup Blood Group Dropdown
        setupBloodGroupSpinner()

        // --- NEW: Setup Hospital Dropdown ---
        setupHospitalSpinner()
        // --- END NEW ---

        // 3. Setup Button Click Listener
        binding.btnPostRequest.setOnClickListener {
            postBloodRequest()
        }
    }

    private fun setupBloodGroupSpinner() {
        val bloodGroups = resources.getStringArray(R.array.blood_groups)
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, bloodGroups)
        // Corrected the ID to match your XML
        (binding.layoutBloodGroup.editText as? AutoCompleteTextView)?.setAdapter(adapter)
    }

    // --- NEW FUNCTION ---
    private fun setupHospitalSpinner() {
        // Create a list of strings with "Name, Address"
        val hospitalDisplayList = hospitals.map { "${it.name}, ${it.address}" }
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, hospitalDisplayList)
        // Use the new ID from your XML
        (binding.layoutHospital.editText as? AutoCompleteTextView)?.setAdapter(adapter)
    }
    // --- END NEW FUNCTION ---

    private fun postBloodRequest() {
        val patientName = binding.etPatientName.text.toString().trim()
        val bloodGroup = (binding.layoutBloodGroup.editText as? AutoCompleteTextView)?.text.toString()
        val units = binding.etUnits.text.toString().trim()
        val age = binding.etAge.text.toString().trim()
        val phone = binding.etPhone.text.toString().trim()
        // --- MODIFIED: Read from the new AutoCompleteTextView ---
        val hospital = (binding.layoutHospital.editText as? AutoCompleteTextView)?.text.toString()
        // --- END MODIFIED ---
        val currentUserId = auth.currentUser?.uid

        // 1. Validation runs first
        if (!validateForm(patientName, bloodGroup, units, age, phone, hospital, currentUserId)) {
            return // Stop if validation fails
        }

        // 2. If validation passes, show the confirmation dialog
        AlertDialog.Builder(this)
            .setTitle("Confirm Request")
            .setMessage("Are you sure you want to post this blood request? The details cannot be edited later.")
            .setPositiveButton("Yes, Post") { _, _ ->
                submitRequestToFirebase(
                    currentUserId!!, patientName, bloodGroup, units, age, phone, hospital
                )
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun submitRequestToFirebase(
        posterId: String, patientName: String, bloodGroup: String,
        units: String, age: String, phone: String, hospitalAddressString: String
    ) {
        binding.loadingOverlay.visibility = View.VISIBLE

        // --- NEW: Find the matching Hospital object ---
        // This finds the full hospital object (with lat/long)
        // by matching the "Name, Address" string from the dropdown
        val selectedHospital = hospitals.find {
            "${it.name}, ${it.address}" == hospitalAddressString
        }
        // --- END NEW ---

        val dbRef = database.reference.child("patients") // Using "patients" as per your code
        val newRequestKey = dbRef.push().key

        if (newRequestKey == null) {
            Toast.makeText(this, "Failed to create request. Try again.", Toast.LENGTH_SHORT).show()
            binding.loadingOverlay.visibility = View.GONE
            return
        }

        // --- MODIFIED: Create the request object with location data ---
        val request = PatientRequest(
            requestId = newRequestKey,
            posterId = posterId,
            patientName = patientName,
            bloodType = bloodGroup,
            unitsRequired = units.toInt(),
            patientAge = age.toInt(),
            contactPhone = phone,
            hospitalName = hospitalAddressString, // This is the "Name, Address" string
            timestamp = System.currentTimeMillis(),
            hospitalLat = selectedHospital?.latitude,  // <-- ADDED
            hospitalLng = selectedHospital?.longitude  // <-- ADDED
        )
        // --- END MODIFIED ---

        dbRef.child(newRequestKey).setValue(request)
            .addOnCompleteListener { task ->
                binding.loadingOverlay.visibility = View.GONE
                if (task.isSuccessful) {
                    Toast.makeText(this, "Blood request posted successfully!", Toast.LENGTH_LONG).show()
                    val intent = Intent(this, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this, "Failed to post request: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun validateForm(
        name: String, bloodGroup: String, units: String,
        age: String, phone: String, hospital: String, userId: String?
    ): Boolean {

        // This phone validation should come after checking if phone.isEmpty()
        // I'll move it down.

        if (userId == null) {
            Toast.makeText(this, "You must be logged in to post a request.", Toast.LENGTH_SHORT).show()
            return false
        }
        if (name.isEmpty()) {
            binding.layoutPatientName.error = "Patient name is required"
            return false
        } else {
            binding.layoutPatientName.error = null
        }

        if (bloodGroup.isEmpty()) {
            binding.layoutBloodGroup.error = "Blood group is required"
            return false
        } else {
            binding.layoutBloodGroup.error = null
        }

        if (units.isEmpty()) {
            binding.layoutUnits.error = "Units are required"
            return false
        } else {
            binding.layoutUnits.error = null
        }

        if (age.isEmpty()) {
            binding.layoutAge.error = "Age is required"
            return false
        } else {
            binding.layoutAge.error = null
        }

        if (phone.isEmpty()) {
            binding.layoutPhone.error = "Phone number is required"
            return false
        } else {
            binding.layoutPhone.error = null
        }

        // --- MOVED PHONE LENGTH CHECK HERE ---
        if (phone.length != 10){
            binding.layoutPhone.error = "Phone number must be 10 digits"
            return false
        } else {
            binding.layoutPhone.error = null
        }
        // --- END MOVE ---

        if (hospital.isEmpty()) {
            binding.layoutHospital.error = "Hospital name is required"
            return false
        } else {
            binding.layoutHospital.error = null
        }

        return true
    }
}
