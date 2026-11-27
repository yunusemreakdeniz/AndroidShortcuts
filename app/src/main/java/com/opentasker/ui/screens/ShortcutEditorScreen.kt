package com.opentasker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.draw.zIndex
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.opentasker.app.R
import com.opentasker.core.actions.ActionArgumentSensitivity
import com.opentasker.core.actions.ActionMetadataRegistry
import com.opentasker.core.model.ActionSpec
import com.opentasker.core.model.Task

/**
 * Shortcuts-style editor: the shortcut's icon, name and Run button on top, its actions as a column
 * of blocks, and "Add action" at the bottom. Advanced settings stay in the classic task dialog.
 */
@Composable
internal fun ShortcutEditorScreen(
    task: Task,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onRun: () -> Unit,
    onOpenDetails: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onAddAction: () -> Unit,
    onEditAction: (Int, ActionSpec) -> Unit,
    onMoveAction: (Int, Int) -> Unit,
    onDeleteAction: (Int) -> Unit,
    onPin: (android.graphics.Bitmap) -> Unit = {},
    onUpdateAction: (Int, ActionSpec) -> Unit = { _, _ -> },
) {
    // The block open for editing in place, like Shortcuts; -1 when none.
    var expanded by remember(task.id) { mutableStateOf(-1) }
    // Long-press and drag a block to move it; it lands where it is dropped.
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    var dragFrom by remember(task.id) { mutableStateOf(-1) }
    var dragOffset by remember(task.id) { mutableStateOf(0f) }
    val haptics = androidx.compose.ui.platform.LocalHapticFeedback.current
    fun dropTarget(): Int {
        // The hint row above the steps shifts list positions by one.
        val shift = if (task.actions.size >= 2) 1 else 0
        val visible = listState.layoutInfo.visibleItemsInfo.filter { it.index >= shift }
        val me = visible.firstOrNull { it.index == dragFrom + shift } ?: return -1
        val center = me.offset + me.size / 2 + dragOffset
        val hit = visible.firstOrNull { center >= it.offset && center < it.offset + it.size }
            ?: if (center < visible.first().offset) visible.first() else visible.last()
        return (hit.index - shift).coerceIn(0, task.actions.lastIndex)
    }
    val context = androidx.compose.ui.platform.LocalContext.current
    val styles = rememberShortcutStyles()
    val pinIcon = rememberPinIcon(task, styles)
    var styling by remember { mutableStateOf(false) }
    if (styling) {
        ShortcutStyleDialog(
            task = task,
            current = styles[task.id] ?: ShortcutStyle(),
            onDismiss = { styling = false },
            onSave = { ShortcutStyles.set(context, task.id, it); styling = false },
        )
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.shortcut_editor_back))
            }
            Spacer(Modifier.weight(1f))
            Box {
                var menuOpen by remember { mutableStateOf(false) }
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.shortcut_editor_more))
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(text = { Text(stringResource(R.string.shortcut_editor_rename)) }, onClick = { menuOpen = false; onRename() })
                    DropdownMenuItem(text = { Text(stringResource(R.string.shortcut_style_title)) }, onClick = { menuOpen = false; styling = true })
                    DropdownMenuItem(text = { Text(stringResource(R.string.shortcut_add_to_home)) }, onClick = { menuOpen = false; onPin(pinIcon()) })
                    DropdownMenuItem(text = { Text(stringResource(R.string.shortcut_editor_details)) }, onClick = { menuOpen = false; onOpenDetails() })
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.shortcut_editor_delete_shortcut), color = MaterialTheme.colorScheme.error) },
                        onClick = { menuOpen = false; onDelete() },
                    )
                }
            }
            Button(onClick = onRun, enabled = task.actions.isNotEmpty(), modifier = Modifier.padding(end = 12.dp)) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.shortcut_editor_run))
            }
        }
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Surface(
                onClick = { styling = true },
                shape = RoundedCornerShape(18.dp),
                color = styledColor(task, styles),
                modifier = Modifier.size(60.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        styledIcon(task, styles),
                        contentDescription = stringResource(R.string.shortcut_style_title),
                        tint = Color.White,
                        modifier = Modifier.size(30.dp),
                    )
                }
            }
            Column {
                Text(
                    text = task.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = pluralStringResource(R.plurals.shortcuts_action_count, task.actions.size, task.actions.size),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (task.actions.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.shortcut_editor_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
            if (task.actions.size >= 2) {
                item(key = "drag-hint") {
                    Text(
                        text = stringResource(R.string.shortcut_editor_drag_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            itemsIndexed(task.actions, key = { index, action -> "${action.id}-$index" }) { index, action ->
                val metadata = ActionMetadataRegistry.get(action.type)
                val inline = metadata != null && metadata.fields.isNotEmpty() && editableInline(metadata)
                val dragging = dragFrom == index
                ActionBlock(
                    modifier = Modifier
                        .zIndex(if (dragging) 1f else 0f)
                        .graphicsLayer {
                            translationY = if (dragging) dragOffset else 0f
                            val scale = if (dragging) 1.03f else 1f
                            scaleX = scale
                            scaleY = scale
                        }
                        .pointerInput(index, task.actions.size) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = {
                                    expanded = -1
                                    dragFrom = index
                                    dragOffset = 0f
                                    haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                },
                                onDragEnd = {
                                    val to = dropTarget()
                                    if (to in task.actions.indices && to != dragFrom) onMoveAction(dragFrom, to)
                                    dragFrom = -1
                                    dragOffset = 0f
                                },
                                onDragCancel = {
                                    dragFrom = -1
                                    dragOffset = 0f
                                },
                                onDrag = { change, amount ->
                                    change.consume()
                                    dragOffset += amount.y
                                },
                            )
                        },
                    action = action,
                    isFirst = index == 0,
                    isLast = index == task.actions.lastIndex,
                    expanded = expanded == index,
                    onEdit = {
                        if (inline) expanded = if (expanded == index) -1 else index else onEditAction(index, action)
                    },
                    onAdvanced = { onEditAction(index, action) },
                    inlineEditor = if (inline && expanded == index && metadata != null) {
                        {
                            InlineActionFields(
                                action = action,
                                metadata = metadata,
                                variables = remember(task.actions, index) {
                                    task.actions.take(index).mapNotNull(::blockOutputName).distinct()
                                },
                                onDone = { updated ->
                                    expanded = -1
                                    if (updated != action) onUpdateAction(index, updated)
                                },
                                onMore = { expanded = -1; onEditAction(index, action) },
                            )
                        }
                    } else {
                        null
                    },
                    onMoveUp = { onMoveAction(index, index - 1) },
                    onMoveDown = { onMoveAction(index, index + 1) },
                    onDelete = { onDeleteAction(index) },
                )
            }
        }
        Surface(color = MaterialTheme.colorScheme.surfaceContainerLowest) {
            FilledTonalButton(
                onClick = onAddAction,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(52.dp),
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.shortcut_editor_add_action))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ActionBlock(
    modifier: Modifier = Modifier,
    action: ActionSpec,
    isFirst: Boolean,
    isLast: Boolean,
    expanded: Boolean,
    onEdit: () -> Unit,
    onAdvanced: () -> Unit,
    inlineEditor: (@Composable () -> Unit)?,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit,
) {
    val metadata = ActionMetadataRegistry.get(action.type)
    val name = metadata?.let { stringResource(it.nameRes) } ?: stringResource(R.string.action_unknown_name)
    val parts = remember(action) { blockParts(action) }
    val output = remember(action) { blockOutputName(action) }
    var menuOpen by remember { mutableStateOf(false) }
    Surface(
        onClick = onEdit,
        shape = RoundedCornerShape(18.dp),
        color = shortcutCardColor(),
        tonalElevation = 1.dp,
        modifier = modifier.fillMaxWidth(),
    ) {
      Column {
        Row(
            modifier = Modifier.padding(start = 14.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(actionColor(action.type), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    actionIcon(action.type),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (action.enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (parts.isNotEmpty() || output != null) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(top = 4.dp),
                    ) {
                        parts.forEach { part ->
                            if (part.variable) {
                                VariablePill(part.text)
                            } else {
                                Text(
                                    text = part.text,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.align(Alignment.CenterVertically),
                                )
                            }
                        }
                        if (output != null) {
                            Text(
                                text = "→",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.align(Alignment.CenterVertically),
                            )
                            VariablePill(output)
                        }
                    }
                }
            }
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.shortcut_editor_action_menu, name))
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    if (!isFirst) {
                        DropdownMenuItem(text = { Text(stringResource(R.string.shortcut_editor_move_up)) }, onClick = { menuOpen = false; onMoveUp() })
                    }
                    if (!isLast) {
                        DropdownMenuItem(text = { Text(stringResource(R.string.shortcut_editor_move_down)) }, onClick = { menuOpen = false; onMoveDown() })
                    }
                    DropdownMenuItem(text = { Text(stringResource(R.string.inline_more)) }, onClick = { menuOpen = false; onAdvanced() })
                    DropdownMenuItem(text = { Text(stringResource(R.string.shortcut_editor_delete)) }, onClick = { menuOpen = false; onDelete() })
                }
            }
        }
        if (expanded && inlineEditor != null) {
            Column(modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp)) { inlineEditor() }
        }
      }
    }
}

