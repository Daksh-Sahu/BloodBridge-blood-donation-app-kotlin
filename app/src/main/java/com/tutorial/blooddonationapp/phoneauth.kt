package com.tutorial.blooddonationapp

import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthMissingActivityForRecaptchaException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.tutorial.blooddonationapp.databinding.ActivityPhoneauthBinding
import java.util.concurrent.TimeUnit



class phoneauth : AppCompatActivity() {
    private lateinit var binding: ActivityPhoneauthBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private var number: String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityPhoneauthBinding.inflate(layoutInflater)
        firebaseAuth = FirebaseAuth.getInstance()
        setContentView(binding.root)
        binding.sendOTPBtn.setOnClickListener {
            number = binding.phoneEditTextNumber.text.trim().toString()
            if (number!!.isNotEmpty()) {
                if (number!!.length == 10) {
                    number = "+91$number"
                    binding.phoneProgressBar.visibility = View.VISIBLE
                    val options = PhoneAuthOptions.newBuilder(firebaseAuth)
                        .setPhoneNumber(number!!)
                        .setTimeout(60L, TimeUnit.SECONDS)
                        .setActivity(this)
                        .setCallbacks(callbacks)
                        .build()
                    PhoneAuthProvider.verifyPhoneNumber(options)
                } else {
                    Toast.makeText(this, "Please Enter correct Number", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Please Enter Number", Toast.LENGTH_SHORT).show()
            }
        }
    }
    val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
            signInWithPhoneAuthCredential(credential)
        }
        override fun onVerificationFailed(e: FirebaseException) {
            when (e) {
                is FirebaseAuthInvalidCredentialsException -> {
                    Log.d(TAG, "onVerificationFailed:$e")
                }
                is FirebaseTooManyRequestsException -> {
                    // The SMS quota for the project has been exceeded
                    Log.d(TAG, "onVerificationFailed:$e")
                }
                is FirebaseAuthMissingActivityForRecaptchaException -> {
                    // reCAPTCHA verification attempted with null Activity
                    Log.d(TAG, "onVerificationFailed:$e")
                }
            }

            // Show a message and update the UI
        }

        override fun onCodeSent(
            verificationId: String,
            token: PhoneAuthProvider.ForceResendingToken,
        ) {
            val intent = Intent(this@phoneauth, otpactivity::class.java)
            intent.putExtra("OTP", verificationId)
            intent.putExtra("resendToken", token)
            intent.putExtra("phonenumber",number)
            startActivity(intent)
            binding.phoneProgressBar.visibility = View.INVISIBLE
        }
    }
    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        firebaseAuth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    startActivity(Intent(this, ProfileScreen::class.java))
                    finish()

                } else {
                    Log.w(TAG, "signInWithCredential:failure", task.exception)

                }
            }
    }
}