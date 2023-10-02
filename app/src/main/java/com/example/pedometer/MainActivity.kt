package com.example.pedometer

import android.content.Context
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.PermissionChecker
import com.jjoe64.graphview.GraphView
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries
import org.apache.commons.math3.stat.descriptive.SummaryStatistics
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException
import kotlin.math.abs
import kotlin.math.sqrt


class MainActivity : AppCompatActivity() {
    private var mSensorManager: SensorManager? = null
    private var mAccelerometer: Sensor? = null
    private var mStepCounter: Sensor? = null

    private lateinit var rawGraph: GraphView
    private lateinit var peakGraph: GraphView

    private lateinit var rawXSeries: LineGraphSeries<DataPoint>
    private lateinit var rawYSeries: LineGraphSeries<DataPoint>
    private lateinit var rawZSeries: LineGraphSeries<DataPoint>
    private lateinit var rawSeries: LineGraphSeries<DataPoint>
    private val smoothXSeries: LineGraphSeries<DataPoint>? = null
    private val smoothYSeries: LineGraphSeries<DataPoint>? = null
    private val smoothZSeries: LineGraphSeries<DataPoint>? = null
    private val smoothSeries: LineGraphSeries<DataPoint>? = null
    private lateinit var peakSeries: LineGraphSeries<DataPoint>

    private var txtStepCount: TextView? = null
    private var txtCalculatedStepCount: TextView? = null

    private var rawAccelValues = FloatArray(3)

    private val gravity = floatArrayOf(0f, 0f, 0f)

    private var readingCount = 0
    private var peakCount = 0
    private var stepCount = 0
    private var calculatedStepCount = 0
    private var initialCounterValue = 0
    private val LAG_SIZE = 6
    private val DATA_SAMPLING_SIZE = 20
    private val zscoreCalculationValues = ArrayList<Double>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if(PermissionChecker.checkSelfPermission(this,
                android.Manifest.permission.ACTIVITY_RECOGNITION) == PermissionChecker.PERMISSION_DENIED){
            //ask for permission
            requestPermissions(arrayOf(android.Manifest.permission.ACTIVITY_RECOGNITION), 0);
        }

        /*if(PermissionChecker.checkSelfPermission(this,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PermissionChecker.PERMISSION_DENIED){
            //ask for permission
            requestPermissions(arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE), 0);
        }*/

        val context: Context = this
        txtStepCount = findViewById(R.id.txtStepCount)
        txtCalculatedStepCount = findViewById(R.id.txtCalculatedStep)

        rawGraph = findViewById(R.id.graphRaw)
        peakGraph = findViewById(R.id.graphPeak)

        rawXSeries = LineGraphSeries()
        rawXSeries.color = Color.RED
        initializeGraphControl(rawGraph, rawXSeries, 0.0, 20.0, 80.0)
        rawYSeries = LineGraphSeries()
        rawYSeries.color = Color.GREEN
        initializeGraphControl(rawGraph, rawYSeries, 0.0, 20.0, 80.0)
        rawZSeries = LineGraphSeries()
        rawZSeries.color = Color.YELLOW
        initializeGraphControl(rawGraph, rawZSeries, 0.0, 20.0, 80.0)
        rawSeries = LineGraphSeries()
        rawSeries.color = Color.MAGENTA
        initializeGraphControl(rawGraph, rawSeries, 0.0, 5.0, 80.0)

        peakSeries = LineGraphSeries()
        initializeGraphControl(peakGraph, peakSeries, -1.0, 1.0, 80.0)

        mSensorManager = context.getSystemService(SENSOR_SERVICE) as SensorManager?

        mAccelerometer = mSensorManager!!.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        mStepCounter = mSensorManager!!.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

