package com.example.aplicacionantivishing.receiver

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.provider.ContactsContract
import android.provider.Settings
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.aplicacionantivishing.manager.CallAnalyzer
import com.example.aplicacionantivishing.manager.SettingsManager
import com.example.aplicacionantivishing.overlay.OverlayService
import com.example.aplicacionantivishing.util.CallHistoryUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Recibe los cambios de estado de llamada y, tras analizar el número,
 * muestra una burbuja (overlay) sobre la pantalla de llamada.
 */
class CallReceiver : BroadcastReceiver() {

    companion object {
        private var lastIncomingNumber: String? = null
        private var lastIncomingName:   String? = null

        private var callInProgress      = false
        private var shouldAlertLater    = false
        private var waitingForNumber    = false
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        context ?: return
        if (intent?.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return

        val stateStr       = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
        @Suppress("DEPRECATION") // seguimos soportando API 24
        val incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)

        when (stateStr) {
            TelephonyManager.EXTRA_STATE_RINGING -> handleRinging(context, incomingNumber)
            TelephonyManager.EXTRA_STATE_IDLE    -> handleIdle(context)
        }
    }

    /* ───────────────────────── ESTADO RINGING ───────────────────────── */
    private fun handleRinging(ctx: Context, incomingNumber: String?) {
        callInProgress = true

        if (!incomingNumber.isNullOrEmpty()) {
            lastIncomingNumber = incomingNumber
            lastIncomingName   = getContactName(ctx, incomingNumber)
            waitingForNumber   = false

            val locked = (ctx.getSystemService(Context.KEYGUARD_SERVICE)
                    as KeyguardManager).isKeyguardLocked
            if (!locked) analyseAndShow(ctx, lastIncomingNumber, lastIncomingName)
            else         shouldAlertLater = true

        } else if (!waitingForNumber) {              // número oculto, espera 500 ms
            waitingForNumber = true
            Handler().postDelayed({
                if (waitingForNumber && lastIncomingNumber == null) {
                    val locked = (ctx.getSystemService(Context.KEYGUARD_SERVICE)
                            as KeyguardManager).isKeyguardLocked
                    if (!locked) analyseAndShow(ctx, null, null)
                    else         shouldAlertLater = true
                }
            }, 500)
        }
    }

    /* ───────────────────────── ESTADO IDLE ───────────────────────── */
    private fun handleIdle(ctx: Context) {
        if (!callInProgress) return

        // quita burbuja activa, si existe
        ctx.stopService(Intent(ctx, OverlayService::class.java))

        if (shouldAlertLater) analyseAndShow(ctx, lastIncomingNumber, lastIncomingName)

        // reset flags
        callInProgress    = false
        shouldAlertLater  = false
        waitingForNumber  = false
        lastIncomingNumber = null
        lastIncomingName   = null
    }

    /* ────────────────── Analiza y lanza overlay ────────────────── */
    private fun analyseAndShow(ctx: Context, numberRaw: String?, contact: String?) {
        CoroutineScope(Dispatchers.IO).launch {
            val normalized = if (numberRaw != null && !numberRaw.startsWith("+"))
                "+34$numberRaw" else numberRaw

            val risk = CallAnalyzer.analyzeNumber(ctx, normalized, contact ?: "")

            CallHistoryUtils.addEntry(
                ctx,
                normalized ?: "desconocido",
                risk.toString()
            )

            if (!SettingsManager.areAlertsEnabled(ctx)) return@launch

            if (!Settings.canDrawOverlays(ctx)) {
                // abre ajustes para conceder permiso la primera vez
                val p = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${ctx.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                ctx.startActivity(p)
            } else {
                val svc = Intent(ctx, OverlayService::class.java).apply {
                    putExtra("PHONE", normalized ?: "desconocido")
                    putExtra("RISK",  risk.toString())
                }

                ctx.startService(svc)
            }
        }
    }

    /* ─────────────── Obtener nombre de contacto ─────────────── */
    private fun getContactName(ctx: Context, number: String): String? {
        val uri = ContactsContract.PhoneLookup.CONTENT_FILTER_URI
            .buildUpon().appendPath(number).build()

        ctx.contentResolver.query(
            uri,
            arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME),
            null, null, null
        )?.use { c -> if (c.moveToFirst()) return c.getString(0) }

        return null
    }
}
