package com.example.aplicacionantivishing.receiver

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.provider.ContactsContract
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.aplicacionantivishing.manager.CallAnalyzer
import com.example.aplicacionantivishing.manager.SettingsManager
import com.example.aplicacionantivishing.service.AlertService        // ← NUEVO
import com.example.aplicacionantivishing.util.CallHistoryUtils

class CallReceiver : BroadcastReceiver() {

    companion object {
        private var lastIncomingNumber: String? = null
        private var lastIncomingName  : String? = null
        private var callInProgress    = false
        private var shouldLaunchAfterCall = false
        private var isWaitingForNumber   = false
        private var alertLaunched        = false
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d("CallReceiver", "▶ onReceive  intent=$intent  ctx=$context")

        if (intent?.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return

        val stateStr       = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
        val incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)
        Log.d("CallReceiver", "Estado=$stateStr  Número=$incomingNumber")

        when (stateStr) {
            TelephonyManager.EXTRA_STATE_RINGING -> {
                callInProgress = true

                if (!incomingNumber.isNullOrEmpty()) {
                    lastIncomingNumber = incomingNumber
                    lastIncomingName   = getContactName(context, incomingNumber)
                    isWaitingForNumber = false

                    context?.let { ctx ->
                        val locked = (ctx.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager).isKeyguardLocked
                        if (!locked) launchAlert(ctx, lastIncomingNumber, lastIncomingName)
                        else         shouldLaunchAfterCall = true
                    }
                } else {
                    if (!isWaitingForNumber) {
                        isWaitingForNumber = true
                        Handler().postDelayed({
                            if (isWaitingForNumber && lastIncomingNumber.isNullOrEmpty()) {
                                context?.let { ctx ->
                                    val locked = (ctx.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager).isKeyguardLocked
                                    if (!locked) launchAlert(ctx, null, null) else shouldLaunchAfterCall = true
                                }
                            }
                        }, 500)
                    }
                }
            }

            TelephonyManager.EXTRA_STATE_IDLE -> {
                if (callInProgress) {
                    context?.let { ctx ->
                        if (!alertLaunched) {
                            when {
                                lastIncomingNumber.isNullOrEmpty() -> launchAlert(ctx, null, null)
                                shouldLaunchAfterCall             -> launchAlert(ctx, lastIncomingNumber, lastIncomingName)
                            }
                        }
                    }
                    // Reset flags
                    callInProgress        = false
                    shouldLaunchAfterCall = false
                    isWaitingForNumber    = false
                    alertLaunched         = false
                    lastIncomingNumber    = null
                    lastIncomingName      = null
                }
            }
        }
    }

    /* ───────────── ALERTA + HISTORIAL ───────────── */
    private fun launchAlert(ctx: Context, incomingNumber: String?, contactName: String?) {
        alertLaunched = true

        val number = if (incomingNumber != null && !incomingNumber.startsWith("+")) "+34$incomingNumber" else incomingNumber
        val risk   = CallAnalyzer.analyzeNumber(ctx, number, contactName)

        CallHistoryUtils.addEntry(ctx, number ?: "desconocido", risk)

        // Si el usuario ha desactivado las alertas visuales -> sólo registramos historial
        if (!SettingsManager.areAlertsEnabled(ctx)) {
            Log.d("CallReceiver", "Alertas OFF → sólo historial")
            return
        }

        /* ─── Lanzar foreground-service que mostrará la Activity ─── */
        val svcIntent = Intent(ctx, AlertService::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
            putExtra("PHONE_NUMBER", number)
            putExtra("CONTACT_NAME", contactName ?: "Desconocido")
            putExtra("RISK_LEVEL",   risk)
        }
        ContextCompat.startForegroundService(ctx, svcIntent)
        Log.d("CallReceiver", "AlertService solicitado (mostrará AlertActivity)")
    }

    /* ────────── Nombre de contacto ────────── */
    private fun getContactName(context: Context?, number: String): String? {
        context ?: return null
        val uri = ContactsContract.PhoneLookup.CONTENT_FILTER_URI.buildUpon()
            .appendPath(number).build()

        context.contentResolver.query(
            uri,
            arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME),
            null, null, null
        )?.use { c ->
            if (c.moveToFirst()) return c.getString(0)
        }
        return null
    }
}
