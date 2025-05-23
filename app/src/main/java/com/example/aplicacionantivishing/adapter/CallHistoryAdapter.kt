package com.example.aplicacionantivishing.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.aplicacionantivishing.R
import java.text.SimpleDateFormat
import java.util.*

data class CallHistoryEntry(
    val phoneNumber: String,
    val riskLevel: String,
    val timestamp:  Long
)

class CallHistoryAdapter(
    private val items: List<CallHistoryEntry>
) : RecyclerView.Adapter<CallHistoryAdapter.VH>() {

    private val sdf = SimpleDateFormat("dd/MM · HH:mm", Locale.getDefault())

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvNumber: TextView = v.findViewById(R.id.tv_number)
        val tvDate  : TextView = v.findViewById(R.id.tv_date)
        val tvRisk  : TextView = v.findViewById(R.id.tv_risk)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_call, parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val e = items[position]

        /* ① si el nº está en la agenda, mostramos el nombre  */
        val display = lookupContactName(holder.itemView.context, e.phoneNumber)
            ?: e.phoneNumber
        holder.tvNumber.text = display

        holder.tvDate.text = sdf.format(Date(e.timestamp))

        holder.tvRisk.text = when (e.riskLevel) {
            "safe"        -> "Seguro"
            "suspicious"  -> "Sospechoso"
            else          -> "Peligroso"
        }
        val colorRes = when (e.riskLevel) {
            "safe"        -> R.color.safeGreen
            else          -> R.color.dangerRed
        }
        holder.tvRisk.setTextColor(
            ContextCompat.getColor(holder.itemView.context, colorRes)
        )
    }

    override fun getItemCount(): Int = items.size

    /* ---------- helper para buscar nombre ------------- */
    private fun lookupContactName(ctx: Context, number: String): String? {
        val uri = android.provider.ContactsContract.PhoneLookup.CONTENT_FILTER_URI
            .buildUpon().appendPath(number).build()

        ctx.contentResolver.query(
            uri,
            arrayOf(android.provider.ContactsContract.PhoneLookup.DISPLAY_NAME),
            null, null, null
        )?.use { c ->
            if (c.moveToFirst()) return c.getString(0)
        }
        return null
    }
}

