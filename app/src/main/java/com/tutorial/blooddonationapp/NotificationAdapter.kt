package com.tutorial.blooddonationapp

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.tutorial.blooddonationapp.databinding.ItemNotificationBinding

class NotificationAdapter(
    private var notifications: List<Notification>
) : RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder>() {

    inner class NotificationViewHolder(val binding: ItemNotificationBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(notification: Notification) {
            binding.tvNotificationTitle.text = notification.title
            binding.tvNotificationMessage.text = notification.message
            // --- NEW ---
            binding.tvDonorName.text = "Donor: ${notification.donorName}"
            binding.tvDonorPhone.text = "Contact: ${notification.donorPhone}"
            binding.tvDonorPhone.setOnClickListener {
                val phoneNumber = notification.donorPhone
                // Check if the phone number is valid and not our default text
                if (phoneNumber.isNotBlank() && phoneNumber != "Not provided") {
                    // Create the intent to open the dialer
                    val uri = Uri.parse("tel:$phoneNumber")
                    val intent = Intent(Intent.ACTION_DIAL, uri)
                    // Start the activity
                    binding.root.context.startActivity(intent)
                } else {
                    Toast.makeText(binding.root.context, "Donor has not provided a phone number", Toast.LENGTH_SHORT).show()
                }
            }
            // --- END NEW ---
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val binding = ItemNotificationBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return NotificationViewHolder(binding)
    }
    override fun getItemCount(): Int = notifications.size
    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        holder.bind(notifications[position])
    }
}