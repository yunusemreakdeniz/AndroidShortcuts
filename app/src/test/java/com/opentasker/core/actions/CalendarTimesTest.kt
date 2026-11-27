package com.opentasker.core.actions

import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CalendarTimesTest {
    @Test
    fun readsDateAndTime() {
        assertEquals(1_798_741_800_000L to false, CalendarTimes.parse("2026-12-31 18:30", ZoneOffset.UTC))
    }

    @Test
    fun aDateAloneIsAllDay() {
        assertEquals(1_798_675_200_000L to true, CalendarTimes.parse("2026-12-31", ZoneOffset.UTC))
    }

    @Test
    fun rejectsOtherText() {
        assertNull(CalendarTimes.parse("yarın", ZoneOffset.UTC))
    }
}
