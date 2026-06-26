package com.tutorial.blooddonationapp

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.tutorial.blooddonationapp.databinding.ActivityRequestDetailsBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

// Note: Ensure AcceptedDonationActivity and DonationSuccessActivity classes exist

class RequestDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRequestDetailsBinding
    private var patientRequest: PatientRequest? = null
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()
    private val activeDonationsRef = database.getReference("active_donations") // Reference for live tracking

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRequestDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolBar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        patientRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("EXTRA_PATIENT_REQUEST", PatientRequest::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra("EXTRA_PATIENT_REQUEST")
        }

        populateDetails()

        binding.btnAcceptRequest.setOnClickListener {
            acceptRequest()
        }
    }

    private fun populateDetails() {
        patientRequest?.let {
            binding.tvDetailPatientName.text = it.patientName ?: "N/A"
            binding.tvDetailBloodGroup.text = it.bloodType ?: "N/A"
            binding.tvDetailUnits.text = "${it.unitsRequired ?: 0} Units"
            binding.tvDetailAge.text = "${it.patientAge ?: 0} Years"
            binding.tvDetailPhone.text = it.contactPhone ?: "N/A"
            binding.tvDetailHospital.text = it.hospitalName ?: "N/A"
        }
    }

    private fun acceptRequest() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Please log in to accept a request", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Confirm Donation")
            .setMessage("Are you sure you want to accept this request?")
            .setPositiveButton("Yes, I'll Donate") { _, _ ->
                patientRequest?.let { req ->
                    lifecycleScope.launch {
                        proceedWithDonation(currentUser.uid, req)
                    }
                } ?: run {
                    Toast.makeText(this, "Error: Request data is missing.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // --- **MODIFIED: Combined and Corrected Transaction Logic** ---
    private fun proceedWithDonation(donorId: String, request: PatientRequest) {
        val context = this@RequestDetailsActivity
        val rootRef = database.reference

        // Safety checks
        if (request.requestId.isNullOrEmpty() || request.posterId.isNullOrEmpty()) {
            Toast.makeText(context, "FATAL ERROR: Request IDs are missing!", Toast.LENGTH_LONG).show()
            return
        }

        // 1. Get donor's current donation count and details
        rootRef.child("users").child(donorId).get().addOnSuccessListener { snapshot ->
            val currentDonations = snapshot.child("donationsMade").getValue(Long::class.java) ?: 0L
            val nextEligibleDateString = calculateNextEligibleDate()

            val donorName = snapshot.child("name").getValue(String::class.java)
            val donorPhone = snapshot.child("phone").getValue(String::class.java)
            val newDonationCount = currentDonations + 1 // Used for profile update

            // Determine the source node for deletion
            val deletionNode = if (request.isEmergency) "emergency_requests" else "patients"

            // 2. Setup Tracking Data (Used by the Hospital's monitoring screen)
            val trackingData = ActiveDonationTracking(
                donorId = donorId,
                donorName = donorName,
                donorPhone = donorPhone,
                requestInfo = request,
                currentLat = 0.0,
                currentLng = 0.0,
                status = "Accepted",
                acceptedAt = ServerValue.TIMESTAMP
            )

            // 3. Setup Notification
            val notificationId = rootRef.child("notifications").child(request.posterId!!).push().key
            val notificationMessage = mapOf(
                "title" to "Request Accepted!",
                "message" to "Your blood request has been accepted by donor ${donorName ?: "an anonymous donor"}!",
                "donorId" to donorId,
                "donorPhone" to donorPhone
            )

            // 4. Create Multi-path Update Map (Atomic Transaction)
            val updates = mutableMapOf<String, Any?>()

            // A. Update Donor Profile Stats
            updates["/users/$donorId/donationsMade"] = newDonationCount
            updates["/users/$donorId/nextEligibleDate"] = nextEligibleDateString

            // B. History Records
            updates["/history/$donorId/${request.requestId}"] = request
            updates["/history/${request.posterId}/${request.requestId}"] = request

            // C. Delete Request and Create Live Tracking Session (CRITICAL FIX)
            updates["/$deletionNode/${request.requestId}"] = null // Deletes from /patients OR /emergency_requests
            updates["/active_donations/$donorId"] = trackingData // Creates the live tracking session

            // D. Notification
            updates["/notifications/${request.posterId}/$notificationId"] = notificationMessage

            // 5. Execute the update
            rootRef.updateChildren(updates)
                .addOnSuccessListener {
                    Log.d("DonationSuccess", "Transaction successful. Deleted request from $deletionNode.")
                    Toast.makeText(context, "Donation accepted! Thank you!", Toast.LENGTH_LONG).show()

                    // Navigate the DONOR to the SUCCESS screen
                    val intent = Intent(context, DonationSuccessActivity::class.java)
                    intent.putExtra("EXTRA_PATIENT_REQUEST", request)
                    context.startActivity(intent)
                    context.finish()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Transaction Failed: ${e.message}", Toast.LENGTH_LONG).show()
                    Log.e("DonationError", "Multi-path update failed:", e)
                }

        }.addOnFailureListener { e ->
            val errorMessage = e.message ?: "Unknown error"
            Toast.makeText(context, "Failed to read profile: $errorMessage", Toast.LENGTH_LONG).show()
        }
    }

    private fun calculateNextEligibleDate(): String {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, 90)
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        return sdf.format(calendar.time)
    }
}