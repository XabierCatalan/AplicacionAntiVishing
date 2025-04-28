package com.example.aplicacionantivishing.model

data class CallRecord(
    val phoneNumber: String,
    val riskLevel: String,
    val timestamp: Long
)