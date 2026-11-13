package com.example.aod

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

class AODTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        val manager = SamsungAodManager(applicationContext)
        val status = manager.inspectDeviceStatus()

        qsTile?.apply {
            state = if (status.autoServiceRunning || status.isNativeAodEnabled) {
                Tile.STATE_ACTIVE
            } else {
                Tile.STATE_INACTIVE
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                subtitle = if (status.hasSamsungAodPackage) "Samsung" else "AMOLED"
            }
            updateTile()
        }
    }

    override fun onClick() {
        super.onClick()
        SamsungAodManager(applicationContext).applyMaximumPerformanceOptimizations(
            enableAod = true,
            tapToShow = false
        )
        val intent = Intent(this, AODDisplayActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val pendingIntent = PendingIntent.getActivity(
                this,
                1001,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            startActivityAndCollapse(pendingIntent)
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }
}
