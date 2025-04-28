package com.example.aplicacionantivishing.ui

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.example.aplicacionantivishing.R
import com.example.aplicacionantivishing.manager.SettingsManager

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportFragmentManager
            .beginTransaction()
            .replace(android.R.id.content, SettingsFragment())
            .commit()
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preferences, rootKey)

            // Gestionar botón de "Restaurar valores por defecto"
            val resetButton = findPreference<Preference>("reset_weights")
            resetButton?.setOnPreferenceClickListener {
                SettingsManager.resetDefaultWeights(requireContext())
                Toast.makeText(requireContext(), "Pesos restaurados a valores por defecto", Toast.LENGTH_SHORT).show()
                true
            }
        }
    }
}
