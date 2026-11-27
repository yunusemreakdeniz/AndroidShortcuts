package com.opentasker.widget

import com.opentasker.core.model.ActionSpec
import com.opentasker.core.model.Task
import org.junit.Assert.assertEquals
import org.junit.Test

class LauncherShortcutsTest {
    private fun task(id: Long, steps: Int = 1) = Task(id = id, name = "T$id", actions = List(steps) { ActionSpec(type = "log") })

    @Test
    fun newestFourWithStepsAreOffered() {
        val tasks = listOf(task(1), task(5), task(3), task(9, steps = 0), task(7), task(2))
        assertEquals(listOf(7L, 5L, 3L, 2L), LauncherShortcuts.pick(tasks).map { it.id })
    }

    @Test
    fun roomTakenByPublishedShortcutsIsRespected() {
        assertEquals(listOf(3L), LauncherShortcuts.pick(listOf(task(1), task(3)), room = 1).map { it.id })
        assertEquals(emptyList<Long>(), LauncherShortcuts.pick(listOf(task(1)), room = -2).map { it.id })
    }
}
