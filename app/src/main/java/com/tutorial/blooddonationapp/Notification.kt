package com.tutorial.blooddonationapp

data class Notification(
    var notificationId: String = "",
    val title: String = "",
    val message: String = "",
    val donorId: String = "",
    val posterId: String = "",
    val timestamp: Long = 0L,
    val donorName: String = "",
    val donorPhone: String = ""
)