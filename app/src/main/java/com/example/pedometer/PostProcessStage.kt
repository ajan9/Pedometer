package com.example.pedometer

import android.os.Environment
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException

class PostProcessStage(
    private val inputQueue: MutableList<DataPoint>?,
    private val newStepInterface: OnNewStepDetected,
    private val endOfDataInterface: OnEndOfData
) :
    Runnable {
    private var current: DataPoint? = null
    private var dp: DataPoint? = null
    private var active = false

    /*
          Section for parameter definitions
       */
    private val timeThreshold = 200

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
                } else {

                    // If the time difference exceeds the threshold, we have a confirmed step
                    if (dp!!.getTime() - current!!.getTime() > timeThreshold) {
                        current?.let { write(it, "step")
                        current = dp
                        newStepInterface.incrementSteps()
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