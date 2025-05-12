package com.example.aplicacionantivishing.ui          // ⬅️ ajústalo a tu paquete

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.aplicacionantivishing.R
import com.example.aplicacionantivishing.adapter.CallHistoryAdapter
import com.example.aplicacionantivishing.adapter.CallHistoryEntry
import com.example.aplicacionantivishing.manager.CallAnalyzer

import com.example.aplicacionantivishing.manager.SettingsManager

class MainActivity : AppCompatActivity() {

    companion object { private const val PERMISSION_REQUEST_CODE = 1001 }

    private lateinit var recyclerViewHistory: RecyclerView
    private lateinit var callHistoryAdapter: CallHistoryAdapter
    private val callHistoryList = mutableListOf<CallHistoryEntry>()

    /* ───────────────────────── LIFE-CYCLE ───────────────────────── */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        checkAndRequestPermissions()

        recyclerViewHistory = findViewById(R.id.recyclerViewHistory)
        recyclerViewHistory.layoutManager = LinearLayoutManager(this)
        callHistoryAdapter = CallHistoryAdapter(callHistoryList)
        recyclerViewHistory.adapter = callHistoryAdapter

        // Botón Ajustes
        findViewById<Button>(R.id.button_open_settings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        // Botón Simular llamada: SIEMPRE visible
        findViewById<Button>(R.id.button_simulate_call).setOnClickListener {
            simulateReportedIncomingCall()
        }
    }

    /** Al volver a primer plano refrescamos el historial */
    override fun onResume() {
        super.onResume()
        cargarHistorial()
    }

    /* ───────────────────────── HISTORIAL ───────────────────────── */
    private fun cargarHistorial() {
        callHistoryList.clear()

        val prefs = getSharedPreferences("call_history", Context.MODE_PRIVATE)
        val history = prefs.getStringSet("history", emptySet()) ?: emptySet()

        // ordenar por timestamp descendente (lo más reciente arriba)
        history.sortedByDescending { it.substringBefore('|').toLong() }
            .forEach { entry ->
                val parts = entry.split("|")
                if (parts.size == 3) {
                    val number = parts[1]
                    val risk   = parts[2]
                    val display = lookupContactName(this, number) ?: number
                    callHistoryList.add(CallHistoryEntry(display, risk))
                }
            }

        callHistoryAdapter.notifyDataSetChanged()
    }

    /** Busca el nombre del contacto; devuelve null si no existe. */
    private fun lookupContactName(ctx: Context, number: String): String? {
        val uri = android.provider.ContactsContract.PhoneLookup.CONTENT_FILTER_URI
            .buildUpon().appendPath(number).build()

        ctx.contentResolver.query(
            uri,
            arrayOf(android.provider.ContactsContract.PhoneLookup.DISPLAY_NAME),
            null, null, null
        )?.use { c ->
            if (c.moveToFirst()) return c.getString(0)
        }
        return null
    }

    /* ───────────────────────── SIMULACIÓN ───────────────────────── */
    private fun simulateReportedIncomingCall() {
        val fakeNumber = "+34682865557"      // número de prueba
        val riskLevel  = CallAnalyzer.analyzeNumber(this, fakeNumber, "Spam Ander")

        saveCallToHistory(fakeNumber, riskLevel)
        startActivity(
            Intent(this, AlertActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                putExtra("PHONE_NUMBER", fakeNumber)
                putExtra("CONTACT_NAME", "Desconocido")
                putExtra("RISK_LEVEL",   riskLevel)
            }
        )

        Toast.makeText(this, "Llamada simulada añadida", Toast.LENGTH_SHORT).show()
        Log.d("MainActivity","Simulada llamada $fakeNumber risk=$riskLevel")
    }

    /** Guarda una entrada en SharedPreferences */
    private fun saveCallToHistory(number: String, risk: String) {
        val prefs = getSharedPreferences("call_history", Context.MODE_PRIVATE)
        val set   = prefs.getStringSet("history", mutableSetOf())!!.toMutableSet()
        set.add("${System.currentTimeMillis()}|$number|$risk")
        prefs.edit().putStringSet("history", set).apply()
    }

    /* ─────────────────────── PERMISOS RUN-TIME ─────────────────────── */
    private fun checkAndRequestPermissions() {
        val needed = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
            != PackageManager.PERMISSION_GRANTED) needed += Manifest.permission.READ_PHONE_STATE
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG)
            != PackageManager.PERMISSION_GRANTED) needed += Manifest.permission.READ_CALL_LOG
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
            != PackageManager.PERMISSION_GRANTED) needed += Manifest.permission.READ_CONTACTS



        if (needed.isNotEmpty())
            ActivityCompat.requestPermissions(this,
                needed.toTypedArray(), PERMISSION_REQUEST_CODE)
    }

    override fun onRequestPermissionsResult(
        code: Int, perms: Array<out String>, results: IntArray
    ) {
        super.onRequestPermissionsResult(code, perms, results)
        if (code == PERMISSION_REQUEST_CODE && results.any { it != PackageManager.PERMISSION_GRANTED })
            Toast.makeText(this,
                "La app puede funcionar limitado sin todos los permisos.",
                Toast.LENGTH_SHORT).show()
    }
}
