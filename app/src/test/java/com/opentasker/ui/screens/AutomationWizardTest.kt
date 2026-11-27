package com.opentasker.ui.screens

import com.opentasker.core.model.ContextType
import org.junit.Assert.assertEquals
import org.junit.Test

class AutomationWizardTest {
    @Test
    fun everyDayIsJustATimeWindow() {
        val triggers = timeTrigger(7, 0, setOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"))
        assertEquals(1, triggers.size)
        assertEquals(mapOf("start" to "07:00", "end" to "07:01"), triggers[0].config)
    }

    @Test
    fun chosenDaysAddADayTriggerInWeekOrder() {
        val triggers = timeTrigger(23, 59, setOf("FRI", "MON"))
        assertEquals("00:00", triggers[0].config["end"])
        assertEquals(ContextType.DAY, triggers[1].type)
        assertEquals("MON,FRI", triggers[1].config["days"])
    }

    @Test
    fun batteryAndChargingTriggers() {
        assertEquals(mapOf("key" to "battery_level", "operator" to "<=", "value" to "20"), batteryTrigger(20).config)
        assertEquals("true", chargingTrigger(true).config["value"])
    }
}
