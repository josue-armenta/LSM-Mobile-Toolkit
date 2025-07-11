package com.unade.lsm.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import org.apache.commons.math3.analysis.interpolation.SplineInterpolator
import java.math.BigDecimal
import java.math.RoundingMode

class SampleRecollect(
    private val batchId: String,
    private val signSentence: Int,
    private val repository: Repository,
    private val accSamples: MutableMap<Long, DoubleArray> = mutableMapOf(),
    private val gyroSamples: MutableMap<Long, DoubleArray> = mutableMapOf(),
    private val eulaSamples: MutableMap<Long, DoubleArray> = mutableMapOf(),
    private val quaSamples: MutableMap<Long, DoubleArray> = mutableMapOf(),
    private val emgSamples: MutableMap<Long, DoubleArray> = mutableMapOf(),
) {

    companion object {
        const val M_SIZE = 1024
    }

    private enum class SensorType {
        ACCELEROMETER,
        GYROSCOPE,
        EULA,
        QUATERNION,
        EMG
    }

    fun insertAccelerometerSample(sample: DoubleArray) {
        accSamples[System.currentTimeMillis()] = sample
    }


    fun insertGyroSample(sample: DoubleArray) {
        gyroSamples[System.currentTimeMillis()] = sample
    }


    fun insertEulerAnglesSample(sample: DoubleArray) {
        eulaSamples[System.currentTimeMillis()] = sample
    }


    fun insertQuaternionSample(sample: DoubleArray) {
        quaSamples[System.currentTimeMillis()] = sample
    }


    fun insertEmgSample(sample: DoubleArray) {
        emgSamples[System.currentTimeMillis()] = sample
    }

    private fun getStartEndTimestamps(): Pair<Long, Long> {
        val startTimestamps = listOf(
            accSamples.keys.first(), gyroSamples.keys.first(),
            eulaSamples.keys.first(), quaSamples.keys.first(), emgSamples.keys.first()
        )
        val endTimestamps = listOf(
            accSamples.keys.last(), gyroSamples.keys.last(),
            eulaSamples.keys.last(), quaSamples.keys.last(), emgSamples.keys.last()
        )

        return Pair(startTimestamps.max(), endTimestamps.min())
    }

    private fun getTimeSteps(interval: Pair<Long, Long>, numberSteps: Int = M_SIZE): List<Double> {

        val interval_time = interval.second - interval.first
        val period_duration = BigDecimal(interval_time).divide(BigDecimal(numberSteps))
        period_duration.setScale(2, RoundingMode.FLOOR)
        val step = period_duration.toDouble()

        val timestamps: MutableList<Double> = mutableListOf()

        for (i in 0..<M_SIZE) {
            timestamps.add(interval.first + i * step)
        }

        return timestamps
    }

    private suspend fun resampleSensorLectures(
        sensorType: SensorType,
        samples: MutableMap<Long, DoubleArray>,
        timeSteps: List<Double>
    ): List<List<Double>> {
        val numCols = getNumberCols(sensorType)

        val keys: MutableList<Double> = mutableListOf()
        val ts: MutableList<MutableList<Double>> = mutableListOf()

        for (i in 0..<numCols)
            ts.add(mutableListOf())

        samples.keys.forEach { key ->
            samples[key]?.let { data ->
                keys.add(key.toDouble())
                for (i in 0..<numCols) {
                    ts[i].add(data[i])
                }
            }
        }

        val x = keys.toDoubleArray()

        return withContext(Dispatchers.Default) {
            ts.map { y ->
                async {
                    val pif = SplineInterpolator().interpolate(x, y.toDoubleArray())
                    timeSteps.map { pif.value(it) }
                }
            }.awaitAll()
        }
    }

    private fun getHeader(): String {
        val acc = "acc_x,acc_y,acc_z"
        val gyro = "gyro_x,gyro_y,gyro_z"
        val eula = "eula_pitch,eula_roll,eula_yaw"
        val qua = "qua_w,qua_x,qua_y,qua_z"
        val emg = "ch0,ch1,ch2,ch3,ch4,ch5,ch6,ch7"
        return "sample_index,${acc},${gyro},${eula},${qua},${emg}\n"
    }

    private fun getNumberCols(sensorType: SensorType): Int {
        return when (sensorType) {
            SensorType.ACCELEROMETER -> 3
            SensorType.GYROSCOPE -> 3
            SensorType.EULA -> 3
            SensorType.QUATERNION -> 4
            SensorType.EMG -> 8
        }
    }

    private fun saveCsvFile(
        context: Context,
        samples: List<List<Double>>
    ): String {
        val filename = "${signSentence}_${batchId}.csv"
        context.openFileOutput(filename, Context.MODE_PRIVATE).use {
            it.write(getHeader().toByteArray())
            for (i in 0..<M_SIZE) {
                val strBuilder = StringBuilder()
                strBuilder.append("$i,")
                for (j in samples.indices) {
                    strBuilder.append("${samples[j][i]},")
                }
                it.write("${strBuilder.substring(0, strBuilder.lastIndex)}\n".toByteArray())
            }
        }
        return filename
    }

    private suspend fun resample(timeSteps: List<Double>): List<List<Double>> {
        val output = withContext(Dispatchers.Default) {
            listOf(
                async { resampleSensorLectures(SensorType.ACCELEROMETER, accSamples, timeSteps) },
                async { resampleSensorLectures(SensorType.GYROSCOPE, gyroSamples, timeSteps) },
                async { resampleSensorLectures(SensorType.EULA, eulaSamples, timeSteps) },
                async { resampleSensorLectures(SensorType.QUATERNION, quaSamples, timeSteps) },
                async { resampleSensorLectures(SensorType.EMG, emgSamples, timeSteps) }
            ).awaitAll()
        }
        return output.flatten()
    }

    suspend fun saveSamples(context: Context) {
        val resampledSensors = resample(getTimeSteps(getStartEndTimestamps()))
        withContext(Dispatchers.IO) {
            val filename = saveCsvFile(context, resampledSensors)
            repository.insertSamplesFile(filename)
        }
    }
}