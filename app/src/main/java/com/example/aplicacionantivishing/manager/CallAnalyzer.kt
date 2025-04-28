package com.example.aplicacionantivishing.manager

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

object CallAnalyzer {

    fun analyzeNumber(context: Context, phoneNumber: String?): String {
        if (phoneNumber.isNullOrEmpty()) {
            return "dangerous" // Número oculto: alerta directa
        }

        var confidence = 100

        val hasInternet = isInternetAvailable(context)

        if (isPrefixInBlacklist(phoneNumber)) confidence -= 90
        if (contactNameIsSuspicious(context, phoneNumber)) confidence -= if (hasInternet) 70 else 80
        if (hasInternet && isNumberReportedInOsint(phoneNumber)) confidence -= 50
        if (hasInternet && isNumberVerifiedInOsint(phoneNumber)) confidence += 15
        if (!isSavedInContacts(context, phoneNumber)) confidence -= if (hasInternet) 10 else 20
        if (isInternationalCall(phoneNumber) && !isPrefixInBlacklist(phoneNumber)) confidence -= if (hasInternet) 30 else 40
        if (isFirstTimeCalling(context, phoneNumber)) confidence -= if (hasInternet) 10 else 20
        if (isSavedInContacts(context, phoneNumber)) confidence += if (hasInternet) 10 else 20
        if (hasCalledBefore(context, phoneNumber)) confidence += 10
        if (isNationalPrefix(phoneNumber)) confidence += 5

        // Normalizar confianza
        if (confidence > 100) confidence = 100
        if (confidence < 0) confidence = 0

        return when {
            confidence < 30 -> "dangerous"
            confidence in 30..69 -> "suspicious"
            else -> "safe"
        }
    }

    private fun isInternetAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun isPrefixInBlacklist(phoneNumber: String): Boolean {
        // TODO: Comprobar si el prefijo está en la lista negra
        return false
    }

    private fun contactNameIsSuspicious(context: Context, phoneNumber: String): Boolean {
        // TODO: Comprobar si el contacto tiene palabras como "spam", "fraude", etc.
        return false
    }

    private fun isNumberReportedInOsint(phoneNumber: String): Boolean {
        // TODO: Consultar bases de datos de spam online (Teledigo, Listaspam)
        return false
    }

    private fun isNumberVerifiedInOsint(phoneNumber: String): Boolean {
        // TODO: Consultar si el número está verificado como seguro en OSINT
        return false
    }

    private fun isSavedInContacts(context: Context, phoneNumber: String): Boolean {
        // TODO: Comprobar si el número está en la agenda
        return false
    }

    private fun isInternationalCall(phoneNumber: String): Boolean {
        // TODO: Analizar si es una llamada internacional
        return false
    }

    private fun isFirstTimeCalling(context: Context, phoneNumber: String): Boolean {
        // TODO: Comprobar si es la primera vez que llama
        return false
    }

    private fun hasCalledBefore(context: Context, phoneNumber: String): Boolean {
        // TODO: Comprobar historial de llamadas
        return false
    }

    private fun isNationalPrefix(phoneNumber: String): Boolean {
        // TODO: Comprobar si el prefijo es nacional (España)
        return false
    }
}
