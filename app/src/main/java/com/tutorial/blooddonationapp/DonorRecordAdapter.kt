package com.tutorial.blooddonationapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class DonorRecordAdapter(
    private val records: List<DonorRecord>,
    private val onItemClick: (DonorRecord) -> Unit
) : RecyclerView.Adapter<DonorRecordAdapter.DonorRecordViewHolder>() {
    
    class DonorRecordViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val donorName: TextView = view.findViewById(R.id.donorName)
        val donorId: TextView = view.findViewById(R.id.donorIdText)
        val bloodType: TextView = view.findViewById(R.id.bloodTypeText)
        val donationDate: TextView = view.findViewById(R.id.donationDateText)
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DonorRecordViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_donor_card, parent, false)
        return DonorRecordViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: DonorRecordViewHolder, position: Int) {
        val record = records[position]
        
        holder.donorName.text = record.donorInfo.name
        holder.donorId.text = "ID: ${record.donorId}"
        holder.bloodType.text = record.donorInfo.bloodGroup
        holder.donationDate.text = record.date
        
        holder.itemView.setOnClickListener {
            onItemClick(record)
        }
    }
    
    override fun getItemCount() = records.size
}
