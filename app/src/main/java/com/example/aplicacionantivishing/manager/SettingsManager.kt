package com.example.aplicacionantivishing.manager

import android.content.Context
import android.preference.PreferenceManager

object SettingsManager {

    // Valores por defecto (Con Internet)
    private const val DEFAULT_SUSPICIOUS_NAME = "-70"
    private const val DEFAULT_INTERNATIONAL_CALL = "-30"
    private const val DEFAULT_FIRST_CALL = "-10"
    private const val DEFAULT_VERIFIED_CONTACT = "10"
    private const val DEFAULT_NATIONAL_CALL = "5"
    private const val DEFAULT_BLACKLIST_PENALTY = "-90"

    // Valores por defecto (Sin Internet)
    private const val DEFAULT_SUSPICIOUS_NAME_NO_INTERNET = "-80"
    private const val DEFAULT_INTERNATIONAL_CALL_NO_INTERNET = "-40"
    private const val DEFAULT_FIRST_CALL_NO_INTERNET = "-20"
    private const val DEFAULT_VERIFIED_CONTACT_NO_INTERNET = "20"
    private const val DEFAULT_NATIONAL_CALL_NO_INTERNET = "15"

    // Restauración de valores por defecto
    fun resetDefaultWeights(context: Context) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val editor = prefs.edit()

        // Con Internet
        editor.putString("penalty_suspicious_name", DEFAULT_SUSPICIOUS_NAME)
        editor.putString("penalty_international_call", DEFAULT_INTERNATIONAL_CALL)
        editor.putString("penalty_first_call", DEFAULT_FIRST_CALL)
        editor.putString("bonus_verified_contact", DEFAULT_VERIFIED_CONTACT)
        editor.putString("bonus_national_call", DEFAULT_NATIONAL_CALL)

        // Sin Internet
        editor.putString("penalty_suspicious_name_no_internet", DEFAULT_SUSPICIOUS_NAME_NO_INTERNET)
        editor.putString("penalty_international_call_no_internet", DEFAULT_INTERNATIONAL_CALL_NO_INTERNET)
        editor.putString("penalty_first_call_no_internet", DEFAULT_FIRST_CALL_NO_INTERNET)
        editor.putString("bonus_verified_contact_no_internet", DEFAULT_VERIFIED_CONTACT_NO_INTERNET)
        editor.putString("bonus_national_call_no_internet", DEFAULT_NATIONAL_CALL_NO_INTERNET)

        // Penalización por Blacklist
        editor.putString("blacklist_penalty", DEFAULT_BLACKLIST_PENALTY)

        editor.apply()
    }

    /**
     * Obtiene los valores según la disponibilidad de Internet
     */
    fun getPenaltySuspiciousName(context: Context, hasInternet: Boolean): Int {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val key = if (hasInternet) "penalty_suspicious_name" else "penalty_suspicious_name_no_internet"
        return prefs.getString(key, if (hasInternet) DEFAULT_SUSPICIOUS_NAME else DEFAULT_SUSPICIOUS_NAME_NO_INTERNET)!!.toInt()
    }

    fun getPenaltyInternationalCall(context: Context, hasInternet: Boolean): Int {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val key = if (hasInternet) "penalty_international_call" else "penalty_international_call_no_internet"
        return prefs.getString(key, if (hasInternet) DEFAULT_INTERNATIONAL_CALL else DEFAULT_INTERNATIONAL_CALL_NO_INTERNET)!!.toInt()
    }

    fun getPenaltyFirstCall(context: Context, hasInternet: Boolean): Int {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val key = if (hasInternet) "penalty_first_call" else "penalty_first_call_no_internet"
        return prefs.getString(key, if (hasInternet) DEFAULT_FIRST_CALL else DEFAULT_FIRST_CALL_NO_INTERNET)!!.toInt()
    }

    fun getBonusVerifiedContact(context: Context, hasInternet: Boolean): Int {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val key = if (hasInternet) "bonus_verified_contact" else "bonus_verified_contact_no_internet"
        return prefs.getString(key, if (hasInternet) DEFAULT_VERIFIED_CONTACT else DEFAULT_VERIFIED_CONTACT_NO_INTERNET)!!.toInt()
    }

    fun getBonusNationalCall(context: Context, hasInternet: Boolean): Int {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val key = if (hasInternet) "bonus_national_call" else "bonus_national_call_no_internet"
        return prefs.getString(key, if (hasInternet) DEFAULT_NATIONAL_CALL else DEFAULT_NATIONAL_CALL_NO_INTERNET)!!.toInt()
    }

    /**
     * 🔍 **Penalización para prefijos en Blacklist**
     */
    fun getBlacklistPenalty(context: Context): Int {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getString("blacklist_penalty", DEFAULT_BLACKLIST_PENALTY)!!.toInt()
    }
}
