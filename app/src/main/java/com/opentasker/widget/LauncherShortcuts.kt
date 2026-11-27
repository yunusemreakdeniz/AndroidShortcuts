package com.opentasker.widget

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.opentasker.app.R
import com.opentasker.core.model.Task

/**
 * The shortcuts shown when the app icon is long-pressed (and offered to the assistant): the newest
 * few, kept in step with the list. Only ids this sync owns are replaced, so shortcuts a task
 * published itself with "Publish shortcut" stay.
 */
object LauncherShortcuts {
    private const val PREFIX = "auto_task_"
    private const val MAX = 4

    /** The tasks to offer: newest first, at most [MAX]. Kept pure for tests. */
    fun pick(tasks: List<Task>, room: Int = MAX): List<Task> =
        tasks.filter { it.actions.isNotEmpty() }.sortedByDescending { it.id }.take(room.coerceIn(0, MAX))

    fun sync(context: Context, tasks: List<Task>) {
        runCatching {
            val existing = ShortcutManagerCompat.getDynamicShortcuts(context)
            val owned = existing.filter { it.id.startsWith(PREFIX) }.map { it.id }
            val foreign = existing.size - owned.size
            val room = ShortcutManagerCompat.getMaxShortcutCountPerActivity(context) - foreign
            val wanted = pick(tasks, room).mapIndexed { rank, task -> build(context, task, rank) }
            val stale = owned - wanted.map { it.id }.toSet()
            if (stale.isNotEmpty()) ShortcutManagerCompat.removeDynamicShortcuts(context, stale)
            wanted.forEach { ShortcutManagerCompat.pushDynamicShortcut(context, it) }
        }
    }

    private fun build(context: Context, task: Task, rank: Int): ShortcutInfoCompat {
        val run = Intent(context, TaskRunActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            putExtra(TaskRunActivity.EXTRA_TASK_ID, task.id)
            putExtra(TaskRunActivity.EXTRA_SOURCE, TaskRunActivity.SOURCE_SHORTCUT)
        }
        val label = task.name.ifBlank { context.getString(R.string.app_name) }.take(25)
        return ShortcutInfoCompat.Builder(context, PREFIX + task.id)
            .setShortLabel(label)
            .setLongLabel(task.name.ifBlank { label }.take(80))
            .setIcon(IconCompat.createWithResource(context, R.mipmap.ic_launcher))
            .setIntent(run)
            .setRank(rank)
            .build()
    }
}
