package com.tutorial.blooddonationapp

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.bumptech.glide.Glide
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.tutorial.blooddonationapp.databinding.ActivityProfileScreenBinding
import java.io.File

// Assume UserProfile data class is defined with nullable stats:
/*
data class UserProfile(
    // ... other fields ...
    val donationsMade: Long? = null,
    val nextEligibleDate: String? = null
)
*/

class ProfileScreen : AppCompatActivity() {
    private lateinit var binding: ActivityProfileScreenBinding
    private val auth = FirebaseAuth.getInstance()
    private lateinit var googleSignInClient: GoogleSignInClient
    private val GALLERY_REQUEST = 1001
    private val CAMERA_REQUEST = 1002
    private val PERMISSION_REQUEST_CODE = 101
    private var imageUri: Uri? = null
    private var profileSaved = false

    private var userPuId: String? = null
    private val database = FirebaseDatabase.getInstance() // Database instance added

    // 🌟 CRITICAL FIX: Variables to preserve existing stats 🌟
    private var preservedDonationsMade: Long? = null
    private var preservedNextEligibleDate: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.google_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        binding.emailEt.setText(auth.currentUser?.email ?: "")

        binding.profileImage.setOnClickListener { onProfileImageClick() }
        binding.editIcon.setOnClickListener { onProfileImageClick() }

        binding.saveProfileBtn.setOnClickListener {
            saveUserProfile()
        }

