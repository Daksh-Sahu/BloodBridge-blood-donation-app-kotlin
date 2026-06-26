package com.tutorial.blooddonationapp

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException

class HospitalSignInActivity : AppCompatActivity() {


    private lateinit var signInCard: CardView
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var ivPasswordToggle: ImageView
    private lateinit var btnSignIn: Button
    private lateinit var tvSignUp: TextView
    private lateinit var tvStatus: TextView
    private lateinit var progressBar: ProgressBar

    private var isPasswordVisible = false



//    private val signUpLauncher = registerForActivityResult(
//        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
//    ) { result ->
//        if (result.resultCode == RESULT_OK) {
//            // Lock the dropdown
//            autoCompleteHospital.isEnabled = false
//            autoCompleteHospital.isFocusable = false
//            autoCompleteHospital.isClickable = false
//            autoCompleteHospital.isCursorVisible = false
//
//            // Gray out the text and hint
////            autoCompleteHospital.setTextColor(ContextCompat.getColor(this, R.color.gray_500))
////            autoCompleteHospital.setHintTextColor(ContextCompat.getColor(this, R.color.gray_400))
//
//            // Optionally make the card visually disabled too
//           hospitalSelectionCard.alpha = 0.4f
//
//            Toast.makeText(this, "Hospital selection locked after sign-up", Toast.LENGTH_SHORT).show()
//        }
//    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hospital_signin)

        // Initialize views
        initializeViews()


        // Setup listeners
        setupListeners()
    }

    private fun initializeViews() {

        signInCard = findViewById(R.id.signInCard)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        ivPasswordToggle = findViewById(R.id.ivPasswordToggle)
        btnSignIn = findViewById(R.id.btnSignIn)
        tvSignUp = findViewById(R.id.tvSignUp)
//        tvStatus = findViewById(R.id.tvStatus)
        progressBar = findViewById(R.id.progressBar)
    }




    private fun setupListeners() {
        // Password toggle
        ivPasswordToggle.setOnClickListener {
            togglePasswordVisibility()
        }

        // Email text watcher
        etEmail.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updateSignInButtonState()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Password text watcher
        etPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updateSignInButtonState()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Sign in button
        btnSignIn.setOnClickListener {
            handleSignIn()
        }

        // Sign up link
        tvSignUp.setOnClickListener {
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
        }


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
        
        // Move cursor to end
        etPassword.setSelection(etPassword.text.length)
    }

    private fun updateSignInButtonState() {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString()

        val isValid = isValidEmail(email) && password.isNotEmpty()


        btnSignIn.isEnabled = isValid
        btnSignIn.alpha = if (isValid) 1.0f else 0.5f
    }


    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }




    private fun handleSignIn() {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString()

        if (!isValidEmail(email) || password.isEmpty()) {
            Toast.makeText(this, "Please enter a valid email and password", Toast.LENGTH_SHORT).show()
            return
        }

        progressBar.visibility = View.VISIBLE
        btnSignIn.isEnabled = false
        btnSignIn.text = ""

        val auth = FirebaseAuth.getInstance()

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                progressBar.visibility = View.GONE
                btnSignIn.isEnabled = true
                btnSignIn.text = getString(R.string.sign_in)

                if (task.isSuccessful) {
                    // ✅ Email verification check is REMOVED
                    Toast.makeText(
                        this,
                        "Sign-in successful!",
                        Toast.LENGTH_LONG
                    ).show()

                    val sharedPref = getSharedPreferences("UserPrefs", MODE_PRIVATE)
                    sharedPref.edit().putString("userType", "hospital").apply()

                    // ✅ Navigate to DashboardActivity
                    startActivity(Intent(this, BeginningInfoActivity::class.java))
                    finish() // Finish this activity so the user can't go back to it

                } else {
                    // Handle login failures
                    val error = task.exception
                    val message = when (error) {
                        is FirebaseAuthInvalidUserException -> "Account not found. Please sign up first."
                        is FirebaseAuthInvalidCredentialsException -> "Account not found/Invalid password."
                        is FirebaseNetworkException -> "Network error. Please check your connection."
                        else -> error?.localizedMessage ?: "Sign in failed. Please try again."
                    }

                    Log.e("FirebaseAuth", "Sign in failed", error)
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                }
            }
    }
}
