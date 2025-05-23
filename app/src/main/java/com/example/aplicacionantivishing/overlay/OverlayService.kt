package com.example.aplicacionantivishing.overlay

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.*
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import com.example.aplicacionantivishing.R

class OverlayService : Service() {

    private lateinit var wm: WindowManager
    private lateinit var bubble: View
    private lateinit var params: WindowManager.LayoutParams

    // Guardamos los datos para el click
    private var currentNumber: String = "desconocido"
    private var currentRisk:   String = "safe"

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return START_NOT_STICKY
        }

        // Recogemos y almacenamos los extras
        currentNumber = intent?.getStringExtra("PHONE") ?: currentNumber
        currentRisk   = intent?.getStringExtra("RISK")  ?: currentRisk

        if (!::bubble.isInitialized) {
            createBubble()
        }
        updateBubble(currentNumber, currentRisk)
        return START_NOT_STICKY
    }

    private fun createBubble() {
        bubble = LayoutInflater.from(this)
            .inflate(R.layout.view_call_overlay, null)

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else WindowManager.LayoutParams.TYPE_PHONE

        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,    // ancho del contenido
            WindowManager.LayoutParams.WRAP_CONTENT,    // alto del contenido
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER                  // centrado en pantalla
        }

        wm.addView(bubble, params)

        bubble.setOnClickListener {
            startActivity(
                Intent(this, com.example.aplicacionantivishing.ui.AlertActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    putExtra("PHONE_NUMBER", currentNumber)
                    putExtra("RISK_LEVEL",   currentRisk)
                }
            )
            stopSelf()
        }
    }

    private fun updateBubble(number: String, risk: String) {
        // Color y mensaje según safe/dangerous
        val isSafe = risk.equals("safe", ignoreCase = true)
        val bgColor = if (isSafe)
            ContextCompat.getColor(this, R.color.safeGreen)
        else
            ContextCompat.getColor(this, R.color.dangerRed)

        val msg = if (isSafe) "Llamada segura" else "⚠ Posible estafa"

        // Actualizamos CardView y textos
        bubble.findViewById<CardView>(R.id.card)
            .setCardBackgroundColor(bgColor)
        bubble.findViewById<TextView>(R.id.tvNumber).text = number
        bubble.findViewById<TextView>(R.id.tvMsg).text    = msg
    }

    override fun onDestroy() {
        if (::bubble.isInitialized) wm.removeView(bubble)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
