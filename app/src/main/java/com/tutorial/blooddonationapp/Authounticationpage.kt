package com.tutorial.blooddonationapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.tutorial.blooddonationapp.databinding.ActivityAuthounticationpageBinding

class Authounticationpage : AppCompatActivity() {
    private lateinit var binding: ActivityAuthounticationpageBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthounticationpageBinding.inflate(layoutInflater)
        auth = FirebaseAuth.getInstance()
        enableEdgeToEdge()
        setContentView(binding.root)
        binding.publicuserBtn.setOnClickListener {
            val intent= Intent(this, publicuserlogin::class.java)
            startActivity(intent)
        }
        binding.hospitaluserBtn.setOnClickListener {
            val intent= Intent(this, HospitalSignInActivity::class.java)
            startActivity(intent)
        }
    }
    override fun onStart() {
        super.onStart()

//        if (auth.currentUser != null) {
//            val intent = Intent(this, MainActivity::class.java)
//            startActivity(intent)
//            finish()
//        }
    }
}