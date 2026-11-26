package com.opentasker.ui.share

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.opentasker.app.OpenTaskerApp_NoHilt
import com.opentasker.app.R
import com.opentasker.core.contexts.ShareContextEvents
import com.opentasker.core.engine.EventTaskVariables
import com.opentasker.core.engine.ExecutionAdmissionRegistry
import com.opentasker.core.engine.ExecutionEnvelope
import com.opentasker.core.engine.executeAndLogTask
import com.opentasker.core.logging.AppLogger
import com.opentasker.core.model.Task
import com.opentasker.ui.screens.rememberShortcutStyles
import com.opentasker.ui.screens.styledColor
import com.opentasker.ui.screens.styledIcon
import com.opentasker.ui.theme.OpenTaskerTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Share sheet entry like Shortcuts' "Shortcuts" share target: pick a shortcut and it runs with the
 * shared text or link in {{ ShortcutInput }} (plus the share_* variables automations already get).
 */
class ShareToShortcutActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val event = runCatching { ShareContextEvents.parseIntent(intent) }.getOrNull()
        if (event == null) {
            Toast.makeText(this, R.string.share_invalid, Toast.LENGTH_LONG).show()
            finish()
            return
        }
        val variables = shareVariables(EventTaskVariables.from(event))
        setContent {
            OpenTaskerTheme {
                var tasks by remember { mutableStateOf<List<Task>?>(null) }
                LaunchedEffect(Unit) {
                    tasks = withContext(Dispatchers.IO) {
                        runCatching {
                            OpenTaskerApp_NoHilt.db.taskDao().getAll()
                                .mapNotNull { entity -> entity.toDomainDecodeResult().takeIf { it.issue == null }?.value }
                                .sortedBy { it.name.lowercase() }
                        }.getOrDefault(emptyList())
                    }
                }
                SharePickerDialog(
                    preview = variables[INPUT_VARIABLE].orEmpty(),
                    tasks = tasks,
                    onDismiss = { finish() },
                    onPick = { task -> run(task, variables) },
                )
            }
        }
    }

    private fun run(task: Task, variables: Map<String, String>) {
        val appContext = applicationContext
        finish()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            val message = try {
                val result = executeAndLogTask(
                    appContext = appContext,
                    db = OpenTaskerApp_NoHilt.db,
                    task = task,
                    source = SOURCE_SHARE,
                    initialVariables = variables,
                    execution = ExecutionEnvelope.create(task, SOURCE_SHARE),
                    visibleActivity = true,
                    admissionController = ExecutionAdmissionRegistry.current(appContext),
                )
                if (result.report.success) null else appContext.getString(R.string.widget_task_result_failed, task.name, result.report.durationMs)
            } catch (e: Exception) {
                AppLogger.error(TAG, "Shared run failed", e)
                appContext.getString(R.string.widget_task_run_failed)
            }
            if (message != null) {
                withContext(Dispatchers.Main) { Toast.makeText(appContext, message, Toast.LENGTH_SHORT).show() }
            }
        }
    }

    companion object {
        const val INPUT_VARIABLE = "ShortcutInput"
        const val SOURCE_SHARE = "Share"
        private const val TAG = "ShareToShortcut"

        /** The share variables plus one simple input: the text, else the first link or file. */
        fun shareVariables(share: Map<String, String>): Map<String, String> {
            val input = share["share_text"].orEmpty().ifBlank { share["share_uri"].orEmpty() }
            return share + (INPUT_VARIABLE to input)
        }
    }
}

@Composable
private fun SharePickerDialog(
    preview: String,
    tasks: List<Task>?,
    onDismiss: () -> Unit,
    onPick: (Task) -> Unit,
) {
    val styles = rememberShortcutStyles()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.share_pick_title), fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (preview.isNotBlank()) {
                    Text(
                        text = preview,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                when {
                    tasks == null -> Unit
                    tasks.isEmpty() -> Text(stringResource(R.string.share_pick_empty))
                    else -> LazyColumn(
                        modifier = Modifier.heightIn(max = 420.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(tasks, key = { it.id }) { task ->
                            Surface(
                                onClick = { onPick(task) },
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .background(styledColor(task, styles), RoundedCornerShape(10.dp)),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(styledIcon(task, styles), contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                    }
                                    Text(task.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}