        mSensorManager!!.registerListener(mSensorEventListener, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL)
        mSensorManager!!.registerListener(mSensorEventListener, mStepCounter, SensorManager.SENSOR_DELAY_FASTEST)
    }


    private val mSensorEventListener: SensorEventListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            when (event.sensor.type) {
                Sensor.TYPE_ACCELEROMETER -> inferStepFromAccelerometerData(event)
                //Sensor.TYPE_STEP_COUNTER -> incrementStepCount(event)
            }
        }

        override fun onAccuracyChanged(sensor: Sensor, i: Int) {}
    }


    private fun initializeGraphControl(
        graph: GraphView,
        series: LineGraphSeries<DataPoint>,
        minY: Double,
        maxY: Double,
        maxX: Double
    ) {
        graph.viewport.isYAxisBoundsManual = true
        graph.viewport.setMinY(minY)
        graph.viewport.setMaxY(maxY)
        graph.viewport.isXAxisBoundsManual = true
        graph.viewport.setMinX(0.0)
        graph.viewport.setMaxX(maxX)
        graph.viewport.isScalable = true
        graph.viewport.setScalableY(true)
        graph.addSeries(series)
        series.appendData(DataPoint(0.0, 0.0), true, 20000)
    }

    fun inferStepFromAccelerometerData(event: SensorEvent) {
        try {
            readingCount += 1
            rawAccelValues[0] = event.values[0]
            rawAccelValues[1] = event.values[1]
            rawAccelValues[2] = event.values[2]
            rawAccelValues = isolateGravity(rawAccelValues)
            val rawMagnitude = sqrt(
                (
                        rawAccelValues[0] * rawAccelValues[0] + rawAccelValues[1] * rawAccelValues[1] + rawAccelValues[2] * rawAccelValues[2]).toDouble()
            )
            this.plotGraph(rawMagnitude, readingCount, rawSeries)
            //this.plotGraph(rawAccelValues[0].toDouble(), readingCount, rawXSeries)
            //this.plotGraph(rawAccelValues[1].toDouble(), readingCount, rawYSeries)
            //this.plotGraph(rawAccelValues[2].toDouble(), readingCount, rawZSeries)

            /*try {
                val file: File = File(
                    Environment.getExternalStorageDirectory()
                        .toString() + File.separator
                            + "Download" //folder name
                            + File.separator
                            + "accdata.csv"
                )
                // if file doesnt exists, then create it
                if (!file.exists()) {
                    file.createNewFile()
                }
                val fw = FileWriter(file.absoluteFile)
                val bw = BufferedWriter(fw)
                bw.write(";")
                bw.write("\n")
                bw.write(rawMagnitude.toString())
                bw.close()
                Log.d("path", fw.toString())
            } catch (e: IOException) {
                e.printStackTrace()
                Log.d("path2", "aaa")
            }*/



            if (zscoreCalculationValues.size < DATA_SAMPLING_SIZE) {
                zscoreCalculationValues.add(rawMagnitude)
            } else if (zscoreCalculationValues.size == DATA_SAMPLING_SIZE) {
                calculatedStepCount += detectPeak(
                    zscoreCalculationValues, LAG_SIZE, 0.30, 0.2
                )
                SharedState.instance?.steps ?: calculatedStepCount
                txtCalculatedStepCount?.text = calculatedStepCount.toString()
                zscoreCalculationValues.clear()
                zscoreCalculationValues.add(rawMagnitude)
            }
        } catch (ex: Exception) {
            Log.e("Ex", ex.message!!)
        }
    }

    /*@Throws(IOException::class)
    fun File.writeAsCSV(values: List<List<String>>) {
        val csv = values.joinToString("\n") { line -> line.joinToString(", ") }
        writeText(csv)
    }*/

    private fun isolateGravity(sensorValues: FloatArray): FloatArray {
        val alpha = 0.8f
        val acceleration = floatArrayOf(0f, 0f, 0f)
        gravity[0] = alpha * gravity[0] + (1 - alpha) * sensorValues[0]
        gravity[1] = alpha * gravity[1] + (1 - alpha) * sensorValues[1]
        gravity[2] = alpha * gravity[2] + (1 - alpha) * sensorValues[2]

        // Remove the gravity contribution with the high-pass filter.
        acceleration[0] = sensorValues[0] - gravity[0]
        acceleration[1] = sensorValues[1] - gravity[1]
        acceleration[2] = sensorValues[2] - gravity[2]

        return acceleration
    }

    private fun detectPeak(inputs: List<Double>, lag: Int, threshold: Double, influence: Double): Int {
        var peaksDetected = 0
        //init stats instance
        val stats = SummaryStatistics()

        //the results (peaks, 1 or -1) of algorithm
        val signals = MutableList<Int>(inputs.size) { 0 }
        //filter out the signals (peaks) from original list (using influence arg)
        val filteredY = ArrayList<Double>(inputs)
        //the current average of the rolling window
        val avgFilter = MutableList<Double>(inputs.size) { 0.0 }
        //the current standard deviation of the rolling window
        val stdFilter = MutableList<Double>(inputs.size) { 0.0 }

        //init avgFilter and stdFilter
        for (i in 0 until lag) {
            stats.addValue(inputs[i])
            filteredY.add(inputs[i])
        }
        avgFilter[lag - 1] = stats.mean
        stdFilter[lag - 1] = stats.standardDeviation
        stats.clear()
        peakCount += LAG_SIZE
        for (i in lag until inputs.size) {
            this.plotGraph(abs(inputs[i] - avgFilter[i - 1]), readingCount, rawXSeries)
            this.plotGraph(threshold * stdFilter[i - 1], readingCount, rawYSeries)
            peakCount += 1
            if (abs(inputs[i] - avgFilter[i - 1]) > threshold * stdFilter[i - 1]) {
                //this is a signal (i.e. peak), determine if it is a positive or negative signal
                if (inputs[i] > avgFilter[i - 1]) {
                    signals[i] = 1
                    if (inputs[i] > 1.7 && inputs[i] < 3.5) {
                        peaksDetected += 1
                        peakSeries.appendData(DataPoint(peakCount.toDouble(), 1.0), true, 20000)
                    } else {
                        peakSeries.appendData(DataPoint(peakCount.toDouble(), 0.0), true, 20000)
                    }
                } else {
                    signals[i] = -1
                    peakSeries.appendData(DataPoint(peakCount.toDouble(), -1.0), true, 20000)
                }
                //filter this signal out using influence
                filteredY[i] = influence * inputs[i] + (1 - influence) * filteredY[i - 1]
            } else {
                //ensure this signal remains a zero
                signals[i] = 0
                peakSeries.appendData(DataPoint(peakCount.toDouble(), 0.0), true, 20000)
                //ensure this value is not filtered
                filteredY[i] = inputs[i]
            }
            //update rolling average and deviation
            for (j in i - lag until i) {
                stats.addValue(filteredY[j])
            }
            avgFilter[i] = stats.mean
            stdFilter[i] = stats.standardDeviation

        }
        return peaksDetected
    }

    private fun plotGraph(reading: Double, readingCount: Int, series: LineGraphSeries<DataPoint>?) {
        series?.appendData(DataPoint(readingCount.toDouble(), reading), true, 20000)
    }

    /*fun incrementStepCount(event: SensorEvent) {
        if (initialCounterValue == 0) {
            initialCounterValue = event.values[0].toInt()
        }
        if (event.values[0] > 0) {
            stepCount = event.values[0].toInt() - initialCounterValue
            txtStepCount!!.text = stepCount.toString()
        }
    }*/

}