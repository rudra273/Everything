package com.rudra.everything.feature.applock.domain

import android.content.Context
import android.content.Intent
import android.provider.Settings

object SettingsPackageResolver {
    private val knownSettingsPackages = setOf(
        "com.android.settings",
    )

    fun resolve(context: Context): Set<String> {
        val packageManager = context.packageManager
        val intent = Intent(Settings.ACTION_SETTINGS)
        val packages = knownSettingsPackages.toMutableSet()

        packageManager.resolveActivity(intent, 0)
            ?.activityInfo
            ?.packageName
            ?.let(packages::add)

        packageManager.queryIntentActivities(intent, 0)
            .mapNotNull { it.activityInfo?.packageName }
            .forEach(packages::add)

        return packages
    }
}
