package com.example.aplicacionantivishing.ui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

import android.content.Intent
import com.example.aplicacionantivishing.receiver.CallReceiver
import android.telephony.TelephonyManager

import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import com.example.aplicacionantivishing.R


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // ✅ Pedir permisos en tiempo de ejecución
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.READ_PHONE_STATE), 1)
        }

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 2)
        }

        // ✅ Simular una llamada entrante para pruebas
        val fakeIntent = Intent()
        fakeIntent.putExtra(TelephonyManager.EXTRA_STATE, TelephonyManager.EXTRA_STATE_RINGING)
        fakeIntent.putExtra(TelephonyManager.EXTRA_INCOMING_NUMBER, "+34600111222")

        val callReceiver = CallReceiver()
        callReceiver.onReceive(this, fakeIntent)
    }
}

