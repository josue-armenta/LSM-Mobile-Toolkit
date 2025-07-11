package com.unade.lsm.adapters

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.unade.lsm.R

class BluetoothListAdapter(
    private val deviceList: MutableList<BluetoothDevice> = mutableListOf(),
    private val onItemClick: (BluetoothDevice) -> Unit
) :
    RecyclerView.Adapter<BluetoothListAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_bluetooth_device, parent, false)
        return ViewHolder(view) {
            onItemClick(deviceList[it])
        }
    }

    override fun getItemCount(): Int {
        return deviceList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(deviceList[position])
    }

    @SuppressLint("NotifyDataSetChanged", "MissingPermission")
    fun addDevice(device: BluetoothDevice) {
        val existing = deviceList.firstOrNull { it.address == device.address }
        device.name?.let {
            if (existing == null) {
                deviceList.add(device)
                notifyDataSetChanged()
            }
        }
    }

    fun clear() {
        deviceList.clear()
        notifyDataSetChanged()
    }

    class ViewHolder(
        itemView: View,
        onItemClicked: (Int) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        init {
            itemView.setOnClickListener {
                onItemClicked(adapterPosition)
            }
        }

        private val deviceName = itemView.findViewById<TextView>(R.id.device_name_txt)
        private val deviceAddress = itemView.findViewById<TextView>(R.id.device_address_txt)

        @SuppressLint("MissingPermission")
        fun bind(bluetoothDevice: BluetoothDevice) {
            deviceName.text = bluetoothDevice.name
            deviceAddress.text = bluetoothDevice.address
        }

    }
}