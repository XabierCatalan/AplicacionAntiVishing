package com.example.aplicacionantivishing.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.aplicacionantivishing.R
import com.example.aplicacionantivishing.adapter.CallHistoryAdapter
import com.example.aplicacionantivishing.adapter.CallHistoryEntry
import com.example.aplicacionantivishing.manager.CallAnalyzer

class MainActivity : AppCompatActivity() {

    companion object {
        private const val PERMISSION_REQUEST_CODE = 1001
    }

    private lateinit var recyclerViewHistory: RecyclerView
    private lateinit var callHistoryAdapter: CallHistoryAdapter
    private val callHistoryList = mutableListOf<CallHistoryEntry>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        checkAndRequestPermissions()

        recyclerViewHistory = findViewById(R.id.recyclerViewHistory)
        recyclerViewHistory.layoutManager = LinearLayoutManager(this)
        callHistoryAdapter = CallHistoryAdapter(callHistoryList)
        recyclerViewHistory.adapter = callHistoryAdapter

        val buttonSettings = findViewById<Button>(R.id.button_open_settings)
        buttonSettings.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        cargarHistorial()
        simulateReportedIncomingCall()

    }

    private fun simulateReportedIncomingCall() {
        val fakePhoneNumber: String = "+31646036429" // 📱 Aquí pon un número que sepamos que esté reportado en Teledigo o ListaSpam
        val fakeContactName: String? = null // 👤 No está en agenda

        // Analizamos como si fuese una llamada real
        val riskLevel = CallAnalyzer.analyzeNumber(this, fakePhoneNumber, fakeContactName)

        val sharedPrefs = getSharedPreferences("call_history", Context.MODE_PRIVATE)
        val editor = sharedPrefs.edit()
        val existingHistory = sharedPrefs.getStringSet("history", mutableSetOf()) ?: mutableSetOf()

        val timestamp = System.currentTimeMillis()
        val newEntry = "$timestamp|$fakePhoneNumber|$riskLevel"

        existingHistory.add(newEntry)

        editor.putStringSet("history", existingHistory)
        editor.apply()

        val intent = Intent(this, AlertActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra("PHONE_NUMBER", fakePhoneNumber)
            putExtra("CONTACT_NAME", fakeContactName ?: "Desconocido")
            putExtra("RISK_LEVEL", riskLevel)
        }
        startActivity(intent)

        Log.d("MainActivity", "Simulación de llamada de número reportado: $fakePhoneNumber ➔ Nivel: $riskLevel")
    }


    private fun cargarHistorial() {
        callHistoryList.clear()

        val sharedPrefs = getSharedPreferences("call_history", Context.MODE_PRIVATE)
        val historySet = sharedPrefs.getStringSet("history", emptySet()) ?: emptySet()

        for (entry in historySet) {
            val parts = entry.split("|")
            if (parts.size == 3) {
                val phoneNumber = parts[1]
                val riskLevel = parts[2]
                callHistoryList.add(CallHistoryEntry(phoneNumber, riskLevel))
            }
        }

        callHistoryAdapter.notifyDataSetChanged()
    }

    private fun checkAndRequestPermissions() {
        val permissionsNeeded = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
            != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.READ_PHONE_STATE)
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG)
            != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.READ_CALL_LOG)
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
            != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.READ_CONTACTS)
        }

        if (permissionsNeeded.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsNeeded.toTypedArray(), PERMISSION_REQUEST_CODE)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSION_REQUEST_CODE) {
            // Aquí podrías verificar si todos los permisos fueron concedidos
            for (result in grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    // Podrías notificar al usuario si quieres
                }
            }
        }
    }
}