/** Block colors by what the step does; white icons pass contrast on each. */
internal fun actionColor(type: String): Color = when (type.substringBefore('.')) {
    "input", "menu", "list", "alert", "result", "date", "dictate" -> Color(0xFF6D28D9)
    "http", "url", "download", "ping", "mqtt", "wol", "integration" -> Color(0xFF2563EB)
    "flow", "task", "profile" -> Color(0xFF475569)
    "var", "text", "array", "math", "random", "datetime", "data", "clipboard" -> Color(0xFFA16207)
    "sound", "track", "media", "tts" -> Color(0xFFBE185D)
    "alarm", "timer", "email", "phone", "share", "maps", "app", "sms", "intent", "home", "shortcut" -> Color(0xFFC2410C)
    "notify" -> Color(0xFF0F766E)
    else -> Color(0xFF15803D)
}

private data class BlockPart(val text: String, val variable: Boolean)

private val VARIABLE_REFERENCE = Regex("""\{\{\s*([A-Za-z_][A-Za-z0-9_.]*)\s*\}\}""")

/** The block's arguments as text with `{{ Name }}` references split out as variable pills. */
private fun blockParts(action: ActionSpec): List<BlockPart> {
    val parts = mutableListOf<BlockPart>()
    action.args.entries
        .filter { (key, value) -> value.isNotBlank() && key != "var" }
        .take(3)
        .forEach { (key, value) ->
            if (ActionArgumentSensitivity.isSensitive(action.type, key, action.args)) {
                parts += BlockPart(ActionArgumentSensitivity.REDACTED, variable = false)
                return@forEach
            }
            var last = 0
            VARIABLE_REFERENCE.findAll(value).forEach { match ->
                value.substring(last, match.range.first).trim().takeIf(String::isNotEmpty)?.let { parts += BlockPart(it.take(60), false) }
                parts += BlockPart(match.groupValues[1], true)
                last = match.range.last + 1
            }
            value.substring(last).trim().takeIf(String::isNotEmpty)?.let { parts += BlockPart(it.take(60), false) }
        }
    return parts
}

/** The variable the block writes first, as Shortcuts shows a step's result. */
internal fun blockOutputName(action: ActionSpec): String? {
    val output = ActionMetadataRegistry.get(action.type)?.outputs?.firstOrNull() ?: return null
    val base = output.nameArgumentKeys.firstNotNullOfOrNull { key -> action.args[key]?.takeIf(String::isNotBlank) }
        ?: output.defaultName
        ?: return null
    return base + output.suffix
}

/** Soft, readable pill colours; dark text on each passes contrast in both themes. */
private val pillColors = listOf(
    Color(0xFFEDE7FB) to Color(0xFF5B21B6), Color(0xFFFDEBDD) to Color(0xFF9A3412),
    Color(0xFFDDF2EF) to Color(0xFF0F5F58), Color(0xFFDCE7FD) to Color(0xFF1D4ED8),
)

@Composable
private fun VariablePill(name: String) {
    val (background, text) = pillColors[Math.floorMod(name.hashCode(), pillColors.size)]
    Text(
        text = name,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = text,
        modifier = Modifier
            .background(background, RoundedCornerShape(9.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}
