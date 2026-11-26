package com.opentasker.core.actions

import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import com.opentasker.core.engine.ActionContext
import com.opentasker.core.engine.ActionResult
import com.opentasker.core.model.VariableNamePolicy
import com.opentasker.core.platform.BackgroundActivityStart
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Shortcuts-style steps that talk to the person running the task: ask for text, choose from a
 * menu, show an alert, show a result. Each one opens [InteractiveDialogActivity] and suspends until
 * it answers. Dismissing a dialog (Cancel, back, tapping outside) stops the task, the way Apple
 * Shortcuts does, unless the step is set to continue after failure.
 */
internal object InteractiveDialogCoordinator {
    const val EXTRA_REQUEST_ID = "com.opentasker.dialog.REQUEST_ID"
    const val EXTRA_KIND = "com.opentasker.dialog.KIND"
    const val EXTRA_TITLE = "com.opentasker.dialog.TITLE"
    const val EXTRA_MESSAGE = "com.opentasker.dialog.MESSAGE"
    const val EXTRA_ITEMS = "com.opentasker.dialog.ITEMS"
    const val EXTRA_INPUT_TYPE = "com.opentasker.dialog.INPUT_TYPE"
    const val EXTRA_DEFAULT = "com.opentasker.dialog.DEFAULT"
    const val EXTRA_CANCELABLE = "com.opentasker.dialog.CANCELABLE"

    const val KIND_INPUT = "input"
    const val KIND_MENU = "menu"
    const val KIND_ALERT = "alert"
    const val KIND_RESULT = "result"
    const val KIND_LIST_MULTI = "list_multi"
    const val KIND_DATE = "date"
    const val KIND_DICTATE = "dictate"
    const val KIND_PHOTO = "photo"
    const val KIND_FILE = "file"
    const val EXTRA_DATE_MODE = "com.opentasker.dialog.DATE_MODE"
    const val EXTRA_LANGUAGE = "com.opentasker.dialog.LANGUAGE"

    /** How long the dialog may take to appear before we assume Android blocked it. */
    private const val SHOW_TIMEOUT_MS = 5_000L

    /** How long a person may take to answer. Below the engine's step timeout for these actions. */
    const val ANSWER_TIMEOUT_MS = 9 * 60_000L

    private val nextId = AtomicLong(0)
    private val pending = ConcurrentHashMap<String, Pending>()

    private class Pending(
        val shown: CompletableDeferred<Unit> = CompletableDeferred(),
        val answer: CompletableDeferred<DialogAnswer> = CompletableDeferred(),
    )

    sealed class DialogAnswer {
        data class Accepted(val text: String = "", val index: Int = -1) : DialogAnswer()
        data object Dismissed : DialogAnswer()
        /** The step could not ask at all (no speech recognizer, for example). */
        data class Failed(val reason: String) : DialogAnswer()
    }

    sealed class Outcome {
        data class Answered(val answer: DialogAnswer) : Outcome()
        data object NotShown : Outcome()
        data object TimedOut : Outcome()
    }

    suspend fun ask(context: Context, fill: Intent.() -> Unit): Outcome {
        val requestId = "dialog-${nextId.incrementAndGet()}"
        val entry = Pending()
        pending[requestId] = entry
        try {
            context.startActivity(
                Intent(context, InteractiveDialogActivity::class.java)
                    .putExtra(EXTRA_REQUEST_ID, requestId)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
                    .apply(fill),
            )
            // Android silently drops activity starts from the background unless an exemption
            // applies (a tile, widget, notification or home-screen tap, or the app on screen).
            withTimeoutOrNull(SHOW_TIMEOUT_MS) { entry.shown.await() } ?: return Outcome.NotShown
            val answer = withTimeoutOrNull(ANSWER_TIMEOUT_MS) { entry.answer.await() } ?: return Outcome.TimedOut
            return Outcome.Answered(answer)
        } finally {
            pending.remove(requestId)
            InteractiveDialogActivity.closeIfOpen(requestId)
        }
    }

    fun markShown(requestId: String) {
        pending[requestId]?.shown?.complete(Unit)
    }

