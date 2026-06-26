package com.tutorial.blooddonationapp

import android.os.Bundle
import android.view.MenuItem
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.tutorial.blooddonationapp.databinding.ActivityAboutUsBinding
import com.tutorial.blooddonationapp.databinding.ItemTeamMemberBinding

class About_us: AppCompatActivity(){
    private lateinit var binding: ActivityAboutUsBinding
    data class TeamMember(val initial: String, val name: String)
    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityAboutUsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
        val teamMembers = listOf(
            TeamMember("H", "Hrishikesh"),
            TeamMember("D","Daksh"),
            TeamMember("A", "Aditya"),
            TeamMember("R","Rakesh")
        )
        teamMembers.forEach { member ->
            val itemBinding = ItemTeamMemberBinding.inflate(layoutInflater, binding.teamGridLayout, false)
            itemBinding.tvInitial.text = member.initial
            itemBinding.tvName.text = member.name
            binding.teamGridLayout.addView(itemBinding.root)
        }
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

}