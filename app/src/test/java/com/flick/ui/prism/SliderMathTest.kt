package com.flick.ui.prism

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SliderMathTest {
    private val d = 1e-6f

    @Test fun snapRoundsToStepsAndClamps() {
        assertEquals(0.25f, SliderMath.snap(0.13f, 4), d)
        assertEquals(0f, SliderMath.snap(0.12f, 4), d)
        assertEquals(1f, SliderMath.snap(1.3f, 0), d)
        assertEquals(0f, SliderMath.snap(-0.2f, 10), d)
        assertEquals(0.5f, SliderMath.snap(0.5f, 0), d)
    }

    @Test fun valueAtAccountsForThumbAndClamps() {
        assertEquals(0f, SliderMath.valueAt(22f, 300f, 44f), d)
        assertEquals(0.5f, SliderMath.valueAt(150f, 300f, 44f), d)
        assertEquals(1f, SliderMath.valueAt(400f, 300f, 44f), d)
    }

    @Test fun draggedIsRelativeToStartAndClamps() {
        assertEquals(0.75f, SliderMath.dragged(0.5f, 64f, 300f, 44f), d)
        assertEquals(1f, SliderMath.dragged(0.9f, 200f, 300f, 44f), d)
    }

    @Test fun ticksOncePerDetent() {
        assertEquals(3, SliderMath.tickZone(0.3f, 10))
        assertEquals(3, SliderMath.tickZone(0.32f, 10))
        assertEquals(4, SliderMath.tickZone(0.2f, 0))
        assertEquals(20, SliderMath.tickZone(1f, 0))
        assertEquals(0, SliderMath.tickZone(0.02f, 0))
        assertTrue(SliderMath.shouldTick(2, 3))
        assertFalse(SliderMath.shouldTick(3, 3))
    }
}
