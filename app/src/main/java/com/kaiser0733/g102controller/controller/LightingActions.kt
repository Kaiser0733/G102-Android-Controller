package com.kaiser0733.g102controller.controller

import com.kaiser0733.g102controller.settings.LightingConfig

/** Local edits never invoke the sender; only the three explicit actions do. */
class LightingActions(
    var config: LightingConfig = LightingConfig(),
    private val send: (List<ByteArray>, String) -> Unit,
) {
    enum class Action { ON, OFF, APPLY }

    fun execute(action: Action) {
        val packets = when (action) {
            Action.ON -> RgbCommandComposer.composeRgbOn(config)
            Action.OFF -> RgbCommandComposer.composeRgbOff()
            Action.APPLY -> RgbCommandComposer.composeModeSwitch() + RgbCommandComposer.composeEffect(config)
        }
        val label = when (action) {
            Action.ON -> "RGB ON"
            Action.OFF -> "RGB OFF"
            Action.APPLY -> "APPLY"
        }
        send(packets, label)
    }
}
