package com.opentasker.ui.screens

import com.opentasker.core.model.ActionSpec
import com.opentasker.core.model.Task
import org.junit.Assert.assertEquals
import org.junit.Test

class ShortcutSearchTest {
    private val tasks = listOf(
        Task(id = 1, name = "Pil durumu", actions = listOf(ActionSpec(type = "battery.get"), ActionSpec(type = "result.show"))),
        Task(id = 3, name = "Eve yol tarifi", actions = listOf(ActionSpec(type = "maps.directions"))),
        Task(id = 2, name = "Metni cevir", actions = listOf(ActionSpec(type = "input.ask"), ActionSpec(type = "url.open"), ActionSpec(type = "log"))),
    )

    @Test
    fun searchMatchesAnyPartIgnoringCase() {
        assertEquals(listOf(1L), filterAndSortShortcuts(tasks, "DURUM", ShortcutSort.NAME).map { it.id })
    }

    @Test
    fun sortsByNameNewestAndActionCount() {
        assertEquals(listOf(3L, 2L, 1L), filterAndSortShortcuts(tasks, "", ShortcutSort.NAME).map { it.id })
        assertEquals(listOf(3L, 2L, 1L), filterAndSortShortcuts(tasks, "", ShortcutSort.NEWEST).map { it.id })
        assertEquals(listOf(2L, 1L, 3L), filterAndSortShortcuts(tasks, "", ShortcutSort.MOST_ACTIONS).map { it.id })
    }
}
