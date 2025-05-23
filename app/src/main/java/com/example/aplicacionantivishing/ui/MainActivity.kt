package com.example.aplicacionantivishing.ui          // ← ajusta a tu paquete

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
import com.example.aplicacionantivishing.util.CallHistoryUtils
import android.provider.Settings
import android.net.Uri
import androidx.appcompat.app.AlertDialog

class MainActivity : AppCompatActivity() {

    private val OVERLAY_REQ = 2001

    companion object { private const val PERMISSION_REQUEST_CODE = 1001 }

    private lateinit var recyclerViewHistory : RecyclerView
    private lateinit var callHistoryAdapter  : CallHistoryAdapter
    private val        callHistoryList       = mutableListOf<CallHistoryEntry>()

    /* ───────────────────────── LIFE-CYCLE ───────────────────────── */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        checkAndRequestPermissions()

        if (!Settings.canDrawOverlays(this)) askOverlayPermission()

        recyclerViewHistory = findViewById(R.id.recyclerViewHistory)
        recyclerViewHistory.layoutManager = LinearLayoutManager(this)
        callHistoryAdapter = CallHistoryAdapter(callHistoryList)
        recyclerViewHistory.adapter = callHistoryAdapter

        /* Ajustes */
        findViewById<Button>(R.id.button_open_settings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        /* Botón para pruebas */
        findViewById<Button>(R.id.button_simulate_call).setOnClickListener {
            simulateReportedIncomingCall()
        }
    }

    override fun onResume() {
        super.onResume()
        cargarHistorial()
    }

    /* ─────────────────────── HISTORIAL ─────────────────────── */
    private fun cargarHistorial() {
        callHistoryList.clear()
        callHistoryList.addAll(CallHistoryUtils.getEntries(this))   // utilitario centralizado
        callHistoryAdapter.notifyDataSetChanged()
    }

    /* ─────────────── SIMULACIÓN DE LLAMADA ─────────────── */
    private fun simulateReportedIncomingCall() {
        val fakeNumber = "+34639025863"
        val risk       = CallAnalyzer.analyzeNumber(this, fakeNumber, "Prueba")

        // Guardamos usando el helper (lleva timestamp)
        CallHistoryUtils.addEntry(this, fakeNumber, risk)

        startActivity(Intent(this, AlertActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra("PHONE_NUMBER", fakeNumber)
            putExtra("CONTACT_NAME", "Desconocido")
            putExtra("RISK_LEVEL",   risk)
        })

        Toast.makeText(this, "Llamada simulada añadida", Toast.LENGTH_SHORT).show()
        Log.d("MainActivity","Simulada llamada $fakeNumber  risk=$risk")
    }

    private fun askOverlayPermission() {
        AlertDialog.Builder(this)
            .setTitle("Permiso de superposición")
            .setMessage("Necesitamos mostrar una ventana flotante sobre la pantalla "
                    +"de llamada para avisarte de posibles fraudes.\n\n"
                    +"Pulsa «Conceder» y activa «Mostrar sobre otras apps».")
            .setPositiveButton("Conceder") { _, _ ->
                startActivityForResult(
                    Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")),
                    OVERLAY_REQ
                )
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    /* ─────────────────────── PERMISOS RUN-TIME ─────────────────────── */
    private fun checkAndRequestPermissions() {
        val needed = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE )
            != PackageManager.PERMISSION_GRANTED) needed += Manifest.permission.READ_PHONE_STATE
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS )
            != PackageManager.PERMISSION_GRANTED) needed += Manifest.permission.POST_NOTIFICATIONS

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG  )
            != PackageManager.PERMISSION_GRANTED) needed += Manifest.permission.READ_CALL_LOG
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS )
            != PackageManager.PERMISSION_GRANTED) needed += Manifest.permission.READ_CONTACTS

        if (needed.isNotEmpty())
            ActivityCompat.requestPermissions(this,
                needed.toTypedArray(), PERMISSION_REQUEST_CODE)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, results: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, results)
        if (requestCode == PERMISSION_REQUEST_CODE &&
            results.any { it != PackageManager.PERMISSION_GRANTED })
            Toast.makeText(this,
                "La app funcionará con capacidades limitadas sin todos los permisos.",
                Toast.LENGTH_SHORT).show()
    }
}
