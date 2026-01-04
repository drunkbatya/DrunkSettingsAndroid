package com.drunkbatya.drunksettings.ui.data

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.drawable.toBitmap
import com.drunkbatya.drunksettings.ui.model.AppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.Collator

@Composable
fun rememberInstalledApps() = run {
    val context = LocalContext.current
    produceState<List<AppInfo>?>(initialValue = null, context) {
        value = withContext(Dispatchers.IO) {
            loadInstalledApps(context)
        }
    }
}

private fun loadInstalledApps(context: Context): List<AppInfo> {
    val pm = context.packageManager
    val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
    val collator = Collator.getInstance()
    val iconSizePx = (40f * context.resources.displayMetrics.density + 0.5f).toInt()
    return apps
        .map { info ->
            val label = pm.getApplicationLabel(info).toString()
            val isSystem = (info.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            val isUpdatedSystem = (info.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
            val isUserApp = !isSystem && !isUpdatedSystem
            val icon = runCatching {
                pm.getApplicationIcon(info)
                    .toBitmap(iconSizePx, iconSizePx)
                    .asImageBitmap()
            }.getOrNull()
            AppInfo(info.packageName, label, isUserApp, icon)
        }
        .sortedWith { a, b -> collator.compare(a.label, b.label) }
}
