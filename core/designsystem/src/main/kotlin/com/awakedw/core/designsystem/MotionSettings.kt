package com.awakedw.core.designsystem

import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Read the platform animation scale once for the current composition.
 *
 * Android does not expose one identical "reduce motion" switch on every API
 * level. The animator and transition scales are the common system-level
 * signal used by accessibility settings, device settings and test devices.
 * A zero scale means that decorative motion should be removed rather than
 * merely made faster. This is intentionally a display concern only; it does
 * not add a user preference or change any water-recording behaviour.
 */
@Composable
fun rememberReduceMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        val resolver = context.contentResolver
        listOf(
            Settings.Global.ANIMATOR_DURATION_SCALE,
            Settings.Global.TRANSITION_ANIMATION_SCALE,
        ).mapNotNull { key ->
            runCatching { Settings.Global.getFloat(resolver, key) }.getOrNull()
        }.any { scale -> scale <= 0f }
    }
}

/** Return a stable duration for motion that can be disabled by the system. */
fun motionDurationMillis(
    durationMillis: Int,
    reduceMotion: Boolean,
): Int = if (reduceMotion) 0 else durationMillis
