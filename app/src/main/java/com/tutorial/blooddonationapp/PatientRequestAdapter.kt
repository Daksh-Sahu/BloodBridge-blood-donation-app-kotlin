package com.tutorial.blooddonationapp

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.tutorial.blooddonationapp.databinding.ItemPatientRequestBinding

class PatientRequestAdapter(
    private var patientRequests: List<PatientRequest>,
    private val onItemClicked: (PatientRequest) -> Unit
) : RecyclerView.Adapter<PatientRequestAdapter.PatientRequestViewHolder>() {

    inner class PatientRequestViewHolder(val binding: ItemPatientRequestBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(request: PatientRequest) {
            val context = binding.root.context

            // Get default colors
            val colorWhite = ContextCompat.getColor(context, android.R.color.white)
            val colorRedPrimary = ContextCompat.getColor(context, R.color.red)
            val colorDarkGray = ContextCompat.getColor(context, android.R.color.darker_gray)
            val colorBlack = ContextCompat.getColor(context, android.R.color.black)

            // --- Bind Core Data ---
            binding.tvPatientName.text = request.patientName ?: "N/A"
            binding.tvBloodGroup.text = request.bloodType ?: "N/A"
            binding.tvHospitalName.text = request.hospitalName ?: "N/A"
            binding.tvUnits.text = "${request.unitsRequired ?: 0} Units"

            // 1. EMERGENCY INDICATION LOGIC
            if (request.isEmergency) {
                // Style for EMERGENCY CARD
                binding.root.setCardBackgroundColor(ContextCompat.getColor(context, R.color.red_700))
                binding.root.strokeColor = colorWhite
                binding.root.strokeWidth = 4

                binding.tvEmergencyBadge.visibility = View.VISIBLE
                binding.tvEmergencyBadge.text = "🚨 EMERGENCY"

                binding.tvPatientName.setTextColor(colorWhite)
                binding.tvBloodGroup.setTextColor(colorRedPrimary)
                binding.tvUnits.setTextColor(colorWhite)
                binding.tvHospitalName.setTextColor(colorWhite)

            } else {
                // Standard Card Appearance
                binding.root.setCardBackgroundColor(colorWhite)
                binding.root.strokeWidth = 0
                binding.tvEmergencyBadge.visibility = View.GONE

                binding.tvPatientName.setTextColor(colorBlack)
                binding.tvBloodGroup.setTextColor(colorRedPrimary)
                binding.tvUnits.setTextColor(colorDarkGray)
                binding.tvHospitalName.setTextColor(colorDarkGray)
            }

            // 2. PRIMARY CARD CLICK HANDLER
            binding.root.setOnClickListener {
                onItemClicked(request)
            }

            // 🌟 3. MAP BUTTON IMPLEMENTATION (RESTORED TO ORIGINAL WORKING LOGIC) 🌟
            val lat = request.hospitalLat
            val lng = request.hospitalLng

            // Only show the map button if we have location data
            if (lat != null && lng != null) {
                binding.btnCardMap.visibility = View.VISIBLE
                binding.btnCardMap.setOnClickListener {
                    // Create the Geo URI
                    val gmmIntentUri = Uri.parse("geo:$lat,$lng?q=$lat,$lng(${request.hospitalName})")
                    val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                    // Set the package to ensure it opens Google Maps
                    mapIntent.setPackage("com.google.android.apps.maps")

                    // Start the activity from the card's context
                    binding.root.context.startActivity(mapIntent)
                }
            } else {
                // Hide the button if no location is saved
                binding.btnCardMap.visibility = View.GONE
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PatientRequestViewHolder {
        val binding = ItemPatientRequestBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PatientRequestViewHolder(binding)
    }

    override fun getItemCount(): Int = patientRequests.size

    override fun onBindViewHolder(holder: PatientRequestViewHolder, position: Int) {
        holder.bind(patientRequests[position])
    }

    fun updateList(newList: List<PatientRequest>) {
        patientRequests = newList
        notifyDataSetChanged()
    }
}