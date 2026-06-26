package com.tutorial.blooddonationapp

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.widget.addTextChangedListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage // 🌟 IMPORTED 🌟
import java.text.SimpleDateFormat
import java.util.*

// Ensure DonorInfo uses nullable Int for age and DonorRecord has imageUri: String?

class NewDonorRecordActivity : AppCompatActivity() {

    // ... (Existing variable declarations)
    private lateinit var backButton: ImageButton
    private lateinit var hospitalNameText: TextView
    private lateinit var dateText: TextView
    private lateinit var donorIdInput: EditText
    private lateinit var donorInfoCard: LinearLayout
    private lateinit var donorNameText: TextView
    private lateinit var donorAgeText: TextView
    private lateinit var donorGenderText: TextView
    private lateinit var donorBloodTypeText: TextView
    private lateinit var bpInput: EditText
    private lateinit var weightInput: EditText
    private lateinit var unitsInput: EditText
    private lateinit var photoUploadButton: Button
    private lateinit var photoStatusIcon: ImageView
    private lateinit var photoStatusText: TextView
    private lateinit var photoContainer: FrameLayout
    private lateinit var createRecordButton: Button
    private lateinit var photoPreview: ImageView
    // ...

    private var currentDonorInfo: DonorInfo? = null
    private var photoUploaded = false
    private val PICK_IMAGE_REQUEST = 100
    private var selectedImageUri: Uri? = null

