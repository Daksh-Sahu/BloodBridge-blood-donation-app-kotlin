package com.tutorial.blooddonationapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.viewpager2.widget.ViewPager2
import androidx.core.content.edit
import androidx.core.view.WindowCompat
// ✅ Firebase Imports
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

// ✅ Your specified data class

class DashboardActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var ivMenu: ImageView
    private lateinit var ivNotification: ImageView
    private lateinit var tvNotificationBadge: TextView
    private lateinit var tvUserName: TextView
    private lateinit var tvUserPhone: TextView
    private lateinit var tvHospitalName: TextView
    private lateinit var viewPager: ViewPager2
    private lateinit var llEmergencySOS: LinearLayout
    private lateinit var llNewDonorRecord: LinearLayout
    // Navigation drawer items
    private lateinit var ivCloseDrawer: ImageView
    private lateinit var llDashboard: LinearLayout
    private lateinit var llProfile: LinearLayout
    private lateinit var llDonations: LinearLayout
    private lateinit var llAbout: LinearLayout
    private lateinit var llLogout: LinearLayout
    // ✅ Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    // Class variables to hold user data
    private var userFullName: String = "Hospital User"
    private var userPhoneNumber: String = "Not provided"
    private var unreadNotificationCount: Int = 0
    private var hospitalName: String = "No hospital selected"
    private var hospitalAddress: String = "Address not available"

    // ✅ We now track two IDs
    private var authUid: String = "" // This is the Firebase Auth ID (the "key")
    private var customUserId: String = "HU-000000" // This is the "HU-..." ID (the "value")

    companion object {
        const val EXTRA_FULL_NAME = "fullName"
        const val EXTRA_PHONE_NUMBER = "phoneNumber"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, true)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        // ✅ Initialize Firebase
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        if (auth.currentUser == null) {
            showToast("Not logged in. Redirecting...")
            logout()
            return
        }

        // ✅ The user is logged in, set the AUTHENTICATION ID
        authUid = auth.currentUser!!.uid

        initializeViews()
        setupListeners()
        setupImageSlider()
        updateNotificationBadge()

        // ✅ Fetch user data from Firebase using the authUid
        getUserInfoFromFirebase()

        // --- Start of OnBackPressed handling ---
        val onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START)
                } else {
                    finish()
                }
            }
        }
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
        // --- End of OnBackPressed handling ---
    }

    override fun onResume() {
        super.onResume()
        // ✅ Refresh data from Firebase every time the dashboard is shown
        getUserInfoFromFirebase()
    }

    // ✅ This function fetches the profile using the authUid
    private fun getUserInfoFromFirebase() {
        if (authUid.isEmpty()) {
            showToast("Error: User ID is missing.")
            logout()
            return
        }

        // Use the authUid as the key to find the user
        val userRef = database.getReference("hospital_users").child(authUid)

        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    // Get the profile object
                    val profile = snapshot.getValue(HospitalUserProfile::class.java)

                    if (profile != null) {
                        // ✅ Update class variables with data from Firebase
                        userFullName = profile.fullName ?: "Hospital User"
                        userPhoneNumber = profile.phoneNumber ?: "Not provided"
                        hospitalName = profile.hospitalName ?: "No hospital selected"
                        hospitalAddress = profile.hospitalAddress ?: "Address not available"

                        // ✅ Get the CUSTOM "HU-" ID from the 'userId' field
                        customUserId = profile.userId ?: "HU-Error"

                        // ✅ Update the UI with the new data
                        updateUserInfo()

                        // ✅ Save the fresh data to SharedPreferences as a cache
                        saveInfoToPrefs()

                    } else {
                        showToast("Error: Could not read profile data.")
                        loadInfoFromPrefs() // Fallback to local cache
                        updateUserInfo()
                    }
                } else {
                    // This can happen if user verified email but hasn't entered info yet
                    showToast("Please complete your profile.")
                    // Send them to BeginningInfoActivity if they haven't filled it out
                    if (userFullName == "Hospital User" || userPhoneNumber == "Not provided") {
                        startActivity(Intent(this@DashboardActivity, BeginningInfoActivity::class.java))
                    } else {
                        loadInfoFromPrefs()
                        updateUserInfo()
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                showToast("Failed to load data: ${error.message}")
                loadInfoFromPrefs()
                updateUserInfo()
            }
        })
    }

    // ✅ This function now reads our custom ID from prefs
    private fun loadInfoFromPrefs() {
        val prefs = getSharedPreferences("user_session", MODE_PRIVATE)
        userFullName = prefs.getString("user_full_name", "Hospital User") ?: "Hospital User"
        userPhoneNumber = prefs.getString("user_phone_number", "Not provided") ?: "Not provided"
        hospitalName = prefs.getString("hospital_name", "No hospital selected") ?: "No hospital selected"
        hospitalAddress = prefs.getString("hospital_address", "Address not available") ?: "Address not available"
        customUserId = prefs.getString("custom_user_id", "HU-000000") ?: "HU-000000"
        authUid = auth.currentUser?.uid ?: "" // Re-set authUid
    }

    // ✅ This function now saves our custom ID to prefs
    private fun saveInfoToPrefs() {
        val prefs = getSharedPreferences("user_session", MODE_PRIVATE)
        prefs.edit {
            putString("user_full_name", userFullName)
            putString("user_phone_number", userPhoneNumber)
            putString("hospital_name", hospitalName)
            putString("hospital_address", hospitalAddress)
            putString("custom_user_id", customUserId) // ✅ Save the "HU-" ID
            putBoolean("is_logged_in", true)
        }
    }

    private fun initializeViews() {
        drawerLayout = findViewById(R.id.drawer_layout)
        ivMenu = findViewById(R.id.ivMenu)
        ivNotification = findViewById(R.id.ivNotification)
        tvNotificationBadge = findViewById(R.id.tvNotificationBadge)
        tvUserName = findViewById(R.id.tvUserName)
        tvUserPhone = findViewById(R.id.tvUserPhone)
        tvHospitalName = findViewById(R.id.tvHospitalName)

        viewPager = findViewById(R.id.imageSlider)
        llEmergencySOS = findViewById(R.id.llEmergencySOS)
        llNewDonorRecord = findViewById(R.id.llNewDonorRecord)

        // Navigation drawer items
        ivCloseDrawer = findViewById(R.id.ivCloseDrawer)
        llDashboard = findViewById(R.id.llDashboard)
        llProfile = findViewById(R.id.llProfile)
        llDonations = findViewById(R.id.llDonations)
        llAbout = findViewById(R.id.llAbout)
        llLogout = findViewById(R.id.llLogout)
    }

    private fun setupListeners() {
        // Header buttons
        ivMenu.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        // Action buttons
        llEmergencySOS.setOnClickListener {
            val intent = Intent(this, EmergencySOSActivity::class.java)
            startActivity(intent)
        }

        llNewDonorRecord.setOnClickListener {
            val intent = Intent(this, NewDonorRecordActivity::class.java)
            startActivity(intent)
        }

        // Navigation drawer items
        ivCloseDrawer.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
        }

        llDashboard.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
        }

        llProfile.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java).apply {
                putExtra(ProfileActivity.EXTRA_FULL_NAME, userFullName)
                putExtra(ProfileActivity.EXTRA_PHONE_NUMBER, userPhoneNumber)
                // ✅ Pass the CUSTOM "HU-" ID to the profile activity
                putExtra("USER_ID", customUserId)
            }
            startActivity(intent)
            drawerLayout.closeDrawer(GravityCompat.START)
        }

        llDonations.setOnClickListener {
            val intent = Intent(this, DonationsActivity::class.java)
            startActivity(intent)
            drawerLayout.closeDrawer(GravityCompat.START)
        }

        llAbout.setOnClickListener {
            val intent = Intent(this, AboutActivity::class.java)
            startActivity(intent)
            drawerLayout.closeDrawer(GravityCompat.START)
        }

        llLogout.setOnClickListener {
            showLogoutDialog()
        }
    }

    // ✅ This function now displays the CUSTOM "HU-" ID
    private fun updateUserInfo() {
        tvUserName.text = userFullName
        tvUserPhone.text = userPhoneNumber
        tvHospitalName.text = getString(R.string.hospital_info, hospitalName, hospitalAddress)
        // This assumes R.id.tvUserId is in your drawer header
        findViewById<TextView>(R.id.tvUserId).text = customUserId
    }

    private fun setupImageSlider() {
        val images = listOf(
            R.drawable.bd6,
            R.drawable.bd2,
            R.drawable.bd3,
            R.drawable.bd4,
            R.drawable.bd5,
            R.drawable.bd1
        )
        val adapter = ImageSliderAdapter(images)
        viewPager.adapter = adapter
        setupAutoScroll()
    }

    private fun setupAutoScroll() {
        val handler = android.os.Handler(mainLooper)
        val runnable = object : Runnable {
            override fun run() {
                val adapter = viewPager.adapter
                if (adapter != null && adapter.itemCount > 0) {
                    val currentItem = viewPager.currentItem
                    val nextItem = if (currentItem == adapter.itemCount - 1) 0 else currentItem + 1
                    viewPager.setCurrentItem(nextItem, true)
                }
                handler.postDelayed(this, 4000) // 4 seconds
            }
        }
        handler.postDelayed(runnable, 4000)
    }

    private fun updateNotificationBadge() {
        if (unreadNotificationCount > 0) {
            tvNotificationBadge.visibility = View.VISIBLE
            tvNotificationBadge.text = if (unreadNotificationCount > 9) "9+" else unreadNotificationCount.toString()
        } else {
            tvNotificationBadge.visibility = View.GONE
        }
    }

    private fun showLogoutDialog() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Confirm Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Logout") { _, _ ->
                logout()
            }
            .setNegativeButton("Cancel") { _, _ ->
                drawerLayout.closeDrawer(GravityCompat.START)
            }
            .show()
    }

    // ✅ Updated to include Firebase Sign Out
    private fun logout() {
        // ✅ Sign out from Firebase Auth
        auth.signOut()

        // Clear all local data
        val prefs = getSharedPreferences("user_session", MODE_PRIVATE)
        prefs.edit().clear().apply()
        prefs.edit().putBoolean("is_logged_in", false).apply()

        // ... (clear all other shared prefs) ...
        getSharedPreferences("donor_data", MODE_PRIVATE).edit().clear().apply()
        getSharedPreferences("settings", MODE_PRIVATE).edit().clear().apply()
        getSharedPreferences("DonorRecords", MODE_PRIVATE).edit().clear().apply()
        getSharedPreferences("UserPrefs", MODE_PRIVATE).edit().clear().apply()

        cacheDir.deleteRecursively()
        filesDir.deleteRecursively()

        showToast("Logged out successfully")

        val intent = Intent(this, Authounticationpage::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}