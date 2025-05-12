package com.example.aplicacionantivishing.ui       // ajusta si tu paquete es distinto

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.*
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

    /** Fragmento de ajustes */
    class SettingsFragment : PreferenceFragmentCompat() {

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preferences, rootKey)

            buildPrefixLists()
            bindResetDefaultsButton()
        }

        /* ─────────────── LISTAS BLACK/WHITE DINÁMICAS ─────────────── */
        private fun buildPrefixLists() {
            val ctx  = requireContext()
            val cat  = findPreference<PreferenceCategory>("prefix_lists") ?: return

            /* 1) Mapa prefijo → países =================================================== */
            val map = mutableMapOf<String, MutableList<String>>()
            ctx.resources.openRawResource(R.raw.paises).bufferedReader().useLines { lines ->
                lines.drop(1).forEach { line ->
                    val parts = line.split(';')
                    if (parts.size >= 4) {
                        val country = parts[0].trim()
                        val prefix  = "+" + parts[3].trim()
                        map.getOrPut(prefix) { mutableListOf() }.add(country)
                    }
                }
            }
            val sorted = map.toSortedMap()          // +1, +20, +34…

            /* 2) Helper para MultiSelectListPreference ================== */
            fun makeMultiSelect(key: String, title: String, baseSummary: String)
                    : MultiSelectListPreference {

                val p = MultiSelectListPreference(ctx).apply {
                    this.key         = key
                    this.title       = title
                    entries          = sorted.map { "${it.key}  ·  ${it.value.joinToString(" / ")}" }
                        .toTypedArray()
                    entryValues      = sorted.keys.toTypedArray()
                    isPersistent     = true
                    // contador de seleccionados
                    setOnPreferenceChangeListener { pref, newVal ->
                        val sel = (newVal as? Set<*>)?.size ?: 0
                        pref.summary = "$baseSummary  •  seleccionados: $sel"
                        true
                    }
                }

                // resumen inicial
                val selNow = preferenceManager.sharedPreferences
                    ?.getStringSet(key, emptySet())
                    ?.size ?: 0
                p.summary = "$baseSummary  •  seleccionados: $selNow"
                return p
            }

            /* 3) Añadir a la categoría ================================= */
            cat.addPreference(
                makeMultiSelect(
                    key       = "blacklist_prefixes",
                    title     = "Prefijos en Blacklist",
                    baseSummary = "Prefijos bloqueados"
                )
            )
            cat.addPreference(
                makeMultiSelect(
                    key       = "whitelist_prefixes",
                    title     = "Prefijos en Whitelist",
                    baseSummary = "Prefijos siempre permitidos"
                )
            )
        }

        /* ─────────────── BOTÓN RESET DEFAULTS ─────────────── */
        private fun bindResetDefaultsButton() {
            findPreference<Preference>("reset_values")?.setOnPreferenceClickListener {
                SettingsManager.resetDefaultWeights(requireContext())
                Toast.makeText(requireContext(),
                    "Valores restablecidos correctamente", Toast.LENGTH_SHORT).show()
                true
            }
        }



    }
}
