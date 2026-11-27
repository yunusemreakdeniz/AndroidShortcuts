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
    Profile(id = 1, name = "Sabah", enabled = true, enterTaskId = 5, contexts = listOf(ContextSpec(type = ContextType.TIME, config = mapOf("start" to "07:00", "end" to "08:00")))),
    Profile(id = 2, name = "Şarja takılınca", enabled = false, enterTaskId = 3, contexts = listOf(ContextSpec(type = ContextType.STATE))),
)

private val catalogue by lazy { runCatching { com.opentasker.core.actions.registerActionMetadata() } }

@Composable
private fun Frame(dark: Boolean, content: @Composable () -> Unit) {
    catalogue
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

@PreviewTest
@Preview(name = "Kisayollar Home Large Text", locale = "tr", widthDp = 390, heightDp = 844, fontScale = 1.5f, showBackground = true)
@Composable
fun KisayollarHomeLargeText() = Frame(false) {
    ShortcutsHomeScreen(sampleTasks, PaddingValues(), onRunTask = {}, onOpenTask = {}, onCreateTask = {})
}

@PreviewTest
@Preview(name = "Kisayollar Editor Large Text", locale = "tr", widthDp = 390, heightDp = 844, fontScale = 1.5f, showBackground = true)
@Composable
fun KisayollarEditorLargeText() = Frame(false) {
    ShortcutEditorScreen(
        task = sampleTasks[0], contentPadding = PaddingValues(), onBack = {}, onRun = {}, onOpenDetails = {},
        onRename = {}, onDelete = {}, onAddAction = {}, onEditAction = { _, _ -> }, onMoveAction = { _, _ -> }, onDeleteAction = {},
    )
}

@PreviewTest
@Preview(name = "Kisayollar New Shortcut Dialog", locale = "tr", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
fun KisayollarNewShortcutDialog() = Frame(false) {
    ShortcutNameDialog(title = "Yeni kısayol", confirmLabel = "Oluştur", initialName = "", existingNames = emptyList(), onDismiss = {}, onConfirm = {})
}

@PreviewTest
@Preview(name = "Kisayollar Style Dialog", locale = "tr", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
fun KisayollarStyleDialog() = Frame(false) {
    ShortcutStyleDialog(task = sampleTasks[0], current = ShortcutStyle(colorIndex = 1, iconKey = "translate"), onDismiss = {}, onSave = {})
}

@PreviewTest
@Preview(name = "Kisayollar Automation Dark", locale = "tr", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
fun KisayollarAutomationDark() = Frame(true) {
    AutomationScreen(sampleProfiles, sampleTasks, PaddingValues(), onToggle = { _, _ -> }, onOpen = {}, onEditTriggers = {}, onNewAutomation = {})
}

@Composable
private fun PickerPreview(query: String) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val all = com.opentasker.core.actions.ActionMetadataRegistry.all().filter { it.pickerVisible }.map {
        LocalizedActionMetadata(it, context.getString(it.nameRes), context.getString(it.descriptionRes), context.getString(it.categoryRes))
    }
    ActionPickerContent(query, {}, filterActionPickerItems(all, query), {})
}

@PreviewTest
@Preview(name = "Kisayollar Action Picker", locale = "tr", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
fun KisayollarActionPicker() = Frame(false) { PickerPreview("") }

@PreviewTest
@Preview(name = "Kisayollar Action Picker Search Dark", locale = "tr", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
fun KisayollarActionPickerSearchDark() = Frame(true) { PickerPreview("pil") }

@PreviewTest
@Preview(name = "Kisayollar Inline Edit", locale = "tr", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
fun KisayollarInlineEdit() = Frame(false) {
    val metadata = com.opentasker.core.actions.ActionMetadataRegistry.get("result.show")
    if (metadata != null) {
        androidx.compose.foundation.layout.Column(modifier = Modifier.fillMaxSize()) {
            InlineActionFields(
                action = sampleTasks[2].actions[1],
                metadata = metadata,
                variables = listOf("Battery", "Input"),
                onDone = {},
                onMore = {},
            )
        }
    }
}

@PreviewTest
@Preview(name = "Kisayollar Welcome", locale = "tr", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
fun KisayollarWelcome() = Frame(false) {
    WelcomeDialog(onStarters = {}, onEmpty = {})
}

@PreviewTest
@Preview(name = "Kisayollar Wizard Time", locale = "tr", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
fun KisayollarWizardTime() = Frame(false) {
    AutomationWizardDialog(type = ContextType.TIME, tasks = sampleTasks, onDismiss = {}, onCreate = { _, _ -> })
}

@PreviewTest
@Preview(name = "Kisayollar Wizard State Dark", locale = "tr", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
fun KisayollarWizardStateDark() = Frame(true) {
    AutomationWizardDialog(type = ContextType.STATE, tasks = sampleTasks, onDismiss = {}, onCreate = { _, _ -> })
}

private val menuTask = Task(id = 9, name = "Kahve mi çay mı", actions = appendWithBlockCloser(
    emptyList(),
    ActionSpec(type = "menu.choose", args = mapOf("title" to "Ne içelim?", "items" to "Kahve\nÇay")),
).toMutableList().apply {
    add(2, ActionSpec(type = "result.show", args = mapOf("text" to "Kahve hazırlanıyor")))
    add(5, ActionSpec(type = "result.show", args = mapOf("text" to "Çay demleniyor")))
})

@PreviewTest
@Preview(name = "Kisayollar Editor Blocks", locale = "tr", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
fun KisayollarEditorBlocks() = Frame(false) {
    ShortcutEditorScreen(
        task = menuTask, contentPadding = PaddingValues(), onBack = {}, onRun = {}, onOpenDetails = {},
        onRename = {}, onDelete = {}, onAddAction = {}, onEditAction = { _, _ -> }, onMoveAction = { _, _ -> }, onDeleteAction = {},
    )
}
