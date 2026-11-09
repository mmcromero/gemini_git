package com.mmcromero.promptflutuante

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent

class AIAccessibilityService : AccessibilityService() {

    private val aiAppPackages = setOf(
        "com.openai.chatgpt",
        "com.google.android.apps.bard",
        "com.anthropic.claude.chat",
        "com.perplexity.perplexity",
        "com.microsoft.copilot"
    )

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString() ?: return

            val intent = Intent(this, FloatingService::class.java).apply {
                action = if (aiAppPackages.contains(packageName)) {
                    "SHOW_BUBBLE"
                } else {
                    "HIDE_BUBBLE"
                }
            }
            startService(intent)
        }
    }

    override fun onInterrupt() {}
}
