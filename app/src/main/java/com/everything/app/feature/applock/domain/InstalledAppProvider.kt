package com.everything.app.feature.applock.domain

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class InstalledAppProvider(
    private val context: Context,
) {
    suspend fun loadLaunchableApps(): List<InstalledApp> = withContext(Dispatchers.IO) {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        context.packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL)
            .asSequence()
            .map { resolveInfo ->
                val label = resolveInfo.loadLabel(context.packageManager)?.toString().orEmpty()
                InstalledApp(
                    packageName = resolveInfo.activityInfo.packageName,
                    label = label.ifBlank { resolveInfo.activityInfo.packageName },
                )
            }
            .filter { it.packageName != context.packageName }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
            .toList()
    }
}
