package com.tutorial.blooddonationapp

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Filter
import android.widget.Filterable
import android.widget.TextView

class HospitalAdapter(
    context: Context,
    private val hospitals: List<Hospital2>
) : ArrayAdapter<Hospital2>(context, 0, hospitals), Filterable {

    private var filteredHospitals: List<Hospital2> = hospitals

    override fun getCount(): Int = filteredHospitals.size

    override fun getItem(position: Int): Hospital2? = filteredHospitals.getOrNull(position)

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_hospital, parent, false)

        val hospital = getItem(position)
        
        val tvHospitalName = view.findViewById<TextView>(R.id.tvHospitalName)
        val tvHospitalAddress = view.findViewById<TextView>(R.id.tvHospitalAddress)

        hospital?.let {
            tvHospitalName.text = it.name
            tvHospitalAddress.text = it.getFullAddress()
        }

        return view
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val query = constraint?.toString()?.lowercase()?.trim() ?: ""
                
                val filtered = if (query.isEmpty()) {
                    hospitals
                } else {
                    hospitals.filter { hospital ->
                        hospital.getSearchableText().contains(query)
                    }
                }

                return FilterResults().apply {
                    values = filtered
                    count = filtered.size
                }
            }

            @Suppress("UNCHECKED_CAST")
            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                filteredHospitals = (results?.values as? List<Hospital2>) ?: emptyList()
                notifyDataSetChanged()
            }
        }
    }
}
