package com.tutorial.blooddonationapp

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class SplashScreen : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private val database = FirebaseDatabase.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.splash_screen)

        auth = FirebaseAuth.getInstance()

        // Set a short delay before routing
        Handler(Looper.getMainLooper()).postDelayed({
            checkUserSessionAndRoute()
        }, 1500)
    }

    private fun checkUserSessionAndRoute() {
        val currentUser = auth.currentUser

        if (currentUser == null) {
            // Case 1: No user logged in
            navigateTo(Authounticationpage::class.java)
            return
        }

        // User is Authenticated. Determine Role via Database Check.
        val userId = currentUser.uid

        // Use a flag to track if we've successfully routed the user
        val routingAttempted = BooleanArray(1) { false }

        // -------------------------------------------------------------------
        // 1. Check for Hospital Role (High Priority)
        // -------------------------------------------------------------------
        database.getReference("hospital_users").child(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists() && !routingAttempted[0]) {
                        // Case 2: User is Hospital Staff (Found in restrictive node)
                        routingAttempted[0] = true
                        navigateTo(DashboardActivity::class.java)
                    } else if (!routingAttempted[0]) {
                        // If not a hospital user, immediately check the public node.
                        checkPublicUserNode(userId, routingAttempted)
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    // Database error during role check. Fall back.
                    if (!routingAttempted[0]) navigateTo(Authounticationpage::class.java)
                }
            })
    }

    // Checks the /users node for public donor registration
    private fun checkPublicUserNode(userId: String, routingAttempted: BooleanArray) {
        database.getReference("users").child(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists() && !routingAttempted[0]) {
                        // Case 3: User is Public Donor (Found in standard node)
                        routingAttempted[0] = true
                        navigateTo(MainActivity::class.java)
                    } else if (!routingAttempted[0]) {
                        // Case 4: Authenticated, but profile is missing in BOTH nodes.
                        routingAttempted[0] = true
                        navigateTo(Authounticationpage::class.java)
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    if (!routingAttempted[0]) navigateTo(Authounticationpage::class.java)
                }
            })
    }

    private fun navigateTo(activityClass: Class<*>) {
        startActivity(Intent(this, activityClass))
        finish()
    }

    override fun onBackPressed() {
        // Do nothing during splash screen
    }
}