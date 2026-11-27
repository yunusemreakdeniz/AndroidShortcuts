package com.opentasker.ui.screens

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.opentasker.app.R

/** Gallery shortcuts offered on first launch: useful at once and need no setup. */
internal val starterShortcutIds = listOf(4L, 2L, 10L)

/**
 * First launch, Shortcuts style: what shortcuts and automations are in three lines, then either
 * a few ready shortcuts to start from or an empty list.
 */
@Composable
internal fun WelcomeDialog(onStarters: () -> Unit, onEmpty: () -> Unit) {
    AlertDialog(
        onDismissRequest = onEmpty,
        title = { Text(stringResource(R.string.welcome_title), fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                WelcomeRow(Icons.Outlined.PlayArrow, Color(0xFF3B3BDB), R.string.welcome_shortcuts_title, R.string.welcome_shortcuts_body)
                WelcomeRow(Icons.Outlined.Bolt, Color(0xFF0F766E), R.string.welcome_automation_title, R.string.welcome_automation_body)
                WelcomeRow(Icons.Outlined.AutoAwesome, Color(0xFFC2410C), R.string.welcome_gallery_title, R.string.welcome_gallery_body)
            }
        },
        confirmButton = { FilledTonalButton(onClick = onStarters) { Text(stringResource(R.string.welcome_starters)) } },
        dismissButton = { TextButton(onClick = onEmpty) { Text(stringResource(R.string.welcome_empty)) } },
    )
}

@Composable
private fun WelcomeRow(icon: ImageVector, color: Color, @StringRes title: Int, @StringRes body: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(color, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
        }
        Column {
            Text(stringResource(title), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(stringResource(body), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
