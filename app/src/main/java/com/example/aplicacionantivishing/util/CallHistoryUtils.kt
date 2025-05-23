package com.example.aplicacionantivishing.util

import android.content.Context
import com.example.aplicacionantivishing.adapter.CallHistoryEntry

object CallHistoryUtils {

    /** Guarda la llamada en SharedPreferences */
    fun addEntry(ctx: Context, number: String, risk: String) {
        val prefs = ctx.getSharedPreferences("call_history", Context.MODE_PRIVATE)
        val set   = prefs.getStringSet("history", mutableSetOf())!!.toMutableSet()

        val raw = "${System.currentTimeMillis()}|$number|$risk"
        set.add(raw)

        prefs.edit().putStringSet("history", set).apply()
    }

    /** Devuelve la lista parseada y ordenada (más recientes primero) */
    fun getEntries(ctx: Context): List<CallHistoryEntry> {
        val prefs = ctx.getSharedPreferences("call_history", Context.MODE_PRIVATE)
        val set   = prefs.getStringSet("history", emptySet()) ?: emptySet()

        return set.mapNotNull { line ->
            val p = line.split("|")
            if (p.size == 3) CallHistoryEntry(p[1], p[2], p[0].toLong()) else null
        }.sortedByDescending { it.timestamp }
    }
}
