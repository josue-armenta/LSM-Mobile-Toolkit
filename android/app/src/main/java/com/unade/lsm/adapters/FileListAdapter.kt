package com.unade.lsm.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.unade.lsm.R
import com.unade.lsm.database.models.SamplesFile

class FileListAdapter(private val callback: (filename: String) -> Unit) :
    RecyclerView.Adapter<FileListAdapter.ViewHolder>() {

    private var dataset: List<SamplesFile> = emptyList()

    class ViewHolder(
        itemView: View,
        onItemLongCLick: (Int) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        init {
            itemView.setOnClickListener  {
                onItemLongCLick(adapterPosition)
            }
        }

        private val filenameText: TextView = itemView.findViewById(R.id.filename_text)
        private val syncIcon: ImageView = itemView.findViewById(R.id.sync_img)
        fun bind(file: SamplesFile) {
            filenameText.text = file.name
            if (file.uploaded) {
                syncIcon.visibility = View.VISIBLE
            } else {
                syncIcon.visibility = View.INVISIBLE
            }
        }

    }

    @SuppressLint("NotifyDataSetChanged")
    fun setItems(items: List<SamplesFile>) {
        this.dataset = items
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.local_file_item, parent, false)
        return ViewHolder(view) {
            callback(dataset[it].name)
        }
    }

    override fun getItemCount(): Int {
        return dataset.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(dataset[position])
    }
}