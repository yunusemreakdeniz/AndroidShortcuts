package com.opentasker.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import androidx.compose.ui.graphics.toArgb
import com.opentasker.app.OpenTaskerApp_NoHilt
import com.opentasker.app.R
import com.opentasker.core.model.Task
import com.opentasker.ui.screens.ShortcutStyles
import com.opentasker.ui.screens.styledColor
import kotlinx.coroutines.runBlocking

/** Home-screen grid of shortcuts, like the Shortcuts widget: tap a tile to run it. */
class ShortcutGridWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { id -> manager.updateAppWidget(id, views(context, id)) }
        manager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_grid)
    }

    companion object {
        private const val MAX_TILES = 12

        /** Called when shortcuts change so placed grids show the current list. */
        fun refresh(context: Context) {
            runCatching {
                val manager = AppWidgetManager.getInstance(context)
                val ids = manager.getAppWidgetIds(ComponentName(context, ShortcutGridWidgetProvider::class.java))
                if (ids.isNotEmpty()) manager.notifyAppWidgetViewDataChanged(ids, R.id.widget_grid)
            }
        }

        private fun views(context: Context, widgetId: Int): RemoteViews {
            val adapter = Intent(context, ShortcutGridWidgetService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
            }
            val template = PendingIntent.getActivity(
                context,
                widgetId,
                Intent(context, TaskRunActivity::class.java).apply {
                    action = Intent.ACTION_VIEW
                    putExtra(TaskRunActivity.EXTRA_SOURCE, TaskRunActivity.SOURCE_WIDGET)
                },
                // Each tile fills in its own task id, so the template must stay mutable.
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
            )
            return RemoteViews(context.packageName, R.layout.widget_shortcuts_grid).apply {
                @Suppress("DEPRECATION")
                setRemoteAdapter(R.id.widget_grid, adapter)
                setEmptyView(R.id.widget_grid, R.id.widget_grid_empty)
                setPendingIntentTemplate(R.id.widget_grid, template)
            }
        }

        internal fun loadTasks(): List<Task> = runCatching {
            runBlocking {
                OpenTaskerApp_NoHilt.db.taskDao().getAll()
                    .mapNotNull { entity -> entity.toDomainDecodeResult().takeIf { it.issue == null }?.value }
                    .sortedBy { it.name.lowercase() }
                    .take(MAX_TILES)
            }
        }.getOrDefault(emptyList())
    }
}

class ShortcutGridWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory = Factory(applicationContext)

    private class Factory(private val context: Context) : RemoteViewsFactory {
        private var tasks: List<Task> = emptyList()

        override fun onCreate() = Unit
        override fun onDataSetChanged() {
            tasks = ShortcutGridWidgetProvider.loadTasks()
        }
        override fun onDestroy() = Unit
        override fun getCount(): Int = tasks.size
        override fun getLoadingView(): RemoteViews? = null
        override fun getViewTypeCount(): Int = 1
        override fun getItemId(position: Int): Long = tasks.getOrNull(position)?.id ?: position.toLong()
        override fun hasStableIds(): Boolean = true

        override fun getViewAt(position: Int): RemoteViews {
            val task = tasks.getOrNull(position) ?: return RemoteViews(context.packageName, R.layout.widget_shortcuts_item)
            val styles = ShortcutStyles.styles(context).value
            return RemoteViews(context.packageName, R.layout.widget_shortcuts_item).apply {
                setTextViewText(R.id.widget_item_name, task.name)
                setInt(R.id.widget_item_bg, "setColorFilter", styledColor(task, styles).toArgb())
                setContentDescription(R.id.widget_item_root, task.name)
                setOnClickFillInIntent(
                    R.id.widget_item_root,
                    Intent().putExtra(TaskRunActivity.EXTRA_TASK_ID, task.id),
                )
            }
        }
    }
}
