package io.heckel.ntfy.util

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HaHintsTest {
    @Test
    fun gunnerBowlAlertHints() {
        val h = HaHints.parse(listOf("ha-sticky", "ha-persistent", "ha-chan:GUNNER_FOOD", "ha-imp:max",
            "ha-vib:100.5000.100.5000.100", "warning"))
        assertTrue(h.sticky)
        assertTrue(h.persistent)
        assertFalse(h.alertOnce)
        assertEquals("GUNNER_FOOD", h.channel)
        assertEquals("max", h.importance)
        assertArrayEquals(longArrayOf(100, 5000, 100, 5000, 100), h.vibration)
        assertTrue(h.channelId()!!.startsWith("ha_gunner_food_"))
    }

    @Test
    fun noHintsMeansStockBehavior() {
        val h = HaHints.parse(listOf("warning", "dog"))
        assertEquals(HaHints(), h)
        assertNull(h.channelId())
    }

    @Test
    fun badValuesAreIgnored() {
        val h = HaHints.parse(listOf("ha-vib:100.x", "ha-led:red", "ha-imp:loud", "ha-timeout:-5", "ha-vis:everyone",
            "ha-progress:5/0", "ha-when:abc"))
        assertEquals(HaHints(), h)
        assertNull(HaHints.parseVibration(""))
        assertNull(HaHints.parseVibration("70000"))
    }

    @Test
    fun timersProgressAndMisc() {
        val h = HaHints.parse(listOf("ha-alert_once", "ha-timeout:60", "ha-vis:public", "ha-group:doors",
            "ha-when:1795641439", "ha-countdown", "ha-progress:45/200", "ha-led:FF0000"))
        assertTrue(h.alertOnce)
        assertEquals(60L, h.timeoutSeconds)
        assertEquals("public", h.visibility)
        assertEquals("doors", h.group)
        assertEquals(1795641439L, h.whenSeconds)
        assertTrue(h.chronometer && h.countdown)
        assertEquals(45, h.progress)
        assertEquals(200, h.progressMax)
        assertEquals("#ff0000", h.led)
        assertTrue(HaHints.parse(listOf("ha-progress:indeterminate")).progressIndeterminate)
        assertEquals(100, HaHints.parse(listOf("ha-progress:30")).progressMax)
    }

    @Test
    fun channelIdChangesWhenVibrationChanges() {
        val a = HaHints.parse(listOf("ha-chan:Water Leak", "ha-vib:100.200"))
        val b = HaHints.parse(listOf("ha-chan:Water Leak", "ha-vib:100.300"))
        assertNotEquals(a.channelId(), b.channelId())
        assertTrue(a.channelId()!!.startsWith("ha_water_leak_"))
    }

    @Test
    fun hintTagsAreHiddenFromDisplay() {
        assertEquals(listOf("warning"), visibleTags(listOf("warning", "ha-sticky", "ha-chan:x")))
    }
}
