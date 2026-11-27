package com.opentasker.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.automirrored.outlined.CallSplit
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.BatteryStd
import androidx.compose.material.icons.outlined.Bluetooth
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Brightness6
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.Casino
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.Contacts
import androidx.compose.material.icons.outlined.DataArray
import androidx.compose.material.icons.outlined.DoNotDisturbOn
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.FlashlightOn
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Photo
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Sms
import androidx.compose.material.icons.outlined.StopCircle
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material.icons.outlined.VpnKey
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.ui.graphics.vector.ImageVector

/** One recognisable icon per action, so blocks and picker rows are easy to tell apart. */
internal fun actionIcon(type: String): ImageVector = exactIcons[type] ?: prefixIcon(type)

private val exactIcons: Map<String, ImageVector> = mapOf(
    "input.ask" to Icons.Outlined.Edit,
    "menu.choose" to Icons.AutoMirrored.Outlined.List,
    "list.choose" to Icons.AutoMirrored.Outlined.List,
    "alert.show" to Icons.Outlined.Info,
    "result.show" to Icons.Outlined.ChatBubbleOutline,
    "date.ask" to Icons.Outlined.Event,
    "dictate.text" to Icons.Outlined.Mic,
    "photo.pick" to Icons.Outlined.Photo,
    "photo.take" to Icons.Outlined.PhotoCamera,
    "image.resize" to Icons.Outlined.Photo,
    "pdf.create" to Icons.AutoMirrored.Outlined.Article,
    "calendar.add" to Icons.Outlined.Event,
    "file.pick" to Icons.Outlined.Folder,
    "file.share" to Icons.Outlined.Share,
    "tts.speak" to Icons.Outlined.RecordVoiceOver,
    "random.number" to Icons.Outlined.Casino,
    "list.random" to Icons.Outlined.Casino,
    "number.format" to Icons.Outlined.Calculate,
    "timer.set" to Icons.Outlined.Timer,
    "flow.wait" to Icons.Outlined.HourglassEmpty,
    "flow.foreach" to Icons.Outlined.Repeat,
    "flow.endfor" to Icons.Outlined.Repeat,
    "flow.try" to Icons.Outlined.Shield,
    "flow.catch" to Icons.Outlined.Shield,
    "flow.endtry" to Icons.Outlined.Shield,
    "flow.stop" to Icons.Outlined.StopCircle,
    "task.run" to Icons.Outlined.PlayCircleOutline,
    "task.stop" to Icons.Outlined.StopCircle,
    "home.go" to Icons.Outlined.Home,
    "url.open" to Icons.Outlined.Link,
    "url.parts" to Icons.Outlined.Link,
    "torch.set" to Icons.Outlined.FlashlightOn,
    "brightness.set" to Icons.Outlined.Brightness6,
    "vpn.tunnel" to Icons.Outlined.VpnKey,
    "sun.times" to Icons.Outlined.WbSunny,
    "vibrate" to Icons.Outlined.Vibration,
    "log" to Icons.AutoMirrored.Outlined.Article,
    "download" to Icons.Outlined.Download,
    "ping" to Icons.Outlined.Language,
    "contacts.lookup" to Icons.Outlined.Contacts,
    "device.info" to Icons.Outlined.Info,
    "network.info" to Icons.Outlined.Wifi,
)

private fun prefixIcon(type: String): ImageVector {
    val prefix = type.substringBefore('.')
    return when {
        type.startsWith("flow.if") || type.startsWith("flow.else") || type == "flow.endif" -> Icons.AutoMirrored.Outlined.CallSplit
        prefix == "var" || prefix == "array" -> Icons.Outlined.DataArray
        prefix == "text" -> Icons.Outlined.TextFields
        prefix == "datetime" -> Icons.Outlined.Schedule
        prefix == "clipboard" -> Icons.Outlined.ContentPaste
        prefix == "file" || prefix == "data" -> Icons.Outlined.Folder
        prefix == "http" || prefix == "integration" || prefix == "mqtt" || prefix == "url" -> Icons.Outlined.Language
        prefix == "notify" -> Icons.Outlined.Notifications
        prefix == "sound" || prefix == "track" || prefix == "media" -> Icons.Outlined.MusicNote
        prefix == "volume" -> Icons.Outlined.VolumeUp
        prefix == "alarm" -> Icons.Outlined.Alarm
        prefix == "battery" -> Icons.Outlined.BatteryStd
        prefix == "math" -> Icons.Outlined.Calculate
        prefix == "share" -> Icons.Outlined.Share
        prefix == "maps" -> Icons.Outlined.Map
        prefix == "email" -> Icons.Outlined.Email
        prefix == "phone" -> Icons.Outlined.Phone
        prefix == "sms" -> Icons.Outlined.Sms
        prefix == "wifi" -> Icons.Outlined.Wifi
        prefix == "bluetooth" -> Icons.Outlined.Bluetooth
        prefix == "dnd" || prefix == "zen" -> Icons.Outlined.DoNotDisturbOn
        prefix == "ime" -> Icons.Outlined.Keyboard
        prefix == "app" || prefix == "intent" || prefix == "shortcut" -> Icons.Outlined.Apps
        prefix == "settings" || prefix == "screen" || prefix == "state" || prefix == "tile" -> Icons.Outlined.Tune
        else -> Icons.Outlined.Bolt
    }
}
