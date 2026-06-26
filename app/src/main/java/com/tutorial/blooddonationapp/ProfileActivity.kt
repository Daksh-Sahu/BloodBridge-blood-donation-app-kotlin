package com.tutorial.blooddonationapp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage


class ProfileActivity : AppCompatActivity() {
    private val PICK_PROFILE_IMAGE = 200
    private var selectedProfileUri: Uri? = null

    private lateinit var ivBack: ImageView
    private lateinit var ivProfileImage: ImageView
    private lateinit var ivCameraIcon: ImageView
    private lateinit var tvFullNameValue: TextView
    private lateinit var tvPhoneValue: TextView
    private lateinit var tvFullNameEdit: EditText
    private lateinit var tvPhoneEdit: EditText
    private lateinit var llFullNameDisplay: LinearLayout
    private lateinit var llPhoneDisplay: LinearLayout
    private lateinit var llFullNameEdit: LinearLayout
    private lateinit var llPhoneEdit: LinearLayout
    private lateinit var ivEditName: ImageView
    private lateinit var ivEditPhone: ImageView
    private lateinit var ivSaveName: ImageView
    private lateinit var ivCancelName: ImageView
    private lateinit var ivSavePhone: ImageView
    private lateinit var ivCancelPhone: ImageView
    private lateinit var tvEmailValue: TextView
    private lateinit var tvUserId: TextView

    // Firebase variables
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var storage: FirebaseStorage
    private var currentUserId: String? = null

    private var originalFullName: String = ""
    private var originalPhoneNumber: String = ""

    companion object {
        const val EXTRA_FULL_NAME = "fullName"
        const val EXTRA_PHONE_NUMBER = "phoneNumber"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        storage = FirebaseStorage.getInstance()
        currentUserId = auth.currentUser?.uid

        // Check if user is logged in
        if (currentUserId == null) {
            Toast.makeText(this, "Error: User not logged in", Toast.LENGTH_LONG).show()
            finish() // Close activity if no user
            return
        }

        getUserInfo() // ✅ Re-added this function to get Intent data
        initializeViews()
        setupListeners()
        populateUserInfo() // This will now use the Intent data as a fallback
    }

    // ✅ This function is now back
    private fun getUserInfo() {
        originalFullName = intent.getStringExtra(EXTRA_FULL_NAME) ?: ""
        originalPhoneNumber = intent.getStringExtra(EXTRA_PHONE_NUMBER) ?: ""
    }


    private fun initializeViews() {
        ivBack = findViewById(R.id.ivBack)
        ivProfileImage = findViewById(R.id.ivProfileImage)
        ivCameraIcon = findViewById(R.id.ivCameraIcon)
        // Display views
        tvFullNameValue = findViewById(R.id.tvFullNameValue)
        tvPhoneValue = findViewById(R.id.tvPhoneValue)
        llFullNameDisplay = findViewById(R.id.llFullNameDisplay)
        llPhoneDisplay = findViewById(R.id.llPhoneDisplay)
        // Edit views
        tvFullNameEdit = findViewById(R.id.tvFullNameEdit)
        tvPhoneEdit = findViewById(R.id.tvPhoneEdit)
        llFullNameEdit = findViewById(R.id.llFullNameEdit)
        llPhoneEdit = findViewById(R.id.llPhoneEdit)
        // Action buttons
        ivEditName = findViewById(R.id.ivEditName)
        ivEditPhone = findViewById(R.id.ivEditPhone)
        ivSaveName = findViewById(R.id.ivSaveName)
        ivCancelName = findViewById(R.id.ivCancelName)
        ivSavePhone = findViewById(R.id.ivSavePhone)
        ivCancelPhone = findViewById(R.id.ivCancelPhone)
        tvEmailValue = findViewById(R.id.tvEmailValue)
        tvUserId = findViewById(R.id.tvUserId)

    }

    private fun setupListeners() {
        ivBack.setOnClickListener {
            finish()
        }

        ivCameraIcon.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "image/*"
            startActivityForResult(
                Intent.createChooser(intent, "Select Profile Picture"),
                PICK_PROFILE_IMAGE
            )
        }


        // Full Name Edit Actions
        ivEditName.setOnClickListener {
            startEditingName()
        }

        ivSaveName.setOnClickListener {
            saveNameChanges()
        }

        ivCancelName.setOnClickListener {
            cancelNameEdit()
        }

        // Phone Edit Actions
        ivEditPhone.setOnClickListener {
            startEditingPhone()
        }

        ivSavePhone.setOnClickListener {
            savePhoneChanges()
        }

