package com.opentasker.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.ui.semantics.Role
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.opentasker.app.R
import com.opentasker.core.model.ContextSpec
import com.opentasker.core.model.ContextType
import com.opentasker.core.model.Task

/** Trigger types the simple wizard can set up itself; the rest go to the detailed editor. */
internal fun wizardSupports(type: ContextType): Boolean =
    type in setOf(ContextType.TIME, ContextType.DAY, ContextType.STATE, ContextType.APPLICATION)

private val DAY_CODES = listOf(
    "MON" to R.string.context_day_mon, "TUE" to R.string.context_day_tue, "WED" to R.string.context_day_wed,
    "THU" to R.string.context_day_thu, "FRI" to R.string.context_day_fri, "SAT" to R.string.context_day_sat,
    "SUN" to R.string.context_day_sun,
)

/** The trigger a time and day choice becomes: a one-minute window, plus the days unless every day. */
internal fun timeTrigger(hour: Int, minute: Int, days: Set<String>): List<ContextSpec> {
    val start = "%02d:%02d".format(hour, minute)
    val endMinutes = (hour * 60 + minute + 1) % (24 * 60)
    val end = "%02d:%02d".format(endMinutes / 60, endMinutes % 60)
    val time = ContextSpec(type = ContextType.TIME, config = mapOf("start" to start, "end" to end))
    if (days.isEmpty() || days.size == 7) return listOf(time)
    val ordered = DAY_CODES.map { it.first }.filter { it in days }
    return listOf(time, ContextSpec(type = ContextType.DAY, config = mapOf("days" to ordered.joinToString(","))))
}

internal fun batteryTrigger(below: Int) =
    ContextSpec(type = ContextType.STATE, config = mapOf("key" to "battery_level", "operator" to "<=", "value" to below.toString()))

internal fun chargingTrigger(connected: Boolean) =
    ContextSpec(type = ContextType.STATE, config = mapOf("key" to "charging", "operator" to "=", "value" to connected.toString()))

/**
 * Shortcuts-style "New automation": first "When?" in plain words (a time and days, charging,
 * battery, an app), then which shortcut to run. [onCreate] gets the triggers, or null when the
 * type needs the detailed editor (location, events).
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun AutomationWizardDialog(
    type: ContextType,
    tasks: List<Task>,
    onDismiss: () -> Unit,
    onCreate: (List<ContextSpec>?, Task) -> Unit,
) {
    var triggers by remember(type) { mutableStateOf<List<ContextSpec>?>(null) }
    if (!wizardSupports(type) || triggers != null) {
        NewAutomationShortcutDialog(type = type, tasks = tasks, onDismiss = onDismiss, onPick = { onCreate(triggers, it) })
        return
    }
    val time = rememberTimePickerState(initialHour = 7, initialMinute = 0, is24Hour = true)
    var days by remember { mutableStateOf(DAY_CODES.map { it.first }.toSet()) }
    var stateChoice by remember { mutableStateOf("charging_on") }
    var battery by remember { mutableFloatStateOf(20f) }
    var app by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val apps = remember(type) {
        if (type != ContextType.APPLICATION) emptyList() else runCatching {
            val pm = context.packageManager
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
                .map { it.activityInfo.packageName to it.loadLabel(pm).toString() }
                .filter { it.first != context.packageName }
                .distinctBy { it.first }
                .sortedBy { it.second.lowercase() }
        }.getOrDefault(emptyList())
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.wizard_when), fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                if (type == ContextType.TIME) {
                    TimeInput(state = time)
                }
                if (type == ContextType.TIME || type == ContextType.DAY) {
                    Text(stringResource(R.string.wizard_days), style = MaterialTheme.typography.labelLarge)
                    val all = DAY_CODES.map { it.first }.toSet()
                    val weekdays = setOf("MON", "TUE", "WED", "THU", "FRI")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(
                            all to R.string.wizard_every_day,
                            weekdays to R.string.wizard_weekdays,
                            setOf("SAT", "SUN") to R.string.wizard_weekend,
                        ).forEach { (set, label) ->
                            FilterChip(
                                selected = days == set,
                                onClick = { days = set },
                                label = { Text(stringResource(label)) },
                                leadingIcon = if (days == set) { { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp)) } } else null,
                            )
                        }
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        DAY_CODES.forEach { (code, label) ->
                            FilterChip(
                                selected = code in days,
                                onClick = { days = if (code in days) days - code else days + code },
                                label = { Text(stringResource(label)) },
                                leadingIcon = if (code in days) { { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp)) } } else null,
                            )
                        }
                    }
                }
                if (type == ContextType.STATE) {
                    listOf(
                        "charging_on" to R.string.wizard_charging_on,
                        "charging_off" to R.string.wizard_charging_off,
                        "battery_low" to R.string.wizard_battery_low,
                    ).forEach { (key, label) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(selected = stateChoice == key, role = Role.RadioButton, onClick = { stateChoice = key }),
                        ) {
                            RadioButton(selected = stateChoice == key, onClick = null)
                            Text(stringResource(label), modifier = Modifier.padding(start = 8.dp, top = 12.dp, bottom = 12.dp))
                        }
                    }
                    if (stateChoice == "battery_low") {
                        Text(stringResource(R.string.wizard_battery_value, battery.toInt()))
                        Slider(value = battery, onValueChange = { battery = it }, valueRange = 5f..50f, steps = 8)
                    }
                }
                if (type == ContextType.APPLICATION) {
                    Text(stringResource(R.string.wizard_app), style = MaterialTheme.typography.labelLarge)
                    LazyColumn(modifier = Modifier.heightIn(max = 320.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(apps, key = { it.first }) { (pkg, label) ->
                            Surface(
                                onClick = { app = pkg },
                                shape = RoundedCornerShape(10.dp),
                                color = if (app == pkg) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(label, modifier = Modifier.padding(12.dp))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            FilledTonalButton(
                enabled = when (type) {
                    ContextType.DAY -> days.isNotEmpty()
                    ContextType.APPLICATION -> app != null
                    else -> true
                },
                onClick = {
                    triggers = when (type) {
                        ContextType.TIME -> timeTrigger(time.hour, time.minute, days)
                        ContextType.DAY -> listOf(
                            ContextSpec(type = ContextType.DAY, config = mapOf("days" to DAY_CODES.map { it.first }.filter { it in days }.joinToString(","))),
                        )
                        ContextType.STATE -> listOf(
                            when (stateChoice) {
                                "charging_off" -> chargingTrigger(false)
                                "battery_low" -> batteryTrigger(battery.toInt())
                                else -> chargingTrigger(true)
                            },
                        )
                        else -> listOf(ContextSpec(type = ContextType.APPLICATION, config = mapOf("package" to app.orEmpty())))
                    }
                },
            ) { Text(stringResource(R.string.wizard_next)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}
