package com.tutorial.blooddonationapp


import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
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
import com.tutorial.blooddonationapp.databinding.ActivityOtpactivityBinding

class otpactivity : AppCompatActivity() {
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var binding: ActivityOtpactivityBinding
    private lateinit var phoneno: String
    private lateinit var otp: String
    private lateinit var token: PhoneAuthProvider.ForceResendingToken
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityOtpactivityBinding.inflate(layoutInflater)
        firebaseAuth= FirebaseAuth.getInstance()
        enableEdgeToEdge()
        setContentView(binding.root)

        otp=intent.getStringExtra("OTP").toString()
        phoneno=intent.getStringExtra("phonenumber").toString()
        token=intent.getParcelableExtra("resendToken")!!

        attachingwatcher()
        resendviewvisibility()
        binding.verifyOTPBtn.setOnClickListener {
            val enteredOtp = getEnteredOtp()
            if(enteredOtp.isNotEmpty()){
                if (enteredOtp.length == 6){
                    val credential = PhoneAuthProvider.getCredential(otp, enteredOtp)
                    binding.otpProgressBar.visibility=View.VISIBLE
                    signInWithPhoneAuthCredential(credential)
                }else{
                    Toast.makeText(this, "Please enter correct OTP", Toast.LENGTH_SHORT).show()
                }
            }else{
                Toast.makeText(this, "Please enter OTP", Toast.LENGTH_SHORT).show()
            }
        }
        binding.resendTextView.setOnClickListener {
            resendVerificationCode()
            resendviewvisibility()
        }
    }

    private fun resendviewvisibility(){
        binding.otpEditText1.setText("")
        binding.otpEditText2.setText("")
        binding.otpEditText3.setText("")
        binding.otpEditText4.setText("")
        binding.otpEditText5.setText("")
        binding.otpEditText6.setText("")
        binding.resendTextView.visibility=View.INVISIBLE
        binding.resendTextView.isEnabled=false

        Handler(Looper.getMainLooper()).postDelayed({
            binding.resendTextView.visibility=View.VISIBLE
            binding.resendTextView.isEnabled=true
        },
            60000)
    }
    private fun resendVerificationCode() {
        val options = PhoneAuthOptions.newBuilder(firebaseAuth)
            .setPhoneNumber(phoneno)
            .setTimeout(60L, java.util.concurrent.TimeUnit.SECONDS)
            .setActivity(this)
            .setCallbacks(callbacks)
            .setForceResendingToken(token)
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
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
        }

        override fun onCodeSent(
            verificationId: String,
            token: PhoneAuthProvider.ForceResendingToken,
        ) {
            otp=verificationId
            this@otpactivity.token =token
        }
    }


    fun attachingwatcher(){
        binding.otpEditText1.addTextChangedListener(EditTextWatcher(binding.otpEditText1, binding.otpEditText2, null))
        binding.otpEditText2.addTextChangedListener(EditTextWatcher(binding.otpEditText2, binding.otpEditText3, binding.otpEditText1))
        binding.otpEditText3.addTextChangedListener(EditTextWatcher(binding.otpEditText3, binding.otpEditText4, binding.otpEditText2))
        binding.otpEditText4.addTextChangedListener(EditTextWatcher(binding.otpEditText4, binding.otpEditText5, binding.otpEditText3))
        binding.otpEditText5.addTextChangedListener(EditTextWatcher(binding.otpEditText5, binding.otpEditText6, binding.otpEditText4))
        binding.otpEditText6.addTextChangedListener(EditTextWatcher(binding.otpEditText6, null, binding.otpEditText5))

    }

    inner class EditTextWatcher(private val currentView: View, private val nextView: View?, private val prevView: View?): TextWatcher{
        override fun afterTextChanged(p0: Editable?) {
            val text = p0.toString()
            if (text.length == 1) {
                nextView?.requestFocus()
            } else if (text.isEmpty()) {
                prevView?.requestFocus()
            }
        }
        override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) { }

        override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) { }

    }
    private fun getEnteredOtp(): String {
        return binding.otpEditText1.text.toString().trim() +
                binding.otpEditText2.text.toString().trim() +
                binding.otpEditText3.text.toString().trim() +
                binding.otpEditText4.text.toString().trim() +
                binding.otpEditText5.text.toString().trim() +
                binding.otpEditText6.text.toString().trim()
    }
    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        firebaseAuth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    binding.otpProgressBar.visibility=View.INVISIBLE
                    startActivity(Intent(this, ProfileScreen::class.java))
                    finish()

                } else {
                    Toast.makeText(this, "please enter correct OTP", Toast.LENGTH_SHORT).show()
                }
            }

    }
}