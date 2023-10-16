package com.example.pedometer

import android.os.Environment
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException
import kotlin.math.ceil


/**
 * Created by Jamie Brynes on 1/22/2017.
 */
class PreProcessStage(
    private val inputQueue: MutableList<DataPoint>,
    private val outputQueue: MutableList<DataPoint>?
) :
    Runnable {
    private val window: ArrayList<DataPoint> = ArrayList()
    private var dp: DataPoint? = null
    private val interpolationTime = 10 // In ms.
    private val timeScalingFactor = 1000000f // Convert ns to ms.
    private var interpolationCount = 0
    private var startTime = -1f
    private var active = false

    override fun run() {
        active = true
        while (active) {

            // If there is a new point, retrieve it, limit operations on inputQueue to not block other threads.
            if (inputQueue.isNotEmpty()) {
                dp = inputQueue.removeAt(0)
            }

            //Scale time and add to window.
            if (dp != null) {

                // This signals the end of the data stream.
                if (dp!!.getEos()) {
                    active = false
                    outputQueue?.add(dp!!)
                    continue
                }

                // Handling for the first data point in the stream.
                if (startTime == -1f) {
                    startTime = dp!!.getTime()
                }
                dp!!.setTime(scaleTime(dp!!.getTime()))
                window.add(dp!!)
                dp = null
            }

            // We have enough data points to interpolate.
            if (window.size >= 2) {
                val time1 = window[0].getTime()
                val time2 = window[1].getTime()

                // This defines the number of points that could exist in between the points.
                val numberOfPoints =
                    ceil(((time2 - time1) / interpolationTime).toDouble()).toInt()
                for (i in 0 until numberOfPoints) {
                    val interpTime = interpolationCount.toFloat() * interpolationTime

                    // Check if the next interpolated time is between these two points.
                    if (time1 <= interpTime && interpTime < time2) {
                        val interpolated = linearInterpolate(window[0], window[1], interpTime)
                        outputQueue?.add(interpolated)
                        write(interpolated, "interpolated")
                        interpolationCount += 1
                    }
                }

                // Remove the oldest element in the list.
                window.removeAt(0)
            }
        }
    }

    private fun scaleTime(ogTime: Float): Float {
        return (ogTime - startTime) / timeScalingFactor
    }

    private fun linearInterpolate(dp1: DataPoint, dp2: DataPoint, interpTime: Float): DataPoint {
        val dt = dp2.getTime() - dp1.getTime()
        val dv = dp2.getMagnitude() - dp1.getMagnitude()
        val mag = dv / dt * (interpTime - dp1.getTime()) + dp1.getMagnitude()
        return DataPoint(interpTime, mag, mag)
    }

    private fun write(data: DataPoint, file: String){
        try {
            val file: File = File(
                Environment.getExternalStorageDirectory()
                    .toString() + File.separator
                        + "Download" //folder name
                        + File.separator
                        + "$file.csv"
            )
            // if file doesnt exists, then create it
            if (!file.exists()) {
                file.createNewFile()
            }
            val fw = FileWriter(file.absoluteFile, true)
            val bw = BufferedWriter(fw)

            bw.write(data.getMagnitude().toString())
            bw.write(",")
            bw.write(data.getTime().toString())
            bw.write("\n")

            bw.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}