package com.example.pedometer

import android.os.Environment
import android.util.Log
import com.example.pedometer.PostProcessStage.OnEndOfData
import com.example.pedometer.PostProcessStage.OnNewStepDetected
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.util.Collections
import kotlin.math.sqrt


/**
 * Created by Jamie Brynes on 1/22/2017.
 */
class StepCounter(
    /**
     * [Setter function for the sampling frequency.]
     * @param samplingFreq [The sampling frequency to set.]
     */
    var samplingFreq: Int
) {
    /**
     * This is the interface for getting updates when we get a step occuring. Simple listener/broadcaster pattern.
     */
    interface OnStepUpdateListener {
        /**
         * This method will be called by a background thread and hence any UI changes done in the implementation of this MUST be done in the runInUiThread() way.
         */
        fun onStepUpdate(steps: Int)
    }

    interface OnFinishedProcessingListener {
        /**
         * This method will get called when the remaining data points finish processing. The owner of the object can make UI adjustments as necessary.
         */
        fun onFinishedProcessing()
    }
    /**
     * [Getter function for the sampling frequency.]
     * @return [Returns the sampling frequency.]
     */

    /**
     * [Getter function for the number steps.]
     * @return [Returns the number of steps.]
     */
    @get:Synchronized
    var steps = 0
        private set
    private var active = false
    private var rawData: MutableList<DataPoint>? = null
    private var ppData: MutableList<DataPoint>? = null
    private var smoothData: MutableList<DataPoint>? = null
    private var peakScoreData: MutableList<DataPoint>? = null
    private var peakData: MutableList<DataPoint>? = null
    private val callbacks: ArrayList<OnStepUpdateListener>
    private val newStepCallback: OnNewStepDetected
    private val eodCallback: OnEndOfData
    private var finishCallback: OnFinishedProcessingListener? = null

    /**
     * [Constructor for the StepCounter module.]
     * @param  samplingFreq [This parameter describes the sampling frequency of the sensor.]
     * @return              [Instance of Step Counter]
     */
    init {
        newStepCallback = object : OnNewStepDetected {
            override fun incrementSteps() {
                incSteps()
            }
        }
        eodCallback = object : OnEndOfData {
            override fun eodCallback() {
                if (finishCallback != null) {
                    finishCallback!!.onFinishedProcessing()
                }
            }
        }

        // Initialize callback list.
        callbacks = ArrayList()
    }

    /**
     * This function describes the set-up required for a new data recording.
     */
    private fun setUp() {
        steps = 0
        active = false

        // Initialize thread-safe lists.
        rawData = Collections.synchronizedList(ArrayList())
        ppData = Collections.synchronizedList(ArrayList())
        smoothData = Collections.synchronizedList(ArrayList())
        peakScoreData = Collections.synchronizedList(ArrayList())
        peakData = Collections.synchronizedList(ArrayList())
    }

    /**
     * [This function starts the Step Counter algorithm.]
     */
    fun start() {
        if (!active) {
            // Reset threads and stages.
            setUp()
            active = true
            Thread(PreProcessStage(rawData!!, ppData)).start()
            Thread(FilterStage(ppData, smoothData)).start()
            Thread(ScoringStage(smoothData, peakScoreData)).start()
            Thread(DetectionStage(peakScoreData,
                peakData
            )).start()
            Thread(PostProcessStage(peakData, newStepCallback, eodCallback)).start()

        }
    }

    /**
     * [This function stops the Step Counter algorithm. Current behavior is to finish processing all remaining samples before ending the threads.]
     */
    fun stop() {
        if (active) {
            //Signal that this is the end of the data stream. This is a special data point that says 'end of stream.'
            active = false
            val dp = DataPoint(0f, 0f, 0f)
            dp.setEos(true)
            rawData!!.add(dp)
        }
    }

    /**
     * [This function allows the user to add a callback for when we get a new step!]]
     * @param listener [Implementation of the OnStepUpdateListener]
     */
    @Synchronized
    fun addOnStepUpdateListener(listener: OnStepUpdateListener) {
        callbacks.add(listener)
    }

    @Synchronized
    fun setOnFinishedProcessingListener(listener: OnFinishedProcessingListener?) {
        finishCallback = listener
    }

    /**
     * [This function allows for callbacks to listeners when steps is updated.]
     */
    @Synchronized
    fun incSteps() {
        steps++
        for (listener in callbacks) {
            listener.onStepUpdate(steps)
        }
    }

    /**
     * [This function is the public interface to add a new accelerometer sample to the step counter algorithm.]
     * @param time   [The timestamp of the sample in nanoseconds.]
     * @param sample [An array of accelerometer values [x,y,z] in m/s^2.]
     */
    fun processSample(time: Long, sample: FloatArray) {
        if (active) {
            var magnitude = 0f
            for (m in sample) {
                magnitude += m * m
            }
            magnitude = sqrt(magnitude.toDouble()).toFloat()
            val new_dp = DataPoint(time, magnitude, magnitude)
            rawData!!.add(new_dp)
            write(new_dp, "raw")
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

                val time = data.getTime() / 1000000

                bw.write(data.getMagnitude().toString())
                bw.write(",")
                bw.write(time.toString())
                bw.write("\n")

                bw.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
    }
}