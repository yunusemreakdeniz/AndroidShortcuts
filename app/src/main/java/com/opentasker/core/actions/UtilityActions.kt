package com.opentasker.core.actions

import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import com.opentasker.core.engine.ActionContext
import com.opentasker.core.engine.ActionResult
import com.opentasker.core.model.VariableNamePolicy
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import kotlin.math.pow
import kotlin.random.Random

private fun utilityOutput(args: Map<String, String>, defaultName: String): String? =
    VariableNamePolicy.normalize(args["var"]?.takeIf(String::isNotBlank) ?: defaultName)

/** Get Battery Level: percentage, charging state and power source. */
class GetBatteryAction : DeclaredAction(ActionCatalog.require("battery.get")) {
    override suspend fun run(ctx: ActionContext, args: Map<String, String>): ActionResult {
        val output = utilityOutput(args, "Battery") ?: return ActionResult.Failure("invalid output variable")
        // A sticky broadcast: registering with a null receiver just returns the latest value.
        val status = ctx.app.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            ?: return ActionResult.Failure("battery status unavailable")
        val level = status.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = status.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        if (level < 0 || scale <= 0) return ActionResult.Failure("battery level unavailable")
        val percent = (level * 100f / scale).toInt()
        val state = status.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val charging = state == BatteryManager.BATTERY_STATUS_CHARGING || state == BatteryManager.BATTERY_STATUS_FULL
        val source = when (status.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)) {
            BatteryManager.BATTERY_PLUGGED_AC -> "ac"
            BatteryManager.BATTERY_PLUGGED_USB -> "usb"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "wireless"
            BatteryManager.BATTERY_PLUGGED_DOCK -> "dock"
            else -> "none"
        }
        ctx.variables.set(output, percent.toString())
        ctx.variables.set("${output}_charging", charging.toString())
        ctx.variables.set("${output}_source", source)
        ctx.logger("Battery: $percent%, charging=$charging, source=$source")
        return ActionResult.Success
    }
}

/** Calculate: evaluates an arithmetic expression such as `(2 + 3) * 4 / 5 ^ 2 % 3`. */
class CalculateAction : DeclaredAction(ActionCatalog.require("math.calculate")) {
    override suspend fun run(ctx: ActionContext, args: Map<String, String>): ActionResult {
        val expression = args["expression"]?.takeIf(String::isNotBlank)
            ?: return ActionResult.Failure("missing expression")
        val output = utilityOutput(args, "Result") ?: return ActionResult.Failure("invalid output variable")
        val value = runCatching { MathExpression.evaluate(expression) }
            .getOrElse { return ActionResult.Failure("cannot calculate: ${it.message}") }
        ctx.variables.set(output, MathExpression.format(value))
        return ActionResult.Success
    }
}

/** Random Number: a whole number between min and max, both included. */
class RandomNumberAction : DeclaredAction(ActionCatalog.require("random.number")) {
    override suspend fun run(ctx: ActionContext, args: Map<String, String>): ActionResult {
        val min = args["min"]?.trim().orEmpty().ifEmpty { "1" }.toLongOrNull()
            ?: return ActionResult.Failure("min must be a whole number")
        val max = args["max"]?.trim().orEmpty().ifEmpty { "100" }.toLongOrNull()
            ?: return ActionResult.Failure("max must be a whole number")
        if (min > max) return ActionResult.Failure("min must not be greater than max")
        if (max - min > MAX_RANDOM_SPAN || max - min < 0) return ActionResult.Failure("the range is too large")
        val output = utilityOutput(args, "Random") ?: return ActionResult.Failure("invalid output variable")
        ctx.variables.set(output, Random.nextLong(min, max + 1).toString())
        return ActionResult.Success
    }

    private companion object {
        const val MAX_RANDOM_SPAN = 1_000_000_000_000_000L
    }
}

/**
 * A small recursive-descent calculator: + - * / % ^, unary minus, parentheses, decimals.
 * No variables or functions, so a value pasted from a variable cannot run anything.
 */
internal object MathExpression {
    private const val MAX_LENGTH = 1_000
    private const val MAX_DEPTH = 64

    fun evaluate(source: String): Double {
        require(source.length <= MAX_LENGTH) { "the expression is longer than $MAX_LENGTH characters" }
        val parser = Parser(source.replace(',', '.'))
        val value = parser.expression()
        parser.skipSpaces()
        require(parser.atEnd()) { "unexpected '${parser.rest()}'" }
        require(value.isFinite()) { "the result is not a finite number" }
        return value
    }

