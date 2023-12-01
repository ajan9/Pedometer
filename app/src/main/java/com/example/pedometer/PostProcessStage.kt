package com.example.pedometer

import android.os.Environment
import android.util.Log
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException
import kotlin.math.abs

class PostProcessStage(
    private val inputQueue: MutableList<DataPoint>?,
    private val newStepInterface: OnNewStepDetected,
    private val endOfDataInterface: OnEndOfData
) :
    Runnable {
    private var current: DataPoint? = null
    private var last2: DataPoint? = null
    private var last1: DataPoint? = null
    private var dp: DataPoint? = null
    private var active = false

    /*
          Section for parameter definitions
       */
    private val timeThreshold = 200
    private val threshold = 10.1f
    private var similarityThreshold = -5f

    interface OnNewStepDetected {
        fun incrementSteps()
    }

    interface OnEndOfData {
        fun eodCallback()
    }

    override fun run() {
        active = true
        while (active) {
            if (inputQueue?.isNotEmpty() == true) {
                dp = inputQueue.removeAt(0)
            }
            if (dp != null) {
                if (dp!!.getEos()) {
                    active = false
                    endOfDataInterface.eodCallback()
                    continue
                }

                // First point handler.
                if (current == null) {
                    current = dp
                    last2 = dp
                    last1 = dp
                } else {
                    // If the time difference exceeds the threshold, we have a confirmed step
                    if (dp!!.getTime() - current!!.getTime() > timeThreshold) {
                        last2 = last1
                        last1 = current
                        val similarity = calculateSimilarity(dp!!, last2!!) // Izračunajte sličnost između trenutnog dp i prethodnog current
                        if (similarity >= similarityThreshold) {
                            current?.let { write(it, "step")}
                            newStepInterface.incrementSteps()
                            current = dp
                        }
                    } else {
                        // Keep the point with the largest magnitude.
                        if (dp!!.getMagnitude() > current!!.getMagnitude()) {
                            current = dp
                        }
                    }
                }
                dp = null
            }
        }
    }

    private fun calculateSimilarity(dp: DataPoint, last2: DataPoint): Float {
        if (dp.getOldMagnitude() < threshold && dp.getOldMagnitude() > 9.1f) {
            return Float.NEGATIVE_INFINITY
        }
        return -abs(last2.getOldMagnitude() - dp.getOldMagnitude())
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

            bw.write(data.getOldMagnitude().toString())
            bw.write(",")
            bw.write(data.getTime().toString())
            bw.write("\n")

            bw.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}