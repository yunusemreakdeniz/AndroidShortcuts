package com.opentasker.ui.screens

import com.opentasker.core.actions.ActionMetadataRegistry
import com.opentasker.core.actions.registerActionMetadata
import org.junit.Assert.assertTrue
import org.junit.Test

class GalleryTemplatesTest {
    @Test
    fun everyGalleryStepIsARegisteredAction() {
        registerActionMetadata()
        val unknown = galleryShortcuts.flatMap { shortcut -> shortcut.actions { "" }.map { shortcut.id to it.type } }
            .filter { (_, type) -> ActionMetadataRegistry.get(type) == null }
        assertTrue("Gallery steps without a registered action: $unknown", unknown.isEmpty())
    }

    @Test
    fun galleryIdsAreUnique() {
        val ids = galleryShortcuts.map { it.id }
        assertTrue("Duplicate gallery ids: $ids", ids.size == ids.toSet().size)
    }
}
