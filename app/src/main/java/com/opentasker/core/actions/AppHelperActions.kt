package com.opentasker.core.actions

import android.content.Intent
import android.net.Uri
import android.provider.AlarmClock
import com.opentasker.core.engine.ActionContext
import com.opentasker.core.engine.ActionResult

/**
 * Shortcuts-style app helpers built on standard Android intents: the system clock, mail, dialer,
 * share sheet and maps handle the request, so no extra permission or app-specific API is needed.
 */
private const val MAX_HELPER_TEXT = 16_000

private fun launch(ctx: ActionContext, intent: Intent, what: String): ActionResult = try {
    startActivityFromAction(ctx, intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), what)
} catch (e: android.content.ActivityNotFoundException) {
    ActionResult.Failure("no app on this device can handle $what")
} catch (e: Exception) {
    ActionResult.Failure("could not open $what: ${e.message}")
}

private fun Map<String, String>.flag(key: String, default: Boolean): Boolean =
    this[key]?.trim()?.lowercase()?.let { it == "true" } ?: default

/** Set Alarm: hands the time to the clock app; with "skip UI" it is saved without opening the app. */
class SetAlarmAction : DeclaredAction(ActionCatalog.require("alarm.set")) {
    override suspend fun run(ctx: ActionContext, args: Map<String, String>): ActionResult {
        val hour = args["hour"]?.trim()?.toIntOrNull()?.takeIf { it in 0..23 }
            ?: return ActionResult.Failure("hour must be 0-23")
        val minute = args["minute"]?.trim().orEmpty().ifEmpty { "0" }.toIntOrNull()?.takeIf { it in 0..59 }
            ?: return ActionResult.Failure("minute must be 0-59")
        val intent = Intent(AlarmClock.ACTION_SET_ALARM)
            .putExtra(AlarmClock.EXTRA_HOUR, hour)
            .putExtra(AlarmClock.EXTRA_MINUTES, minute)
            .putExtra(AlarmClock.EXTRA_SKIP_UI, args.flag("skip_ui", true))
        args["label"]?.takeIf(String::isNotBlank)?.let { intent.putExtra(AlarmClock.EXTRA_MESSAGE, it.take(200)) }
        ctx.logger("Set alarm %02d:%02d".format(hour, minute))
        return launch(ctx, intent, "the alarm")
    }
}

/** Start Timer: a countdown in the clock app. */
class StartTimerAction : DeclaredAction(ActionCatalog.require("timer.set")) {
    override suspend fun run(ctx: ActionContext, args: Map<String, String>): ActionResult {
        val seconds = args["seconds"]?.trim()?.toIntOrNull()?.takeIf { it in 1..86_400 }
            ?: return ActionResult.Failure("seconds must be 1-86400")
        val intent = Intent(AlarmClock.ACTION_SET_TIMER)
            .putExtra(AlarmClock.EXTRA_LENGTH, seconds)
            .putExtra(AlarmClock.EXTRA_SKIP_UI, args.flag("skip_ui", true))
        args["label"]?.takeIf(String::isNotBlank)?.let { intent.putExtra(AlarmClock.EXTRA_MESSAGE, it.take(200)) }
        ctx.logger("Start timer: $seconds s")
        return launch(ctx, intent, "the timer")
    }
}

/** Compose Email: opens a mail app with the recipients, subject and body filled in. */
class ComposeEmailAction : DeclaredAction(ActionCatalog.require("email.compose")) {
    override suspend fun run(ctx: ActionContext, args: Map<String, String>): ActionResult {
        val to = args["to"].orEmpty().split(',', ';', ' ', '\n').map(String::trim).filter(String::isNotEmpty)
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:"))
            .putExtra(Intent.EXTRA_EMAIL, to.toTypedArray())
            .putExtra(Intent.EXTRA_SUBJECT, args["subject"].orEmpty().take(1_000))
            .putExtra(Intent.EXTRA_TEXT, args["body"].orEmpty().take(MAX_HELPER_TEXT))
        ctx.logger("Compose email to ${to.size} recipient(s)")
        return launch(ctx, intent, "the mail app")
    }
}

/** Call Phone Number: opens the dialer with the number; the person presses call. */
class DialNumberAction : DeclaredAction(ActionCatalog.require("phone.dial")) {
    override suspend fun run(ctx: ActionContext, args: Map<String, String>): ActionResult {
        val number = args["number"]?.filter { it.isDigit() || it in "+*#" }?.takeIf(String::isNotEmpty)
            ?: return ActionResult.Failure("missing phone number")
        return launch(ctx, Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", number, null)), "the dialer")
    }
}

/** Share: opens Android's share sheet with the text. */
class ShareTextAction : DeclaredAction(ActionCatalog.require("share.text")) {
    override suspend fun run(ctx: ActionContext, args: Map<String, String>): ActionResult {
        val text = args["text"]?.takeIf(String::isNotEmpty) ?: return ActionResult.Failure("missing text")
        val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text.take(MAX_HELPER_TEXT))
        val chooser = Intent.createChooser(send, args["title"]?.takeIf(String::isNotBlank)?.take(200))
        return launch(ctx, chooser, "the share sheet")
    }
}

/** Get Directions: opens maps with a route to the destination. */
class DirectionsAction : DeclaredAction(ActionCatalog.require("maps.directions")) {
    override suspend fun run(ctx: ActionContext, args: Map<String, String>): ActionResult {
        val destination = args["destination"]?.trim()?.takeIf(String::isNotEmpty)
            ?: return ActionResult.Failure("missing destination")
        val mode = args["mode"]?.trim()?.lowercase().orEmpty().ifEmpty { "driving" }
        if (mode !in setOf("driving", "walking", "transit", "bicycling")) {
            return ActionResult.Failure("mode must be driving, walking, transit or bicycling")
        }
        val uri = Uri.parse("https://www.google.com/maps/dir/").buildUpon()
            .appendQueryParameter("api", "1")
            .appendQueryParameter("destination", destination.take(500))
            .appendQueryParameter("travelmode", mode)
            .build()
        return launch(ctx, Intent(Intent.ACTION_VIEW, uri), "maps")
    }
}
