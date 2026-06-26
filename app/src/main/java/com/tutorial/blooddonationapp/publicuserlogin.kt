package com.tutorial.blooddonationapp

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.tutorial.blooddonationapp.databinding.ActivityPublicuserloginBinding


class publicuserlogin : AppCompatActivity() {
    private lateinit var binding: ActivityPublicuserloginBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityPublicuserloginBinding.inflate(layoutInflater)
        firebaseAuth= FirebaseAuth.getInstance()
        val gso =GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.google_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient= GoogleSignIn.getClient(this,gso)
        enableEdgeToEdge()
        setContentView(binding.root)

        //login page
        binding.signinBtn.setOnClickListener {
            signin()
        }

        //google sign in
        binding.googleSignInLayout.setOnClickListener {
            googleSignIn()
        }

        //travel to sign up page
        binding.textViewSignUp.setOnClickListener {
            val intent = Intent(this, Signup_page::class.java)
            startActivity(intent)
            finish()
        }

        //phone authountication
        binding.phoneSignInLayout.setOnClickListener {
            val intent = Intent(this, phoneauth::class.java)
            startActivity(intent)
        }
    }

    private fun signin(){
        val email = binding.emailEt.text.toString()
        val pass = binding.passET.text.toString()
        if (email.isNotEmpty() && pass.isNotEmpty()) {
            firebaseAuth.signInWithEmailAndPassword(email, pass).addOnCompleteListener {
                if (it.isSuccessful) {
                    val sharedPref = getSharedPreferences("UserPrefs", MODE_PRIVATE)
                    sharedPref.edit().putString("userType", "public").apply()


                    val intent = Intent(this, ProfileScreen::class.java)
                    startActivity(intent)
                } else {
                    Toast.makeText(this@publicuserlogin, "No such account are present", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }else{
            Toast.makeText(this@publicuserlogin, "Empty Fields Are not Allowed !!", Toast.LENGTH_SHORT).show()
        }
    }
    private fun googleSignIn(){
        binding.progressBar.visibility = View.VISIBLE
        val signInIntent = googleSignInClient.signInIntent
        launcher.launch(signInIntent)
    }
    private val launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
        result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            if (task.isSuccessful) {
                val account = task.result
                val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                firebaseAuth.signInWithCredential(credential).addOnCompleteListener {
                    binding.progressBar.visibility = View.GONE
                    if (it.isSuccessful) {
                        val intent: Intent = Intent(this, ProfileScreen::class.java)
                        intent.putExtra("email", account.email)
                        intent.putExtra("name", account.displayName)
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(this, it.exception.toString(), Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(this, task.exception.toString(), Toast.LENGTH_SHORT).show()
            }
        }
    }
}



