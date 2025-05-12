package com.example.aplicacionantivishing.manager

import android.content.Context
import android.preference.PreferenceManager
import android.util.Log
import com.example.aplicacionantivishing.network.OsintChecker
import kotlinx.coroutines.runBlocking

object CallAnalyzer {

    fun analyzeNumber(context: Context, phoneNumber: String?, contactName: String?): String {
        if (phoneNumber.isNullOrEmpty()) {
            Log.d("CallAnalyzer", "Número oculto detectado → Confianza = 0%")
            return "dangerous"
        }

        var confidence = 100
        Log.d("CallAnalyzer", "Confianza inicial: $confidence%")

        val hasInternet = InternetManager.isInternetAvailable(context)

        if (isPrefixInBlacklist(context, phoneNumber)) {
            confidence -= 90
            Log.d("CallAnalyzer", "Prefijo en blacklist ➔ -90 ➔ Confianza ahora: $confidence%")
        }
        if (contactNameIsSuspicious(contactName)) {
            val penalty = if (hasInternet) 70 else 80
            confidence -= penalty
            Log.d("CallAnalyzer", "Nombre sospechoso ➔ -$penalty ➔ Confianza ahora: $confidence%")
        }
        if (hasInternet && isNumberReportedInOsint(phoneNumber)) {
            confidence -= 50
            Log.d("CallAnalyzer", "Número reportado en OSINT ➔ -50 ➔ Confianza ahora: $confidence%")
        }

        if (!isSavedInContacts(context, phoneNumber)) {
            val penalty = if (hasInternet) 10 else 20
            confidence -= penalty
            Log.d("CallAnalyzer", "Número NO en contactos ➔ -$penalty ➔ Confianza ahora: $confidence%")
        } else {
            val bonus = if (hasInternet) 10 else 20
            confidence += bonus
            Log.d("CallAnalyzer", "Número en contactos ➔ +$bonus ➔ Confianza ahora: $confidence%")
        }

        if (isInternationalCall(phoneNumber)
            && !isPrefixInBlacklist(context, phoneNumber)
            && !isPrefixInWhitelist(context, phoneNumber)) {
            val penalty = if (hasInternet) 30 else 40
            confidence -= penalty
            Log.d("CallAnalyzer", "Llamada internacional (no blacklist, no whitelist) ➔ -$penalty ➔ Confianza ahora: $confidence%")
        }

        if (isFirstTimeCalling(context, phoneNumber)) {
            val penalty = if (hasInternet) 10 else 20
            confidence -= penalty
            Log.d("CallAnalyzer", "Primera vez que llama ➔ -$penalty ➔ Confianza ahora: $confidence%")
        }

        if (isNationalPrefix(phoneNumber)) {
            confidence += 5
            Log.d("CallAnalyzer", "Prefijo nacional ➔ +5 ➔ Confianza ahora: $confidence%")
        }

        if (confidence > 100) confidence = 100
        if (confidence < 0) confidence = 0



        Log.d("CallAnalyzer", "Confianza final: $confidence%")

        return when {
            confidence < 30 -> "dangerous"
            confidence in 30..69 -> "suspicious"
            else -> "safe"
        }
    }

    private fun isPrefixInBlacklist(context: Context, phoneNumber: String): Boolean {
        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context)
        val blacklist = sharedPrefs.getStringSet("blacklist_prefixes", emptySet()) ?: emptySet()

        return blacklist.any { prefix -> phoneNumber.startsWith(prefix) }
    }

    private fun isPrefixInWhitelist(context: Context, phoneNumber: String): Boolean {
        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context)
        val whitelist = sharedPrefs.getStringSet("whitelist_prefixes", emptySet()) ?: emptySet()

        return whitelist.any { prefix -> phoneNumber.startsWith(prefix) }
    }

    private fun contactNameIsSuspicious(contactName: String?): Boolean {
        if (contactName.isNullOrEmpty()) return false

        val suspiciousKeywords = listOf("spam", "fraude", "scam", "estafa", "telemarketing")

        return suspiciousKeywords.any { keyword ->
            contactName.lowercase().contains(keyword)
        }
    }

    private fun isNumberReportedInOsint(phoneNumber: String): Boolean {
        return runBlocking {
            if (OsintChecker.isReportedInTeledigo(phoneNumber)) {
                Log.d("CallAnalyzer", "Número encontrado en Teledigo")
                true
            } else if (OsintChecker.isReportedInListaSpam(phoneNumber)) {
                Log.d("CallAnalyzer", "Número encontrado en ListaSpam")
                true
            } else {
                Log.d("CallAnalyzer", "Número NO encontrado en OSINT")
                false
            }
        }
    }



    private fun isSavedInContacts(context: Context, phoneNumber: String): Boolean {
        val contentResolver = context.contentResolver

        val uri = android.provider.ContactsContract.PhoneLookup.CONTENT_FILTER_URI.buildUpon()
            .appendPath(phoneNumber)
            .build()

        val cursor = contentResolver.query(
            uri,
            arrayOf(android.provider.ContactsContract.PhoneLookup._ID),
            null,
            null,
            null
        )

        cursor?.use {
            if (it.moveToFirst()) {
                return true // 📱 El número existe en los contactos
            }
        }
        return false // 🚫 No encontrado
    }


    private fun isInternationalCall(phoneNumber: String): Boolean {
        val nationalPrefix = "+34" // Asumimos España de momento

        return !phoneNumber.startsWith(nationalPrefix)
    }

    private fun isFirstTimeCalling(context: Context, phoneNumber: String): Boolean {
        val sharedPrefs = context.getSharedPreferences("call_history", Context.MODE_PRIVATE)
        val historySet = sharedPrefs.getStringSet("history", emptySet()) ?: return true

        return historySet.none { entry ->
            entry.contains("|$phoneNumber|")
        }
    }


    private fun isNationalPrefix(phoneNumber: String): Boolean {
        val nationalPrefix = "+34"

        return phoneNumber.startsWith(nationalPrefix)
    }
}
