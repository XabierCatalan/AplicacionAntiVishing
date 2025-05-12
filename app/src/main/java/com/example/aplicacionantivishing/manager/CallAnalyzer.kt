package com.example.aplicacionantivishing.manager

import android.content.Context
import android.preference.PreferenceManager
import android.util.Log
import com.example.aplicacionantivishing.network.OsintChecker
import kotlinx.coroutines.runBlocking
import java.io.BufferedReader
import java.io.InputStreamReader
import com.example.aplicacionantivishing.R
import android.telephony.TelephonyManager

object CallAnalyzer {

    private var nationalPrefix: String? = null

    fun loadSimPrefix(context: Context) {
        if (nationalPrefix != null) return

        val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        val simCountryIso = telephonyManager.simCountryIso.uppercase()

        try {
            val inputStream = context.resources.openRawResource(R.raw.paises)
            val reader = BufferedReader(InputStreamReader(inputStream))
            reader.useLines { lines ->
                lines.drop(1).forEach { line ->
                    val parts = line.split(";")
                    if (parts.size >= 4) {
                        val countryIso = parts[1].trim().uppercase()
                        if (countryIso == simCountryIso) {
                            nationalPrefix = "+" + parts[3].trim()
                            return@forEach
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("CallUtils", "Error al cargar los prefijos: ${e.message}")
        }
    }

    fun getNationalPrefix(): String? = nationalPrefix

    fun clearSimPrefix() {
        nationalPrefix = null
        Log.d("CallUtils", "Prefijo nacional eliminado de memoria.")
    }

    fun cleanInternationalPrefix(context: Context, phoneNumber: String): String {
        var cleanNumber = phoneNumber

        try {
            val inputStream = context.resources.openRawResource(R.raw.paises)
            val reader = BufferedReader(InputStreamReader(inputStream))

            reader.useLines { lines ->
                lines.drop(1).forEach { line ->
                    val parts = line.split(";")
                    if (parts.size >= 4) {
                        val prefix = "+" + parts[3].trim()

                        // Si el número empieza por este prefijo, lo quitamos y paramos la búsqueda
                        if (phoneNumber.startsWith(prefix)) {
                            cleanNumber = phoneNumber.removePrefix(prefix)
                            Log.d("CallAnalyzer", "Prefijo $prefix detectado y eliminado → Número limpio: $cleanNumber")
                            return cleanNumber
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("CallAnalyzer", "Error al cargar los prefijos: ${e.message}")
        }

        Log.d("CallAnalyzer", "No se detectó prefijo internacional en: $phoneNumber")
        return cleanNumber
    }

    fun analyzeNumber(context: Context, phoneNumber: String?, contactName: String?): String {


        loadSimPrefix(context)
        if (phoneNumber.isNullOrEmpty()) {
            Log.d("CallAnalyzer", "Número oculto detectado → Confianza = 0%")
            return "dangerous"
        }

        var confidence = 100
        Log.d("CallAnalyzer", "Confianza inicial: $confidence%")

        val hasInternet = InternetManager.isInternetAvailable(context)

        val penaltySuspiciousName = SettingsManager.getPenaltySuspiciousName(context, hasInternet)
        val penaltyInternational = SettingsManager.getPenaltyInternationalCall(context, hasInternet)
        val penaltyFirstCall = SettingsManager.getPenaltyFirstCall(context, hasInternet)
        val bonusVerifiedContact = SettingsManager.getBonusVerifiedContact(context, hasInternet)
        val bonusNationalCall = SettingsManager.getBonusNationalCall(context, hasInternet)

        if (isPrefixInBlacklist(context, phoneNumber)) {
            confidence -= 90
            Log.d("CallAnalyzer", "Prefijo en blacklist ➔ -90 ➔ Confianza ahora: $confidence%")
        }
        if (contactNameIsSuspicious(contactName)) {
            val penalty = if (hasInternet) 70 else 80
            confidence -= penalty
            Log.d("CallAnalyzer", "Nombre sospechoso ➔ -$penalty ➔ Confianza ahora: $confidence%")
        }
        if (hasInternet && isNumberReportedInOsint(context, phoneNumber)) {
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

        if (isNationalPrefix(context, phoneNumber)) {
            confidence += 5
            Log.d("CallAnalyzer", "Prefijo nacional ➔ +5 ➔ Confianza ahora: $confidence%")
        }

        clearSimPrefix()

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

    private fun isNumberReportedInOsint(context: Context, phoneNumber: String): Boolean {

        // Llamada al método que limpia el prefijo
        val cleanNumber = cleanInternationalPrefix(context, phoneNumber)

        Log.d("CallAnalyzer", "Número limpio para OSINT: $cleanNumber")

        return runBlocking {
            if (OsintChecker.isReportedInTeledigo(cleanNumber)) {
                Log.d("CallAnalyzer", "Número encontrado en Teledigo")
                true
            } else if (OsintChecker.isReportedInListaSpam(cleanNumber)) {
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
        val prefix = getNationalPrefix()
        return prefix?.let {
            !phoneNumber.startsWith(it)
        } ?: false
    }

    private fun isFirstTimeCalling(context: Context, phoneNumber: String): Boolean {
        val sharedPrefs = context.getSharedPreferences("call_history", Context.MODE_PRIVATE)
        val historySet = sharedPrefs.getStringSet("history", emptySet()) ?: return true

        return historySet.none { entry ->
            entry.contains("|$phoneNumber|")
        }
    }


    private fun isNationalPrefix(context: Context, phoneNumber: String): Boolean {
        val prefix = getNationalPrefix()
        return prefix?.let {
            phoneNumber.startsWith(it)
        } ?: false
    }

}
