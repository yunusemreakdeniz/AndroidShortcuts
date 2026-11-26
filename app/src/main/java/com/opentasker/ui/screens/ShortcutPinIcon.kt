package com.opentasker.ui.screens

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.LayoutDirection
import com.opentasker.core.model.Task

/** Adaptive icon canvas: 108dp at 4x. The glyph sits well inside the 72dp safe zone. */
private const val PIN_ICON_PX = 432
private const val PIN_GLYPH_PX = 160

/**
 * Draws a shortcut's own look (its colour and icon) as a home-screen icon, so a pinned shortcut
 * looks like its tile instead of the app icon. Returns a function so it only renders on tap.
 */
@Composable
internal fun rememberPinIcon(task: Task, styles: Map<Long, ShortcutStyle>): () -> Bitmap {
    val color = styledColor(task, styles)
    val painter = rememberVectorPainter(styledIcon(task, styles))
    val density = LocalDensity.current
    return remember(color, painter, density) {
        {
            val image = ImageBitmap(PIN_ICON_PX, PIN_ICON_PX)
            CanvasDrawScope().draw(density, LayoutDirection.Ltr, Canvas(image), Size(PIN_ICON_PX.toFloat(), PIN_ICON_PX.toFloat())) {
                drawRect(color)
                val inset = (PIN_ICON_PX - PIN_GLYPH_PX) / 2f
                translate(inset, inset) {
                    with(painter) { draw(Size(PIN_GLYPH_PX.toFloat(), PIN_GLYPH_PX.toFloat()), colorFilter = ColorFilter.tint(Color.White)) }
                }
            }
            image.asAndroidBitmap()
        }
    }
}
