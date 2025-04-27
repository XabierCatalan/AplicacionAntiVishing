package com.example.aplicacionantivishing

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

import android.content.Intent
import com.example.aplicacionantivishing.receiver.CallReceiver
import android.telephony.TelephonyManager

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // PRUEBA MANUAL DE CallReceiver
        val fakeIntent = Intent()
        fakeIntent.putExtra(TelephonyManager.EXTRA_STATE, TelephonyManager.EXTRA_STATE_RINGING)
        fakeIntent.putExtra(TelephonyManager.EXTRA_INCOMING_NUMBER, "+34600111222")

        val callReceiver = CallReceiver()
        callReceiver.onReceive(this, fakeIntent)
    }
}

