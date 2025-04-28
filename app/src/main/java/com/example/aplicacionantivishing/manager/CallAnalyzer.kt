package com.example.aplicacionantivishing.manager

import android.content.Context

object CallAnalyzer {

    fun analyzeNumber(context: Context, phoneNumber: String?): String {
        if (phoneNumber.isNullOrEmpty()) {
            return "dangerous" // Número oculto
        }
        return "suspicious"
    }
}
