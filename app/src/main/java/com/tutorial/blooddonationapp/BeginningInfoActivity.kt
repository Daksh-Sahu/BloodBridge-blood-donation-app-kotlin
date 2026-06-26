package com.tutorial.blooddonationapp

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class BeginningInfoActivity : AppCompatActivity() {

    private lateinit var etFullName: EditText
    private lateinit var etPhoneNumber: EditText
    private lateinit var tvPhoneCounter: TextView
    private lateinit var btnProceed: Button
    private lateinit var alertContainer: LinearLayout
    private lateinit var fullNameErrorContainer: LinearLayout
    private lateinit var phoneErrorContainer: LinearLayout
    private lateinit var tvFullNameError: TextView
    private lateinit var tvPhoneError: TextView
    private var hospitalName: String? = null
    private var hospitalAddress: String? = null

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_beginning_info)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        val prefs = getSharedPreferences("user_session", MODE_PRIVATE)
        hospitalName = prefs.getString("hospital_name", "No hospital selected")
        hospitalAddress = prefs.getString("hospital_address", "Address not available")

        initializeViews()
        setupListeners()

        // 🌟 NEW: Fetch existing data from Firebase to pre-fill 🌟
        fetchAndPreFillProfile()
    }

    private fun initializeViews() {
        etFullName = findViewById(R.id.etFullName)
        etPhoneNumber = findViewById(R.id.etPhoneNumber)
        tvPhoneCounter = findViewById(R.id.tvPhoneCounter)
        btnProceed = findViewById(R.id.btnProceed)
        alertContainer = findViewById(R.id.alertContainer)
        fullNameErrorContainer = findViewById(R.id.fullNameErrorContainer)
        phoneErrorContainer = findViewById(R.id.phoneErrorContainer)
        tvFullNameError = findViewById(R.id.tvFullNameError)
        tvPhoneError = findViewById(R.id.tvPhoneError)
    }

    private fun setupListeners() {
        // Phone number text watcher for character counter and validation
        etPhoneNumber.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val length = s?.length ?: 0
                tvPhoneCounter.text = "$length/10"

                if (phoneErrorContainer.visibility == View.VISIBLE && length > 0) {
                    hidePhoneError()
                }
            }

            override fun afterTextChanged(s: Editable?) {
                // Filter only digits
                val input = s.toString()
                val digitsOnly = input.filter { it.isDigit() }

                if (input != digitsOnly) {
                    etPhoneNumber.removeTextChangedListener(this)
                    etPhoneNumber.setText(digitsOnly)
                    etPhoneNumber.setSelection(digitsOnly.length)
                    etPhoneNumber.addTextChangedListener(this)
                }
            }
        })

        // Full name text watcher
        etFullName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (fullNameErrorContainer.visibility == View.VISIBLE && !s.isNullOrBlank()) {
                    hideFullNameError()
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        btnProceed.setOnClickListener {
            handleProceedClick()
        }
    }

    // 🌟 NEW FUNCTION: Fetches and pre-fills user data 🌟
    private fun fetchAndPreFillProfile() {
        val userId = auth.currentUser?.uid

        if (userId == null) {
            // Cannot fetch without a user ID
            return
        }

        // Reference to the user's profile node
        val dbRef = database.getReference("hospital_users").child(userId)

        dbRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    // Fetch existing name and phone number
                    val existingFullName = snapshot.child("fullName").getValue(String::class.java)
                    val existingPhoneNumber = snapshot.child("phoneNumber").getValue(String::class.java)

                    // Pre-fill fields if data is found
                    if (!existingFullName.isNullOrEmpty()) {
                        etFullName.setText(existingFullName)
                    }
                    if (!existingPhoneNumber.isNullOrEmpty()) {
                        etPhoneNumber.setText(existingPhoneNumber)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@BeginningInfoActivity, "Failed to load profile data.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun handleProceedClick() {
        val fullName = etFullName.text.toString().trim()
        val phoneNumber = etPhoneNumber.text.toString().trim()

        var hasErrors = false

        // Validate full name
        if (fullName.isEmpty()) {
            showFullNameError(getString(R.string.full_name_required))
            hasErrors = true
        } else {
            hideFullNameError()
        }

        // Validate phone number
        if (phoneNumber.isEmpty()) {
            showPhoneError(getString(R.string.phone_required))
            hasErrors = true
        } else if (phoneNumber.length != 10) {
            showPhoneError(getString(R.string.phone_invalid))
            hasErrors = true
        } else {
            hidePhoneError()
        }

        if (hasErrors) {
            showAlert()
            return
        }

        hideAlert()
        proceedToDashboard(fullName, phoneNumber)
    }

    private fun showFullNameError(message: String) {
        tvFullNameError.text = message
        fullNameErrorContainer.visibility = View.VISIBLE
        etFullName.setBackgroundResource(R.drawable.alert_background)
    }

    private fun hideFullNameError() {
        fullNameErrorContainer.visibility = View.GONE
        etFullName.setBackgroundResource(R.drawable.input_background)
    }

    private fun showPhoneError(message: String) {
        tvPhoneError.text = message
        phoneErrorContainer.visibility = View.VISIBLE
        etPhoneNumber.setBackgroundResource(R.drawable.alert_background)
    }

    private fun hidePhoneError() {
        phoneErrorContainer.visibility = View.GONE
        etPhoneNumber.setBackgroundResource(R.drawable.input_background)
    }

    private fun showAlert() {
        alertContainer.visibility = View.VISIBLE

        alertContainer.postDelayed({
            hideAlert()
        }, 4000)
    }

    private fun hideAlert() {
        alertContainer.visibility = View.GONE
    }

    private fun proceedToDashboard(fullName: String, phoneNumber: String) {
        // Save user info locally
        val prefs = getSharedPreferences("user_session", MODE_PRIVATE)
        val userEmail = auth.currentUser?.email ?: "Not provided"

        // Save updated info to SharedPreferences
        prefs.edit {
            putString("user_full_name", fullName)
            putString("user_phone_number", phoneNumber)
            putBoolean("is_logged_in", true)
            putString("user_email", userEmail)
            apply()
        }

        // Save the profile updates to Firebase
        saveProfileToFirebase(fullName, phoneNumber)

        // Proceed to Dashboard
        val intent = Intent(this, DashboardActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    private fun saveProfileToFirebase(fullName: String, phoneNumber: String) {
        val userId = auth.currentUser?.uid

        if (userId == null) {
            Toast.makeText(this, "Error: Could not get user ID to save profile", Toast.LENGTH_SHORT).show()
            return
        }

        // Update map uses only the fields that were just edited/validated
        val profileUpdates = mapOf<String, Any>(
            "fullName" to fullName,
            "phoneNumber" to phoneNumber
        )

        // Use .updateChildren() to merge data and avoid overwriting existing fields (like hospital info)
        database.getReference("hospital_users").child(userId)
            .updateChildren(profileUpdates)
            .addOnSuccessListener {
                // Cloud update success
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to update profile to cloud: ${it.message}", Toast.LENGTH_LONG).show()
            }
    }
}