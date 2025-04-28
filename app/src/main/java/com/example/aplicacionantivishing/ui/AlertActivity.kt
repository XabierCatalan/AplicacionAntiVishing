package com.example.aplicacionantivishing.ui

import android.os.Bundle
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.aplicacionantivishing.R

class AlertActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        setContentView(R.layout.activity_alert)

        val incomingNumber = intent.getStringExtra("PHONE_NUMBER") ?: "Número desconocido"
        val riskLevel = intent.getStringExtra("RISK_LEVEL") ?: "unknown"

        val layout = findViewById<LinearLayout>(R.id.alertLayout)
        val textView = findViewById<TextView>(R.id.textViewAlert)

        when (riskLevel) {
            "safe" -> {
                layout.setBackgroundColor(resources.getColor(R.color.safeGreen, null))
                textView.text = "Número seguro:\n$incomingNumber"
            }
            "suspicious" -> {
                layout.setBackgroundColor(resources.getColor(R.color.suspiciousYellow, null))
                textView.text = "Número sospechoso:\n$incomingNumber"
            }
            "dangerous" -> {
                layout.setBackgroundColor(resources.getColor(R.color.dangerRed, null))
                textView.text = "¡PELIGRO DE ESTAFA!\n$incomingNumber"
            }
            else -> {
                layout.setBackgroundColor(resources.getColor(R.color.gray, null))
                textView.text = "Número desconocido:\n$incomingNumber"
            }
        }
    }
}
