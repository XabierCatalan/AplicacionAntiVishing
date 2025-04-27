package com.example.aplicacionantivishing.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import android.util.Log

class CallReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        // Verificamos que el Intent no sea null (por seguridad absoluta)
        intent?.let {
            val state = it.getStringExtra(TelephonyManager.EXTRA_STATE)

            if (state == TelephonyManager.EXTRA_STATE_RINGING) {
                val incomingNumber = it.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)

                if (incomingNumber == null) {
                    Log.d("CallReceiver", "Llamada entrante detectada: Número oculto")
                } else {
                    Log.d("CallReceiver", "Llamada entrante detectada: $incomingNumber")
                }
            }
        }
    }
}

