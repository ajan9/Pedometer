package com.example.pedometer

import android.os.Environment
import android.util.Log
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException
import kotlin.math.pow
import kotlin.math.sqrt

class DetectionStage(
    private val inputQueue: MutableList<DataPoint>?,
    private val outputQueue: MutableList<DataPoint>?
) :
    Runnable {
    private var dp: DataPoint? = null
    private var active = false
    private var count = 0
    private var mean = 0f
    private var std = 0f

    /*
          Section for defining parameters.
       */
    private val threshold = 1.2f
    override fun run() {
        active = true
        while (active) {
            if (inputQueue?.isNotEmpty() == true) {
                dp = inputQueue.removeAt(0)
            }
            if (dp != null) {

                // Special handing for end of stream.
                if (dp!!.getEos()) {
                    active = false
                    outputQueue?.add(dp!!)
                    continue
                }

                // Update calculations of std and mean.
                count++
                val o_mean = mean
                when (count) {
                    1 -> {
                        mean = dp!!.getMagnitude()
                        std = 0f
                    }

                    2 -> {
                        mean = (mean + dp!!.getMagnitude()) / 2
                        std = sqrt(
                            (dp!!.getMagnitude() - mean).toDouble().pow(2.0) + (o_mean - mean).toDouble()
                                .pow(2.0)
                        ).toFloat() / 2
                    }

                    else -> {
                        mean = (dp!!.getMagnitude() + (count - 1) * mean) / count
                        std = sqrt(
                            (count - 2) * std.toDouble().pow(2.0) / (count - 1) + (o_mean - mean).toDouble()
                                .pow(2.0) + (dp!!.getMagnitude() - mean).toDouble().pow(2.0) / count
                        ).toFloat()
                    }
                }

                // Once we have enough data points to have a reasonable mean/standard deviation, start detecting
                if (count > 15) {
                    if (dp!!.getMagnitude() - mean > std * threshold) {
                        // This is a peak
                        val new_dp = DataPoint(dp!!.getTime(), dp!!.getMagnitude(), dp!!.getOldMagnitude())
                        outputQueue?.add(new_dp)
                        write(new_dp, "detection")
                    }
                }
                dp = null
            }
        }
    }

        fun write(data: DataPoint, file: String) {
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