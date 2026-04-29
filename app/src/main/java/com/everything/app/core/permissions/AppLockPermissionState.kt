package com.everything.app.core.permissions

data class AppLockPermissionState(
    val usageAccess: Boolean,
    val overlay: Boolean,
    val notifications: Boolean,
) {
    val allGranted: Boolean = usageAccess && overlay && notifications
}
