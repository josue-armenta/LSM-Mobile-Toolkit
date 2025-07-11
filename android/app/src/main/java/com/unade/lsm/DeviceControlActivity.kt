package com.unade.lsm

import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.unade.lsm.adapters.MessagesAdapter
import com.unade.lsm.services.BluetoothLeService
import com.unade.lsm.utils.GForceGattAttributes
import com.unade.lsm.utils.GForceHandler
import com.unade.lsm.utils.GForceHandler.CMD_SET_DATA_NOTIF_SWITCH
import com.unade.lsm.utils.GForceHandler.CMD_SET_EMG_RAWDATA_CONFIG
import com.unade.lsm.utils.GForceHandler.DataNotifFlags
import com.unade.lsm.utils.GForceHandler.ResponseResult
import com.unade.lsm.viewmodels.DeviceControlViewModel

class DeviceControlActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_DEVICE_MAC_ADDRESS = "EXTRA_DEVICE_MAC_ADDRESS"
        const val EXTRA_DEVICE_MODEL = "EXTRA_DEVICE_MODEL"
        const val TAG = "DeviceControlActivity"
    }

    private var deviceAddress: String = ""
    private var deviceModel: String = ""

    private var bluetoothService: BluetoothLeService? = null
    private var mCommandChara: BluetoothGattCharacteristic? = null
    private var mDataChara: BluetoothGattCharacteristic? = null

    private var mConnected = false

    private var messagesList: RecyclerView? = null
    private var messageInput: EditText? = null
    private var disabledSignalBtn: ImageButton? = null
    private var enabledSignalBtn: ImageButton? = null
    private var recordingBtn: ImageButton? = null
    private var sendBtn: ImageButton? = null

    private var progressDialog: ProgressDialog? = null

    private val viewModel: DeviceControlViewModel by viewModels()

    private val messagesAdapter = MessagesAdapter()

    // Code to manage Service lifecycle.
    private val serviceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            bluetoothService = (service as BluetoothLeService.LocalBinder).getService()
            bluetoothService?.let { bluetooth ->
                if (!bluetooth.initialize()) {
                    Log.e(TAG, "Unable to initialize Bluetooth")
                    finish()
                }
                bluetooth.connect(deviceAddress)
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            bluetoothService = null
        }

    }

    private val gattUpdateReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothLeService.ACTION_GATT_CONNECTED -> {
                    mConnected = true
                    updateConnectionState(R.string.connected)
                }

                BluetoothLeService.ACTION_GATT_DISCONNECTED -> {
                    mConnected = false
                    updateConnectionState(R.string.disconnected)
                }

                BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED -> {
                    handleGattServices(bluetoothService?.getSupportedGattServices())
                }

                BluetoothLeService.ACTION_WRITTEN_DESCRIPTOR -> {
                    handleWrittenDescriptor(intent.getStringExtra(BluetoothLeService.EXTRA_DATA))
                }

                BluetoothLeService.ACTION_WRITTEN_CHARACTERISTIC -> {
                    handleWrittenCharacteristic(intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA))
                }

                BluetoothLeService.ACTION_DATA_AVAILABLE -> {
                    viewModel.handleData(intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA))
                }
            }
        }
    }

    private fun startSensorNotifications() {
        bluetoothService!!.setCharacteristicNotification(mDataChara!!, true)
    }

    private fun stopSensorNotifications() {
        bluetoothService!!.setCharacteristicNotification(mDataChara!!, false)
    }

    private fun handleWrittenDescriptor(charaUuid: String?) {
        if (charaUuid != null) {
            when (charaUuid) {
                GForceGattAttributes.GFORCE_COMMAND_CHARACTERISTIC -> {

                    val cmd = GForceHandler.setDataNotifSwitch(
                        DataNotifFlags.DNF_ACCELERATE +
                                DataNotifFlags.DNF_GYROSCOPE +
                                DataNotifFlags.DNF_EULERANGLE +
                                DataNotifFlags.DNF_QUATERNION +
                                DataNotifFlags.DNF_EMG_RAW
                    )

                    bluetoothService!!.sendCommand(mCommandChara!!, cmd)
                }
            }
        }
    }

    private fun handleWrittenCharacteristic(data: ByteArray?) {
        data?.let {
            if (it.size >= 2) {
                val responseCode = it[0].toInt()
                val commandCode = it[1].toInt()
                when (responseCode) {
                    ResponseResult.RSP_CODE_SUCCESS -> {
                        when (commandCode) {
                            CMD_SET_DATA_NOTIF_SWITCH -> {
                                val cmd = GForceHandler.setEmgRawDataConfig()
                                bluetoothService!!.sendCommand(mCommandChara!!, cmd)
                            }

                            CMD_SET_EMG_RAWDATA_CONFIG -> {
                                if (mConnected) {
                                    enabledSignalBtn?.visibility = View.VISIBLE
                                    disabledSignalBtn?.visibility = View.GONE
                                }
                            }
                        }
                    }

                    else -> {
                        Log.i("RSP_CODE", "RSP_CODE: $commandCode")
                    }
                }
            }
        }
    }

    private fun handleGattServices(supportedGattServices: List<BluetoothGattService?>?) {
        supportedGattServices?.let {
            val dataService =
                it.firstOrNull { s ->
                    s!!.uuid.toString() == GForceGattAttributes.GFORCE_DATA_SERVICE
                }
            if (dataService != null) {
                val charas = dataService.characteristics
                mCommandChara =
                    charas.firstOrNull { c ->
                        c.uuid.toString() == GForceGattAttributes.GFORCE_COMMAND_CHARACTERISTIC
                    }
                mDataChara =
                    charas.firstOrNull { c ->
                        c.uuid.toString() == GForceGattAttributes.GFORCE_DATA_CHARACTERISTIC
                    }
                if (mCommandChara != null && mDataChara != null) {
                    bluetoothService!!.setCharacteristicNotification(mCommandChara!!, true)
                } else {
                    throw Exception("GATT Characteristics not found.")
                }
            } else {
                throw Exception("GATT Service not found")
            }
        }
    }

    private fun updateConnectionState(connectionState: Int) {
        Toast.makeText(this, resources.getString(connectionState), Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_control)
        deviceAddress = intent.getStringExtra(EXTRA_DEVICE_MAC_ADDRESS)!!
        deviceModel = intent.getStringExtra(EXTRA_DEVICE_MODEL)!!

        val gattServiceIntent = Intent(this, BluetoothLeService::class.java)
        bindService(gattServiceIntent, serviceConnection, Context.BIND_AUTO_CREATE)

        messagesList = findViewById(R.id.list)

        messagesList?.adapter = messagesAdapter

        messageInput = findViewById(R.id.input)
        enabledSignalBtn = findViewById(R.id.enabledBtn)
        disabledSignalBtn = findViewById(R.id.disabledBtn)
        recordingBtn = findViewById(R.id.recordingBtn)
        sendBtn = findViewById(R.id.sendBtn)

        enabledSignalBtn?.setOnClickListener {
            viewModel.setCapturing(true)
        }

        recordingBtn?.setOnClickListener {
            viewModel.setCapturing(false)
            performTranslation()
        }

        sendBtn?.setOnClickListener {
           val message = messageInput?.text.toString()
            messagesAdapter.addMessage(message)
            messageInput?.setText("")
        }

        viewModel.translation.observe(this){
            it?.let { translation ->
                val txt = messageInput?.text.toString()
                messageInput?.setText(txt+translation)
            }
        }

        viewModel.capturing.observe(this) {
            it?.let { capturing ->
                if (capturing) {
                    startSensorNotifications()
                    enabledSignalBtn?.visibility = View.GONE
                    recordingBtn?.visibility = View.VISIBLE
                } else {
                    stopSensorNotifications()
                    enabledSignalBtn?.visibility = View.VISIBLE
                    recordingBtn?.visibility = View.GONE
                }
            }
        }
    }

    private fun performTranslation() {
        progressDialog = ProgressDialog.show(this, "SLR", "Traduciendo...", true, false)
        Handler(Looper.getMainLooper()).postDelayed({
            progressDialog?.dismiss()
            viewModel.performTranslation()
        }, (500..1500).shuffled().last().toLong())
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onResume() {
        super.onResume()
        registerReceiver(gattUpdateReceiver, makeGattUpdateIntentFilter())
        if (bluetoothService != null) {
            val result = bluetoothService!!.connect(deviceAddress)
            Log.d(TAG, "Connect request result=$result")
        }
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(gattUpdateReceiver)
    }

    private fun makeGattUpdateIntentFilter(): IntentFilter {
        return IntentFilter().apply {
            addAction(BluetoothLeService.ACTION_GATT_CONNECTED)
            addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED)
            addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED)
            addAction(BluetoothLeService.ACTION_DATA_AVAILABLE)
            addAction(BluetoothLeService.ACTION_WRITTEN_DESCRIPTOR)
            addAction(BluetoothLeService.ACTION_WRITTEN_CHARACTERISTIC)
        }
    }

}