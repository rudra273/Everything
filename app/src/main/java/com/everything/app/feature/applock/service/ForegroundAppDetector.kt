package com.everything.app.feature.applock.service

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context

class ForegroundAppDetector(
    context: Context,
) {
    private val usageStatsManager =
        context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    fun currentForegroundPackage(): String? {
        val end = System.currentTimeMillis()
        val begin = end - LOOKBACK_MILLIS
        val events = usageStatsManager.queryEvents(begin, end)
        val event = UsageEvents.Event()
        var foregroundPackage: String? = null

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND ||
                event.eventType == UsageEvents.Event.ACTIVITY_RESUMED
            ) {
                foregroundPackage = event.packageName
            }
        }

        return foregroundPackage
    }

    private companion object {
        const val LOOKBACK_MILLIS = 5_000L
    }
}
