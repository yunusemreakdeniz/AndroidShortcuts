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
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
            TextButton(onClick = onEditTriggers) {
                Text(stringResource(R.string.automation_edit_triggers))
            }
        }
    }
}

@Composable
private fun AutomationCard(profile: Profile, task: Task?, onToggle: (Boolean) -> Unit, onOpen: () -> Unit) {
    val triggers = profile.contexts.map { stringResource(contextTitleRes(it.type)) }
    val whenText = if (triggers.isEmpty()) stringResource(R.string.automation_no_trigger) else triggers.joinToString(" + ")
    val thenText = task?.name ?: stringResource(R.string.automation_no_shortcut)
    val switchLabel = stringResource(
        if (profile.enabled) R.string.automation_switch_on else R.string.automation_switch_off,
        profile.name,
    )
    Surface(
        onClick = onOpen,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
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
