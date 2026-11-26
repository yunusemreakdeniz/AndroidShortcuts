package com.opentasker.ui.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.opentasker.core.model.ActionSpec
import com.opentasker.core.model.ContextSpec
import com.opentasker.core.model.ContextType
import com.opentasker.core.model.Profile
import com.opentasker.core.model.Task
import com.opentasker.ui.theme.OpenTaskerTheme

/**
 * Kısayollar' own screens rendered in Turkish at phone size, light and dark. CI writes these as
 * PNGs (updateDebugScreenshotTest) and uploads them, so every screen can be reviewed per build.
 */

private val sampleTasks = listOf(
    Task(id = 1, name = "Metni çevir", actions = listOf(
        ActionSpec(type = "input.ask", args = mapOf("title" to "Çevrilecek metin", "var" to "Input")),
        ActionSpec(type = "url.open", args = mapOf("url" to "https://translate.google.com/?text={{ Input }}")),
    )),
    Task(id = 2, name = "Hızlı zamanlayıcı", actions = listOf(ActionSpec(type = "timer.set", args = mapOf("seconds" to "300")))),
    Task(id = 3, name = "Pil durumu", actions = listOf(
        ActionSpec(type = "battery.get", args = mapOf("var" to "Battery")),
        ActionSpec(type = "result.show", args = mapOf("text" to "{{ Battery }}%")),
    )),
    Task(id = 4, name = "Eve yol tarifi", actions = listOf(ActionSpec(type = "maps.directions", args = mapOf("destination" to "Ev")))),
    Task(id = 5, name = "Günaydın", actions = listOf(ActionSpec(type = "tts.speak", args = mapOf("text" to "Günaydın")))),
)

private val sampleProfiles = listOf(
    Profile(id = 1, name = "Sabah", enabled = true, enterTaskId = 5, contexts = listOf(ContextSpec(type = ContextType.TIME))),
    Profile(id = 2, name = "Şarja takılınca", enabled = false, enterTaskId = 3, contexts = listOf(ContextSpec(type = ContextType.STATE))),
)

@Composable
private fun Frame(dark: Boolean, content: @Composable () -> Unit) {
    OpenTaskerTheme(darkTheme = dark) {
        Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) { content() }
    }
}

@PreviewTest
@Preview(name = "Kisayollar Home Light", locale = "tr", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
fun KisayollarHomeLight() = Frame(false) {
    ShortcutsHomeScreen(sampleTasks, PaddingValues(), onRunTask = {}, onOpenTask = {}, onCreateTask = {})
}

@PreviewTest
@Preview(name = "Kisayollar Home Dark", locale = "tr", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
fun KisayollarHomeDark() = Frame(true) {
    ShortcutsHomeScreen(sampleTasks, PaddingValues(), onRunTask = {}, onOpenTask = {}, onCreateTask = {})
}

@PreviewTest
@Preview(name = "Kisayollar Home Empty", locale = "tr", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
fun KisayollarHomeEmpty() = Frame(false) {
    ShortcutsHomeScreen(emptyList(), PaddingValues(), onRunTask = {}, onOpenTask = {}, onCreateTask = {})
}

@PreviewTest
@Preview(name = "Kisayollar Editor Light", locale = "tr", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
fun KisayollarEditorLight() = Frame(false) {
    ShortcutEditorScreen(
        task = sampleTasks[0], contentPadding = PaddingValues(), onBack = {}, onRun = {}, onOpenDetails = {},
        onRename = {}, onDelete = {}, onAddAction = {}, onEditAction = { _, _ -> }, onMoveAction = { _, _ -> }, onDeleteAction = {},
    )
}

@PreviewTest
@Preview(name = "Kisayollar Editor Dark", locale = "tr", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
fun KisayollarEditorDark() = Frame(true) {
    ShortcutEditorScreen(
        task = sampleTasks[2], contentPadding = PaddingValues(), onBack = {}, onRun = {}, onOpenDetails = {},
        onRename = {}, onDelete = {}, onAddAction = {}, onEditAction = { _, _ -> }, onMoveAction = { _, _ -> }, onDeleteAction = {},
    )
}

@PreviewTest
@Preview(name = "Kisayollar Automation Light", locale = "tr", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
fun KisayollarAutomationLight() = Frame(false) {
    AutomationScreen(sampleProfiles, sampleTasks, PaddingValues(), onToggle = { _, _ -> }, onOpen = {}, onEditTriggers = {}, onNewAutomation = {})
}

@PreviewTest
@Preview(name = "Kisayollar Gallery Light", locale = "tr", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
fun KisayollarGalleryLight() = Frame(false) {
    GalleryScreen(PaddingValues(), onAdd = {})
}

@PreviewTest
@Preview(name = "Kisayollar Gallery Dark", locale = "tr", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
fun KisayollarGalleryDark() = Frame(true) {
    GalleryScreen(PaddingValues(), onAdd = {})
}