        ivCancelPhone.setOnClickListener {
            cancelPhoneEdit()
        }

        // Phone number input filter - only digits
        tvPhoneEdit.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val input = s.toString()
                val digitsOnly = input.filter { it.isDigit() }
                if (input != digitsOnly || digitsOnly.length > 10) {
                    tvPhoneEdit.removeTextChangedListener(this)
                    tvPhoneEdit.setText(digitsOnly.take(10))
                    tvPhoneEdit.setSelection(tvPhoneEdit.text.length)
                    tvPhoneEdit.addTextChangedListener(this)
                }
            }
        })
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_PROFILE_IMAGE && resultCode == RESULT_OK && data?.data != null) {
            selectedProfileUri = data.data

            // Load image in circular view locally
            Glide.with(this)
                .load(selectedProfileUri)
                .circleCrop()
                .placeholder(R.drawable.default_profile_image)
                .into(ivProfileImage)

            // Upload to Firebase Storage
            uploadProfileImageToStorage()

        } else if (resultCode == RESULT_CANCELED) {
            Toast.makeText(this, "Image selection cancelled", Toast.LENGTH_SHORT).show()
        }
    }

    private fun uploadProfileImageToStorage() {
        if (selectedProfileUri == null || currentUserId == null) {
            Toast.makeText(this, "No image selected or user not logged in", Toast.LENGTH_SHORT)
                .show()
            return
        }

        val storageRef = storage.reference.child("profile_images/$currentUserId.jpg")
        val uploadTask = storageRef.putFile(selectedProfileUri!!)

        uploadTask.addOnSuccessListener {
            storageRef.downloadUrl.addOnSuccessListener { uri ->
                val downloadUrl = uri.toString()
                saveProfileUrlToDatabase(downloadUrl)
            }.addOnFailureListener {
                Toast.makeText(this, "Failed to get download URL", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Image upload failed: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveProfileUrlToDatabase(downloadUrl: String) {
        if (currentUserId == null) return

        // ✅ Changed branch to "hospital_users"
        val userRef = database.getReference("hospital_users").child(currentUserId!!)
        userRef.child("profileImageUrl").setValue(downloadUrl)
            .addOnSuccessListener {
                Toast.makeText(this, "Profile picture updated!", Toast.LENGTH_SHORT).show()
                val prefs = getSharedPreferences("user_session", MODE_PRIVATE)
                prefs.edit().putString("profile_image_uri", downloadUrl).apply()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to save image URL to database", Toast.LENGTH_SHORT)
                    .show()
            }
    }


    private fun populateUserInfo() {
        val prefs = getSharedPreferences("user_session", MODE_PRIVATE)
        val userEmailFromPrefs = prefs.getString("user_email", "Not provided") ?: "Not provided"


        currentUserId?.let { uid ->
            // ✅ Changed branch to "hospital_users"
            val userRef = database.getReference("hospital_users").child(uid)

            userRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        // User profile exists in DB, load it
                        val userProfile = snapshot.getValue(HospitalUserProfile::class.java)
                        userProfile?.let {
                            originalFullName = it.fullName ?: originalFullName // ✅ Use intent data as fallback
                            originalPhoneNumber = it.phoneNumber ?: originalPhoneNumber // ✅ Use intent data as fallback

                            tvFullNameValue.text = if (originalFullName.isNotEmpty()) originalFullName else "Not provided"
                            tvPhoneValue.text = if (originalPhoneNumber.isNotEmpty()) originalPhoneNumber else "Not provided"
                            tvEmailValue.text = it.email ?: userEmailFromPrefs
                            tvUserId.text = it.userId ?: "No user id generated please login"

                            if (!it.profileImageUrl.isNullOrEmpty()) {
                                Glide.with(this@ProfileActivity)
                                    .load(it.profileImageUrl)
                                    .circleCrop()
                                    .placeholder(R.drawable.default_profile_image)
                                    .into(ivProfileImage)
                            }
                        }
                    } else {
                        // ✅ No profile in DB. Use Intent data and create one.
                        Toast.makeText(this@ProfileActivity, "Creating profile...", Toast.LENGTH_SHORT).show()
                        tvEmailValue.text = userEmailFromPrefs
                        tvFullNameValue.text = if (originalFullName.isNotEmpty()) originalFullName else "Not provided"
                        tvPhoneValue.text = if (originalPhoneNumber.isNotEmpty()) originalPhoneNumber else "Not provided"

                        // Create the profile in Firebase using Intent data
                        createInitialProfileInDatabase(userEmailFromPrefs, originalFullName, originalPhoneNumber)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@ProfileActivity, "Failed to load profile: ${error.message}", Toast.LENGTH_SHORT).show()
                    // Fallback to Intent data if DB read fails
                    populateFromIntentData()
                }
            })
        } ?: populateFromIntentData() // Fallback if no user ID
    }

    // ✅ New Function: Creates the user's profile in DB for the first time
    private fun createInitialProfileInDatabase(email: String, name: String, phone: String) {
        if (currentUserId == null) return

        // ✅ Changed branch to "hospital_users"
        val userRef = database.getReference("hospital_users").child(currentUserId!!)

        val newProfile = HospitalUserProfile(
            userId = currentUserId,
            fullName = if (name.isNotEmpty()) name else null, // ✅ Save name from intent
            phoneNumber = if (phone.isNotEmpty()) phone else null, // ✅ Save phone from intent
            email = email,
            profileImageUrl = null
        )

        userRef.setValue(newProfile).addOnFailureListener {
            Toast.makeText(this, "Failed to create initial profile: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // ✅ Renamed function to be more accurate
    private fun populateFromIntentData() {
        // This is a fallback if Firebase fails. Use the data passed in the intent.
        val prefs = getSharedPreferences("user_session", MODE_PRIVATE)
        tvFullNameValue.text = if (originalFullName.isNotEmpty()) originalFullName else "Not provided"
        tvPhoneValue.text = if (originalPhoneNumber.isNotEmpty()) originalPhoneNumber else "Not provided"
        tvEmailValue.text = prefs.getString("user_email", "Not provided") ?: "Not provided"

        val profileImageUri = prefs.getString("profile_image_uri", null)
        if (profileImageUri != null) {
            Glide.with(this)
                .load(Uri.parse(profileImageUri))
                .circleCrop()
                .placeholder(R.drawable.default_profile_image)
                .into(ivProfileImage)
        }
    }


    private fun startEditingName() {
        tvFullNameEdit.setText(originalFullName)
        llFullNameDisplay.visibility = View.GONE
        llFullNameEdit.visibility = View.VISIBLE
    }

    private fun saveNameChanges() {
        val newName = tvFullNameEdit.text.toString().trim()
        if (newName.isNotEmpty()) {
            originalFullName = newName
            tvFullNameValue.text = newName
            llFullNameEdit.visibility = View.GONE
            llFullNameDisplay.visibility = View.VISIBLE

            updateProfileField("fullName", newName)
        } else {
            Toast.makeText(this, "Name cannot be empty", Toast.LENGTH_SHORT).show()
        }
    }


    private fun cancelNameEdit() {
        llFullNameEdit.visibility = View.GONE
        llFullNameDisplay.visibility = View.VISIBLE
    }

    private fun startEditingPhone() {
        tvPhoneEdit.setText(originalPhoneNumber)
        llPhoneDisplay.visibility = View.GONE
        llPhoneEdit.visibility = View.VISIBLE
    }

    private fun savePhoneChanges() {
        val newPhone = tvPhoneEdit.text.toString().trim()
        if (newPhone.length == 10) {
            originalPhoneNumber = newPhone
            tvPhoneValue.text = newPhone
            llPhoneEdit.visibility = View.GONE
            llPhoneDisplay.visibility = View.VISIBLE

            updateProfileField("phoneNumber", newPhone)
        } else {
            Toast.makeText(this, "Phone number must be exactly 10 digits", Toast.LENGTH_SHORT)
                .show()
        }
    }

    private fun updateProfileField(field: String, value: String) {
        if (currentUserId == null) {
            Toast.makeText(this, "Error: User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        // ✅ Changed branch to "hospital_users"
        val userRef = database.getReference("hospital_users").child(currentUserId!!)
        userRef.child(field).setValue(value)
            .addOnSuccessListener {
                Toast.makeText(this, "$field updated successfully", Toast.LENGTH_SHORT).show()
                val prefs = getSharedPreferences("user_session", MODE_PRIVATE)
                prefs.edit()
                    .putString(if (field == "fullName") "user_full_name" else "user_phone_number", value)
                    .apply()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to update $field: ${it.message}", Toast.LENGTH_SHORT)
                    .show()
            }
    }


    private fun cancelPhoneEdit() {
        llPhoneEdit.visibility = View.GONE
        llPhoneDisplay.visibility = View.VISIBLE
    }

    override fun finish() {
        val resultIntent = Intent().apply {
            putExtra("fullName", originalFullName)
            putExtra("phoneNumber", originalPhoneNumber)
        }
        setResult(RESULT_OK, resultIntent)
        super.finish()
    }
}