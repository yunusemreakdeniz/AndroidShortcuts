package com.opentasker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.opentasker.app.R
import com.opentasker.core.actions.ActionMetadata
import com.opentasker.core.capabilities.ActionCapabilityRegistry
import com.opentasker.core.capabilities.CapabilityLevel
import com.opentasker.core.model.ActionSpec
import com.opentasker.core.model.Task

/** The steps most shortcuts start with, shown first like the Shortcuts app's suggestions. */
private val SUGGESTED_ACTION_IDS = listOf(
    "input.ask", "menu.choose", "result.show", "http.request", "url.open",
    "notify.show", "flow.if", "var.set", "clipboard.set", "share.text",
)

private const val SUGGESTED = "suggested"

/** Shortcuts-style "Add action" sheet: category chips, search, coloured rows. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ActionPickerSheet(
    query: String,
    onQueryChange: (String) -> Unit,
    actions: List<LocalizedActionMetadata>,
    onDismiss: () -> Unit,
    onSelect: (ActionMetadata) -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        ActionPickerContent(query, onQueryChange, actions, onSelect, Modifier.fillMaxHeight(0.92f))
    }
}

/** The sheet's content, separate so previews and screenshot tests can render it without a window. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ActionPickerContent(
    query: String,
    onQueryChange: (String) -> Unit,
    actions: List<LocalizedActionMetadata>,
    onSelect: (ActionMetadata) -> Unit,
    modifier: Modifier = Modifier,
) {
    var category by rememberSaveable { mutableStateOf(SUGGESTED) }
    val categories = remember(actions) { actions.map { it.category }.distinct().sorted() }
    val shown = remember(actions, category, query) {
        when {
            query.isNotBlank() -> actions.sortedBy { it.name }
            category == SUGGESTED -> SUGGESTED_ACTION_IDS.mapNotNull { id -> actions.firstOrNull { it.metadata.id == id } }
            else -> actions.filter { it.category == category }.sortedBy { it.name }
        }
    }
    run {
        Column(modifier = modifier) {
            Text(
                text = stringResource(R.string.dialog_add_action),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                placeholder = { Text(stringResource(R.string.action_picker_search_hint)) },
                label = { Text(stringResource(R.string.action_picker_search_label)) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
            if (query.isBlank()) {
                Row(
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = category == SUGGESTED,
                        onClick = { category = SUGGESTED },
                        label = { Text(stringResource(R.string.action_picker_suggested)) },
                    )
                    categories.forEach { name ->
                        FilterChip(selected = category == name, onClick = { category = name }, label = { Text(name) })
                    }
                }
            }
            if (shown.isEmpty()) {
                Text(
                    text = stringResource(R.string.action_picker_no_results),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(24.dp),
                )
            }
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(shown, key = { it.metadata.id }) { item ->
                    ActionPickerRow(item = item, onSelect = { onSelect(item.metadata) })
                }
            }
        }
    }
}

@Composable
private fun ActionPickerRow(item: LocalizedActionMetadata, onSelect: () -> Unit) {
    val capability = ActionCapabilityRegistry.get(item.metadata.id)
    Surface(
        onClick = onSelect,
        enabled = capability.canAdd,
        shape = RoundedCornerShape(16.dp),
        color = shortcutCardColor(),
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(actionColor(item.metadata.id), RoundedCornerShape(11.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    actionIcon(item.metadata.id),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(item.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(
                    text = if (capability.canAdd) item.description else stringResource(capability.reasonRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (capability.canAdd) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (capability.canAdd && capability.level != CapabilityLevel.Supported) {
                    Text(
                        text = stringResource(R.string.action_picker_needs_setup),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
            }
            if (capability.canAdd) {
                Icon(Icons.Filled.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
