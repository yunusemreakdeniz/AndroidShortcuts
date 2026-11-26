package com.opentasker.ui.share

import org.junit.Assert.assertEquals
import org.junit.Test

class ShareToShortcutVariablesTest {
    @Test
    fun sharedTextBecomesShortcutInput() {
        val vars = ShareToShortcutActivity.shareVariables(mapOf("share_text" to "Merhaba", "share_uri" to ""))
        assertEquals("Merhaba", vars["ShortcutInput"])
        assertEquals("Merhaba", vars["share_text"])
    }

    @Test
    fun aLinkOrFileIsTheInputWhenThereIsNoText() {
        val vars = ShareToShortcutActivity.shareVariables(mapOf("share_text" to " ", "share_uri" to "content://x/1"))
        assertEquals("content://x/1", vars["ShortcutInput"])
    }
}
