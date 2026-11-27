package com.opentasker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.opentasker.app.R
import com.opentasker.core.actions.ActionField
import com.opentasker.core.actions.ActionMetadata
import com.opentasker.core.actions.FieldType
import com.opentasker.core.model.ActionSpec

/** Field types edited right inside the block; anything else goes to the full editor. */
private val INLINE_TYPES = setOf(FieldType.TEXT, FieldType.NUMBER, FieldType.MULTILINE, FieldType.CHECKBOX, FieldType.DROPDOWN)

/** True when every field of the action can be edited inside its block. */
internal fun editableInline(metadata: ActionMetadata): Boolean = metadata.fields.all { it.fieldType in INLINE_TYPES }

/**
 * Shortcuts-style editing in place: the step's fields open inside its block, and the outputs of
 * the steps above sit underneath as variables to tap in (the "magic variables").
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun InlineActionFields(
    action: ActionSpec,
    metadata: ActionMetadata,
    variables: List<String>,
    onDone: (ActionSpec) -> Unit,
    onMore: () -> Unit,
) {
    val draft = remember(action) { mutableStateMapOf<String, String>().apply { putAll(action.args) } }
    var focused by remember(action) { mutableStateOf<String?>(null) }
    var missing by remember(action) { mutableStateOf<ActionField?>(null) }
    val textKeys = metadata.fields.filter { it.fieldType in setOf(FieldType.TEXT, FieldType.MULTILINE, FieldType.NUMBER) }.map { it.key }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 10.dp)) {
        metadata.fields.filter { it.fieldType in INLINE_TYPES }.forEach { field ->
            val label = stringResource(field.labelRes)
            when (field.fieldType) {
                FieldType.CHECKBOX -> Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    Switch(
                        checked = draft[field.key] == "true",
                        onCheckedChange = { draft[field.key] = it.toString() },
                    )
                }
                FieldType.DROPDOWN -> Column {
                    Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        field.options.forEach { option ->
                            FilterChip(
                                selected = draft[field.key] == option.value,
                                onClick = { draft[field.key] = option.value },
                                label = { Text(stringResource(option.labelRes)) },
                            )
                        }
                    }
                }
                else -> OutlinedTextField(
                    value = draft[field.key].orEmpty(),
                    onValueChange = { draft[field.key] = it },
                    label = { Text(label) },
                    placeholder = field.hintRes?.let { hint -> { Text(stringResource(hint)) } },
                    singleLine = field.fieldType != FieldType.MULTILINE,
                    minLines = if (field.fieldType == FieldType.MULTILINE) 2 else 1,
                    isError = missing == field,
                    keyboardOptions = if (field.fieldType == FieldType.NUMBER) KeyboardOptions(keyboardType = KeyboardType.Number) else KeyboardOptions.Default,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { if (it.isFocused) focused = field.key },
                )
            }
        }

        if (variables.isNotEmpty() && textKeys.isNotEmpty()) {
            Text(
                stringResource(R.string.inline_variables),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                variables.forEach { name ->
                    AssistChip(
                        onClick = {
                            val key = focused?.takeIf { it in textKeys } ?: textKeys.first()
                            val current = draft[key].orEmpty()
                            draft[key] = (if (current.isEmpty() || current.endsWith(" ")) current else "$current ") + "{{ $name }}"
                        },
                        label = { Text(name) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = Color(0xFFFFE8D9),
                            labelColor = Color(0xFF9A3412),
                        ),
                    )
                }
            }
        }

        missing?.let {
            Text(
                stringResource(R.string.inline_required, stringResource(it.labelRes)),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            FilledTonalButton(onClick = {
                val empty = metadata.fields.firstOrNull { it.required && draft[it.key].isNullOrBlank() }
                if (empty != null) {
                    missing = empty
                } else {
                    onDone(action.copy(args = draft.filterValues { it.isNotEmpty() }))
                }
            }) { Text(stringResource(R.string.inline_done)) }
            TextButton(onClick = onMore) { Text(stringResource(R.string.inline_more)) }
        }
    }
}
