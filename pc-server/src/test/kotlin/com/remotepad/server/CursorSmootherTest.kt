package com.remotepad.server

import com.remotepad.server.input.CursorSmoother
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CursorSmootherTest {

    @Test
    fun `without smoothing delta is scaled by sensitivity`() {
        val smoother = CursorSmoother(alpha = 0f, sensitivity = 2f)
        assertEquals(10 to -4, smoother.next(5f, -2f))
    }

    @Test
    fun `smoothing damps a single jerk`() {
        val smoother = CursorSmoother(alpha = 0.5f, sensitivity = 1f)
        assertEquals(5 to 0, smoother.next(10f, 0f))
    }

    @Test
    fun `slow movement accumulates instead of being lost`() {
        val smoother = CursorSmoother(alpha = 0f, sensitivity = 1f)
        val total = (1..10).sumOf { smoother.next(0.3f, 0f).first }
        assertEquals(3, total)
    }

    @Test
    fun `reset clears history`() {
        val smoother = CursorSmoother(alpha = 0.9f, sensitivity = 1f)
        repeat(5) { smoother.next(100f, 0f) }
        smoother.reset()
        assertEquals(0 to 0, smoother.next(0f, 0f))
    }

    @Test
    fun `alpha outside range is rejected`() {
        assertFailsWith<IllegalArgumentException> { CursorSmoother(alpha = 1f) }
    }
}
