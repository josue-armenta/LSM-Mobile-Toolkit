package com.unade.lsm.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.unade.lsm.R

class MessagesAdapter(
    private val messagesList: MutableList<String> = mutableListOf(),
) :
    RecyclerView.Adapter<MessagesAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.incoming_message, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return messagesList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(messagesList[position])
    }

    @SuppressLint("NotifyDataSetChanged", "MissingPermission")
    fun addMessage(message: String) {
        messagesList.add(message)
        notifyDataSetChanged()
    }

    class ViewHolder(
        itemView: View,
    ) : RecyclerView.ViewHolder(itemView) {

        private val messageText = itemView.findViewById<TextView>(R.id.text_message_incoming)

        @SuppressLint("MissingPermission")
        fun bind(message: String) {
            messageText.text = message
        }

    }
}