package com.unade.lsm.services

import android.annotation.SuppressLint
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothProfile
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import com.unade.lsm.utils.GForceGattAttributes
import java.util.UUID

class BluetoothLeService : Service() {

    private val TAG = "BluetoothLeService"
    private val binder = LocalBinder()
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothGatt: BluetoothGatt? = null

    private var connectionState = STATE_DISCONNECTED

    override fun onBind(intent: Intent?): IBinder? {
        return binder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        close()
        return super.onUnbind(intent)
    }

    fun initialize(): Boolean {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter == null) {
            return false
        }
        return true
    }

    @SuppressLint("MissingPermission")
    private fun close() {
        bluetoothGatt?.let { gatt ->
            gatt.close()
            bluetoothGatt = null
        }
    }

    @SuppressLint("MissingPermission")
    fun connect(address: String): Boolean {
        bluetoothAdapter?.let { adapter ->
            try {
                val device = adapter.getRemoteDevice(address)
                bluetoothGatt = device.connectGatt(this, false, bluetoothGattCallback)
                Log.i(TAG, "Connected to device w/ address $address")
                return true
            } catch (exception: IllegalArgumentException) {
                Log.w(TAG, "Device not found with provided address.")
                return false
            }
        } ?: run {
            Log.w(TAG, "BluetoothAdapter not initialized")
            return false
        }
    }

    fun getSupportedGattServices(): List<BluetoothGattService?>? {
        return bluetoothGatt?.services
    }

    @SuppressLint("MissingPermission")
    fun readCharacteristic(characteristic: BluetoothGattCharacteristic) {
        if (bluetoothGatt != null) {
            bluetoothGatt!!.readCharacteristic(characteristic)
        } else {
            Log.w(TAG, "BluetoothGatt not initialized")
        }
    }

    @SuppressLint("MissingPermission")
    fun setCharacteristicNotification(
        characteristic: BluetoothGattCharacteristic,
        enabled: Boolean
    ): Boolean {
        return if (bluetoothGatt != null) {
            bluetoothGatt!!.setCharacteristicNotification(characteristic, enabled)

            val descriptor = characteristic.getDescriptor(
                UUID.fromString(GForceGattAttributes.CLIENT_CHARACTERISTIC_CONFIG)
            )
            descriptor.value =
                if (enabled) BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                else BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE

            bluetoothGatt!!.writeDescriptor(descriptor)
        } else {
            Log.w(TAG, "BluetoothGatt not initialized")
            false
        }
    }

    @SuppressLint("MissingPermission")
    fun writeCharacteristic(
        characteristic: BluetoothGattCharacteristic,
        withoutResponse: Boolean,
        data: ByteArray
    ): Boolean {
        return if (bluetoothGatt != null) {
            characteristic.value = data
            if (withoutResponse)
                characteristic.writeType = 1
            else
                characteristic.writeType = 2
            bluetoothGatt!!.writeCharacteristic(characteristic)
        } else {
            Log.w(TAG, "BluetoothGatt not initialized")
            false
        }
    }

    inner class LocalBinder : Binder() {
        fun getService(): BluetoothLeService {
            return this@BluetoothLeService
        }
    }

    private fun broadcastUpdate(action: String) {
        val intent = Intent(action)
        sendBroadcast(intent)
    }

    private fun broadcastUpdate(
        action: String,
        characteristic: BluetoothGattCharacteristic?,
    ) {
        val intent = Intent(action)
        val data: ByteArray? = characteristic?.value
        intent.putExtra(EXTRA_DATA, data)
        sendBroadcast(intent)
    }

    private fun broadcastUpdate(
        action: String,
        descriptor: BluetoothGattDescriptor?
    ) {
        val intent = Intent(action)
        val data: String = descriptor?.characteristic?.uuid.toString()
        intent.putExtra(EXTRA_DATA, data)
        sendBroadcast(intent)
    }

    fun sendCommand(
        commandCharacteristic: BluetoothGattCharacteristic,
        data: ByteArray,
    ): Boolean {
        return writeCharacteristic(commandCharacteristic, false, data)
    }

    private val bluetoothGattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                connectionState = STATE_CONNECTED
                broadcastUpdate(ACTION_GATT_CONNECTED)
                // Attempts to discover services after successful connection.
                bluetoothGatt?.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                connectionState = STATE_DISCONNECTED
                broadcastUpdate(ACTION_GATT_DISCONNECTED)
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED)
            } else {
                Log.w(TAG, "onServicesDiscovered received: $status")
            }
        }

        @Deprecated("Deprecated in Java")
        override fun onCharacteristicRead(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic)
            }
        }

        @Deprecated("Deprecated in Java")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?
        ) {
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic)
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt?,
            descriptor: BluetoothGattDescriptor?,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_WRITTEN_DESCRIPTOR, descriptor)
            }
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_WRITTEN_CHARACTERISTIC, characteristic)
            }
        }

    }

    companion object {
        const val ACTION_GATT_CONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_CONNECTED"
        const val ACTION_GATT_DISCONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED"
        const val ACTION_GATT_SERVICES_DISCOVERED =
            "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED"
        const val ACTION_DATA_AVAILABLE =
            "com.example.bluetooth.le.ACTION_DATA_AVAILABLE"
        const val ACTION_WRITTEN_DESCRIPTOR =
            "com.example.bluetooth.le.ACTION_DATA_AVAILABLE.ACTION_WRITTEN_DESCRIPTOR"
        const val ACTION_WRITTEN_CHARACTERISTIC =
            "com.example.bluetooth.le.ACTION_DATA_AVAILABLE.ACTION_WRITTEN_CHARACTERISTIC"
        const val EXTRA_DATA = "com.example.bluetooth.le.EXTRA_DATA"

        private const val STATE_DISCONNECTED = 0
        private const val STATE_CONNECTED = 2
    }

}