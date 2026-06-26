package com.tutorial.blooddonationapp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import com.bumptech.glide.Glide
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.tutorial.blooddonationapp.databinding.ActivityMainBinding
import com.tutorial.blooddonationapp.databinding.NavHeaderBinding

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    private lateinit var binding: ActivityMainBinding
    private lateinit var headerBinding: NavHeaderBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient

    // Assume ChatbotActivity.kt exists and is ready
    // You should use the actual class name here, but I'll use ChatbotActivity for demonstration.
    private val CHATBOT_ACTIVITY = ChatbotActivity::class.java

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        firebaseAuth = FirebaseAuth.getInstance()
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.google_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)
        setContentView(binding.root)
        setSupportActionBar(binding.toolBar)

        val toggle = ActionBarDrawerToggle(
            this,
            binding.drawerLayout,
            binding.toolBar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        binding.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        binding.navView.setNavigationItemSelectedListener(this)

        val headerView = binding.navView.getHeaderView(0)
        headerBinding = NavHeaderBinding.bind(headerView)
        loadUserProfileFromFirebase()

        binding.btnRequestBlood.setOnClickListener {
            startActivity(Intent(this, Receiver_page::class.java))
        }
        binding.btnFindCenter.setOnClickListener {
            startActivity(Intent(this, Doner_page::class.java))
        }

        // 🌟 NEW: FAB Click Listener 🌟
        binding.fabChat.setOnClickListener {
            openChatbot()
        }
    }

    // 🌟 NEW FUNCTION: Launches the Chatbot Activity 🌟
    private fun openChatbot() {
        startActivity(Intent(this, CHATBOT_ACTIVITY))
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_toolbar_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_notifications) {
            val intent = Intent(this, NotificationsActivity::class.java)
            startActivity(intent)
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    private fun loadUserProfileFromFirebase() {
        binding.progressBar.visibility = View.VISIBLE
        val currentUser = firebaseAuth.currentUser

        if (currentUser == null) {
            binding.tvDonationsCount.text = "0"
            binding.tvNextEligibleDate.text = "-- -- ----"
            binding.progressBar.visibility = View.GONE
            return
        }

        val dbRef = FirebaseDatabase.getInstance().reference.child("users").child(currentUser.uid)

        dbRef.addValueEventListener(object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                if (snapshot.exists()) {
                    val name = snapshot.child("name").getValue(String::class.java)
                    val bloodGroup = snapshot.child("bloodGroup").getValue(String::class.java)
                    val imageUrl = snapshot.child("profileImageUrl").getValue(String::class.java)
                    val puId = snapshot.child("puId").getValue(String::class.java)
                    updateNavHeader(name, bloodGroup, imageUrl,puId)

                    val donationsMade = snapshot.child("donationsMade").getValue(Long::class.java) ?: 0L
                    val nextEligibleDate = snapshot.child("nextEligibleDate").getValue(String::class.java) ?: "-- -- ----"

                    binding.tvDonationsCount.text = donationsMade.toString()
                    binding.tvNextEligibleDate.text = nextEligibleDate

                } else {
                    binding.tvDonationsCount.text = "0"
                    binding.tvNextEligibleDate.text = "-- -- ----"
                }
                binding.progressBar.visibility = View.GONE
            }

            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                binding.tvDonationsCount.text = "0"
                binding.tvNextEligibleDate.text = "-- -- ----"
                binding.progressBar.visibility = View.GONE
            }
        })
    }

    private fun updateNavHeader(name: String?, bloodGroup: String?, imageUriString: String?, puId: String?) {
        val displayName = name ?: "User Name"

        headerBinding.userName.text = displayName
        headerBinding.tvUserId.text = puId ?: "PU-Error"
        headerBinding.userBloodGroup.text = "Blood Group: ${bloodGroup ?: "Not set"}"
        binding.greetingText.text = "Hello, $displayName!"

        if (!imageUriString.isNullOrEmpty()) {
            val imageUri = Uri.parse(imageUriString)
            Glide.with(this)
                .load(imageUri)
                .placeholder(R.drawable.ic_person_placeholder)
                .error(R.drawable.ic_person_placeholder)
                .into(headerBinding.profileImage)
        } else {
            headerBinding.profileImage.setImageResource(R.drawable.ic_person_placeholder)
        }
        binding.progressBar.visibility = View.GONE
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_home -> { /* ... */ }
            R.id.nav_profile -> {
                startActivity(Intent(this, ProfileScreen::class.java))
            }
            R.id.nav_doner_records -> {
                startActivity(Intent(this, MyDonationsActivity::class.java))
            }
            R.id.nav_history -> {
                startActivity(Intent(this, HistoryActivity::class.java))
            }
            R.id.nav_about_us -> {
                startActivity(Intent(this, About_us::class.java))
            }
            R.id.nav_logout -> {
                val builder = androidx.appcompat.app.AlertDialog.Builder(this)
                builder.setTitle("Logout")
                builder.setMessage("Are you sure you want to logout?")
                builder.setPositiveButton("Confirm") { dialog, _ ->
                    logoutUser()
                    dialog.dismiss()
                }
                builder.setNegativeButton("Cancel") { dialog, _ ->
                    dialog.dismiss()
                }
                builder.create().show()
                true
            }
        }

        binding.drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun logoutUser() {
        firebaseAuth.signOut()
        getSharedPreferences("UserPrefs", MODE_PRIVATE).edit().clear().apply()
        googleSignInClient.signOut().addOnCompleteListener {
            val intent = Intent(this, Authounticationpage::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        binding.navView.setCheckedItem(R.id.nav_home)
    }

    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}