        // 🌟 Modified: Load profile and essential stats
        loadUserProfile()
    }

    // ... (utility functions like showLoading, onProfileImageClick, showImagePickerDialog, openGallery, openCamera, onActivityResult remain the same)

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    private fun onProfileImageClick() {
        // ... (permission logic remains the same)
        val permissions = arrayOf(android.Manifest.permission.CAMERA)
        val notGranted = permissions.filter { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
        if (notGranted.isNotEmpty()) {
            requestPermissions(notGranted.toTypedArray(), PERMISSION_REQUEST_CODE)
        } else {
            showImagePickerDialog()
        }
    }

    private fun showImagePickerDialog() {
        val options = arrayOf("Choose from Gallery", "Take Photo", "None")
        AlertDialog.Builder(this)
            .setTitle("Select Profile Picture")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> openGallery()
                    1 -> openCamera()
                    2 -> {
                        imageUri = null
                        binding.profileImage.setImageResource(R.drawable.ic_person_placeholder)
                    }
                }
            }
            .show()
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, GALLERY_REQUEST)
    }

    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        val photoFile = File(externalCacheDir, "profile_${System.currentTimeMillis()}.jpg")
        photoFile.createNewFile()
        imageUri = FileProvider.getUriForFile(this, "$packageName.provider", photoFile)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
        startActivityForResult(intent, CAMERA_REQUEST)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != Activity.RESULT_OK) return
        when (requestCode) {
            GALLERY_REQUEST -> imageUri = data?.data
            CAMERA_REQUEST -> { /* imageUri already set */ }
        }
        if (imageUri != null) {
            binding.profileImage.setImageURI(imageUri)
        } else {
            Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show()
        }
    }

    // 🌟 MODIFIED: Load data AND preserve donation statistics
    private fun loadUserProfile() {
        showLoading(true)
        val currentUser = auth.currentUser
        if (currentUser == null) {
            showLoading(false)
            return
        }

        val dbRef = database.reference.child("users").child(currentUser.uid)
        dbRef.get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                // --- USER EXISTS ---
                // Load form fields
                binding.nameEt.setText(snapshot.child("name").getValue(String::class.java) ?: "")
                // Note: Ensure age is correctly read as Long if stored as such, or String/Int otherwise
                binding.ageEt.setText(snapshot.child("age").getValue(Long::class.java)?.toString() ?: "")

                // Load stats and preserve them globally
                preservedDonationsMade = snapshot.child("donationsMade").getValue(Long::class.java)
                preservedNextEligibleDate = snapshot.child("nextEligibleDate").getValue(String::class.java)

                // Load other fields
                when (snapshot.child("gender").getValue(String::class.java)) {
                    "Male" -> binding.maleRadio.isChecked = true
                    "Female" -> binding.femaleRadio.isChecked = true
                    "Other" -> binding.otherRadio.isChecked = true
                }
                val bloodGroup = snapshot.child("bloodGroup").getValue(String::class.java) ?: "O+"
                val index = resources.getStringArray(R.array.blood_groups).indexOf(bloodGroup)
                if (index >= 0) binding.bloodGroupSpinner.setSelection(index)
                binding.phoneEt.setText(snapshot.child("phone").getValue(String::class.java) ?: auth.currentUser?.phoneNumber)
                binding.bioEt.setText(snapshot.child("bio").getValue(String::class.java) ?: "")

                userPuId = snapshot.child("puId").getValue(String::class.java)?.uppercase()

                // Regenerate PU ID if missing, though it should exist
                if (userPuId.isNullOrEmpty()) {
                    userPuId = "PU-${(10000000..99999999).random()}"
                }

                val imageUrl = snapshot.child("profileImageUrl").getValue(String::class.java)
                if (!imageUrl.isNullOrEmpty()) {
                    Glide.with(this)
                        .load(imageUrl)
                        .placeholder(R.drawable.ic_person_placeholder)
                        .error(R.drawable.ic_person_placeholder)
                        .into(binding.profileImage)
                }
            } else {
                // --- NEW USER ---
                Toast.makeText(this, "Welcome! Please complete your profile.", Toast.LENGTH_SHORT).show()
                userPuId = "PU-${(10000000..99999999).random()}"

                binding.nameEt.setText(currentUser.displayName ?: "")
                binding.emailEt.setText(currentUser.email ?: "")
                binding.phoneEt.setText(currentUser.phoneNumber ?: "")

                // Stats remain null here, will be initialized to 0/default on save.
            }
            showLoading(false)
        }.addOnFailureListener {
            showLoading(false)
            Toast.makeText(this, "Failed to load profile: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveUserProfile() {
        showLoading(true)

        val currentUser = auth.currentUser
        // userPuId is guaranteed to be set either from DB or generated in loadUserProfile
        if (currentUser == null || userPuId == null) {
            showLoading(false)
            Toast.makeText(this, "User or Donor ID not loaded. Please try again.", Toast.LENGTH_SHORT).show()
            return
        }

        if (imageUri != null) {
            // A new image is selected, upload it first
            val storageRef = FirebaseStorage.getInstance().reference
                .child("profile_images/${currentUser.uid}")

            storageRef.putFile(imageUri!!)
                .addOnSuccessListener {
                    storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                        saveDataToDatabase(downloadUri.toString())
                    }.addOnFailureListener {
                        showLoading(false)
                        Toast.makeText(this, "Failed to get download URL", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    showLoading(false)
                    Toast.makeText(this, "Image upload failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            // No new image selected, just save the other profile data.
            saveDataToDatabase(null)
        }
    }

    // 🌟 MODIFIED: Uses preserved stats to prevent overwriting
    private fun saveDataToDatabase(imageUrl: String?) {
        val currentUser = auth.currentUser ?: return
        val puId = userPuId ?: return

        // 🌟 STEP 1: Determine final donation stats (use preserved data first)
        val finalDonationsMade = preservedDonationsMade ?: 0L
        val finalNextEligibleDate = preservedNextEligibleDate ?: "-- -- ----"

        // Step 2: Collect current form data
        val gender = when (binding.genderGroup.checkedRadioButtonId) {
            R.id.maleRadio -> "Male"
            R.id.femaleRadio -> "Female"
            R.id.otherRadio -> "Other"
            else -> "Not Specified"
        }
        val currentName = binding.nameEt.text.toString()
        val currentBloodGroup = binding.bloodGroupSpinner.selectedItem.toString()
        val currentAge = binding.ageEt.text.toString().toIntOrNull() // Note: Age is saved as Int/Long

        // 3. Data for the main /users profile
        val userProfileData = mutableMapOf<String, Any?>(
            "uid" to currentUser.uid,
            "name" to currentName,
            "age" to currentAge,
            "gender" to gender,
            "bloodGroup" to currentBloodGroup,
            "email" to binding.emailEt.text.toString(),
            "phone" to binding.phoneEt.text.toString(),
            "userType" to "public",
            "bio" to binding.bioEt.text.toString().trim(),
            "puId" to puId.uppercase(),

            // 🌟 CRITICAL FIX: Save the preserved stats 🌟
            "donationsMade" to finalDonationsMade,
            "nextEligibleDate" to finalNextEligibleDate
        )

        if (imageUrl != null) {
            userProfileData["profileImageUrl"] = imageUrl
        }

        // 4. Data for the fast /donor_lookup index (Minimal data for hospital screen)
        // Note: The lookup data structure must mirror the UserProfile if you rely on direct mapping
        val lookupData = mapOf(
            "name" to currentName,
            "age" to currentAge,
            "gender" to gender,
            "bloodGroup" to currentBloodGroup,
            "uid" to currentUser.uid
        )

        // 5. Perform a Multi-Location Update (Atomic Transaction)
        val updates = hashMapOf<String, Any>(
            "/users/${currentUser.uid}" to userProfileData,
            "/donor_lookup/${puId.uppercase()}" to lookupData
        )

        FirebaseDatabase.getInstance().reference.updateChildren(updates)
            .addOnCompleteListener { task ->
                showLoading(false)

                if (task.isSuccessful) {
                    profileSaved = true
                    Toast.makeText(this, "Profile and Lookup Data Saved!", Toast.LENGTH_SHORT).show()

                    val intent = Intent(this, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this, "Failed to save profile: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) showImagePickerDialog()
            else Toast.makeText(this, "Camera permission is required", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onBackPressed() {
        if (!profileSaved) {
            AlertDialog.Builder(this)
                .setTitle("Discard Changes?")
                .setMessage("Are you sure you want to exit without saving your changes?")
                .setPositiveButton("Discard") { _, _ ->
                    super.onBackPressed()
                }
                .setNegativeButton("Cancel", null)
                .show()
        } else {
            super.onBackPressed()
        }
    }
}