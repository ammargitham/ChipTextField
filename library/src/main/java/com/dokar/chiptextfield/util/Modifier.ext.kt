package com.dokar.chiptextfield.util

import androidx.compose.ui.Modifier

internal inline fun Modifier.runIf(
    value: Boolean,
    block: Modifier.() -> Modifier
): Modifier {
    return if (value) {
        this.block()
    } else {
        this
    }
}
