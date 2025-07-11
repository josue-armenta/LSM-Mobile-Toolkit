package com.unade.lsm.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.unade.lsm.models.Accelerometer
import com.unade.lsm.models.Emg
import com.unade.lsm.models.EulerAngles
import com.unade.lsm.models.Gyroscope
import com.unade.lsm.models.Quaternion
import com.unade.lsm.utils.GForceHandler.NotifDataType

class DeviceControlViewModel(private val context: Application) :
    AndroidViewModel(context) {

    private val _capturing: MutableLiveData<Boolean?> = MutableLiveData(null)
    val capturing: LiveData<Boolean?> = _capturing
    private var currentlyCapturing = false

    private var counter = 0

    fun setCapturing(isCapturing: Boolean) {
        _capturing.value = isCapturing
        currentlyCapturing = isCapturing
    }

    fun handleData(data: ByteArray?) {
        if (!currentlyCapturing) return

        data?.let {
            when (data[0].toInt()) {

                NotifDataType.NTF_ACC_DATA -> {
                    Accelerometer.getFromRawBytes(data)
                        ?.let { acc -> }
                }

                NotifDataType.NTF_GYRO_DATA -> {
                    Gyroscope.getFromRawBytes(data)
                        ?.let { gyro -> }
                }

                NotifDataType.NTF_EULER_DATA -> {
                    EulerAngles.getFromRawBytes(data)
                        ?.let { eula -> }
                }

                NotifDataType.NTF_QUAT_FLOAT_DATA -> {
                    Quaternion.getFromRawBytes(data)
                        ?.let { qua -> }
                }

                NotifDataType.NTF_EMG_ADC_DATA -> {
                    Emg.getFromRawBytes(data)
                        ?.let { emg -> }
                }

                else -> throw Error("Received data wasn't expected")

            }
        }
    }


    private val _translation: MutableLiveData<String?> = MutableLiveData(null)
    val translation: LiveData<String?> = _translation

    fun performTranslation() {

    }
}