package com.example.pedometer

class DataPoint(private var time: Float, private var magnitude: Float, private var oldMagnitude: Float) {
    private var eos: Boolean = false

    constructor(time: Long, magnitude: Float, oldMagnitude: Float) : this(time.toFloat(), magnitude, oldMagnitude) {
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

    fun getOldMagnitude(): Float {
        return oldMagnitude
    }

    fun setOldMagnitude(oldMagnitude: Float) {
        this.oldMagnitude = oldMagnitude
    }

    fun getTime(): Float {
        return time
    }

    fun setTime(time: Float) {
        this.time = time
    }
}