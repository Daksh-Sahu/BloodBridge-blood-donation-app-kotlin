package com.tutorial.blooddonationapp

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.tutorial.blooddonationapp.databinding.ActivitySignupPageBinding

class Signup_page : AppCompatActivity() {
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var binding: ActivitySignupPageBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        firebaseAuth= FirebaseAuth.getInstance()
        binding= ActivitySignupPageBinding.inflate(layoutInflater)
        setContentView(binding.root)
        //Already have account
        binding.textViewSignIn.setOnClickListener {
            startActivity(Intent(this, publicuserlogin::class.java))
            finish()
        }

        //creating account in firebase
        binding.signupbutton.setOnClickListener {
            signup()
        }
    }
    private fun signup(){
        val email=binding.emailEt.text.toString()
        val pass=binding.passET.text.toString()
        val conformpass=binding.confompassET.text.toString()
        if(email.isNotEmpty() && pass.isNotEmpty() && conformpass.isNotEmpty()){
            if(pass == conformpass){
                firebaseAuth.createUserWithEmailAndPassword(email,pass).addOnCompleteListener {
                    if (it.isSuccessful){
                        Toast.makeText(this,"Sucessfully Account Created", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, publicuserlogin::class.java))
                        finish()
                    }else{
                        Toast.makeText(this, it.exception.toString(), Toast.LENGTH_SHORT).show()
                    }
                }
            }else{
                Toast.makeText(this,"Please enter correct Conform Password", Toast.LENGTH_SHORT).show()
            }
        }else{
            Toast.makeText(this,"Please fill all the field", Toast.LENGTH_SHORT).show()
        }
    }
}