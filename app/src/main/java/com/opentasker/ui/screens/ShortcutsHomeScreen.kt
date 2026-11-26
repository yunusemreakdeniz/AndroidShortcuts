package com.opentasker.ui.screens

import androidx.compose.foundation.combinedClickable
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.BatteryStd
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.opentasker.app.R
import com.opentasker.core.model.Task

/**
 * Shortcuts-style home: every task as a colored tile. Tapping a tile opens it in the editor; the
 * round button runs it at once.
 */
@Composable
internal fun ShortcutsHomeScreen(
    tasks: List<Task>,
    contentPadding: PaddingValues,
    onRunTask: (Task) -> Unit,
    onOpenTask: (Task) -> Unit,
    onCreateTask: () -> Unit,
    onDeleteTask: (Task) -> Unit = {},
) {
    val styles = rememberShortcutStyles()
    val context = androidx.compose.ui.platform.LocalContext.current
    var styling by remember { mutableStateOf<Task?>(null) }
    var deleting by remember { mutableStateOf<Task?>(null) }
    styling?.let { task ->
        ShortcutStyleDialog(
            task = task,
            current = styles[task.id] ?: ShortcutStyle(),
            onDismiss = { styling = null },
            onSave = { ShortcutStyles.set(context, task.id, it); styling = null },
        )
    }
    deleting?.let { task ->
        DeleteShortcutDialog(task.name, onDismiss = { deleting = null }, onDelete = { deleting = null; onDeleteTask(task) })
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
    ) {
        if (tasks.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp)
                    .align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = stringResource(R.string.shortcuts_empty_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = stringResource(R.string.shortcuts_empty_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
                FilledTonalButton(onClick = onCreateTask) {
                    Text(stringResource(R.string.shortcuts_create))
                }
            }
            return@Box
        }
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 156.dp),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(tasks, key = { it.id }) { task ->
                ShortcutTile(
                    task = task,
                    styles = styles,
                    onRun = { onRunTask(task) },
                    onOpen = { onOpenTask(task) },
                    onStyle = { styling = task },
                    onDelete = { deleting = task },
                )
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun ShortcutTile(
    task: Task,
    styles: Map<Long, ShortcutStyle>,
    onRun: () -> Unit,
    onOpen: () -> Unit,
    onStyle: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val longPressLabel = stringResource(R.string.shortcuts_tile_menu)
    val color = styledColor(task, styles)
    val count = pluralStringResource(R.plurals.shortcuts_action_count, task.actions.size, task.actions.size)
    val tileDescription = stringResource(R.string.shortcuts_tile_description, task.name, count)
    Box {
    Surface(
        color = color,
        contentColor = Color.White,
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier
            .fillMaxWidth()
            // Grows with large text so long names are not cut off.
            .heightIn(min = 116.dp)
            .clip(RoundedCornerShape(22.dp))
            .combinedClickable(
                role = Role.Button,
                onClick = onOpen,
                onLongClick = { menuOpen = true },
                onLongClickLabel = longPressLabel,
            )
            .semantics { contentDescription = tileDescription },
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Icon(styledIcon(task, styles), contentDescription = null, modifier = Modifier.size(26.dp))
                Spacer(Modifier.weight(1f))
                FilledIconButton(
                    onClick = onRun,
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = Color.White.copy(alpha = 0.22f),
                        contentColor = Color.White,
                    ),
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        Icons.Filled.PlayArrow,
                        contentDescription = stringResource(R.string.shortcuts_run, task.name),
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = task.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = count,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.88f),
            )
        }
    }
    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
        DropdownMenuItem(text = { Text(stringResource(R.string.shortcut_editor_run)) }, onClick = { menuOpen = false; onRun() })
        DropdownMenuItem(text = { Text(stringResource(R.string.action_edit)) }, onClick = { menuOpen = false; onOpen() })
        DropdownMenuItem(text = { Text(stringResource(R.string.shortcut_style_title)) }, onClick = { menuOpen = false; onStyle() })
        DropdownMenuItem(
            text = { Text(stringResource(R.string.shortcut_editor_delete_shortcut), color = MaterialTheme.colorScheme.error) },
            onClick = { menuOpen = false; onDelete() },
        )
    }
    }
}

/** Deep tile colors; white text on each passes 4.5:1. */
private val shortcutPalette = listOf(
    Color(0xFF2563EB), Color(0xFF6D28D9), Color(0xFF0F766E), Color(0xFF4338CA),
    Color(0xFF15803D), Color(0xFFBE185D), Color(0xFFC2410C), Color(0xFF9A3412),
)

internal val shortcutPaletteIndices: List<Int> get() = shortcutPalette.indices.toList()

internal fun shortcutPaletteColor(index: Int): Color = shortcutPalette[Math.floorMod(index, shortcutPalette.size)]

internal fun shortcutColor(taskId: Long): Color =
    shortcutPalette[Math.floorMod(taskId, shortcutPalette.size.toLong()).toInt()]

/** Picks an icon from what the shortcut does first. */
internal fun shortcutIcon(task: Task): ImageVector {
    val type = task.actions.firstOrNull { it.enabled }?.type.orEmpty()
    return when {
        type.startsWith("wifi") || type.startsWith("bluetooth") -> Icons.Outlined.Wifi
        type.startsWith("input.") || type.startsWith("menu.") || type.startsWith("list.") ||
            type.startsWith("alert.") || type.startsWith("result.") || type.startsWith("date.") ||
            type.startsWith("dictate.") -> Icons.Outlined.ChatBubbleOutline
        type.startsWith("http") || type.startsWith("url.") || type.startsWith("download") -> Icons.Outlined.Language
        type.startsWith("notify.") || type.startsWith("tts.") -> Icons.Outlined.Notifications
        type.startsWith("sound.") || type.startsWith("track.") || type.startsWith("media.") -> Icons.Outlined.MusicNote
        type.startsWith("alarm.") || type.startsWith("timer.") -> Icons.Outlined.Alarm
        type.startsWith("battery.") -> Icons.Outlined.BatteryStd
        type.startsWith("math.") || type.startsWith("random.") -> Icons.Outlined.Calculate
        type.startsWith("share.") -> Icons.Outlined.Share
        type.startsWith("maps.") -> Icons.Outlined.Map
        type.startsWith("email.") -> Icons.Outlined.Email
        type.startsWith("phone.") || type.startsWith("sms.") -> Icons.Outlined.Phone
        else -> Icons.Outlined.Bolt
    }
}
