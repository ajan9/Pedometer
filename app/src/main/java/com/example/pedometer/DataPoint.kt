package com.example.pedometer

class DataPoint(private var time: Float, private var magnitude: Float) {
    private var eos: Boolean = false

    constructor(time: Long, magnitude: Float) : this(time.toFloat(), magnitude) {
        eos = false
    }

    fun setEos(value: Boolean) {
        eos = value
    }

    fun getEos(): Boolean {
        return eos
    }

    fun getMagnitude(): Float {
        return magnitude
    }

    fun setMagnitude(magnitude: Float) {
        this.magnitude = magnitude
    }

    fun getTime(): Float {
        return time
    }

    fun setTime(time: Float) {
        this.time = time
    }
}