    fun answer(requestId: String, answer: DialogAnswer) {
        pending[requestId]?.answer?.complete(answer)
    }

    fun isPending(requestId: String): Boolean = pending.containsKey(requestId)
}

/** Translucent, non-exported host for the dialogs. It never outlives the step that opened it. */
class InteractiveDialogActivity : Activity() {
    private var requestId: String? = null
    private var dialog: AlertDialog? = null
    private var answered = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val id = intent.getStringExtra(InteractiveDialogCoordinator.EXTRA_REQUEST_ID)
        if (id == null || !InteractiveDialogCoordinator.isPending(id)) {
            finish()
            return
        }
        requestId = id
        open[id] = this
        InteractiveDialogCoordinator.markShown(id)
        when (intent.getStringExtra(InteractiveDialogCoordinator.EXTRA_KIND)) {
            InteractiveDialogCoordinator.KIND_DATE -> showDatePicker()
            InteractiveDialogCoordinator.KIND_DICTATE -> startDictation()
            InteractiveDialogCoordinator.KIND_PHOTO -> startPhotoPicker()
            InteractiveDialogCoordinator.KIND_FILE -> startFilePicker()
            else -> dialog = buildDialog().also { it.show() }
        }
    }

    private fun dialogTheme(): Int {
        val night = (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
            android.content.res.Configuration.UI_MODE_NIGHT_YES
        return if (night) android.R.style.Theme_DeviceDefault_Dialog_Alert else android.R.style.Theme_DeviceDefault_Light_Dialog_Alert
    }

    private fun showDatePicker() {
        val mode = intent.getStringExtra(InteractiveDialogCoordinator.EXTRA_DATE_MODE) ?: "date"
        val now = java.util.Calendar.getInstance()
        if (mode == "time") {
            showTimePicker(null)
            return
        }
        dialog = android.app.DatePickerDialog(
            this, dialogTheme(),
            { _, year, month, day ->
                val date = "%04d-%02d-%02d".format(year, month + 1, day)
                if (mode == "datetime") {
                    dialog?.setOnCancelListener(null)
                    showTimePicker(date)
                } else {
                    reply(InteractiveDialogCoordinator.DialogAnswer.Accepted(date))
                }
            },
            now.get(java.util.Calendar.YEAR), now.get(java.util.Calendar.MONTH), now.get(java.util.Calendar.DAY_OF_MONTH),
        ).apply {
            intent.getStringExtra(InteractiveDialogCoordinator.EXTRA_TITLE)?.takeIf(String::isNotBlank)?.let(::setTitle)
            setOnCancelListener { reply(InteractiveDialogCoordinator.DialogAnswer.Dismissed) }
            show()
        }
    }

    private fun showTimePicker(date: String?) {
        val now = java.util.Calendar.getInstance()
        dialog = android.app.TimePickerDialog(
            this, dialogTheme(),
            { _, hour, minute ->
                val time = "%02d:%02d".format(hour, minute)
                reply(InteractiveDialogCoordinator.DialogAnswer.Accepted(if (date == null) time else "$date $time"))
            },
            now.get(java.util.Calendar.HOUR_OF_DAY), now.get(java.util.Calendar.MINUTE),
            android.text.format.DateFormat.is24HourFormat(this),
        ).apply {
            setOnCancelListener { reply(InteractiveDialogCoordinator.DialogAnswer.Dismissed) }
            show()
        }
    }

    private fun startDictation() {
        val recognize = Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL, android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(android.speech.RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        intent.getStringExtra(InteractiveDialogCoordinator.EXTRA_TITLE)?.takeIf(String::isNotBlank)
            ?.let { recognize.putExtra(android.speech.RecognizerIntent.EXTRA_PROMPT, it) }
        intent.getStringExtra(InteractiveDialogCoordinator.EXTRA_LANGUAGE)?.takeIf(String::isNotBlank)
            ?.let { recognize.putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE, it) }
        try {
            @Suppress("DEPRECATION")
            startActivityForResult(recognize, DICTATION_REQUEST)
        } catch (_: android.content.ActivityNotFoundException) {
            reply(InteractiveDialogCoordinator.DialogAnswer.Failed("no speech recognizer is installed"))
        }
    }

    private fun startPhotoPicker() {
        val pick = if (android.os.Build.VERSION.SDK_INT >= 33) {
            Intent(android.provider.MediaStore.ACTION_PICK_IMAGES)
        } else {
            Intent(Intent.ACTION_GET_CONTENT).setType("image/*").addCategory(Intent.CATEGORY_OPENABLE)
        }
        try {
            @Suppress("DEPRECATION")
            startActivityForResult(pick, PHOTO_REQUEST)
        } catch (_: android.content.ActivityNotFoundException) {
            reply(InteractiveDialogCoordinator.DialogAnswer.Failed("no photo picker is available"))
        }
    }

    private fun startFilePicker() {
        val pick = Intent(Intent.ACTION_OPEN_DOCUMENT).setType("*/*").addCategory(Intent.CATEGORY_OPENABLE)
        try {
            @Suppress("DEPRECATION")
            startActivityForResult(pick, FILE_REQUEST)
        } catch (_: android.content.ActivityNotFoundException) {
            reply(InteractiveDialogCoordinator.DialogAnswer.Failed("no file picker is available"))
        }
    }

    /** The picked file's own name, made safe for the shortcut files folder. */
    private fun displayName(uri: android.net.Uri): String {
        val raw = runCatching {
            contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) c.getString(0) else null
            }
        }.getOrNull().orEmpty()
        val safe = raw.replace(Regex("[^A-Za-z0-9._ -]"), "_").trim().trimStart('.').take(80)
        return safe.ifEmpty { "file_${System.currentTimeMillis()}" }
    }

    /** Copies the picked photo into the shortcut files (Photos/), since the picker's grant is temporary. */
    private fun keepPhoto(uri: android.net.Uri) {
        val ext = when (contentResolver.getType(uri)) {
            "image/png" -> "png"
            "image/webp" -> "webp"
            "image/gif" -> "gif"
            "image/heic", "image/heif" -> "heic"
            else -> "jpg"
        }
        keepPicked(uri, "Photos", "photo_${System.currentTimeMillis()}.$ext")
    }

    /** Copies a picked document into Files/, keeping its name; an existing name gets a time prefix. */
    private fun keepFile(uri: android.net.Uri) {
        val name = displayName(uri)
        val taken = java.io.File(filesDir, "user_files/Files/$name").exists()
        keepPicked(uri, "Files", if (taken) "${System.currentTimeMillis()}_$name" else name)
    }

    private fun keepPicked(uri: android.net.Uri, folder: String, name: String) {
        val resolver = contentResolver
        Thread {
            val answer = runCatching {
                val dir = java.io.File(filesDir, "user_files/$folder").apply { mkdirs() }
                val input = resolver.openInputStream(uri) ?: error("the file could not be opened")
                input.use { source ->
                    java.io.File(dir, name).outputStream().use { target ->
                        var total = 0L
                        val buffer = ByteArray(64 * 1024)
                        while (true) {
                            val read = source.read(buffer)
                            if (read < 0) break
                            total += read
                            if (total > MAX_PHOTO_BYTES) error("the file is larger than 50 MB")
                            target.write(buffer, 0, read)
                        }
                    }
                }
                InteractiveDialogCoordinator.DialogAnswer.Accepted("$folder/$name")
            }.getOrElse { InteractiveDialogCoordinator.DialogAnswer.Failed(it.message ?: "the file could not be saved") }
            runOnUiThread { reply(answer) }
        }.start()
    }

    @Deprecated("Activity result callback for the platform speech recognizer and photo picker.")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PHOTO_REQUEST || requestCode == FILE_REQUEST) {
            val uri = data?.data
            when {
                resultCode != RESULT_OK || uri == null -> reply(InteractiveDialogCoordinator.DialogAnswer.Dismissed)
                requestCode == PHOTO_REQUEST -> keepPhoto(uri)
                else -> keepFile(uri)
            }
            return
        }
        if (requestCode != DICTATION_REQUEST) return
        val text = data?.getStringArrayListExtra(android.speech.RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        if (resultCode == RESULT_OK && text != null) {
            reply(InteractiveDialogCoordinator.DialogAnswer.Accepted(text))
        } else {
            reply(InteractiveDialogCoordinator.DialogAnswer.Dismissed)
        }
    }

    private fun buildDialog(): AlertDialog {
        val title = intent.getStringExtra(InteractiveDialogCoordinator.EXTRA_TITLE).orEmpty()
        val message = intent.getStringExtra(InteractiveDialogCoordinator.EXTRA_MESSAGE).orEmpty()
        val cancelable = intent.getBooleanExtra(InteractiveDialogCoordinator.EXTRA_CANCELABLE, true)
        val night = (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
            android.content.res.Configuration.UI_MODE_NIGHT_YES
        val theme = if (night) android.R.style.Theme_DeviceDefault_Dialog_Alert else android.R.style.Theme_DeviceDefault_Light_Dialog_Alert
        val builder = AlertDialog.Builder(this, theme)
        if (title.isNotBlank()) builder.setTitle(title)
        builder.setOnCancelListener { reply(InteractiveDialogCoordinator.DialogAnswer.Dismissed) }

        when (intent.getStringExtra(InteractiveDialogCoordinator.EXTRA_KIND)) {
            InteractiveDialogCoordinator.KIND_INPUT -> {
                val field = EditText(this).apply {
                    setText(intent.getStringExtra(InteractiveDialogCoordinator.EXTRA_DEFAULT).orEmpty())
                    setSelection(text.length)
                    inputType = when (intent.getStringExtra(InteractiveDialogCoordinator.EXTRA_INPUT_TYPE)) {
                        "number" -> InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL or InputType.TYPE_NUMBER_FLAG_SIGNED
                        "url" -> InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
                        "multiline" -> InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                        else -> InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                    }
                }
                val padding = (20 * resources.displayMetrics.density).toInt()
                val frame = FrameLayout(this).apply {
                    setPadding(padding, padding / 2, padding, 0)
                    addView(field, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
                }
                if (message.isNotBlank()) builder.setMessage(message)
                builder.setView(frame)
                builder.setPositiveButton(android.R.string.ok) { _, _ ->
                    reply(InteractiveDialogCoordinator.DialogAnswer.Accepted(field.text.toString()))
                }
                builder.setNegativeButton(android.R.string.cancel) { _, _ ->
                    reply(InteractiveDialogCoordinator.DialogAnswer.Dismissed)
                }
                return builder.create().also { created ->
                    created.setOnShowListener {
                        field.requestFocus()
                        created.window?.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
                    }
                }
            }

            InteractiveDialogCoordinator.KIND_LIST_MULTI -> {
                val items = intent.getStringArrayExtra(InteractiveDialogCoordinator.EXTRA_ITEMS).orEmpty()
                val checked = BooleanArray(items.size)
                builder.setMultiChoiceItems(items, checked) { _, which, isChecked -> checked[which] = isChecked }
                builder.setPositiveButton(android.R.string.ok) { _, _ ->
                    val chosen = items.filterIndexed { i, _ -> checked[i] }
                    reply(InteractiveDialogCoordinator.DialogAnswer.Accepted(chosen.joinToString("\n"), chosen.size))
                }
                builder.setNegativeButton(android.R.string.cancel) { _, _ ->
                    reply(InteractiveDialogCoordinator.DialogAnswer.Dismissed)
                }
            }

            InteractiveDialogCoordinator.KIND_MENU -> {
                val items = intent.getStringArrayExtra(InteractiveDialogCoordinator.EXTRA_ITEMS).orEmpty()
                builder.setItems(items) { _, which ->
                    reply(InteractiveDialogCoordinator.DialogAnswer.Accepted(items[which], which))
                }
                builder.setNegativeButton(android.R.string.cancel) { _, _ ->
                    reply(InteractiveDialogCoordinator.DialogAnswer.Dismissed)
                }
            }

            InteractiveDialogCoordinator.KIND_RESULT -> {
                builder.setMessage(message)
                builder.setPositiveButton(android.R.string.ok) { _, _ ->
                    reply(InteractiveDialogCoordinator.DialogAnswer.Accepted())
                }
                builder.setNeutralButton(android.R.string.copy) { _, _ ->
                    getSystemService(ClipboardManager::class.java)
                        ?.setPrimaryClip(ClipData.newPlainText("OpenTasker", message))
                    reply(InteractiveDialogCoordinator.DialogAnswer.Accepted())
                }
                builder.setOnCancelListener { reply(InteractiveDialogCoordinator.DialogAnswer.Accepted()) }
            }

            else -> { // alert
                if (message.isNotBlank()) builder.setMessage(message)
                builder.setPositiveButton(android.R.string.ok) { _, _ ->
                    reply(InteractiveDialogCoordinator.DialogAnswer.Accepted())
                }
                if (cancelable) {
                    builder.setNegativeButton(android.R.string.cancel) { _, _ ->
                        reply(InteractiveDialogCoordinator.DialogAnswer.Dismissed)
                    }
                } else {
                    builder.setOnCancelListener { reply(InteractiveDialogCoordinator.DialogAnswer.Accepted()) }
                }
            }
        }
        return builder.create()
    }

    private fun reply(answer: InteractiveDialogCoordinator.DialogAnswer) {
        if (answered) return
        answered = true
        requestId?.let { InteractiveDialogCoordinator.answer(it, answer) }
        finish()
    }

    override fun onDestroy() {
        dialog?.setOnDismissListener(null)
        dialog?.dismiss()
        dialog = null
        // Leaving the screen any other way (home, recents) counts as dismissing the dialog.
        if (!answered && isFinishing) reply(InteractiveDialogCoordinator.DialogAnswer.Dismissed)
        requestId?.let { open.remove(it, this) }
        super.onDestroy()
    }

    companion object {
        private const val DICTATION_REQUEST = 7411
        private const val PHOTO_REQUEST = 7412
        private const val FILE_REQUEST = 7413
        private const val MAX_PHOTO_BYTES = 50L * 1024 * 1024
        private val open = ConcurrentHashMap<String, InteractiveDialogActivity>()

        /** Closes a dialog whose step already ended (timed out or the task was stopped). */
        fun closeIfOpen(requestId: String) {
            open.remove(requestId)?.let { activity ->
                activity.runOnUiThread {
                    activity.answered = true
                    activity.finish()
                }
            }
        }
    }
}

private const val MAX_DIALOG_TEXT = 4_000
private const val MAX_MENU_ITEMS = 100
private const val MAX_INPUT_CHARS = 16_000

private fun dialogFailure(outcome: InteractiveDialogCoordinator.Outcome): ActionResult.Failure = when (outcome) {
    InteractiveDialogCoordinator.Outcome.NotShown -> ActionResult.Failure(
        BackgroundActivityStart.failureMessage("the dialog") +
            " Running the task from a tile, widget, home-screen shortcut or notification also works.",
    )
    InteractiveDialogCoordinator.Outcome.TimedOut -> ActionResult.Failure("no answer within ${InteractiveDialogCoordinator.ANSWER_TIMEOUT_MS / 60_000} minutes")
    is InteractiveDialogCoordinator.Outcome.Answered -> when (val answer = outcome.answer) {
        is InteractiveDialogCoordinator.DialogAnswer.Failed -> ActionResult.Failure(answer.reason)
        else -> ActionResult.Failure("cancelled by the user")
    }
}

private fun dialogOutput(args: Map<String, String>, defaultName: String): String? =
    VariableNamePolicy.normalize(args["var"]?.takeIf(String::isNotBlank) ?: defaultName)

/** Ask for Input: a text, number, URL or multi-line field; the answer goes into a variable. */
class AskForInputAction : DeclaredAction(ActionCatalog.require("input.ask")) {
    override suspend fun run(ctx: ActionContext, args: Map<String, String>): ActionResult {
        val title = args["title"].orEmpty().take(MAX_DIALOG_TEXT)
        val output = dialogOutput(args, "Input") ?: return ActionResult.Failure("invalid output variable")
        val type = args["type"]?.trim()?.lowercase().orEmpty().ifBlank { "text" }
        if (type !in setOf("text", "number", "url", "multiline")) {
            return ActionResult.Failure("type must be text, number, url or multiline")
        }
        val outcome = InteractiveDialogCoordinator.ask(ctx.app) {
            putExtra(InteractiveDialogCoordinator.EXTRA_KIND, InteractiveDialogCoordinator.KIND_INPUT)
            putExtra(InteractiveDialogCoordinator.EXTRA_TITLE, title)
            putExtra(InteractiveDialogCoordinator.EXTRA_MESSAGE, args["message"].orEmpty().take(MAX_DIALOG_TEXT))
            putExtra(InteractiveDialogCoordinator.EXTRA_INPUT_TYPE, type)
            putExtra(InteractiveDialogCoordinator.EXTRA_DEFAULT, args["default"].orEmpty().take(MAX_INPUT_CHARS))
        }
        val answer = (outcome as? InteractiveDialogCoordinator.Outcome.Answered)?.answer
            as? InteractiveDialogCoordinator.DialogAnswer.Accepted
            ?: return dialogFailure(outcome)
        val text = answer.text.take(MAX_INPUT_CHARS)
        if (type == "number" && text.isNotBlank() && text.toDoubleOrNull() == null) {
            return ActionResult.Failure("the answer is not a number")
        }
        ctx.variables.set(output, text)
        ctx.logger("Ask for Input: ${text.length} characters")
        return ActionResult.Success
    }
}

/** Choose from Menu: one tap picks an item; its text and 1-based position go into variables. */
class ChooseFromMenuAction : DeclaredAction(ActionCatalog.require("menu.choose")) {
    override suspend fun run(ctx: ActionContext, args: Map<String, String>): ActionResult {
        val items = args["items"].orEmpty()
            .split('\n')
            .map(String::trim)
            .filter(String::isNotEmpty)
        if (items.isEmpty()) return ActionResult.Failure("the menu has no items")
        if (items.size > MAX_MENU_ITEMS) return ActionResult.Failure("a menu can hold at most $MAX_MENU_ITEMS items")
        val output = dialogOutput(args, "Choice") ?: return ActionResult.Failure("invalid output variable")
        val outcome = InteractiveDialogCoordinator.ask(ctx.app) {
            putExtra(InteractiveDialogCoordinator.EXTRA_KIND, InteractiveDialogCoordinator.KIND_MENU)
            putExtra(InteractiveDialogCoordinator.EXTRA_TITLE, args["title"].orEmpty().take(MAX_DIALOG_TEXT))
            putExtra(InteractiveDialogCoordinator.EXTRA_ITEMS, items.map { it.take(MAX_DIALOG_TEXT) }.toTypedArray())
        }
        val answer = (outcome as? InteractiveDialogCoordinator.Outcome.Answered)?.answer
            as? InteractiveDialogCoordinator.DialogAnswer.Accepted
            ?: return dialogFailure(outcome)
        ctx.variables.set(output, answer.text)
        ctx.variables.set("${output}_index", (answer.index + 1).toString())
        ctx.logger("Choose from Menu: item ${answer.index + 1} of ${items.size}")
        return ActionResult.Success
    }
}

/** Show Alert: a message with OK, and Cancel unless turned off; Cancel stops the task. */
class ShowAlertAction : DeclaredAction(ActionCatalog.require("alert.show")) {
    override suspend fun run(ctx: ActionContext, args: Map<String, String>): ActionResult {
        val showCancel = args["cancel_button"]?.trim()?.lowercase() != "false"
        val outcome = InteractiveDialogCoordinator.ask(ctx.app) {
            putExtra(InteractiveDialogCoordinator.EXTRA_KIND, InteractiveDialogCoordinator.KIND_ALERT)
            putExtra(InteractiveDialogCoordinator.EXTRA_TITLE, args["title"].orEmpty().take(MAX_DIALOG_TEXT))
            putExtra(InteractiveDialogCoordinator.EXTRA_MESSAGE, args["message"].orEmpty().take(MAX_DIALOG_TEXT))
            putExtra(InteractiveDialogCoordinator.EXTRA_CANCELABLE, showCancel)
        }
        (outcome as? InteractiveDialogCoordinator.Outcome.Answered)?.answer
            as? InteractiveDialogCoordinator.DialogAnswer.Accepted
            ?: return dialogFailure(outcome)
        return ActionResult.Success
    }
}

/** Show Result: shows text (usually a variable) with OK and Copy. */
class ShowResultAction : DeclaredAction(ActionCatalog.require("result.show")) {
    override suspend fun run(ctx: ActionContext, args: Map<String, String>): ActionResult {
        val text = args["text"] ?: return ActionResult.Failure("missing text")
        val outcome = InteractiveDialogCoordinator.ask(ctx.app) {
            putExtra(InteractiveDialogCoordinator.EXTRA_KIND, InteractiveDialogCoordinator.KIND_RESULT)
            putExtra(InteractiveDialogCoordinator.EXTRA_TITLE, args["title"].orEmpty().take(MAX_DIALOG_TEXT))
            putExtra(InteractiveDialogCoordinator.EXTRA_MESSAGE, text.take(MAX_INPUT_CHARS))
        }
        (outcome as? InteractiveDialogCoordinator.Outcome.Answered)?.answer
            as? InteractiveDialogCoordinator.DialogAnswer.Accepted
            ?: return dialogFailure(outcome)
        return ActionResult.Success
    }
}

/** Choose from List: tick any number of items; the chosen ones go into a variable, one per line. */
class ChooseFromListAction : DeclaredAction(ActionCatalog.require("list.choose")) {
    override suspend fun run(ctx: ActionContext, args: Map<String, String>): ActionResult {
        val items = args["items"].orEmpty().split('\n').map(String::trim).filter(String::isNotEmpty)
        if (items.isEmpty()) return ActionResult.Failure("the list has no items")
        if (items.size > MAX_MENU_ITEMS) return ActionResult.Failure("a list can hold at most $MAX_MENU_ITEMS items")
        val output = dialogOutput(args, "Chosen") ?: return ActionResult.Failure("invalid output variable")
        val outcome = InteractiveDialogCoordinator.ask(ctx.app) {
            putExtra(InteractiveDialogCoordinator.EXTRA_KIND, InteractiveDialogCoordinator.KIND_LIST_MULTI)
            putExtra(InteractiveDialogCoordinator.EXTRA_TITLE, args["title"].orEmpty().take(MAX_DIALOG_TEXT))
            putExtra(InteractiveDialogCoordinator.EXTRA_ITEMS, items.map { it.take(MAX_DIALOG_TEXT) }.toTypedArray())
        }
        val answer = (outcome as? InteractiveDialogCoordinator.Outcome.Answered)?.answer
            as? InteractiveDialogCoordinator.DialogAnswer.Accepted
            ?: return dialogFailure(outcome)
        val chosen = answer.text.split('\n').filter(String::isNotEmpty)
        ctx.variables.set(output, answer.text)
        ctx.variables.set("${output}_count", chosen.size.toString())
        ctx.variables.setArray("${output}_items", chosen)
        ctx.logger("Choose from List: ${chosen.size} of ${items.size}")
        return ActionResult.Success
    }
}

/** Ask for Date: the system date and/or time picker; the answer is yyyy-MM-dd, HH:mm or both. */
class AskForDateAction : DeclaredAction(ActionCatalog.require("date.ask")) {
    override suspend fun run(ctx: ActionContext, args: Map<String, String>): ActionResult {
        val mode = args["mode"]?.trim()?.lowercase().orEmpty().ifEmpty { "date" }
        if (mode !in setOf("date", "time", "datetime")) return ActionResult.Failure("mode must be date, time or datetime")
        val output = dialogOutput(args, "Date") ?: return ActionResult.Failure("invalid output variable")
        val outcome = InteractiveDialogCoordinator.ask(ctx.app) {
            putExtra(InteractiveDialogCoordinator.EXTRA_KIND, InteractiveDialogCoordinator.KIND_DATE)
            putExtra(InteractiveDialogCoordinator.EXTRA_DATE_MODE, mode)
            putExtra(InteractiveDialogCoordinator.EXTRA_TITLE, args["title"].orEmpty().take(MAX_DIALOG_TEXT))
        }
        val answer = (outcome as? InteractiveDialogCoordinator.Outcome.Answered)?.answer
            as? InteractiveDialogCoordinator.DialogAnswer.Accepted
            ?: return dialogFailure(outcome)
        ctx.variables.set(output, answer.text)
        return ActionResult.Success
    }
}

/** Dictate Text: the system speech recognizer turns speech into text. */
class DictateTextAction : DeclaredAction(ActionCatalog.require("dictate.text")) {
    override suspend fun run(ctx: ActionContext, args: Map<String, String>): ActionResult {
        val output = dialogOutput(args, "Dictation") ?: return ActionResult.Failure("invalid output variable")
        val language = args["language"]?.trim().orEmpty()
        if (language.isNotEmpty() && !Regex("[A-Za-z]{2,3}(-[A-Za-z0-9]{2,8})*").matches(language)) {
            return ActionResult.Failure("language must be a tag such as tr-TR or en-US")
        }
        val outcome = InteractiveDialogCoordinator.ask(ctx.app) {
            putExtra(InteractiveDialogCoordinator.EXTRA_KIND, InteractiveDialogCoordinator.KIND_DICTATE)
            putExtra(InteractiveDialogCoordinator.EXTRA_TITLE, args["prompt"].orEmpty().take(MAX_DIALOG_TEXT))
            putExtra(InteractiveDialogCoordinator.EXTRA_LANGUAGE, language)
        }
        val answer = (outcome as? InteractiveDialogCoordinator.Outcome.Answered)?.answer
            as? InteractiveDialogCoordinator.DialogAnswer.Accepted
            ?: return dialogFailure(outcome)
        ctx.variables.set(output, answer.text.take(MAX_INPUT_CHARS))
        ctx.logger("Dictate Text: ${answer.text.length} characters")
        return ActionResult.Success
    }
}

/** Select Photos: pick one photo; it is copied into the shortcut files and its path saved. */
class PickPhotoAction : DeclaredAction(ActionCatalog.require("photo.pick")) {
    override suspend fun run(ctx: ActionContext, args: Map<String, String>): ActionResult {
        val output = dialogOutput(args, "Photo") ?: return ActionResult.Failure("invalid output variable")
        val outcome = InteractiveDialogCoordinator.ask(ctx.app) {
            putExtra(InteractiveDialogCoordinator.EXTRA_KIND, InteractiveDialogCoordinator.KIND_PHOTO)
        }
        val answer = (outcome as? InteractiveDialogCoordinator.Outcome.Answered)?.answer
            as? InteractiveDialogCoordinator.DialogAnswer.Accepted
            ?: return dialogFailure(outcome)
        ctx.variables.set(output, answer.text)
        ctx.logger("Select Photo: saved ${answer.text}")
        return ActionResult.Success
    }
}

/** Select File: pick any document; it is copied into the shortcut files (Files/) and its path saved. */
class PickFileAction : DeclaredAction(ActionCatalog.require("file.pick")) {
    override suspend fun run(ctx: ActionContext, args: Map<String, String>): ActionResult {
        val output = dialogOutput(args, "File") ?: return ActionResult.Failure("invalid output variable")
        val outcome = InteractiveDialogCoordinator.ask(ctx.app) {
            putExtra(InteractiveDialogCoordinator.EXTRA_KIND, InteractiveDialogCoordinator.KIND_FILE)
        }
        val answer = (outcome as? InteractiveDialogCoordinator.Outcome.Answered)?.answer
            as? InteractiveDialogCoordinator.DialogAnswer.Accepted
            ?: return dialogFailure(outcome)
        ctx.variables.set(output, answer.text)
        ctx.logger("Select File: saved ${answer.text}")
        return ActionResult.Success
    }
}
