package com.opentasker.ui.screens

import androidx.annotation.StringRes
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
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.opentasker.app.R
import com.opentasker.core.model.ActionSpec
import com.opentasker.core.model.Task

/** A ready-made shortcut: its name, a line about it, and the steps it starts with. */
internal data class GalleryShortcut(
    val id: Long,
    @StringRes val nameRes: Int,
    @StringRes val descriptionRes: Int,
    val actions: (title: (Int) -> String) -> List<ActionSpec>,
)

private fun step(type: String, vararg args: Pair<String, String>) = ActionSpec(type = type, args = linkedMapOf(*args))

/** Built from the Shortcuts-style steps; every one works without extra setup. */
internal val galleryShortcuts = listOf(
    GalleryShortcut(1, R.string.gallery_translate_name, R.string.gallery_translate_description) { t ->
        listOf(
            step("input.ask", "title" to t(R.string.gallery_translate_question), "type" to "multiline", "var" to "Input"),
            step("url.open", "url" to "https://translate.google.com/?sl=auto&tl=en&op=translate&text={{ Input }}"),
        )
    },
    GalleryShortcut(2, R.string.gallery_timer_name, R.string.gallery_timer_description) { t ->
        listOf(
            step("input.ask", "title" to t(R.string.gallery_timer_question), "type" to "number", "default" to "5", "var" to "Minutes"),
            step("math.calculate", "expression" to "{{ Minutes }} * 60", "var" to "Seconds"),
            step("timer.set", "seconds" to "{{ Seconds }}", "skip_ui" to "true"),
        )
    },
    GalleryShortcut(3, R.string.gallery_dice_name, R.string.gallery_dice_description) { t ->
        listOf(
            step("random.number", "min" to "1", "max" to "6", "var" to "Dice"),
            step("result.show", "title" to t(R.string.gallery_dice_name), "text" to "{{ Dice }}"),
        )
    },
    GalleryShortcut(4, R.string.gallery_battery_name, R.string.gallery_battery_description) { t ->
        listOf(
            step("battery.get", "var" to "Battery"),
            step("result.show", "title" to t(R.string.gallery_battery_name), "text" to "{{ Battery }}%"),
        )
    },
    GalleryShortcut(5, R.string.gallery_share_name, R.string.gallery_share_description) { t ->
        listOf(
            step("input.ask", "title" to t(R.string.gallery_share_question), "type" to "multiline", "var" to "Input"),
            step("share.text", "text" to "{{ Input }}"),
        )
    },
    GalleryShortcut(6, R.string.gallery_home_name, R.string.gallery_home_description) { t ->
        listOf(step("maps.directions", "destination" to t(R.string.gallery_home_destination), "mode" to "driving"))
    },
    GalleryShortcut(7, R.string.gallery_alarm_name, R.string.gallery_alarm_description) { t ->
        listOf(step("alarm.set", "hour" to "7", "minute" to "0", "label" to t(R.string.gallery_alarm_name), "skip_ui" to "true"))
    },
    GalleryShortcut(8, R.string.gallery_coin_name, R.string.gallery_coin_description) { t ->
        listOf(
            step("random.number", "min" to "1", "max" to "2", "var" to "Coin"),
            step("result.show", "title" to t(R.string.gallery_coin_name), "text" to t(R.string.gallery_coin_result)),
        )
    },
)

/** Shortcuts-style Gallery: ready-made shortcuts, each added with one tap. */
@Composable
internal fun GalleryScreen(
    contentPadding: PaddingValues,
    onAdd: (GalleryShortcut) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text(
                text = stringResource(R.string.gallery_intro),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        items(galleryShortcuts, key = { it.id }) { shortcut ->
            val firstType = shortcut.actions { "" }.firstOrNull()?.type.orEmpty()
            Surface(
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
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(shortcutColor(shortcut.id), RoundedCornerShape(13.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            shortcutIcon(Task(name = "", actions = listOf(ActionSpec(type = firstType)))),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(shortcut.nameRes), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(
                            stringResource(shortcut.descriptionRes),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    FilledTonalButton(onClick = { onAdd(shortcut) }) {
                        Text(stringResource(R.string.gallery_add))
                    }
                }
            }
        }
    }
}
