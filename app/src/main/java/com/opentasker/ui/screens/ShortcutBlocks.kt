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
    val closer = blockClosers[added.type] ?: return actions + added
    return actions + added + ActionSpec(type = closer)
}
