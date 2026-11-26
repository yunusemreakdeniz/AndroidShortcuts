package com.opentasker.ui.screens

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.BatteryStd
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Photo
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material.icons.outlined.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.opentasker.app.R
import com.opentasker.core.model.Task
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** A shortcut's chosen look. Null parts fall back to the automatic choice. */
internal data class ShortcutStyle(val colorIndex: Int? = null, val iconKey: String? = null)

/** Icons a shortcut can wear, keyed by a stable name that is stored. */
internal val shortcutIconChoices: List<Pair<String, ImageVector>> = listOf(
    "bolt" to Icons.Outlined.Bolt, "chat" to Icons.Outlined.ChatBubbleOutline, "web" to Icons.Outlined.Language,
    "bell" to Icons.Outlined.Notifications, "music" to Icons.Outlined.MusicNote, "alarm" to Icons.Outlined.Alarm,
    "timer" to Icons.Outlined.Timer, "battery" to Icons.Outlined.BatteryStd, "calc" to Icons.Outlined.Calculate,
    "share" to Icons.Outlined.Share, "map" to Icons.Outlined.Map, "mail" to Icons.Outlined.Email,
    "phone" to Icons.Outlined.Phone, "wifi" to Icons.Outlined.Wifi, "home" to Icons.Outlined.Home,
    "star" to Icons.Outlined.StarOutline, "heart" to Icons.Outlined.FavoriteBorder, "work" to Icons.Outlined.Work,
    "school" to Icons.Outlined.School, "car" to Icons.Outlined.DirectionsCar, "camera" to Icons.Outlined.PhotoCamera,
    "photo" to Icons.Outlined.Photo, "translate" to Icons.Outlined.Translate, "idea" to Icons.Outlined.Lightbulb,
)

/**
 * Stores each shortcut's look in the app's own preferences, keyed by task id, so the look needs
 * no database migration and survives renames. Removing a shortcut just leaves an unused entry.
 */
internal object ShortcutStyles {
    private const val PREFS = "shortcut_styles"
    private val state = MutableStateFlow<Map<Long, ShortcutStyle>>(emptyMap())
    @Volatile private var loaded = false

    fun styles(context: Context): StateFlow<Map<Long, ShortcutStyle>> {
        if (!loaded) {
            loaded = true
            // Previews and screenshot tests have no real preferences; fall back to automatic looks.
            val prefs = runCatching { context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }.getOrNull()
                ?: return state
            state.value = prefs.all.mapNotNull { (key, value) ->
                val id = key.toLongOrNull() ?: return@mapNotNull null
                val parts = (value as? String)?.split('|') ?: return@mapNotNull null
                id to ShortcutStyle(parts.getOrNull(0)?.toIntOrNull(), parts.getOrNull(1)?.takeIf(String::isNotEmpty))
            }.toMap()
        }
        return state
    }

    fun set(context: Context, taskId: Long, style: ShortcutStyle) {
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(taskId.toString(), "${style.colorIndex ?: ""}|${style.iconKey.orEmpty()}")
            .apply()
        state.value = state.value + (taskId to style)
    }
}

@Composable
internal fun rememberShortcutStyles(): Map<Long, ShortcutStyle> {
    val context = LocalContext.current
    val flow = remember { ShortcutStyles.styles(context) }
    return flow.collectAsState().value
}

internal fun styledColor(task: Task, styles: Map<Long, ShortcutStyle>): Color =
    styles[task.id]?.colorIndex?.let { shortcutPaletteColor(it) } ?: shortcutColor(task.id)

internal fun styledIcon(task: Task, styles: Map<Long, ShortcutStyle>): ImageVector =
    styles[task.id]?.iconKey?.let { key -> shortcutIconChoices.firstOrNull { it.first == key }?.second } ?: shortcutIcon(task)

/** Colour and icon picker for one shortcut. */
@Composable
internal fun ShortcutStyleDialog(task: Task, current: ShortcutStyle, onDismiss: () -> Unit, onSave: (ShortcutStyle) -> Unit) {
    var colorIndex by remember { mutableStateOf(current.colorIndex) }
    var iconKey by remember { mutableStateOf(current.iconKey) }
    val previewColor = colorIndex?.let { shortcutPaletteColor(it) } ?: shortcutColor(task.id)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.shortcut_style_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.shortcut_style_color), style = MaterialTheme.typography.titleSmall)
                shortcutPaletteIndices.chunked(4).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEach { index ->
                            val label = stringResource(R.string.shortcut_style_color_option, index + 1)
                            Surface(
                                onClick = { colorIndex = index },
                                shape = CircleShape,
                                color = shortcutPaletteColor(index),
                                border = if (colorIndex == index) BorderStroke(3.dp, MaterialTheme.colorScheme.onSurface) else null,
                                modifier = Modifier
                                    .size(44.dp)
                                    .semantics { contentDescription = label; selected = colorIndex == index },
                            ) {
                                if (colorIndex == index) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }
                Text(stringResource(R.string.shortcut_style_icon), style = MaterialTheme.typography.titleSmall)
                shortcutIconChoices.chunked(6).forEachIndexed { rowIndex, row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        row.forEachIndexed { columnIndex, (key, icon) ->
                            val label = stringResource(R.string.shortcut_style_icon_option, rowIndex * 6 + columnIndex + 1)
                            val chosen = iconKey == key
                            Surface(
                                onClick = { iconKey = key },
                                shape = RoundedCornerShape(12.dp),
                                color = if (chosen) previewColor else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (chosen) Color.White else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .size(44.dp)
                                    .semantics { contentDescription = label; selected = chosen },
                            ) {
                                Box(contentAlignment = Alignment.Center) { Icon(icon, contentDescription = null) }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(ShortcutStyle(colorIndex, iconKey)) }) { Text(stringResource(R.string.shortcut_style_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_close)) } },
    )
}
