package com.tutorial.blooddonationapp

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.tutorial.blooddonationapp.databinding.ActivityDonationSuccessBinding

class DonationSuccessActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDonationSuccessBinding
    private var patientRequest: PatientRequest? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDonationSuccessBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // --- ** NEW: Get the PatientRequest from the Intent ** ---
        patientRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("EXTRA_PATIENT_REQUEST", PatientRequest::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra("EXTRA_PATIENT_REQUEST")
        }
        // --- ** END NEW ** ---

        binding.btnBackHome.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        // --- ** NEW: Add click listener for the map button ** ---
        binding.btnViewOnMap.setOnClickListener {
            openMap()
        }

        // Show the map button ONLY if we have location data
        if (patientRequest?.hospitalLat != null && patientRequest?.hospitalLng != null) {
            binding.btnViewOnMap.visibility = View.VISIBLE
        }
    }

    // --- ** NEW: Reusable function to open Google Maps ** ---
    private fun openMap() {
        val lat = patientRequest?.hospitalLat
        val lng = patientRequest?.hospitalLng
        val hospitalName = patientRequest?.hospitalName

        if (lat != null && lng != null) {
            // We have precise coordinates
            val gmmIntentUri = Uri.parse("geo:$lat,$lng?q=$lat,$lng($hospitalName)")
            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
            mapIntent.setPackage("com.google.android.apps.maps")
            startActivity(mapIntent)
        } else if (!hospitalName.isNullOrEmpty()) {
            // Fallback: Search by name
            val gmmIntentUri = Uri.parse("geo:0,0?q=$hospitalName")
            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
            mapIntent.setPackage("com.google.android.apps.maps")
            startActivity(mapIntent)
        } else {
            Toast.makeText(this, "Hospital location is not available", Toast.LENGTH_SHORT).show()
        }
    }
}