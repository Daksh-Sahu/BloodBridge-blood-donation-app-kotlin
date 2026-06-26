package com.tutorial.blooddonationapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class AwaitingDonorsActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var cancelButton: Button
    private lateinit var progressBar: View

    // NOTE: These views are assumed to exist based on the XML
    private lateinit var donorInfoContainer: View
    private lateinit var donorNameText: TextView
    private lateinit var donorPhoneText: TextView
    private lateinit var liveLocationText: TextView

    private val handler = Handler(Looper.getMainLooper())

    private val database = FirebaseDatabase.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val requestsRef = database.getReference("emergency_requests")
    private val activeDonationsRef = database.getReference("active_donations")

    private var postedRequestId: String? = null
    private var requestStatusListener: ValueEventListener? = null
    private var trackingListener: ValueEventListener? = null
    private var activeDonorId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_awaiting_donors)

        postedRequestId = intent.getStringExtra(EXTRA_REQUEST_ID)

        if (auth.currentUser == null || postedRequestId == null) {
            Toast.makeText(this, "Error: Missing user or request ID.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        initializeViews()
        setupListeners()

        monitorInitialRequestExistence(postedRequestId!!)
        monitorActiveDonations()
    }

    override fun onDestroy() {
        super.onDestroy()
        requestStatusListener?.let { requestsRef.child(postedRequestId ?: "").removeEventListener(it) }
        trackingListener?.let { activeDonationsRef.removeEventListener(it) }
        handler.removeCallbacksAndMessages(null)
    }

    private fun initializeViews() {
        statusText = findViewById(R.id.statusText)
        cancelButton = findViewById(R.id.cancelButton)
        progressBar = findViewById(R.id.progressBar)

        // Initialize assumed tracking views
        donorInfoContainer = findViewById(R.id.donorInfoContainer)
        donorNameText = findViewById(R.id.donorNameText)
        donorPhoneText = findViewById(R.id.donorPhoneText)
        liveLocationText = findViewById(R.id.liveLocationText)

        // Set tracking info hidden by default
        donorInfoContainer.visibility = View.GONE
    }

    private fun setupListeners() {
        cancelButton.setOnClickListener {
            // Check if the button is disabled to give feedback
            if (!cancelButton.isEnabled) {
                Toast.makeText(this, "Cannot cancel: Donation accepted and in progress.", Toast.LENGTH_SHORT).show()
            } else {
                showCancelConfirmation()
            }
        }
    }

    private fun monitorInitialRequestExistence(requestId: String) {
        requestStatusListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    requestsRef.child(requestId).removeEventListener(this)
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        requestsRef.child(requestId).addValueEventListener(requestStatusListener!!)
    }

    // Monitors the /active_donations node for the matching request ID and updates the UI
    private fun monitorActiveDonations() {
        trackingListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var foundMatch = false

                for (trackingSnapshot in snapshot.children) {
                    val reqId = trackingSnapshot.child("requestInfo/requestId").getValue(String::class.java)

                    if (reqId == postedRequestId) {
                        activeDonorId = trackingSnapshot.key
                        updateTrackingUI(trackingSnapshot)
                        foundMatch = true
                        break
                    }
                }

                if (foundMatch) {
                    // 🌟 FIX: Disable the cancel button when the donor accepts 🌟
                    cancelButton.isEnabled = false
                    // Change button appearance (optional, but good UX)
                    cancelButton.setBackgroundColor(resources.getColor(R.color.gray_500, null))
                } else {
                    // Re-enable if the tracking session somehow ended
                    cancelButton.isEnabled = true
                    cancelButton.setBackgroundColor(resources.getColor(R.color.red_700, null)) // Use your theme color
                    statusText.text = "Waiting for a donor to accept the request..."
                    progressBar.visibility = View.VISIBLE
                    donorInfoContainer.visibility = View.GONE
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@AwaitingDonorsActivity, "Live monitoring failed: ${error.message}", Toast.LENGTH_LONG).show()
            }
        }
        activeDonationsRef.addValueEventListener(trackingListener!!)
    }

    // --- Update UI with Donor Details and Location ---
    private fun updateTrackingUI(snapshot: DataSnapshot) {
        donorInfoContainer.visibility = View.VISIBLE
        progressBar.visibility = View.GONE

        val donorName = snapshot.child("donorName").getValue(String::class.java) ?: "Anonymous Donor"
        val donorPhone = snapshot.child("donorPhone").getValue(String::class.java) ?: "N/A"
        val currentLat = snapshot.child("currentLat").getValue(Double::class.java) ?: 0.0
        val currentLng = snapshot.child("currentLng").getValue(Double::class.java) ?: 0.0

        statusText.text = "ACCEPTED! $donorName is en route."

        donorNameText.text = "Donor: $donorName"
        donorPhoneText.text = "Contact: $donorPhone"
        liveLocationText.text = "Live Location: Lat ${String.format("%.4f", currentLat)}, Lng ${String.format("%.4f", currentLng)}"
    }

    // ... (showCancelConfirmation, cancelRequest, navigateToDashboard, onBackPressed, and companion object remain the same)

    private fun showCancelConfirmation() {
        // [Existing Confirmation Dialog]
        AlertDialog.Builder(this)
            .setTitle("Cancel and Delete Request?")
            .setMessage("Are you sure you want to cancel this emergency request? This action will permanently delete the request from the database, and donors will stop receiving notifications.")
            .setPositiveButton("Yes, Delete Request") { dialog, _ ->
                cancelRequest(postedRequestId!!)
                dialog.dismiss()
            }
            .setNegativeButton("No, Wait", null)
            .show()
    }

    private fun cancelRequest(requestId: String) {
        requestsRef.child(requestId).removeValue()
            .addOnSuccessListener {
                Toast.makeText(this, "Emergency Request deleted successfully.", Toast.LENGTH_SHORT).show()
                // Also attempt to delete the active tracking session if it exists:
                activeDonationsRef.child(activeDonorId ?: "").removeValue()
                navigateToDashboard()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to delete request. Please try again.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun navigateToDashboard() {
        val intent = Intent(this, DashboardActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }

    override fun onBackPressed() {
        if (cancelButton.isEnabled) {
            showCancelConfirmation()
        } else {
            Toast.makeText(this, "Donation in progress. Finish the session first.", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        private const val EXTRA_REQUEST_ID = "extra_request_id"

        fun start(context: Context, requestId: String) {
            val intent = Intent(context, AwaitingDonorsActivity::class.java)
            intent.putExtra(EXTRA_REQUEST_ID, requestId)
            context.startActivity(intent)
        }
    }
}