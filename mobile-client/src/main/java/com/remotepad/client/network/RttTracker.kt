package com.remotepad.client.network

import kotlin.math.roundToLong

/**
 * RTT за відповідями PONG. Окремі заміри стрибають через Wi-Fi, тому показую
 * експоненційне середнє, а не останнє значення.
 */
class RttTracker(private val weight: Double = 0.3) {
    private var average: Double? = null

    fun onPong(sentAtMs: Long, receivedAtMs: Long): Long {
        val sample = (receivedAtMs - sentAtMs).coerceAtLeast(0).toDouble()
        val next = average?.let { it + weight * (sample - it) } ?: sample
        average = next
        return next.roundToLong()
    }

    fun reset() {
        average = null
    }
}
