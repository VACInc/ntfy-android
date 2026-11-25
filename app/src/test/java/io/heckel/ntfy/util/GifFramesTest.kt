package io.heckel.ntfy.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GifFramesTest {
    @Test
    fun shortGifIsSampledEveryStep() {
        assertEquals(listOf(0, 200, 400, 600, 800), GifFrames.frameTimes(1000))
    }

    @Test
    fun longGifIsCappedAndSpreadOverTheWholeDuration() {
        val t = GifFrames.frameTimes(12000)
        assertEquals(GifFrames.MAX_FRAMES, t.size)
        assertEquals(400, t[1])
        assertTrue(t.last() >= 11000)
    }

    @Test
    fun stillOrBrokenGifHasNoFrames() {
        assertEquals(emptyList<Int>(), GifFrames.frameTimes(0))
        assertEquals(emptyList<Int>(), GifFrames.frameTimes(150))
        assertEquals(emptyList<Int>(), GifFrames.frameTimes(-5))
    }

    @Test
    fun framesAreScaledToMaxWidthKeepingAspect() {
        assertEquals(Pair(400, 225), GifFrames.scaledSize(1280, 720))
        assertEquals(Pair(320, 180), GifFrames.scaledSize(320, 180))
        assertEquals(Pair(0, 0), GifFrames.scaledSize(0, 10))
    }

    @Test
    fun frameCountRespectsMemoryBudget() {
        // 400x225 RGB_565 = 180000 bytes; 5 MB fits 27.
        assertEquals(27, GifFrames.framesWithinBudget(400, 225, 30))
        assertEquals(11, GifFrames.framesWithinBudget(400, 225, 11))
        assertEquals(0, GifFrames.framesWithinBudget(0, 0, 11))
    }
}
