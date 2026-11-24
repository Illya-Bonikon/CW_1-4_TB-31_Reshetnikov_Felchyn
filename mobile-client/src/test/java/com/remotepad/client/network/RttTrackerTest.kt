package com.remotepad.client.network

import kotlin.test.Test
import kotlin.test.assertEquals

class RttTrackerTest {

    @Test
    fun `first sample is shown as is`() {
        assertEquals(12, RttTracker().onPong(sentAtMs = 100, receivedAtMs = 112))
    }

    @Test
    fun `single spike is damped`() {
        val tracker = RttTracker(weight = 0.3)
        tracker.onPong(0, 10)
        assertEquals(37, tracker.onPong(0, 100))
    }

    @Test
    fun `clock going backwards does not give negative rtt`() {
        assertEquals(0, RttTracker().onPong(sentAtMs = 200, receivedAtMs = 150))
    }
}
