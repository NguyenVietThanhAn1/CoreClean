package com.coreclean.app.data.datasource.ram

import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager
import com.coreclean.app.domain.model.RamInfo
import com.coreclean.app.domain.model.RunningAppInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import javax.inject.Inject

class RamDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val am by lazy { context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager }
    private val pm: PackageManager get() = context.packageManager

    fun observe(): Flow<RamInfo> = flow {
        while (currentCoroutineContext().isActive) {
            emit(snapshot())
            delay(2_000)
        }
    }

    private fun snapshot(): RamInfo {
        val memInfo = ActivityManager.MemoryInfo()
        am.getMemoryInfo(memInfo)
        val totalMb     = memInfo.totalMem / 1_048_576L
        val availableMb = memInfo.availMem / 1_048_576L
        val usedMb      = totalMb - availableMb
        val threshMb    = memInfo.threshold / 1_048_576L

        // NOTE: runningAppProcesses returns only our own process on Android 5+
        // — by platform design, full process list requires system privileges.
        val topApps = runCatching {
            val processes = am.runningAppProcesses ?: emptyList()
            val pids = processes.map { it.pid }.toIntArray()
            val memInfoArray = am.getProcessMemoryInfo(pids)
            processes.zip(memInfoArray).mapNotNull { (proc, mi) ->
                val memMb = mi.totalPrivateDirty / 1024L
                if (memMb > 0) {
                    val appName = proc.pkgList?.firstOrNull()?.let { pkg ->
                        runCatching {
                            pm.getApplicationInfo(pkg, 0).let { pm.getApplicationLabel(it).toString() }
                        }.getOrNull()
                    } ?: proc.processName
                    RunningAppInfo(
                        packageName = proc.pkgList?.firstOrNull() ?: proc.processName,
                        appName     = appName ?: proc.processName,
                        memoryMb    = memMb
                    )
                } else null
            }.sortedByDescending { it.memoryMb }.take(10)
        }.getOrDefault(emptyList())

        return RamInfo(totalMb, availableMb, usedMb, threshMb, memInfo.lowMemory, topApps)
    }
}
