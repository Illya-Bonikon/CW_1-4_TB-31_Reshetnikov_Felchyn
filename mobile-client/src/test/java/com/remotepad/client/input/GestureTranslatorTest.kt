package com.remotepad.client.input

import com.remotepad.client.data.UserPreference
import com.remotepad.protocol.MouseButton
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GestureTranslatorTest {

    private fun translator(
        sensitivity: Float = 1f,
        acceleration: Boolean = false,
        smoothing: Float = 0f,
        reverse: Boolean = false
    ) = GestureTranslator(UserPreference(sensitivity, acceleration, smoothing, reverse))

    @Test
    fun `sensitivity scales movement`() {
        assertEquals(Delta(4f, -2f), translator(sensitivity = 2f).move(2f, -1f))
    }

    @Test
    fun `fast swipe goes further than slow one with acceleration`() {
        val slow = translator(acceleration = true).move(2f, 0f).x / 2f
        val fast = translator(acceleration = true).move(40f, 0f).x / 40f
        assertTrue(fast > slow, "gain for fast swipe $fast must exceed $slow")
    }

    @Test
    fun `acceleration gain is capped`() {
        val gain = translator(acceleration = true).move(10_000f, 0f).x / 10_000f
        assertEquals(2.5f, gain, 1e-3f)
    }

    @Test
    fun `smoothing damps first jerk`() {
        assertEquals(5f, translator(smoothing = 0.5f).move(10f, 0f).x, 1e-6f)
    }

    @Test
    fun `scroll emits whole notches and keeps the rest`() {
        val t = translator()
        val step = GestureTranslator.SCROLL_STEP_PX
        assertEquals(0, t.scroll(-step * 0.6f))
        assertEquals(1, t.scroll(-step * 0.6f))
    }

    @Test
    fun `reverse scroll flips direction`() {
        val step = GestureTranslator.SCROLL_STEP_PX
        assertEquals(1, translator().scroll(-step))
        assertEquals(-1, translator(reverse = true).scroll(-step))
    }

    @Test
    fun `tap button depends on finger count`() {
        val t = translator()
        assertEquals(MouseButton.LEFT, t.tap(1))
        assertEquals(MouseButton.RIGHT, t.tap(2))
        assertEquals(MouseButton.NONE, t.tap(5))
    }

    @Test
    fun `reset forgets scroll remainder`() {
        val t = translator()
        t.scroll(-GestureTranslator.SCROLL_STEP_PX * 0.9f)
        t.reset()
        assertEquals(0, t.scroll(-GestureTranslator.SCROLL_STEP_PX * 0.2f))
    }
}
