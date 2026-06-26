package com.tutorial.blooddonationapp

import android.os.Parcelable
import com.google.firebase.database.PropertyName
import kotlinx.parcelize.Parcelize
import java.util.Date

@Parcelize
data class DonorRecord(
    val id: String = "",
    val donorId: String = "",
    val donorInfo: DonorInfo = DonorInfo(),
    val hospitalName: String = "",
    val date: String = "",
    val bp: String = "",
    val weight: String = "",
    val unitsdonated: String = "",
    val photoUploaded: Boolean = false,
    val imageUri: String? = null,
    val createdAt: Date = Date()
) : Parcelable

@Parcelize
data class DonorInfo(
    val name: String? = null,
    val age: Int? = null,
    val gender: String? = null,
    @get:PropertyName("bloodGroup")
    @set:PropertyName("bloodGroup")
    var bloodGroup: String? = null,
    val puID: String? = null
) : Parcelable