    /** Whole results print without a decimal point; others keep up to 10 significant decimals. */
    fun format(value: Double): String {
        if (value == Math.rint(value) && kotlin.math.abs(value) < 1e15) return value.toLong().toString()
        return BigDecimal(value).round(MathContext(15, RoundingMode.HALF_UP)).stripTrailingZeros().toPlainString()
    }

    private class Parser(private val text: String) {
        private var pos = 0
        private var depth = 0

        fun atEnd() = pos >= text.length
        fun rest() = text.substring(pos).take(20)
        fun skipSpaces() {
            while (pos < text.length && text[pos].isWhitespace()) pos++
        }

        private fun eat(ch: Char): Boolean {
            skipSpaces()
            if (pos < text.length && text[pos] == ch) {
                pos++
                return true
            }
            return false
        }

        fun expression(): Double {
            var value = term()
            while (true) {
                value = when {
                    eat('+') -> value + term()
                    eat('-') -> value - term()
                    else -> return value
                }
            }
        }

        private fun term(): Double {
            var value = power()
            while (true) {
                value = when {
                    eat('*') || eat('×') -> value * power()
                    eat('/') || eat('÷') -> {
                        val divisor = power()
                        require(divisor != 0.0) { "division by zero" }
                        value / divisor
                    }
                    eat('%') -> {
                        val divisor = power()
                        require(divisor != 0.0) { "division by zero" }
                        value % divisor
                    }
                    else -> return value
                }
            }
        }

        private fun power(): Double {
            val base = unary()
            return if (eat('^')) base.pow(power()) else base
        }

        private fun unary(): Double = when {
            eat('-') -> -unary()
            eat('+') -> unary()
            else -> primary()
        }

        private fun primary(): Double {
            if (eat('(')) {
                depth++
                require(depth <= MAX_DEPTH) { "too many nested parentheses" }
                val value = expression()
                require(eat(')')) { "missing ')'" }
                depth--
                return value
            }
            skipSpaces()
            val start = pos
            while (pos < text.length && (text[pos].isDigit() || text[pos] == '.')) pos++
            require(pos > start) { if (atEnd()) "the expression ends too early" else "unexpected '${rest()}'" }
            return text.substring(start, pos).toDoubleOrNull() ?: throw IllegalArgumentException("bad number '${text.substring(start, pos)}'")
        }
    }
}

/** Get Random Item from List: one line picked at random. */
class RandomItemAction : DeclaredAction(ActionCatalog.require("list.random")) {
    override suspend fun run(ctx: ActionContext, args: Map<String, String>): ActionResult {
        val items = args["items"].orEmpty().lines().map(String::trim).filter(String::isNotEmpty)
        if (items.isEmpty()) return ActionResult.Failure("the list is empty")
        val output = utilityOutput(args, "Item") ?: return ActionResult.Failure("invalid output variable")
        ctx.variables.set(output, items.random())
        return ActionResult.Success
    }
}

/** Format Number: fixed decimals and the device's digit grouping, e.g. 1.234,50 in Turkish. */
class FormatNumberAction : DeclaredAction(ActionCatalog.require("number.format")) {
    override suspend fun run(ctx: ActionContext, args: Map<String, String>): ActionResult {
        val output = utilityOutput(args, "Formatted") ?: return ActionResult.Failure("invalid output variable")
        val formatted = NumberFormatting.format(
            args["number"].orEmpty(),
            args["decimals"]?.trim().orEmpty().ifEmpty { "2" }.toIntOrNull() ?: return ActionResult.Failure("decimals must be a whole number"),
            args["grouping"]?.trim()?.lowercase() != "false",
            java.util.Locale.getDefault(),
        ) ?: return ActionResult.Failure("number is not a number, or decimals is outside 0 to 10")
        ctx.variables.set(output, formatted)
        return ActionResult.Success
    }
}

internal object NumberFormatting {
    fun format(raw: String, decimals: Int, grouping: Boolean, locale: java.util.Locale): String? {
        if (decimals !in 0..10) return null
        val value = raw.trim().replace(',', '.').toBigDecimalOrNull() ?: return null
        val format = java.text.NumberFormat.getNumberInstance(locale).apply {
            minimumFractionDigits = decimals
            maximumFractionDigits = decimals
            isGroupingUsed = grouping
            roundingMode = RoundingMode.HALF_UP
        }
        return format.format(value)
    }
}
