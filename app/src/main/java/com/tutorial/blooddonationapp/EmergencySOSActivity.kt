package com.tutorial.blooddonationapp

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.Window
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.*

class EmergencySOSActivity : AppCompatActivity() {

    // --- UI Components ---
    private lateinit var backButton: ImageView
    private lateinit var currentDateText: TextView
    private lateinit var hospitalNameText: TextView
    private lateinit var hospitalCoordinatesText: TextView
    private lateinit var patientNameInput: EditText
    private lateinit var bloodTypeSpinner: Spinner
    private lateinit var unitsRequiredInput: EditText
    private lateinit var patientAgeInput: EditText
    private lateinit var patientAgeError: TextView
    private lateinit var contactPersonNumberInput: EditText
    private lateinit var postRequestButton: Button
    private lateinit var loadingProgress: ProgressBar
    private lateinit var patientNameError: TextView
    private lateinit var bloodTypeError: TextView
    private lateinit var unitsRequiredError: TextView
    private lateinit var contactPersonNumberError: TextView

    // --- Data Holders ---
    private var hospitalName: String = "Not loading..."
    private var hospitalLat: Double = 0.0
    private var hospitalLng: Double = 0.0

    // --- Firebase Setup ---
    private val database = FirebaseDatabase.getInstance()
    private val auth = FirebaseAuth.getInstance()
    // Correct node for emergency requests
    private val requestsRef = database.getReference("emergency_requests")
    private val hospitalUsersRef = database.getReference("hospital_users")


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_emergency_sos)

        initializeViews()
        setupDate()
        setupBloodTypeSpinner()
        setupInputListeners()
        setupButtons()

        // Fetch hospital data from Firebase profile
        fetchHospitalLocationAndName()
    }

    private fun initializeViews() {
        backButton = findViewById(R.id.backButton)
        currentDateText = findViewById(R.id.currentDateText)
        hospitalNameText = findViewById(R.id.hospitalNameText)
        hospitalCoordinatesText = findViewById(R.id.hospitalCoordinatesText)
        patientNameInput = findViewById(R.id.patientNameInput)
        bloodTypeSpinner = findViewById(R.id.bloodTypeSpinner)
        unitsRequiredInput = findViewById(R.id.unitsRequiredInput)
        patientAgeInput = findViewById(R.id.patientAgeInput)
        patientAgeError = findViewById(R.id.patientAgeError)

        contactPersonNumberInput = findViewById(R.id.contactPersonNumberInput)
        postRequestButton = findViewById(R.id.postRequestButton)
        loadingProgress = findViewById(R.id.loadingProgress)

        // Error TextViews
        patientNameError = findViewById(R.id.patientNameError)
        bloodTypeError = findViewById(R.id.bloodTypeError)
        unitsRequiredError = findViewById(R.id.unitsRequiredError)
        contactPersonNumberError = findViewById(R.id.contactPersonNumberError)
    }

    private fun setupDate() {
        val dateFormat = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
        val currentDate = dateFormat.format(Date())
        currentDateText.text = currentDate
    }

    // 🌟 Fetches hospital name and location from the logged-in user's profile
    private fun fetchHospitalLocationAndName() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Authentication required to fetch hospital data.", Toast.LENGTH_LONG).show()
            return
        }

        hospitalUsersRef.child(currentUser.uid).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                hospitalName = snapshot.child("hospitalName").getValue(String::class.java) ?: "Not found"
                // Keys must match the SignUpActivity save logic ('hospitalLat', 'hospitalLong')
                hospitalLat = snapshot.child("hospitalLat").getValue(Double::class.java) ?: 0.0
                hospitalLng = snapshot.child("hospitalLong").getValue(Double::class.java) ?: 0.0

                // Update UI after successful fetch
                setupHospitalLocation()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@EmergencySOSActivity, "Failed to load hospital location: ${error.message}", Toast.LENGTH_LONG).show()
                setupHospitalLocation()
            }
        })
    }

    private fun setupHospitalLocation() {
        hospitalNameText.text = hospitalName

        // Display clearer text if coordinates are default 0.0
        if (hospitalLat == 0.0 && hospitalLng == 0.0) {
            hospitalCoordinatesText.text = "Location coordinates unavailable"
        } else {
            hospitalCoordinatesText.text = "Lat: %.4f, Long: %.4f".format(hospitalLat, hospitalLng)
        }
    }


    private fun setupBloodTypeSpinner() {
        val bloodTypes = arrayOf(
            "Select blood type",
            "A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"
        )

        val adapter = object : ArrayAdapter<String>(
            this,
            android.R.layout.simple_spinner_item,
            bloodTypes
        ) {
            override fun isEnabled(position: Int): Boolean {
                return position != 0
            }

            override fun getDropDownView(
                position: Int,
                convertView: View?,
                parent: android.view.ViewGroup
            ): View {
                val view = super.getDropDownView(position, convertView, parent)
                val textView = view as TextView
                val colorId = if (position == 0) android.R.color.darker_gray else android.R.color.black
                textView.setTextColor(resources.getColor(colorId, null))
                return view
            }
        }

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        bloodTypeSpinner.adapter = adapter

        bloodTypeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                if (position > 0) {
                    hideError(bloodTypeError)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupInputListeners() {
        patientNameInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { hideError(patientNameError) }
            override fun afterTextChanged(s: Editable?) {}
        })

        unitsRequiredInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { hideError(unitsRequiredError) }
            override fun afterTextChanged(s: Editable?) {}
        })

        patientAgeInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { hideError(patientAgeError) }
            override fun afterTextChanged(s: Editable?) {}
        })

        contactPersonNumberInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { hideError(contactPersonNumberError) }
            override fun afterTextChanged(s: Editable?) {
                if (s != null && s.length > 10) { s.delete(10, s.length) }
            }
        })
    }

    private fun setupButtons() {
        backButton.setOnClickListener {
            finish()
        }

        postRequestButton.setOnClickListener {
            if (validateForm()) {
                showConfirmationDialog()
            }
        }
    }

    private fun validateForm(): Boolean {
        var isValid = true

        val patientName = patientNameInput.text.toString().trim()
        if (patientName.isEmpty()) { showError(patientNameError, "Patient name is required"); isValid = false }
        if (bloodTypeSpinner.selectedItemPosition == 0) { showError(bloodTypeError, "Blood type is required"); isValid = false }

        val unitsRequired = unitsRequiredInput.text.toString().trim()
        if (unitsRequired.isEmpty() || unitsRequired.toIntOrNull() ?: 0 <= 0) {
            showError(unitsRequiredError, "Please enter a valid number of units"); isValid = false
        }

        val patientAge = patientAgeInput.text.toString().trim()
        val age = patientAge.toIntOrNull() ?: 0
        if (patientAge.isEmpty() || age <= 0 || age > 120) {
            showError(patientAgeError, "Please enter a valid age"); isValid = false
        }

        val contactPersonNumber = contactPersonNumberInput.text.toString().trim()
        if (contactPersonNumber.length != 10) {
            showError(contactPersonNumberError, "Please enter a valid 10-digit phone number"); isValid = false
        }

        return isValid
    }

    private fun showError(errorTextView: TextView, message: String) {
        errorTextView.text = message
        errorTextView.visibility = View.VISIBLE
    }

    private fun hideError(errorTextView: TextView) {
        errorTextView.visibility = View.GONE
    }

    private fun showConfirmationDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_confirmation)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val titleText = dialog.findViewById<TextView>(R.id.dialogTitle)
        val messageText = dialog.findViewById<TextView>(R.id.dialogMessage)
        val cancelButton = dialog.findViewById<Button>(R.id.cancelButton)
        val confirmButton = dialog.findViewById<Button>(R.id.confirmButton)

        titleText.text = "Confirm Emergency Request"
        messageText.text = "Are you sure you want to post this emergency SOS request? This request cannot be edited or deleted once confirmed. It will immediately notify all available donors."

        cancelButton.setOnClickListener { dialog.dismiss() }
        confirmButton.setOnClickListener {
            dialog.dismiss()
            submitEmergencyRequest()
        }
        dialog.show()
    }

    private fun submitEmergencyRequest() {
        setSubmissionState(true)

        val newRequestRef = requestsRef.push()
        val requestId = newRequestRef.key

        if (requestId == null) {
            Toast.makeText(this, "Error: Could not generate request ID.", Toast.LENGTH_LONG).show()
            setSubmissionState(false)
            return
        }

        // 🌟 Data map uses fields that match the PatientRequest model 🌟
        val requestData = mapOf(
            "timestamp" to System.currentTimeMillis(),
            "requestDate" to currentDateText.text.toString(),
            "posterId" to auth.currentUser?.uid, // UID of the hospital user
            "patientName" to patientNameInput.text.toString().trim(),
            "bloodType" to bloodTypeSpinner.selectedItem.toString(),
            "unitsRequired" to unitsRequiredInput.text.toString().trim().toInt(),
            "patientAge" to patientAgeInput.text.toString().trim().toInt(),
            "contactPhone" to contactPersonNumberInput.text.toString().trim(), // Contact number
            "hospitalName" to hospitalName,
            "hospitalLat" to hospitalLat,
            "hospitalLng" to hospitalLng,
            "status" to "pending",
            "requestId" to requestId
        )

        // 2. Push to Firebase /emergency_requests node
        newRequestRef.setValue(requestData)
            .addOnSuccessListener {
                Toast.makeText(this, "Emergency SOS request posted successfully!", Toast.LENGTH_SHORT).show()
                setSubmissionState(false)

                // Navigate to the AwaitingDonorsActivity (Monitoring Screen)
                AwaitingDonorsActivity.start(this, requestId)

                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to post request: ${e.message}", Toast.LENGTH_LONG).show()
                setSubmissionState(false)
            }
    }

    private fun setSubmissionState(isSubmitting: Boolean) {
        postRequestButton.isEnabled = !isSubmitting
        if (isSubmitting) {
            postRequestButton.text = "Posting Emergency Request..."
            loadingProgress.visibility = View.VISIBLE
        } else {
            postRequestButton.text = "Post Emergency Request"
            loadingProgress.visibility = View.GONE
        }
    }

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, EmergencySOSActivity::class.java)
            context.startActivity(intent)
        }
    }
}