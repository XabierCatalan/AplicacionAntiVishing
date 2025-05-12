package com.example.aplicacionantivishing.receiver

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.provider.ContactsContract
import android.telephony.TelephonyManager
import android.util.Log
import com.example.aplicacionantivishing.manager.CallAnalyzer
import com.example.aplicacionantivishing.manager.SettingsManager
import com.example.aplicacionantivishing.ui.AlertActivity
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

        if (intent?.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
            Log.d("CallReceiver", "No es PHONE_STATE → return")
            return
        }

        val stateStr       = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
        val incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)
        Log.d("CallReceiver", "Estado=$stateStr  Número=$incomingNumber")

        when (stateStr) {

            TelephonyManager.EXTRA_STATE_RINGING -> {
                callInProgress = true
                Log.d("CallReceiver", "📞 RINGING")

                if (!incomingNumber.isNullOrEmpty()) {
                    lastIncomingNumber = incomingNumber
                    lastIncomingName   = getContactName(context, incomingNumber)
                    Log.d("CallReceiver", "Número capturado=$lastIncomingNumber  nombre=$lastIncomingName")
                    isWaitingForNumber = false

                    context?.let { ctx ->
                        val isLocked = (ctx.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager).isKeyguardLocked
                        Log.d("CallReceiver", "Pantalla bloqueada=$isLocked")
                        if (!isLocked) launchAlert(ctx, lastIncomingNumber, lastIncomingName)
                        else           shouldLaunchAfterCall = true
                    }
                } else {
                    Log.d("CallReceiver", "Número es null/empty: esperando medio segundo")
                    if (!isWaitingForNumber) {
                        isWaitingForNumber = true
                        Handler().postDelayed({
                            if (isWaitingForNumber && lastIncomingNumber.isNullOrEmpty()) {
                                Log.d("CallReceiver", "Tras espera, nº sigue vacío → lanzar alerta anónima")
                                context?.let { ctx ->
                                    val isLocked = (ctx.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager).isKeyguardLocked
                                    if (!isLocked) launchAlert(ctx, null, null)
                                    else           shouldLaunchAfterCall = true
                                }
                            }
                        }, 500)
                    }
                }
            }

            TelephonyManager.EXTRA_STATE_IDLE -> {
                Log.d("CallReceiver", "☎️ IDLE  callInProgress=$callInProgress")
                if (callInProgress) {
                    context?.let { ctx ->
                        if (!alertLaunched) {
                            when {
                                lastIncomingNumber.isNullOrEmpty() -> {
                                    Log.d("CallReceiver", "Al colgar, nº vacío → alerta anónima")
                                    launchAlert(ctx, null, null)
                                }
                                shouldLaunchAfterCall -> {
                                    Log.d("CallReceiver", "Al colgar, lanzamos alerta diferida")
                                    launchAlert(ctx, lastIncomingNumber, lastIncomingName)
                                }
                                else -> Log.d("CallReceiver", "Al colgar, alerta ya lanzada en RINGING")
                            }
                        }
                    }
                    // reset
                    callInProgress        = false
                    shouldLaunchAfterCall = false
                    isWaitingForNumber    = false
                    alertLaunched         = false
                    lastIncomingNumber    = null
                    lastIncomingName      = null
                    Log.d("CallReceiver", "Flags reseteados")
                }
            }

            else -> Log.d("CallReceiver", "Estado no manejado: $stateStr")
        }
    }

    /* ───────────── ALERTA + HISTORIAL ───────────── */
    private fun launchAlert(ctx: Context,
                            incomingNumber: String?,
                            contactName:    String?) {

        alertLaunched = true
        Log.d("CallReceiver", "→ launchAlert  num=$incomingNumber  name=$contactName")

        val number = if (incomingNumber != null && !incomingNumber.startsWith("+"))
            "+34$incomingNumber" else incomingNumber
        Log.d("CallReceiver", "Número normalizado=$number")

        val risk = CallAnalyzer.analyzeNumber(ctx, number, contactName)
        Log.d("CallReceiver", "RiskLevel=$risk  (guardando en historial)")
        CallHistoryUtils.addEntry(ctx, number ?: "desconocido", risk)

        if (!SettingsManager.areAlertsEnabled(ctx)) {
            Log.d("CallReceiver", "Alertas desactivadas → no UI")
            return
        }

        val intent = Intent(ctx, AlertActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra("PHONE_NUMBER", number)
            putExtra("CONTACT_NAME", contactName ?: "Desconocido")
            putExtra("RISK_LEVEL",   risk)
        }
        Log.d("CallReceiver", "startActivity(AlertActivity)")
        ctx.startActivity(intent)
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
            if (c.moveToFirst()) {
                val name = c.getString(0)
                Log.d("CallReceiver", "Contacto encontrado: $name")
                return name
            }
        }
        Log.d("CallReceiver", "Sin contacto para $number")
        return null
    }
}
