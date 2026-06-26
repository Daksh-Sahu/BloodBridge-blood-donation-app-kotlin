package com.tutorial.blooddonationapp

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.Date

// Note: Ensure DonorInfo and DonorRecord classes are also defined correctly (as previously discussed).

@Parcelize
data class PatientRequest(
    var requestId: String? = null,
    // ID of the hospital user who posted the request
    var posterId: String? = null,
    val patientName: String? = null,
    val bloodType: String? = null,
    val unitsRequired: Int? = 0,
    val patientAge: Int? = 0,
    // The contact number to call (used by Donor to reach Hospital)
    val contactPhone: String? = null,
    val hospitalName: String? = null,
    val timestamp: Long? = 0L,
    val hospitalLat: Double? = null,
    val hospitalLng: Double? = null,
    // Flag used for displaying the card prominently
    var isEmergency: Boolean = false
) : Parcelable

// --- Assumed Data Class for Live Tracking ---
// This is what is saved under /active_donations/{donorId}
data class ActiveDonationTracking(
    val donorId: String? = null,
    val donorName: String? = null,
    val donorPhone: String? = null,
    val requestInfo: PatientRequest? = null, // Embeds the original request details
    val currentLat: Double? = 0.0, // Used for live updates
    val currentLng: Double? = 0.0,
    val status: String? = null,
    val acceptedAt: Any? = null // ServerValue.TIMESTAMP
)