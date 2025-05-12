package com.example.aplicacionantivishing.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import android.app.KeyguardManager
import android.os.Handler
import android.provider.ContactsContract
import android.util.Log
import com.example.aplicacionantivishing.manager.CallAnalyzer
import com.example.aplicacionantivishing.manager.SettingsManager
import com.example.aplicacionantivishing.ui.AlertActivity

class CallReceiver : BroadcastReceiver() {

    companion object {
        private var lastIncomingNumber: String? = null
        private var lastIncomingName: String? = null
        private var callInProgress = false
        private var shouldLaunchAfterCall = false
        private var isWaitingForNumber = false
        private var alertLaunched = false
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent == null) return

        val action = intent.action
        if (action != null && action == TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
            val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
            val incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)

            Log.d("CallReceiver", "Acción: $action, Estado: $state, Número: $incomingNumber")

            when (state) {
                TelephonyManager.EXTRA_STATE_RINGING -> {
                    callInProgress = true

                    if (!incomingNumber.isNullOrEmpty()) {
                        lastIncomingNumber = incomingNumber
                        lastIncomingName = getContactName(context, incomingNumber) // 🔥 Aquí capturamos el nombre
                        isWaitingForNumber = false

                        context?.let { ctx ->
                            val keyguardManager = ctx.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
                            val isLocked = keyguardManager.isKeyguardLocked

                            if (!isLocked) {
                                launchAlert(ctx, lastIncomingNumber, lastIncomingName)
                            } else {
                                shouldLaunchAfterCall = true
                            }
                        }
                    } else {
                        if (!isWaitingForNumber) {
                            isWaitingForNumber = true
                            Handler().postDelayed({
                                if (isWaitingForNumber && (lastIncomingNumber.isNullOrEmpty())) {
                                    context?.let { ctx ->
                                        val keyguardManager = ctx.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
                                        val isLocked = keyguardManager.isKeyguardLocked

                                        if (!isLocked) {
                                            launchAlert(ctx, null, null)
                                        } else {
                                            shouldLaunchAfterCall = true
                                        }
                                    }
                                }
                            }, 500) // 🔥 Esperamos solo medio segundo
                        }
                    }
                }

                TelephonyManager.EXTRA_STATE_IDLE -> {
                    if (callInProgress) {
                        context?.let { ctx ->
                            if (!alertLaunched) {
                                if (lastIncomingNumber.isNullOrEmpty()) {
                                    launchAlert(ctx, null, null)
                                } else if (shouldLaunchAfterCall) {
                                    launchAlert(ctx, lastIncomingNumber, lastIncomingName)
                                }
                            }
                        }
                        callInProgress = false
                        shouldLaunchAfterCall = false
                        isWaitingForNumber = false
                        alertLaunched = false
                        lastIncomingNumber = null
                        lastIncomingName = null
                    }
                }

                else -> { /* Ignoramos otros estados */ }
            }
        }
    }

    private fun saveCallToHistory(context: Context, phoneNumber: String, riskLevel: String) {
        val sharedPrefs = context.getSharedPreferences("call_history", Context.MODE_PRIVATE)
        val editor = sharedPrefs.edit()

        // Guardamos en una lista simulada como String (muy simple por ahora)
        val existingHistory = sharedPrefs.getStringSet("history", mutableSetOf()) ?: mutableSetOf()

        val timestamp = System.currentTimeMillis()
        val newEntry = "$timestamp|$phoneNumber|$riskLevel"

        existingHistory.add(newEntry)

        editor.putStringSet("history", existingHistory)
        editor.apply()
    }

    private fun launchAlert(
        context: Context,
        incomingNumber: String?,
        contactName:    String?
    ) {
        alertLaunched = true

        /* 1) normaliza número ------------------------------------------------ */
        val number = if (incomingNumber != null && !incomingNumber.startsWith("+"))
            "+34$incomingNumber"
        else incomingNumber

        /* 2) analiza y guarda ------------------------------------------------ */
        val risk = CallAnalyzer.analyzeNumber(context, number, contactName)
        saveCallToHistory(context, number ?: "desconocido", risk)

        /* 3) si el usuario desactivó las alertas, salimos aquí --------------- */
        if (!SettingsManager.areAlertsEnabled(context)) {
            Log.d("CallReceiver", "Alertas OFF → no se lanza ventana")
            return                        // ← NO se abre AlertActivity
        }

        /* 4) mostrar la alerta ---------------------------------------------- */
        val i = Intent(context, AlertActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra("PHONE_NUMBER", number)
            putExtra("CONTACT_NAME", contactName ?: "Desconocido")
            putExtra("RISK_LEVEL",   risk)
        }
        context.startActivity(i)
    }



    private fun getContactName(context: Context?, phoneNumber: String): String? {
        context ?: return null

        val contentResolver = context.contentResolver
        val uri = ContactsContract.PhoneLookup.CONTENT_FILTER_URI.buildUpon()
            .appendPath(phoneNumber)
            .build()

        val cursor = contentResolver.query(
            uri,
            arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME),
            null,
            null,
            null
        )

        cursor?.use {
            if (it.moveToFirst()) {
                return it.getString(it.getColumnIndexOrThrow(ContactsContract.PhoneLookup.DISPLAY_NAME))
            }
        }

        return null
    }
}
