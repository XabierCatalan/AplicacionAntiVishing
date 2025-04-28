package com.example.aplicacionantivishing.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import android.app.KeyguardManager
import android.os.Handler
import android.util.Log
import com.example.aplicacionantivishing.manager.CallAnalyzer
import com.example.aplicacionantivishing.ui.AlertActivity

class CallReceiver : BroadcastReceiver() {

    companion object {
        private var lastIncomingNumber: String? = null
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
                        isWaitingForNumber = false

                        context?.let { ctx ->
                            val keyguardManager = ctx.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
                            val isLocked = keyguardManager.isKeyguardLocked

                            if (!isLocked) {
                                launchAlert(ctx, lastIncomingNumber)
                            } else {
                                shouldLaunchAfterCall = true
                            }
                        }
                    } else {
                        if (!isWaitingForNumber) {
                            isWaitingForNumber = true
                            Handler().postDelayed({
                                if (isWaitingForNumber && (lastIncomingNumber.isNullOrEmpty())) {
                                    // Número sigue nulo tras 500 ms → Confirmar número oculto
                                    context?.let { ctx ->
                                        val keyguardManager = ctx.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
                                        val isLocked = keyguardManager.isKeyguardLocked

                                        if (!isLocked) {
                                            launchAlert(ctx, null)
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
                            if (!alertLaunched) { // ✅ Solo lanzamos si no se lanzó antes
                                if (lastIncomingNumber.isNullOrEmpty()) {
                                    // Si sigue sin número tras terminar → Número oculto
                                    launchAlert(ctx, null)
                                } else if (shouldLaunchAfterCall) {
                                    launchAlert(ctx, lastIncomingNumber)
                                }
                            }
                        }
                        callInProgress = false
                        shouldLaunchAfterCall = false
                        isWaitingForNumber = false
                        alertLaunched = false // 🔥 Importante: resetear para la próxima llamada
                        lastIncomingNumber = null
                    }
                }


                else -> { /* Ignoramos otros estados */ }
            }
        }
    }

    private fun launchAlert(context: Context, incomingNumber: String?) {

        alertLaunched = true
        val riskLevel = CallAnalyzer.analyzeNumber(context, incomingNumber)

        val intent = Intent(context, AlertActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra("PHONE_NUMBER", incomingNumber ?: "Número desconocido")
            putExtra("RISK_LEVEL", riskLevel)
        }
        context.startActivity(intent)

        Log.d("CallReceiver", "AlertActivity lanzada con RISK_LEVEL: $riskLevel y número: ${incomingNumber ?: "Número desconocido"}")
    }
}
