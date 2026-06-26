package com.tutorial.blooddonationapp

data class Hospital2(
    val id: String,
    val name: String,
    val address: String,
    val lat: Double,
    val long: Double
) {
    fun getDisplayName(): String {
        return name
    }

    fun getFullAddress(): String {
        return address
    }

    fun getSearchableText(): String {
        return "$name $address".lowercase()
    }

    override fun toString(): String {
        return getDisplayName()
    }
}

