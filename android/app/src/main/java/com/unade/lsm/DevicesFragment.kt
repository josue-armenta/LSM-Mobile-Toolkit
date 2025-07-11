package com.unade.lsm

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.unade.lsm.adapters.BluetoothListAdapter
import com.unade.lsm.utils.GForceHandler.DEVICE_MODEL


@Suppress("DEPRECATION")
class DevicesFragment : Fragment() {
    companion object {
        fun newInstance() = DevicesFragment()
    }

    private val PERMISSIONS_REQUEST_CODE = 100
    private val SCAN_PERIOD: Long = 5000
    private var scanning = false

    private val leScanCallback: ScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            result?.let {
                bluetoothListAdapter.addDevice(it.device)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private val bluetoothListAdapter = BluetoothListAdapter {
        activity?.let { fa ->
            val name = it.name
            if (name.contains("gForce")) {
                val model =
                    if (name.contains("200")) DEVICE_MODEL.GFORCE_200 else DEVICE_MODEL.GFORCE_PRO
                val intent = Intent(fa, DeviceControlActivity::class.java)
                intent.putExtra(DeviceControlActivity.EXTRA_DEVICE_MAC_ADDRESS, it.address)
                intent.putExtra(DeviceControlActivity.EXTRA_DEVICE_MODEL, model.name)
                startActivity(intent)
            } else {
                Toast.makeText(fa, "Non-compatible device", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private var bluetoothLeScanner: BluetoothLeScanner? = null
    private lateinit var progressBar: ProgressBar
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_devices, container, false)
        view.findViewById<RecyclerView>(R.id.recycler_view).apply {
            adapter = bluetoothListAdapter
        }
        view.findViewById<SwipeRefreshLayout>(R.id.swipe_refresh_layout).apply {
            setOnRefreshListener {
                startScanning()
                this.isRefreshing = false
            }
        }
        progressBar = view.findViewById(R.id.progress_bar)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        activity?.let {
            val bluetoothManager =
                it.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            bluetoothLeScanner = bluetoothManager.adapter.bluetoothLeScanner

            if (checkPermissions()) {
                startScanning()
            } else
                askPermissions()
        }

    }

    @SuppressLint("MissingPermission")
    private fun startScanning() {
        bluetoothLeScanner?.let {
            val handler = Handler()
            if (!scanning) {
                bluetoothListAdapter.clear()
                handler.postDelayed({
                    scanning = false
                    progressBar.visibility = View.GONE
                    it.stopScan(leScanCallback)
                }, SCAN_PERIOD)
                scanning = true
                progressBar.visibility = View.VISIBLE
                it.startScan(leScanCallback)
            } else {
                scanning = false
                progressBar.visibility = View.GONE
                it.stopScan(leScanCallback)
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSIONS_REQUEST_CODE -> {
                if (grantResults.size == 2
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED
                    && grantResults[1] == PackageManager.PERMISSION_GRANTED
                ) {
                    startScanning()
                }
            }
        }
    }

    private fun askPermissions() {
        requestPermissions(
            arrayOf(
                android.Manifest.permission.BLUETOOTH_SCAN,
                android.Manifest.permission.BLUETOOTH_CONNECT,
            ), PERMISSIONS_REQUEST_CODE
        )
    }

    private fun checkPermissions(): Boolean {
        return activity?.let {
            return ContextCompat.checkSelfPermission(
                it,
                android.Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED
                    && ContextCompat.checkSelfPermission(
                it,
                android.Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } ?: false
    }

    @SuppressLint("MissingPermission")
    override fun onDestroy() {
        super.onDestroy()
        bluetoothLeScanner?.stopScan(leScanCallback)
    }

}