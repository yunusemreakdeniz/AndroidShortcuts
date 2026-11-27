package com.opentasker.ui.screens

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** What a shortcut tile shows about its last tap: a spinner while running, then a tick or a cross. */
enum class RunPhase { RUNNING, SUCCEEDED, FAILED }

/** Run state of shortcuts started from the app, for the Shortcuts-style feedback on their tiles. */
object ShortcutRunState {
    private val _phases = MutableStateFlow<Map<Long, RunPhase>>(emptyMap())
    val phases: StateFlow<Map<Long, RunPhase>> = _phases.asStateFlow()

    fun started(taskId: Long) {
        _phases.value = _phases.value + (taskId to RunPhase.RUNNING)
    }

    fun finished(taskId: Long, success: Boolean) {
        _phases.value = _phases.value + (taskId to if (success) RunPhase.SUCCEEDED else RunPhase.FAILED)
    }

    /** The tick fades away after a moment. */
    fun clear(taskId: Long) {
        _phases.value = _phases.value - taskId
    }
}
