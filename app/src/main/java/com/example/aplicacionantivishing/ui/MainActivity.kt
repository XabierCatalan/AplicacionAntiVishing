package com.example.aplicacionantivishing.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
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

    private lateinit var recyclerView: RecyclerView
    private lateinit var callHistoryAdapter: CallHistoryAdapter

    companion object {
        private const val PERMISSION_REQUEST_CODE = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inicializar RecyclerView
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Inicializar adaptador vacío
        callHistoryAdapter = CallHistoryAdapter(mutableListOf())
        recyclerView.adapter = callHistoryAdapter

        // Pedir permisos necesarios
        checkAndRequestPermissions()

        // Mostrar historial inicial
        mostrarHistorial()

        // Simular llamada (puedes comentar esta línea si quieres)
        simulateIncomingCall()
    }

    private fun mostrarHistorial() {
        val sharedPrefs = getSharedPreferences("call_history", Context.MODE_PRIVATE)
        val historySet = sharedPrefs.getStringSet("history", emptySet()) ?: emptySet()

        val callHistoryList = historySet.mapNotNull { entry ->
            val parts = entry.split("|")
            if (parts.size == 3) {
                CallHistoryEntry(
                    phoneNumber = parts[1],
                    riskLevel = parts[2]
                )
            } else {
                null
            }
        }.sortedByDescending { it.phoneNumber } // Opcional

        callHistoryAdapter.updateList(callHistoryList)
    }

    private fun simulateIncomingCall() {
        val fakePhoneNumber: String? = "+11600111222"
        val fakeContactName: String? = "Jose Luis"

        val riskLevel = CallAnalyzer.analyzeNumber(this, fakePhoneNumber, fakeContactName)

        val sharedPrefs = getSharedPreferences("call_history", Context.MODE_PRIVATE)
        val editor = sharedPrefs.edit()
        val existingHistory = sharedPrefs.getStringSet("history", mutableSetOf())?.toMutableSet() ?: mutableSetOf()

        val timestamp = System.currentTimeMillis()
        val newEntry = "$timestamp|$fakePhoneNumber|$riskLevel"
        existingHistory.add(newEntry)

        editor.putStringSet("history", existingHistory)
        editor.apply()

        mostrarHistorial()

        val intent = Intent(this, AlertActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra("PHONE_NUMBER", fakePhoneNumber)
            putExtra("CONTACT_NAME", fakeContactName)
            putExtra("RISK_LEVEL", riskLevel)
        }
        startActivity(intent)
    }

    private fun checkAndRequestPermissions() {
        val permissionsNeeded = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.READ_PHONE_STATE)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.READ_CALL_LOG)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.READ_CONTACTS)
        }

        if (permissionsNeeded.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsNeeded.toTypedArray(), PERMISSION_REQUEST_CODE)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSION_REQUEST_CODE) {
            for (result in grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    // Podrías mostrar un aviso aquí
                }
            }
        }
    }
}
