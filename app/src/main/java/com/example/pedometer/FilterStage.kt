package com.example.pedometer

import android.os.Environment
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException
import kotlin.math.pow


class FilterStage(
    private val inputQueue: MutableList<DataPoint>?,
    private val outputQueue: MutableList<DataPoint>?
) :
    Runnable {
    private val window: ArrayList<DataPoint> = ArrayList()
    private val filterCoefficients: ArrayList<Float> = generateCoefficients()
    private var filter_sum = 0.0f
    private var active = false
    private var dp: DataPoint?

    init {
        for (i in 0 until FILTER_LENGTH) {
            filter_sum += filterCoefficients[i]
        }
        dp = null
    }

    override fun run() {
        active = true
        while (active) {
            if (inputQueue?.isNotEmpty() == true) {
                dp = inputQueue.removeAt(0)
            }
            if (dp != null) {


                // Special handling for final data point.
                if (dp!!.getEos()) {
                    active = false
                    outputQueue?.add(dp!!)
                    continue
                }
                window.add(dp!!)
                if (window.size == FILTER_LENGTH) {
                    var sum = 0f
                    for (i in 0 until FILTER_LENGTH) {
                        sum += window[i].getMagnitude() * filterCoefficients[i]
                    }
                    val new_dp = DataPoint(window[FILTER_LENGTH / 2].getTime(), sum / filter_sum)
                    outputQueue?.add(new_dp)
                    write(new_dp, "filtered")
                    window.removeAt(0)
                }
                dp = null
            }
        }
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

    companion object {
        private const val FILTER_LENGTH = 13
        private const val FILTER_STD = 0.35f
        private fun generateCoefficients(): ArrayList<Float> {

            // Create a window of the correct size.
            val coeff = ArrayList<Float>()
            for (i in 0 until FILTER_LENGTH) {
                val value = Math.E.pow(
                    -0.5 * ((i - (FILTER_LENGTH - 1) / 2) / (FILTER_STD * (FILTER_LENGTH - 1) / 2)).toDouble()
                        .pow(2.0)
                ).toFloat()
                coeff.add(value)
            }
            return coeff
        }
    }
}