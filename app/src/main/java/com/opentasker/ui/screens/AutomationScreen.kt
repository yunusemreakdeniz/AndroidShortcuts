package com.opentasker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.opentasker.app.R
import com.opentasker.core.model.ContextType
import com.opentasker.core.model.Profile
import com.opentasker.core.model.Task

/**
 * Shortcuts-style Automation tab: each automation as a "when → do" card with an on/off switch.
 * Tapping a card opens its settings; full trigger editing stays under Advanced › Profiles.
 */
@Composable
internal fun AutomationScreen(
    profiles: List<Profile>,
    tasks: List<Task>,
    contentPadding: PaddingValues,
    onToggle: (Profile, Boolean) -> Unit,
    onOpen: (Profile) -> Unit,
    onEditTriggers: () -> Unit,
    onNewAutomation: (ContextType) -> Unit,
) {
    val tasksById = tasks.associateBy { it.id }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text(
                text = stringResource(R.string.automation_intro),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (profiles.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.automation_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(vertical = 24.dp),
                )
            }
        }
        items(profiles, key = { it.id }) { profile ->
            AutomationCard(
                profile = profile,
                task = tasksById[profile.enterTaskId],
                onToggle = { onToggle(profile, it) },
                onOpen = { onOpen(profile) },
            )
        }
        item {
            Text(
                text = stringResource(R.string.automation_new_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                newAutomationTriggers.chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { (type, icon) ->
                            Surface(
                                onClick = { onNewAutomation(type) },
                                shape = RoundedCornerShape(16.dp),
                                color = shortcutCardColor(),
                                tonalElevation = 1.dp,
                                modifier = Modifier.weight(1f),
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 14.dp, horizontal = 6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Text(
                                        text = stringResource(contextTitleRes(type)),
                                        style = MaterialTheme.typography.labelMedium,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        item {
            TextButton(onClick = onEditTriggers) {
                Text(stringResource(R.string.automation_edit_triggers))
            }
        }
    }
}

@Composable
private fun AutomationCard(profile: Profile, task: Task?, onToggle: (Boolean) -> Unit, onOpen: () -> Unit) {
    val triggers = profile.contexts.map { spec ->
        val title = stringResource(contextTitleRes(spec.type))
        val detail = triggerDetail(spec)
        if (detail == null) title else stringResource(R.string.automation_trigger_with_detail, title, detail)
    }
    val whenText = if (triggers.isEmpty()) stringResource(R.string.automation_no_trigger) else triggers.joinToString(" + ")
    val thenText = task?.name ?: stringResource(R.string.automation_no_shortcut)
    val switchLabel = stringResource(
        if (profile.enabled) R.string.automation_switch_on else R.string.automation_switch_off,
        profile.name,
    )
    Surface(
        onClick = onOpen,
        shape = RoundedCornerShape(18.dp),
        color = shortcutCardColor(),
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(shortcutColor(profile.id), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Outlined.Bolt, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                    Text(
                        text = profile.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = stringResource(R.string.automation_when, whenText),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = thenText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Switch(
                checked = profile.enabled,
                onCheckedChange = onToggle,
                modifier = Modifier.semantics { contentDescription = switchLabel },
            )
        }
    }
}

/** The design's "New automation" tiles; each starts the automation with that kind of trigger. */
private val newAutomationTriggers = listOf(
    ContextType.TIME to Icons.Outlined.Schedule,
    ContextType.DAY to Icons.Outlined.CalendarMonth,
    ContextType.LOCATION to Icons.Outlined.LocationOn,
    ContextType.APPLICATION to Icons.Outlined.Apps,
    ContextType.STATE to Icons.Outlined.PhoneAndroid,
    ContextType.EVENT to Icons.Outlined.NotificationsActive,
)

/** Second step of a new automation: which shortcut it runs. */
@Composable
internal fun NewAutomationShortcutDialog(
    type: ContextType,
    tasks: List<Task>,
    onDismiss: () -> Unit,
    onPick: (Task) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.automation_pick_shortcut_title, stringResource(contextTitleRes(type)))) },
        text = {
            if (tasks.isEmpty()) {
                Text(stringResource(R.string.automation_pick_shortcut_empty))
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(tasks, key = { it.id }) { task ->
                        Surface(
                            onClick = { onPick(task) },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(task.name, modifier = Modifier.padding(14.dp), style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_close)) } },
    )
}

/** A short detail for a trigger, such as "07:00–08:00" for a time trigger, or null. */
private fun triggerDetail(spec: com.opentasker.core.model.ContextSpec): String? {
    if (spec.type != ContextType.TIME) return null
    val start = (spec.config["start"] ?: spec.config["from"])?.trim()?.takeIf(String::isNotEmpty)
    val end = (spec.config["end"] ?: spec.config["to"])?.trim()?.takeIf(String::isNotEmpty)
    return when {
        start != null && end != null -> "$start–$end"
        else -> start ?: end
    }
}