    private val database = FirebaseDatabase.getInstance()
    private val storage = FirebaseStorage.getInstance() // 🌟 Storage Instance 🌟
    private val recordsRef = database.getReference("userRecords")
    private val donorLookupRef = database.getReference("donor_lookup")
    private val auth = FirebaseAuth.getInstance()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_new_donor_record)

        initializeViews()
        if (auth.currentUser == null) {
            Toast.makeText(this, "Authentication Failed: Please log in to access the database.", Toast.LENGTH_LONG).show()
        }

        val prefs = getSharedPreferences("user_session", MODE_PRIVATE)
        val hospitalName = prefs.getString("hospital_name", "Not selected") ?: "Not selected"
        hospitalNameText.text = hospitalName

        val currentDate = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(Date())
        dateText.text = currentDate

        setupListeners()
    }

    // ... (initializeViews, setupListeners, fetchDonorInfoFromBackend, updateDonorInfoDisplay, hideDonorInfo remain the same)

    private fun initializeViews() {
        backButton = findViewById(R.id.backButton)
        hospitalNameText = findViewById(R.id.hospitalNameText)
        dateText = findViewById(R.id.dateText)
        donorIdInput = findViewById(R.id.donorIdInput)
        donorInfoCard = findViewById(R.id.donorInfoCard)
        donorNameText = findViewById(R.id.donorNameText)
        donorAgeText = findViewById(R.id.donorAgeText)
        donorGenderText = findViewById(R.id.donorGenderText)
        donorBloodTypeText = findViewById(R.id.donorBloodTypeText)
        bpInput = findViewById(R.id.bpInput)
        weightInput = findViewById(R.id.weightInput)
        unitsInput = findViewById(R.id.unitsInput)
        photoUploadButton = findViewById(R.id.photoUploadButton)
        photoStatusIcon = findViewById(R.id.photoStatusIcon)
        photoStatusText = findViewById(R.id.photoStatusText)
        photoContainer = findViewById(R.id.photoContainer)
        createRecordButton = findViewById(R.id.createRecordButton)
        photoPreview = findViewById(R.id.photoPreview)
    }

    private fun setupListeners() {
        backButton.setOnClickListener {
            finish()
        }

        donorIdInput.addTextChangedListener { text ->
            val donorId = text.toString().uppercase().trim()

            if (donorId.startsWith("PU-") && donorId.length >= 8) {
                fetchDonorInfoFromBackend(donorId)
            } else {
                if (donorId.length < 8) {
                    currentDonorInfo = null
                    hideDonorInfo()
                }
            }
        }

        photoUploadButton.setOnClickListener {
            handlePhotoUpload()
        }

        createRecordButton.setOnClickListener {
            handleCreateRecord()
        }
    }

    private fun fetchDonorInfoFromBackend(donorId: String) {
        val defaultInfo = DonorInfo("", 0, "", "")

        donorLookupRef.child(donorId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d("DB_CHECK", "Lookup Snapshot Exists: ${snapshot.exists()}")

                val fetchedInfo = snapshot.getValue(DonorInfo::class.java)

                if (snapshot.exists() && fetchedInfo != null) {
                    currentDonorInfo = fetchedInfo.copy(puID = donorId)
                    updateDonorInfoDisplay(currentDonorInfo!!, isFound = true)
                } else {
                    currentDonorInfo = null
                    updateDonorInfoDisplay(defaultInfo, isFound = false,
                        message = "Donor ID '$donorId' not found or data is incomplete in lookup node.")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                updateDonorInfoDisplay(defaultInfo, isFound = false,
                    message = "Lookup Error: ${error.message}")
            }
        })
    }

    private fun updateDonorInfoDisplay(info: DonorInfo, isFound: Boolean, message: String? = null) {
        if (isFound) {
            donorInfoCard.visibility = View.VISIBLE
            donorNameText.text = info.name ?: "N/A"

            val displayAge = info.age?.toString() ?: "N/A"
            donorAgeText.text = "$displayAge years"

            donorGenderText.text = info.gender ?: "N/A"
            donorBloodTypeText.text = info.bloodGroup ?: "N/A"
        } else {
            hideDonorInfo()
            if (message != null) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun hideDonorInfo() {
        donorInfoCard.visibility = View.GONE
    }

    private fun handlePhotoUpload() {
        // [Existing photo picking intent logic remains the same]
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        startActivityForResult(Intent.createChooser(intent, "Select Donor Image"), PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE_REQUEST) {
            if (resultCode == Activity.RESULT_OK && data?.data != null) {
                selectedImageUri = data.data

                // Permission fix remains valid here
                contentResolver.takePersistableUriPermission(selectedImageUri!!, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                photoUploaded = true

                // Update UI to show success
                photoStatusIcon.setImageResource(R.drawable.ic_camera)
                photoStatusIcon.setColorFilter(ContextCompat.getColor(this, android.R.color.white), android.graphics.PorterDuff.Mode.SRC_IN)
                photoPreview.setImageURI(selectedImageUri)
                photoPreview.visibility = View.VISIBLE
                photoContainer.setBackgroundResource(R.drawable.bg_circle_green)
                photoStatusText.text = "Photo uploaded successfully!"
                photoStatusText.setTextColor(ContextCompat.getColor(this, R.color.green_600))
                photoUploadButton.visibility = View.GONE

                Toast.makeText(this, "Photo selected successfully!", Toast.LENGTH_SHORT).show()
            } else {
                // ... (Error handling remains the same)
                photoUploaded = false
                photoStatusText.text = "Image selection cancelled or failed."
            }
        }
    }

    private fun handleCreateRecord() {
        // Validation checks
        val donorId = donorIdInput.text.toString().trim()
        val bp = bpInput.text.toString().trim()
        val weight = weightInput.text.toString().trim()
        val units = unitsInput.text.toString().trim()

        if (donorId.isEmpty() || currentDonorInfo == null) {
            Toast.makeText(this, "Please enter a valid donor ID and wait for details to load.", Toast.LENGTH_LONG).show()
            return
        }
        if (bp.isEmpty() || weight.isEmpty() || units.isEmpty()) {
            Toast.makeText(this, "Please fill in all medical information fields.", Toast.LENGTH_SHORT).show()
            return
        }

        // 🌟 STEP 1: Check if an image was selected
        if (photoUploaded && selectedImageUri != null) {
            uploadImageAndSaveRecord(donorId, bp, weight, units)
        } else {
            // If no photo was selected, save record immediately with a null URI
            saveRecordToBackend(donorId, bp, weight, units, null)
        }
    }

    // 🌟 NEW FUNCTION: Uploads image to Firebase Storage 🌟
    private fun uploadImageAndSaveRecord(donorId: String, bp: String, weight: String, units: String) {
        val storageRef = storage.reference.child("donation_records/${UUID.randomUUID()}.jpg")

        storageRef.putFile(selectedImageUri!!)
            .addOnSuccessListener { taskSnapshot ->
                // Get the permanent download URL
                taskSnapshot.storage.downloadUrl.addOnSuccessListener { downloadUri ->
                    // 🌟 STEP 2: Save the record using the permanent HTTPS URL 🌟
                    saveRecordToBackend(donorId, bp, weight, units, downloadUri.toString())
                }.addOnFailureListener { e ->
                    Toast.makeText(this, "Failed to get download URL: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Image upload failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }


    // 🌟 MODIFIED: Takes final image URL (or null) 🌟
    private fun saveRecordToBackend(donorId: String, bp: String, weight: String, units: String, imageUrl: String?) {

        // Ensure creation button is not double-tapped during network lag
        createRecordButton.isEnabled = false

        // Create donor record
        val record = DonorRecord(
            id = UUID.randomUUID().toString(),
            donorId = donorId,
            donorInfo = currentDonorInfo!!,
            hospitalName = hospitalNameText.text.toString(),
            date = dateText.text.toString(),
            bp = bp,
            weight = weight,
            unitsdonated = units,
            // photoUploaded is TRUE only if image URL is available
            photoUploaded = !imageUrl.isNullOrEmpty(),
            createdAt = Date(),
            imageUri = imageUrl // Saved as permanent HTTPS link
        )

        val recordKey = recordsRef.push().key

        if (recordKey != null) {
            val recordToSave = record.copy(id = recordKey)

            recordsRef.child(recordKey).setValue(recordToSave)
                .addOnSuccessListener {
                    createRecordButton.isEnabled = true
                    showSuccessDialog()
                }
                .addOnFailureListener { e ->
                    createRecordButton.isEnabled = true
                    AlertDialog.Builder(this)
                        .setTitle("Error!")
                        .setMessage("Failed to save record to database: ${e.message}")
                        .setIcon(R.drawable.ic_heart_broken)
                        .setPositiveButton("Dismiss", null)
                        .show()
                }
        } else {
            createRecordButton.isEnabled = true
            Toast.makeText(this, "Could not generate a unique record ID. Try again.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showSuccessDialog() {
        AlertDialog.Builder(this)
            .setTitle("Success! 🥳")
            .setMessage("New donor record created and saved to the backend successfully!")
            .setIcon(R.drawable.ic_heart)
            .setPositiveButton("Close") { dialog, _ ->
                dialog.dismiss()
                setResult(Activity.RESULT_OK)
                finish()
            }
            .setCancelable(false)
            .show()
    }
}