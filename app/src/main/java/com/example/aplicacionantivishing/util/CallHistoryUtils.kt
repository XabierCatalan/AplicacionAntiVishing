package com.example.aplicacionantivishing.util

import android.content.Context

/**
 *  Utilidad central para leer / escribir el historial de llamadas
 *  Evita el bug de referencia mutando SIEMPRE una copia del Set.
 */
object CallHistoryUtils {

    fun addEntry(ctx: Context, number: String, risk: String) {
        val prefs   = ctx.getSharedPreferences("call_history", Context.MODE_PRIVATE)
        val current = prefs.getStringSet("history", emptySet()) ?: emptySet()
        val clone   = HashSet(current)                        // ← CLONAMOS
        clone += "${System.currentTimeMillis()}|$number|$risk"
        prefs.edit().putStringSet("history", clone).apply()   // guardamos la copia
    }
}
