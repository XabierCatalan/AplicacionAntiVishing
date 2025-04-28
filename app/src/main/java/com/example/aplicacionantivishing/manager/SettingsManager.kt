package com.example.aplicacionantivishing.manager

import android.content.Context
import android.preference.PreferenceManager

object SettingsManager {

    fun resetDefaultWeights(context: Context) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val editor = prefs.edit()

        // Restaurar valores por defecto de los pesos
        editor.putString("weight_suspicious_name_internet", "-70")
        editor.putString("weight_reported_internet", "-50")
        editor.putString("weight_verified_internet", "15")
        // Aquí añades todos los que quieras restaurar

        editor.apply()
    }
}
