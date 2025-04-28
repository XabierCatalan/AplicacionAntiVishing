package com.example.aplicacionantivishing.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.aplicacionantivishing.R

data class CallHistoryEntry(
    val phoneNumber: String,
    val riskLevel: String
)

class CallHistoryAdapter(
    private var callHistoryList: MutableList<CallHistoryEntry> // 🔥 Ahora es var y mutable
) : RecyclerView.Adapter<CallHistoryAdapter.CallHistoryViewHolder>() {

    class CallHistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val phoneNumberTextView: TextView = itemView.findViewById(R.id.phoneNumberTextView)
        val riskLevelTextView: TextView = itemView.findViewById(R.id.riskLevelTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CallHistoryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_call, parent, false)
        return CallHistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CallHistoryViewHolder, position: Int) {
        val entry = callHistoryList[position]
        holder.phoneNumberTextView.text = entry.phoneNumber

        when (entry.riskLevel) {
            "safe" -> {
                holder.riskLevelTextView.text = "Seguro"
                holder.riskLevelTextView.setTextColor(holder.itemView.context.getColor(R.color.safeGreen))
            }
            "suspicious" -> {
                holder.riskLevelTextView.text = "Sospechoso"
                holder.riskLevelTextView.setTextColor(holder.itemView.context.getColor(R.color.suspiciousYellow))
            }
            "dangerous" -> {
                holder.riskLevelTextView.text = "Peligroso"
                holder.riskLevelTextView.setTextColor(holder.itemView.context.getColor(R.color.dangerRed))
            }
            else -> {
                holder.riskLevelTextView.text = "Desconocido"
                holder.riskLevelTextView.setTextColor(holder.itemView.context.getColor(R.color.gray))
            }
        }
    }

    override fun getItemCount(): Int = callHistoryList.size

    // 🔥 Método para actualizar el historial
    fun updateList(newList: List<CallHistoryEntry>) {
        callHistoryList = newList.toMutableList()
        notifyDataSetChanged()
    }
}
