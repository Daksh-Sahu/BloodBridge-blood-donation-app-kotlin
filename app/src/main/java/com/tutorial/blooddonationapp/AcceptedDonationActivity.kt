package com.tutorial.blooddonationapp

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.tutorial.blooddonationapp.databinding.ActivityAcceptedDonationBinding

// Assuming ActiveDonationTracking and PatientRequest classes are available

class AcceptedDonationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAcceptedDonationBinding
    private val database = FirebaseDatabase.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private var donorId: String? = null
    private var requestId: String? = null

    // Reference to the active donation node specific to the current donor
    private lateinit var activeDonationRef: com.google.firebase.database.DatabaseReference
    private var donationStatusListener: ValueEventListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAcceptedDonationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        donorId = intent.getStringExtra(EXTRA_DONOR_ID)
        requestId = intent.getStringExtra(EXTRA_REQUEST_ID)

        binding.toolBar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        if (donorId == null || requestId == null || donorId != auth.currentUser?.uid) {
            Toast.makeText(this, "Error: Invalid donation tracking session.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // Initialize the specific reference path
        activeDonationRef = database.getReference("active_donations").child(donorId!!)

        // Start monitoring the live location data
        monitorLiveDonationStatus()

        binding.btnComplete.setOnClickListener {
            completeDonationSession()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Remove the listener to stop receiving location updates when the activity is closed
        donationStatusListener?.let { activeDonationRef.removeEventListener(it) }
    }

    private fun monitorLiveDonationStatus() {
        binding.progressBar.visibility = View.VISIBLE

        donationStatusListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                binding.progressBar.visibility = View.GONE

                if (!snapshot.exists()) {
                    // Node was deleted (session closed externally or completed)
                    binding.tvStatus.text = "Session Closed. Thank you for donating!"
                    binding.btnComplete.isEnabled = false
                    Toast.makeText(this@AcceptedDonationActivity, "Session ended.", Toast.LENGTH_SHORT).show()
                    return
                }

                val tracking = snapshot.getValue(ActiveDonationTracking::class.java)

                if (tracking == null || tracking.requestInfo == null) return

                val request = tracking.requestInfo!!

                // 🌟 Update UI with live data 🌟
                binding.tvSessionTitle.text = "Donation for ${request.patientName ?: "Patient"}"
                binding.tvDonorName.text = "Donor: ${tracking.donorName ?: "You"}"
                binding.tvHospitalName.text = "Hospital: ${request.hospitalName}"

                binding.tvPatientName.text = "Patient: ${request.patientName}"
                binding.tvContactPhone.text = "Contact: ${request.contactPhone}"

                // Location display
                val currentLat = tracking.currentLat ?: 0.0
                val currentLng = tracking.currentLng ?: 0.0

                binding.tvLiveLocation.text = "Location: Lat ${String.format("%.4f", currentLat)}, Lng ${String.format("%.4f", currentLng)}"
            }

            override fun onCancelled(error: DatabaseError) {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this@AcceptedDonationActivity, "Failed to monitor location: ${error.message}", Toast.LENGTH_LONG).show()
            }
        }

        activeDonationRef.addValueEventListener(donationStatusListener!!)
    }

    private fun completeDonationSession() {
        AlertDialog.Builder(this)
            .setTitle("Confirm Arrival")
            .setMessage("By confirming, you mark your arrival. This action will stop location sharing and close the donation session.")
            .setPositiveButton("Confirm & Close") { _, _ ->
                // Delete the tracking node to signal session completion
                activeDonationRef.removeValue()
                    .addOnSuccessListener {
                        Toast.makeText(this, "Session closed. Thank you!", Toast.LENGTH_LONG).show()
                        // Navigate back to the dashboard
                        val intent = Intent(this, MainActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                        startActivity(intent)
                        finish()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Failed to close session.", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    companion object {
        private const val EXTRA_DONOR_ID = "extra_donor_id"
        private const val EXTRA_REQUEST_ID = "extra_request_id"

        fun start(context: Context, donorId: String, requestId: String) {
            val intent = Intent(context, AcceptedDonationActivity::class.java)
            intent.putExtra(EXTRA_DONOR_ID, donorId)
            intent.putExtra(EXTRA_REQUEST_ID, requestId)
            context.startActivity(intent)
        }
    }
}