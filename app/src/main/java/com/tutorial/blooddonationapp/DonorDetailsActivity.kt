package com.tutorial.blooddonationapp

import android.net.Uri
import android.os.Bundle
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.bumptech.glide.Glide
import java.text.SimpleDateFormat
import java.util.Locale

class DonorDetailsActivity : AppCompatActivity() {

    // Views from activity_donor_details.xml
    private lateinit var backButton: ImageButton
    private lateinit var profilePhoto: ImageView
    private lateinit var donorNameTitle: TextView
    private lateinit var donorIdSubtitle: TextView
    private lateinit var basicDonorId: TextView
    private lateinit var basicName: TextView
    private lateinit var basicAge: TextView
    private lateinit var basicGender: TextView
    private lateinit var basicBloodType: TextView
    private lateinit var basicDate: TextView
    private lateinit var medicalBP: TextView
    private lateinit var medicalWeight: TextView
    private lateinit var medicalUnits: TextView
    private lateinit var hospitalName: TextView
    private lateinit var hospitalRecordDate: TextView
    private lateinit var uploadedPhoto: ImageView
    private lateinit var photoCaption: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_donor_details)

        initializeViews()
        backButton.setOnClickListener { finish() }

        // Get the Parcelable record from the intent
        val record: DonorRecord? = intent.getParcelableExtra("DONOR_RECORD")

        if (record != null) {
            populateDetails(record)
        } else {
            Toast.makeText(this, "Error: Donor record data missing.", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun initializeViews() {
        backButton = findViewById(R.id.backButton)
        profilePhoto = findViewById(R.id.profilePhoto)
        donorNameTitle = findViewById(R.id.donorNameTitle)
        donorIdSubtitle = findViewById(R.id.donorIdSubtitle)
        basicDonorId = findViewById(R.id.basicDonorId)
        basicName = findViewById(R.id.basicName)
        basicAge = findViewById(R.id.basicAge)
        basicGender = findViewById(R.id.basicGender)
        basicBloodType = findViewById(R.id.basicBloodType)
        basicDate = findViewById(R.id.basicDate)
        medicalBP = findViewById(R.id.medicalBP)
        medicalWeight = findViewById(R.id.medicalWeight)
        medicalUnits = findViewById(R.id.medicalUnits)
        hospitalName = findViewById(R.id.hospitalName)
        hospitalRecordDate = findViewById(R.id.hospitalRecordDate)
        uploadedPhoto = findViewById(R.id.uploadedPhoto)
        photoCaption = findViewById(R.id.photoCaption)
    }

    private fun populateDetails(record: DonorRecord) {
        // --- Safely retrieve donor info ---
        val info = record.donorInfo
        val name = info.name ?: "N/A"
        val id = record.donorId
        val age = info.age?.toString() ?: "N/A"
        val gender = info.gender ?: "N/A"
        val bloodGroup = info.bloodGroup ?: "N/A"
        val donationDate = record.date

        // Attempt to format creation date
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm a", Locale.getDefault())
        val recordCreationTime = try {
            "${dateFormat.format(record.createdAt)} at ${timeFormat.format(record.createdAt)}"
        } catch (e: Exception) {
            record.date // Fallback to recorded date
        }

        // --- Header and Basic Info ---
        donorNameTitle.text = name
        donorIdSubtitle.text = "Donor ID: $id"

        basicDonorId.text = id
        basicName.text = name
        basicAge.text = "$age years"
        basicGender.text = gender
        basicBloodType.text = bloodGroup
        basicDate.text = donationDate

        // --- Medical Info ---
        medicalBP.text = record.bp
        medicalWeight.text = "${record.weight} kg"
        medicalUnits.text = "${record.unitsdonated} units"

        // --- Hospital Info ---
        hospitalName.text = record.hospitalName
        hospitalRecordDate.text = "Record created: $recordCreationTime"

        // --- Photo Display ---
        if (record.photoUploaded && !record.imageUri.isNullOrEmpty()) {
            Glide.with(this)
                .load(Uri.parse(record.imageUri))
                .placeholder(R.drawable.ic_user)
                .error(R.drawable.ic_heart_broken)
                .into(uploadedPhoto)

            photoCaption.text = "Photo captured during donation on $donationDate"
        } else {
            // Show placeholder if no photo uploaded
            uploadedPhoto.setImageResource(R.drawable.ic_camera)
            photoCaption.text = "No photo uploaded for this record."
        }

        // Placeholder for main profile photo (can be replaced if you fetch the user's main profile image later)
        profilePhoto.setImageResource(R.drawable.ic_user)
    }
}