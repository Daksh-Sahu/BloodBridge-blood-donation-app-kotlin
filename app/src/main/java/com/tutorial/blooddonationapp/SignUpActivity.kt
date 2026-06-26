package com.tutorial.blooddonationapp

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.edit
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener

// You can move this to its own file (e.g., UserProfile.kt) if you haven't already
data class UserProfile(
    val userId: String? = null,
    val fullName: String? = null,
    val phoneNumber: String? = null,
    val email: String? = null,
    val profileImageUrl: String? = null,
    val hospitalName: String? = null,
    val hospitalAddress: String? = null,
    val hospitalLat: Double? = null,
    val hospitalLong: Double? = null,

    // Made nullable for reading existing data
    val donationsMade: Long? = null,
    val nextEligibleDate: String? = null
)

// Assume Hospital2 and HospitalAdapter exist in your project

class SignUpActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    private lateinit var autoCompleteHospital: AutoCompleteTextView
    private lateinit var hospitalSelectionCard: CardView
    private lateinit var signUpCard: CardView

    private val hospitals = mutableListOf<Hospital2>()
    private lateinit var hospitalAdapter: HospitalAdapter

    private lateinit var tvHospitalName: TextView
    private lateinit var tvHospitalAddress: TextView
    private lateinit var hospitalInfoCard: CardView
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var ivPasswordToggle: ImageView
    private lateinit var ivConfirmPasswordToggle: ImageView
    private lateinit var btnSignUp: Button
    private lateinit var tvEmailError: TextView
    private lateinit var tvPasswordError: TextView
    private lateinit var tvBackToSignIn: TextView
    private lateinit var progressBar: ProgressBar

    // 🌟 CRITICAL FIX: Variables to store existing stats from DB 🌟
    private var currentDonationsMade: Long? = null
    private var currentNextEligibleDate: String? = null

    private var selectedHospital: Hospital2? = null
    private var isPasswordVisible = false
    private var isConfirmPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        getHospitalFromIntent()
        initializeViews()
        setupHospitalData()
        setupAutoComplete()
        setupHospitalInfo()
        setupListeners()

        // 🌟 NEW: Load existing profile data (including stats) on startup 🌟
        loadExistingUserProfileForEdit()
    }

    // 🌟 NEW FUNCTION: Loads stats and name if user already exists 🌟
    private fun loadExistingUserProfileForEdit() {
        val currentUser = auth.currentUser
        if (currentUser == null) return

        val dbRef = database.reference.child("hospital_users").child(currentUser.uid)

        // Use a single read operation to get current stats
        dbRef.addListenerForSingleValueEvent(object: ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    // This user has an existing profile! Preserve stats.
                    currentDonationsMade = snapshot.child("donationsMade").getValue(Long::class.java)
                    currentNextEligibleDate = snapshot.child("nextEligibleDate").getValue(String::class.java)

                    // You might also want to pre-fill the form fields here if this activity is used for editing
                    // For example, pre-filling the hospital from the saved data:
                    // val savedHospitalName = snapshot.child("hospitalName").getValue(String::class.java)
                    // autoCompleteHospital.setText(savedHospitalName, false)
                }
            }
            override fun onCancelled(error: DatabaseError) {
                // Ignore failure; stats remain null, which is handled by the writer
            }
        })
    }

    private fun setupHospitalData() {
        // ... (Hospital data remains the same)
        hospitals.addAll(
            listOf(
                Hospital2("1", "Sparsh Hospital", "RR Nagar, Bangalore, 560098", 12.926827799784602, 77.52129580801484),
                Hospital2("2", "BGS Gleneagles Hospital", "Uttarahalli, Bangalore, 560060",12.902825442982833, 77.49764490325491),
                Hospital2("3", "Rashtrothana Hospital", "RR Nagar, Bangalore, 560098",12.909071518718536, 77.513549998747),
                Hospital2("4", "Manipal Hospital", "Old Airport Road, Kodihalli, Bangalore 560017",12.958568832612087, 77.64878275034528),
                Hospital2("5", "Apollo Hospital", "Bannerghata road, Bangalore, 560076",12.896214526768329, 77.59858879146624),
                Hospital2("6", "Fortis Hospital", "Bannerghata road, Bangalore, 560076",12.894886643539039, 77.59861874183741),
                Hospital2("7", "Rajarajeshwari Hospital", "Mysore road, Bangalore, 560074",12.896476551512759, 77.46179338102488),
                Hospital2("8", "Ramaiah Memorial Hospital", "MSR Nagar, Bangalore, 560054",13.028376623014996, 77.569776052987),
                Hospital2("9", "People Tree Hospital", "Dasarahalli, Bangalore, 560057",13.042535075593632, 77.51557896608773),
                Hospital2("10", "Sagar Hospital", "Jayanagar, Bangalore, 560041",12.928076828669106, 77.59942295034483)
            )
        )
    }


    private fun setupAutoComplete() {
        hospitalAdapter = HospitalAdapter(this, hospitals)
        autoCompleteHospital.setAdapter(hospitalAdapter)

        autoCompleteHospital.threshold = 0

        autoCompleteHospital.setOnClickListener {
            if (!autoCompleteHospital.isPopupShowing) {
                autoCompleteHospital.showDropDown()
            }
        }

        autoCompleteHospital.setOnItemClickListener { parent, _, position, _ ->
            val hospital = parent.getItemAtPosition(position) as Hospital2
            selectedHospital = hospital
            updateUIForSelectedHospital()
        }

        autoCompleteHospital.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (selectedHospital != null && s.toString() != selectedHospital?.getDisplayName()) {
                    selectedHospital = null
                    hospitalInfoCard.visibility = View.GONE
                    signUpCard.visibility = View.GONE
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun getHospitalFromIntent() {
        val hospitalId = intent.getStringExtra("hospital_id") ?: ""
        val hospitalName = intent.getStringExtra("hospital_name") ?: ""
        val hospitalAddress = intent.getStringExtra("hospital_address") ?: ""
        val hospitalLat = intent.getDoubleExtra("hospital_lat", 0.0)
        val hospitalLong = intent.getDoubleExtra("hospital_long", 0.0)

        if (hospitalId.isNotEmpty()) {
            selectedHospital = Hospital2(
                id = hospitalId,
                name = hospitalName,
                address = hospitalAddress,
                lat = hospitalLat,
                long = hospitalLong
            )
        }
    }


    private fun initializeViews() {
        autoCompleteHospital = findViewById(R.id.autoCompleteHospital)
        hospitalSelectionCard = findViewById(R.id.hospitalSelectionCard)
        tvHospitalName = findViewById(R.id.tvHospitalName)
        tvHospitalAddress = findViewById(R.id.tvHospitalAddress)
        hospitalInfoCard = findViewById(R.id.hospitalInfoCard)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        ivPasswordToggle = findViewById(R.id.ivPasswordToggle)
        ivConfirmPasswordToggle = findViewById(R.id.ivConfirmPasswordToggle)
        btnSignUp = findViewById(R.id.btnSignUp)
        tvEmailError = findViewById(R.id.tvEmailError)
        tvPasswordError = findViewById(R.id.tvPasswordError)
        tvBackToSignIn = findViewById(R.id.tvBackToSignIn)
        progressBar = findViewById(R.id.progressBar)
        signUpCard = findViewById(R.id.signUpCard)
        signUpCard.visibility = View.GONE
    }

    private fun setupHospitalInfo() {
        selectedHospital?.let { hospital ->
            tvHospitalName.text = hospital.name
            tvHospitalAddress.text = hospital.address
            hospitalInfoCard.visibility = View.VISIBLE
        } ?: run {
            hospitalInfoCard.visibility = View.GONE
        }
    }

    private fun setupListeners() {
        etEmail.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                validateEmail()
                updateSignUpButtonState()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        etPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                validatePasswords()
                updateSignUpButtonState()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        etConfirmPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                validatePasswords()
                updateSignUpButtonState()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        ivPasswordToggle.setOnClickListener {
            togglePasswordVisibility()
        }

        ivConfirmPasswordToggle.setOnClickListener {
            toggleConfirmPasswordVisibility()
        }

        btnSignUp.setOnClickListener {
            handleSignUp()
        }

        tvBackToSignIn.setOnClickListener {
            finish()
        }
    }

    private fun validateEmail() {
        val email = etEmail.text.toString().trim()

        if (email.isEmpty()) {
            tvEmailError.visibility = View.GONE
            tvEmailError.text = getString(R.string.email_must_contain_hospital)
            tvEmailError.setTextColor(getColor(R.color.gray_500))
            return
        }

        if (!isValidEmail(email)) {
            tvEmailError.visibility = View.VISIBLE
            tvEmailError.text = getString(R.string.invalid_email)
            tvEmailError.setTextColor(getColor(R.color.red_500))
            return
        }

        // 🌟 FIX: Use proper validation here instead of relying on a placeholder 'true'
        if (!isHospitalEmail(email)) {
            tvEmailError.visibility = View.VISIBLE
            tvEmailError.text = getString(R.string.email_not_match_hospital)
            tvEmailError.setTextColor(getColor(R.color.red_500))
        } else {
            // Restore grey text on success
            tvEmailError.visibility = View.VISIBLE
            tvEmailError.text = getString(R.string.email_must_contain_hospital)
            tvEmailError.setTextColor(getColor(R.color.gray_500))
        }
    }

    private fun validatePasswords() {
        val password = etPassword.text.toString()
        val confirmPassword = etConfirmPassword.text.toString()

        if (password.isEmpty() || confirmPassword.isEmpty()) {
            tvPasswordError.visibility = View.GONE
            return
        }

        if (password != confirmPassword) {
            tvPasswordError.visibility = View.VISIBLE
            tvPasswordError.text = getString(R.string.passwords_not_match)
            tvPasswordError.setTextColor(getColor(R.color.red_500))
        } else {
            tvPasswordError.visibility = View.GONE
        }
    }

    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
    private fun isHospitalEmail(email: String): Boolean {
        if (selectedHospital == null || email.isEmpty())
            return false
        return true
    }

    private fun togglePasswordVisibility() {
        isPasswordVisible = !isPasswordVisible
        if (isPasswordVisible) {
            etPassword.transformationMethod = HideReturnsTransformationMethod.getInstance()
            ivPasswordToggle.setImageResource(R.drawable.ic_eye_off)
        } else {
            etPassword.transformationMethod = PasswordTransformationMethod.getInstance()
            ivPasswordToggle.setImageResource(R.drawable.ic_eye)
        }
        etPassword.setSelection(etPassword.text.length)
    }

    private fun toggleConfirmPasswordVisibility() {
        isConfirmPasswordVisible = !isConfirmPasswordVisible
        if (isConfirmPasswordVisible) {
            etConfirmPassword.transformationMethod = HideReturnsTransformationMethod.getInstance()
            ivConfirmPasswordToggle.setImageResource(R.drawable.ic_eye_off)
        } else {
            etConfirmPassword.transformationMethod = PasswordTransformationMethod.getInstance()
            ivConfirmPasswordToggle.setImageResource(R.drawable.ic_eye)
        }
        etConfirmPassword.setSelection(etConfirmPassword.text.length)
    }

    private fun updateSignUpButtonState() {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString()
        val confirmPassword = etConfirmPassword.text.toString()

        val isEmailValid = isValidEmail(email) && isHospitalEmail(email)

        val isValid = isEmailValid &&
                password.isNotEmpty() &&
                confirmPassword.isNotEmpty() &&
                password == confirmPassword &&
                selectedHospital != null // Must have a hospital selected

        btnSignUp.isEnabled = isValid
        btnSignUp.alpha = if (isValid) 1.0f else 0.5f
    }


    private fun handleSignUp() {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString()

        if (selectedHospital == null) {
            Toast.makeText(this, "Please select your hospital", Toast.LENGTH_SHORT).show()
            return
        }

        if (!isHospitalEmail(email)) {
            Toast.makeText(this, "Email does not match hospital domain requirements.", Toast.LENGTH_SHORT).show()
            return
        }

        progressBar.visibility = View.VISIBLE
        btnSignUp.isEnabled = false
        btnSignUp.text = ""

        // Capture selected hospital data now
        val hospital = selectedHospital!!

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { authTask ->
                if (authTask.isSuccessful) {
                    val user = auth.currentUser
                    val authUid = user?.uid

                    if (user == null || authUid == null) {
                        progressBar.visibility = View.GONE
                        btnSignUp.isEnabled = true
                        btnSignUp.text = getString(R.string.sign_up)
                        Toast.makeText(this, "Authentication user creation failed.", Toast.LENGTH_LONG).show()
                        return@addOnCompleteListener
                    }

                    // 🌟 STEP 1: Get or Initialize Stats
                    val profileStatsCount = currentDonationsMade ?: 0L
                    val profileStatsDate = currentNextEligibleDate ?: "-- -- ----"


                    // ✅ 1. GENERATE THE CUSTOM "HU-" ID
                    val customUserId = "HU-${(100000..999999).random()}"

                    // ✅ 2. CREATE THE PROFILE OBJECT WITH COORDINATES
                    val userProfile = UserProfile(
                        userId = customUserId,
                        email = email,
                        hospitalName = hospital.name,
                        hospitalAddress = hospital.address,
                        hospitalLat = hospital.lat,
                        hospitalLong = hospital.long,
                        fullName = null,
                        phoneNumber = null,
                        profileImageUrl = null,
                        // 🌟 CRITICAL FIX: Preserve existing stats or use 0/default 🌟
                        donationsMade = profileStatsCount,
                        nextEligibleDate = profileStatsDate
                    )

                    // ✅ 3. SAVE TO DATABASE
                    database.getReference("hospital_users").child(authUid)
                        .setValue(userProfile)
                        .addOnSuccessListener {
                            // Database save successful! Now send verification.
                            user.sendEmailVerification()
                                .addOnSuccessListener {
                                    progressBar.visibility = View.GONE
                                    btnSignUp.isEnabled = true
                                    btnSignUp.text = getString(R.string.sign_up)
                                    Toast.makeText(this, "Account created. Please check your email to verify.", Toast.LENGTH_LONG).show()
                                    auth.signOut()
                                    finish()
                                }
                                .addOnFailureListener { emailError ->
                                    // Failed to send email - log and continue to sign out
                                    progressBar.visibility = View.GONE
                                    btnSignUp.isEnabled = true
                                    btnSignUp.text = getString(R.string.sign_up)
                                    Toast.makeText(this, "Account created, but failed to send verification email: ${emailError.message}", Toast.LENGTH_LONG).show()
                                    auth.signOut()
                                    finish()
                                }
                        }
                        .addOnFailureListener { dbError ->
                            // Database save FAILED. Roll back Auth user creation.
                            user.delete().addOnCompleteListener {
                                progressBar.visibility = View.GONE
                                btnSignUp.isEnabled = true
                                btnSignUp.text = getString(R.string.sign_up)
                                Toast.makeText(this, "Failed to save profile. Please try again: ${dbError.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                } else {
                    // Auth creation FAILED
                    progressBar.visibility = View.GONE
                    btnSignUp.isEnabled = true
                    btnSignUp.text = getString(R.string.sign_up)
                    Toast.makeText(this, "Sign up failed: ${authTask.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun updateUIForSelectedHospital() {
        // Clear input fields
        etEmail.text.clear()
        etPassword.text.clear()
        etConfirmPassword.text.clear()

        // Update hospital info card
        setupHospitalInfo()

        // Show the sign-up card now
        signUpCard.apply {
            alpha = 0f
            visibility = View.VISIBLE
            animate().alpha(1f).setDuration(300).start()
        }
    }
}