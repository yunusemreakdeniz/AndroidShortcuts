package com.opentasker.ui.screens

import com.opentasker.core.model.ActionSpec

/** Block openers and the step that closes each, like Shortcuts' "End If" and "End Repeat". */
private val blockClosers = mapOf(
    "flow.if" to "flow.endif",
    "flow.foreach" to "flow.endfor",
    "flow.try" to "flow.endtry",
)

/**
 * Appends a newly added step to a shortcut. A block opener comes with its closing step, as in
 * Shortcuts, so the shortcut is never left with an open block; steps move into it with up/down.
 */
internal fun appendWithBlockCloser(actions: List<ActionSpec>, added: ActionSpec): List<ActionSpec> {
    if (added.type == "menu.choose") return actions + added + menuBranches(added)
    val closer = blockClosers[added.type] ?: return actions + added
    return actions + added + ActionSpec(type = closer)
}

private const val MAX_MENU_BRANCHES = 12

/**
 * Shortcuts' Choose from Menu holds one branch per item. Here each item gets its own If block right
 * after the menu, so the steps for an item go inside its block. A plain item is compared by name,
 * which reads well in the editor; one with characters a condition would treat as operators is
 * compared by its position instead.
 */
internal fun menuBranches(menu: ActionSpec): List<ActionSpec> {
    val output = menu.args["var"]?.trim()?.takeIf(String::isNotEmpty) ?: "Choice"
    val items = menu.args["items"].orEmpty().split('\n').map(String::trim).filter(String::isNotEmpty)
    return items.take(MAX_MENU_BRANCHES).flatMapIndexed { index, item ->
        val plain = item.none { it in "=!<>~&|{}%()" }
        val condition = if (plain) "{{ $output }} == $item" else "{{ ${output}_index }} == ${index + 1}"
        listOf(ActionSpec(type = "flow.if", args = mapOf("condition" to condition)), ActionSpec(type = "flow.endif"))
    }
}

private val OPENERS = setOf("flow.if", "flow.foreach", "flow.try")
private val MIDDLES = setOf("flow.else", "flow.catch")
private val CLOSERS = setOf("flow.endif", "flow.endfor", "flow.endtry")

/** True for the steps that only close or split a block (shown as a slim line, like Shortcuts). */
internal fun isBlockMarker(type: String): Boolean = type in CLOSERS || type in MIDDLES

/**
 * How deep each step sits inside If / Repeat / Try blocks, so the editor can indent it like
 * Shortcuts. "Otherwise" and the end markers line up with their opener. Unbalanced lists never
 * go below zero.
 */
internal fun blockDepths(actions: List<com.opentasker.core.model.ActionSpec>): List<Int> {
    var depth = 0
    return actions.map { action ->
        when (action.type) {
            in OPENERS -> depth.also { depth++ }
            in MIDDLES -> (depth - 1).coerceAtLeast(0)
            in CLOSERS -> { depth = (depth - 1).coerceAtLeast(0); depth }
            else -> depth
        }
    }
}
