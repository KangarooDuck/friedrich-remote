package com.example.friedrichremote.ir

import android.content.Context
import android.hardware.ConsumerIrManager
import android.util.Log

class IrTransmitter(context: Context) {
    private val irManager =
        context.getSystemService(Context.CONSUMER_IR_SERVICE) as? ConsumerIrManager

    fun hasIrBlaster(): Boolean {
        val has = irManager?.hasIrEmitter() == true
        Log.d(TAG, "hasIrBlaster=$has")
        return has
    }

    fun transmit(pattern: IntArray): Boolean {
        Log.d(TAG, "transmit start: freq=${LgAcProtocol.FREQ_HZ}Hz, patternSize=${pattern.size}")

        validatePattern(pattern)

        irManager ?: run {
            Log.e(TAG, "No ConsumerIrManager")
            return false
        }

        return try {
            irManager.transmit(LgAcProtocol.FREQ_HZ, pattern)
            Log.d(TAG, "transmit success")
            true
        } catch (e: Exception) {
            Log.e(TAG, "transmit failed: ${e.message}", e)
            false
        }
    }

    fun transmitTwice(pattern: IntArray): Boolean {
        val success = transmit(pattern)
        if (success) {
            Thread.sleep(20)
            transmit(pattern)
        }
        return success
    }

    private fun validatePattern(pattern: IntArray) {
        check(pattern.isNotEmpty()) { "Pattern must not be empty" }
        check(pattern.size % 2 == 0) {
            "Pattern must have even length (mark-space pairs), got ${pattern.size}"
        }
        pattern.forEach { value ->
            check(value > 0) { "All pattern values must be positive, got $value" }
        }
    }

    companion object {
        const val TAG = "IrTransmitter"
    }
}
