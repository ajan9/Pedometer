package com.example.pedometer

import android.os.Environment
import android.util.Log
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException
import kotlin.math.pow
import kotlin.math.sqrt


/**
 * Created by Jamie Brynes on 1/22/2017.
 */
class ScoringStage(
    private val inputQueue: MutableList<DataPoint>?,
    private val outputQueue: MutableList<DataPoint>?
) :
    Runnable {
    private val window: ArrayList<DataPoint> = ArrayList()
    private var active = false
    private var dp: DataPoint? = null

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
                if (window.size == WINDOW_SIZE) {
                    // Calculate score and append to the output window.
                    val score = scorePeak(window)
                    val midpoint = window.size / 2
                    val new_dp = DataPoint(window[WINDOW_SIZE / 2].getTime(), score, window[midpoint].getOldMagnitude())
                    outputQueue?.add(new_dp)
                    //write(new_dp, "scoring")
                    // Pop out the oldest point.
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

    private fun scorePeak(data: ArrayList<DataPoint>): Float {
        val midpoint = data.size / 2
        var diffLeft = 0f
        var diffRight = 0f
        for (i in 0 until midpoint) {
            diffLeft += data[midpoint].getMagnitude() - data[i].getMagnitude()
        }
        for (j in midpoint + 1 until data.size) {
            diffRight += data[midpoint].getMagnitude() - data[j].getMagnitude()
        }
        return (diffRight + diffLeft) / (WINDOW_SIZE - 1)
    }

    companion object {
        /*
        Section for parameter definitions
     */
        private const val WINDOW_SIZE = 35
    }
}