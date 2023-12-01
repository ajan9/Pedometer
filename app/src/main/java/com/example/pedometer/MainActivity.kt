package com.example.pedometer

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.PermissionChecker
import com.example.pedometer.StepCounter.OnFinishedProcessingListener
import com.example.pedometer.StepCounter.OnStepUpdateListener



class MainActivity : AppCompatActivity() {
    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null
    private var stepCounter: StepCounter? = null
    // sampling frequency in Hz
    private val SAMPLING_FREQUENCY = 100
    private var currentSteps = 0
    private var lastSteps = -1
    private var isEnabled = false
    var tv_stepCount: TextView? = null
    var btn_toggleStepCounter: Button? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if(PermissionChecker.checkSelfPermission(this,
                android.Manifest.permission.ACTIVITY_RECOGNITION) == PermissionChecker.PERMISSION_DENIED){
            //ask for permission
            requestPermissions(arrayOf(android.Manifest.permission.ACTIVITY_RECOGNITION), 0);
        }

        if(PermissionChecker.checkSelfPermission(this,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PermissionChecker.PERMISSION_DENIED){
            //ask for permission
            requestPermissions(arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE), 0);
        }

        tv_stepCount = findViewById(R.id.textView2);
        btn_toggleStepCounter = findViewById(R.id.button)
        btn_toggleStepCounter?.setOnClickListener(startClickListener)

        stepCounter = StepCounter(SAMPLING_FREQUENCY)
        stepCounter!!.addOnStepUpdateListener(object : OnStepUpdateListener {
            override fun onStepUpdate(steps: Int) {
                runOnUiThread {
                    currentSteps = steps
                    val text = "Steps: $currentSteps"
                    tv_stepCount?.text = text
                }
            }
        })
        stepCounter!!.setOnFinishedProcessingListener(object : OnFinishedProcessingListener {
            override fun onFinishedProcessing() {
                runOnUiThread { btn_toggleStepCounter?.isEnabled = true }
            }
        })

        sensorManager = this.getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager!!.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    }

    private val startClickListener = View.OnClickListener {
        if (isEnabled) {
            // Stop sampling
            sensorManager?.unregisterListener(accelerometerEventListener)

            // Stop algorithm.
            isEnabled = false
            btn_toggleStepCounter?.isEnabled = false
            btn_toggleStepCounter?.text = "Start Step Counting"
            stepCounter!!.stop()
        } else {
            // Start algorithm.
            tv_stepCount?.text = "Steps: 0"
            isEnabled = true
            currentSteps = 0
            lastSteps = -1
            stepCounter!!.start()
            btn_toggleStepCounter?.text = "Stop Step Counting"


            // Start sampling
            val periodusecs = (1E6 / SAMPLING_FREQUENCY).toInt()
            sensorManager?.registerListener(accelerometerEventListener, accelerometer, periodusecs)
        }
    }


    private val accelerometerEventListener: SensorEventListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            stepCounter!!.processSample(event.timestamp, event.values)
           /* try {
                val file: File = File(
                    Environment.getExternalStorageDirectory()
                        .toString() + File.separator
                            + "Download" //folder name
                            + File.separator
                            + "acc.csv"
                )
                // if file doesnt exists, then create it
                if (!file.exists()) {
                    file.createNewFile()
                }
                val fw = FileWriter(file.absoluteFile, true)
                val bw = BufferedWriter(fw)

                val time = event.timestamp

                bw.write(event.values[0].toString())
                bw.write(",")
                bw.write(event.values[1].toString())
                bw.write(",")
                bw.write(event.values[2].toString())
                bw.write(",")
                bw.write(time.toString())
                bw.write("\n")

                bw.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }*/
        }

        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
    }




}