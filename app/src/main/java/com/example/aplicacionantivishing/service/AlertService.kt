package com.example.aplicacionantivishing.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.aplicacionantivishing.R
import com.example.aplicacionantivishing.ui.AlertActivity

class AlertService : Service() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        /* ---------- 1. Datos recibidos ---------- */
        val number  = intent?.getStringExtra("PHONE_NUMBER") ?: "Desconocido"
        val contact = intent?.getStringExtra("CONTACT_NAME") ?: "Desconocido"
        val risk    = intent?.getStringExtra("RISK_LEVEL")   ?: "desconocido"

        /* ---------- 2. Notificación foreground ---------- */
        val channelId = "alert_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Alertas",
                NotificationManager.IMPORTANCE_LOW
            )
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Analizando llamada sospechosa")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        // → Elevamos a servicio en primer plano
        startForeground(1001, notification)

        /* ---------- 3. Lanzar AlertActivity ---------- */
        val alertIntent = Intent(this, AlertActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("PHONE_NUMBER", number)
            putExtra("CONTACT_NAME", contact)
            putExtra("RISK_LEVEL",   risk)
        }

        try {
            startActivity(alertIntent)
            Log.d("AlertService", "AlertActivity lanzada desde el foreground service")
        } catch (e: Exception) {
            Log.e("AlertService", "Error al lanzar AlertActivity: ${e.message}")
        }

        /* ---------- 4. Finalizar servicio ---------- */
        stopForeground(STOP_FOREGROUND_REMOVE)   // quita la notificación
        stopSelf()

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
