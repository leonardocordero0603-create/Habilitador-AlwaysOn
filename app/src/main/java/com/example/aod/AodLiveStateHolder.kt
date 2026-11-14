package com.example.aod

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.drawable.Drawable
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.service.notification.StatusBarNotification
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AodNotificationIconItem(
    val key: String,
    val packageName: String,
    val appName: String,
    val iconBitmap: ImageBitmap,
    val isMonochromeSmallIcon: Boolean,
    val postTime: Long
)

data class AodNotificationsSnapshot(
    val icons: List<AodNotificationIconItem> = emptyList(),
    val totalCount: Int = 0
)

data class SamsungMusicState(
    val title: String,
    val artist: String,
    val isPlaying: Boolean,
    val albumArt: ImageBitmap? = null,
    val accentArgb: Int? = null,
    val durationMs: Long = 0L,
    val positionMs: Long = 0L,
    val playbackSpeed: Float = 1f,
    val positionUpdateTimeMs: Long = 0L
)

object AodLiveStateHolder {

    private val SAMSUNG_MUSIC_PACKAGES = setOf(
        "com.sec.android.app.music",
        "com.samsung.android.music"
    )

    private val mainHandler = Handler(Looper.getMainLooper())
    private val mediaLock = Any()

    private val _notifications = MutableStateFlow(AodNotificationsSnapshot())
    val notifications: StateFlow<AodNotificationsSnapshot> = _notifications.asStateFlow()

    private val _samsungMusic = MutableStateFlow<SamsungMusicState?>(null)
    val samsungMusic: StateFlow<SamsungMusicState?> = _samsungMusic.asStateFlow()

    @Volatile
    private var activeSamsungController: MediaController? = null

    private var sessionManager: MediaSessionManager? = null
    private var activeSessionsListener: MediaSessionManager.OnActiveSessionsChangedListener? = null

    private val mediaCallback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) {
            publishSamsungMusicFromController(activeSamsungController)
        }

        override fun onMetadataChanged(metadata: MediaMetadata?) {
            publishSamsungMusicFromController(activeSamsungController)
        }

        override fun onSessionDestroyed() {
            detachCurrentController()
            _samsungMusic.value = null
        }
    }

    fun startMediaMonitoring(context: Context) {
        val appContext = context.applicationContext
        val component = ComponentName(appContext, AodNotificationListenerService::class.java)
        try {
            val manager = appContext.getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager
                ?: return
            synchronized(mediaLock) {
                stopMediaMonitoring()
                sessionManager = manager
                val listener = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
                    handleActiveSessions(controllers)
                }
                activeSessionsListener = listener
                manager.addOnActiveSessionsChangedListener(listener, component, mainHandler)
            }
            handleActiveSessions(manager.getActiveSessions(component))
        } catch (_: SecurityException) {
            // Notification listener permission not yet granted
            detachCurrentController()
            _samsungMusic.value = null
        } catch (_: Throwable) {
            // Ignore transient binder errors
        }
    }

    fun stopMediaMonitoring() {
        synchronized(mediaLock) {
            val listener = activeSessionsListener
            val manager = sessionManager
            if (listener != null && manager != null) {
                try {
                    manager.removeOnActiveSessionsChangedListener(listener)
                } catch (_: Throwable) {
                    // Ignored
                }
            }
            activeSessionsListener = null
            sessionManager = null
            detachCurrentController()
        }
    }

    fun refreshMediaSessions(context: Context) {
        val appContext = context.applicationContext
        val component = ComponentName(appContext, AodNotificationListenerService::class.java)
        try {
            val manager = appContext.getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager
                ?: return
            handleActiveSessions(manager.getActiveSessions(component))
        } catch (_: SecurityException) {
            detachCurrentController()
            _samsungMusic.value = null
        } catch (_: Exception) {
            // Ignored
        }
    }

    fun updateNotifications(context: Context, activeNotifications: Array<StatusBarNotification>?) {
        if (activeNotifications == null) {
            _notifications.value = AodNotificationsSnapshot()
            return
        }

        val appContext = context.applicationContext
        val pm = appContext.packageManager
        val ownPackage = appContext.packageName

        val filtered = activeNotifications
            .asSequence()
            .filter { sbn ->
                if (sbn.packageName == ownPackage) return@filter false
                val flags = sbn.notification.flags
                if ((flags and Notification.FLAG_GROUP_SUMMARY) != 0) return@filter false
                if ((flags and Notification.FLAG_ONGOING_EVENT) != 0) return@filter false
                true
            }
            .sortedByDescending { it.postTime }
            .toList()

        val distinctByPackage = LinkedHashMap<String, AodNotificationIconItem>()
        for (sbn in filtered) {
            if (distinctByPackage.containsKey(sbn.packageName)) continue

            val smallIcon = sbn.notification.smallIcon
            val loadedDrawable: Drawable? = try {
                smallIcon?.loadDrawable(appContext)
                    ?: pm.getApplicationIcon(sbn.packageName)
            } catch (_: Exception) {
                null
            }

            val bitmap = loadedDrawable?.let { drawableToBitmap(it, 64) } ?: continue
            val appName = try {
                val appInfo = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    pm.getApplicationInfo(sbn.packageName, PackageManager.ApplicationInfoFlags.of(0))
                } else {
                    @Suppress("DEPRECATION")
                    pm.getApplicationInfo(sbn.packageName, 0)
                }
                pm.getApplicationLabel(appInfo).toString()
            } catch (_: Exception) {
                sbn.packageName
            }

            distinctByPackage[sbn.packageName] = AodNotificationIconItem(
                key = sbn.key,
                packageName = sbn.packageName,
                appName = appName,
                iconBitmap = bitmap,
                isMonochromeSmallIcon = smallIcon != null,
                postTime = sbn.postTime
            )

            if (distinctByPackage.size >= 5) break
        }

        _notifications.value = AodNotificationsSnapshot(
            icons = distinctByPackage.values.toList(),
            totalCount = filtered.size
        )
    }

    fun clearAll() {
        _notifications.value = AodNotificationsSnapshot()
        detachCurrentController()
        _samsungMusic.value = null
    }

    fun skipToPrevious() {
        try {
            activeSamsungController?.transportControls?.skipToPrevious()
        } catch (_: Exception) {
            // Ignored if session ended
        }
    }

    fun togglePlayPause() {
        val controller = activeSamsungController ?: return
        try {
            val state = controller.playbackState?.state
            if (state == PlaybackState.STATE_PLAYING || state == PlaybackState.STATE_BUFFERING) {
                controller.transportControls.pause()
            } else {
                controller.transportControls.play()
            }
        } catch (_: Exception) {
            // Ignored if session ended
        }
    }

    fun skipToNext() {
        try {
            activeSamsungController?.transportControls?.skipToNext()
        } catch (_: Exception) {
            // Ignored if session ended
        }
    }

    private fun handleActiveSessions(controllers: List<MediaController>?) {
        try {
            val samsungControllers = controllers
                ?.filter {
                    try {
                        it.packageName in SAMSUNG_MUSIC_PACKAGES
                    } catch (_: Throwable) {
                        false
                    }
                }
                .orEmpty()

            val selected = samsungControllers.firstOrNull { controller ->
                try {
                    val st = controller.playbackState?.state
                    st == PlaybackState.STATE_PLAYING || st == PlaybackState.STATE_BUFFERING
                } catch (_: Throwable) {
                    false
                }
            } ?: samsungControllers.firstOrNull { controller ->
                try {
                    val st = controller.playbackState?.state
                    val hasTitle = !extractTitle(controller.metadata).isNullOrBlank()
                    (st == PlaybackState.STATE_PAUSED || st == PlaybackState.STATE_STOPPED) && hasTitle
                } catch (_: Throwable) {
                    false
                }
            } ?: samsungControllers.firstOrNull { controller ->
                try {
                    !extractTitle(controller.metadata).isNullOrBlank()
                } catch (_: Throwable) {
                    false
                }
            }

            if (selected == null) {
                detachCurrentController()
                _samsungMusic.value = null
                return
            }

            synchronized(mediaLock) {
                if (activeSamsungController?.sessionToken != selected.sessionToken) {
                    detachCurrentController()
                    activeSamsungController = selected
                    try {
                        selected.registerCallback(mediaCallback, mainHandler)
                    } catch (_: Throwable) {
                        // Ignored
                    }
                }
            }

            publishSamsungMusicFromController(selected)
        } catch (_: Throwable) {
            detachCurrentController()
            _samsungMusic.value = null
        }
    }

    private fun detachCurrentController() {
        synchronized(mediaLock) {
            activeSamsungController?.let {
                try {
                    it.unregisterCallback(mediaCallback)
                } catch (_: Throwable) {
                    // Ignored
                }
            }
            activeSamsungController = null
        }
    }

    private fun publishSamsungMusicFromController(controller: MediaController?) {
        try {
            if (controller == null || controller.packageName !in SAMSUNG_MUSIC_PACKAGES) {
                _samsungMusic.value = null
                return
            }

            val playback = controller.playbackState
            val metadata = controller.metadata
            val state = playback?.state ?: PlaybackState.STATE_NONE
            if (state == PlaybackState.STATE_NONE || state == PlaybackState.STATE_ERROR) {
                val titleCheck = extractTitle(metadata)
                if (titleCheck.isNullOrBlank()) {
                    _samsungMusic.value = null
                    return
                }
            }

            val title = extractTitle(metadata) ?: "Samsung Music"
            val artist = extractArtist(metadata) ?: ""
            val isPlaying = state == PlaybackState.STATE_PLAYING || state == PlaybackState.STATE_BUFFERING
            val artBitmap = extractAlbumArt(metadata)
            val scaledArt = artBitmap?.let { scaleBitmapDown(it, 160) }
            val accentArgb = scaledArt?.let { sampleAccentArgb(it) }
            val durationMs = metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION)?.coerceAtLeast(0L) ?: 0L
            val positionMs = playback?.position?.coerceAtLeast(0L) ?: 0L
            val speed = playback?.playbackSpeed?.takeIf { it > 0f } ?: 1f
            val updateTime = playback?.lastPositionUpdateTime?.takeIf { it > 0L }
                ?: SystemClock.elapsedRealtime()

            _samsungMusic.value = SamsungMusicState(
                title = title,
                artist = artist,
                isPlaying = isPlaying,
                albumArt = scaledArt?.asImageBitmap(),
                accentArgb = accentArgb,
                durationMs = durationMs,
                positionMs = positionMs,
                playbackSpeed = speed,
                positionUpdateTimeMs = updateTime
            )
        } catch (_: Throwable) {
            _samsungMusic.value = null
        }
    }

    private fun extractTitle(metadata: MediaMetadata?): String? {
        if (metadata == null) return null
        return metadata.getString(MediaMetadata.METADATA_KEY_TITLE)
            ?.takeIf { it.isNotBlank() }
            ?: metadata.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
                ?.takeIf { it.isNotBlank() }
    }

    private fun extractArtist(metadata: MediaMetadata?): String? {
        if (metadata == null) return null
        return metadata.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?.takeIf { it.isNotBlank() }
            ?: metadata.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
                ?.takeIf { it.isNotBlank() }
            ?: metadata.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE)
                ?.takeIf { it.isNotBlank() }
    }

    private fun extractAlbumArt(metadata: MediaMetadata?): Bitmap? {
        if (metadata == null) return null
        return metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: metadata.getBitmap(MediaMetadata.METADATA_KEY_ART)
            ?: metadata.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)
    }

    private fun scaleBitmapDown(source: Bitmap, maxDimension: Int): Bitmap {
        val w = source.width
        val h = source.height
        if (w <= maxDimension && h <= maxDimension) return source
        val ratio = maxDimension.toFloat() / maxOf(w, h).toFloat()
        val targetW = (w * ratio).toInt().coerceAtLeast(1)
        val targetH = (h * ratio).toInt().coerceAtLeast(1)
        return try {
            Bitmap.createScaledBitmap(source, targetW, targetH, true)
        } catch (_: Exception) {
            source
        }
    }

    private fun sampleAccentArgb(bitmap: Bitmap): Int? {
        if (bitmap.isRecycled) return null
        return try {
            val sample = Bitmap.createScaledBitmap(bitmap, 8, 8, true)
            var rSum = 0L
            var gSum = 0L
            var bSum = 0L
            var count = 0
            for (y in 0 until sample.height) {
                for (x in 0 until sample.width) {
                    val px = sample.getPixel(x, y)
                    val r = AndroidColor.red(px)
                    val g = AndroidColor.green(px)
                    val b = AndroidColor.blue(px)
                    // Prefer colorful pixels over pure black/white borders
                    val maxC = maxOf(r, g, b)
                    val minC = minOf(r, g, b)
                    val weight = if (maxC - minC > 24) 3 else 1
                    rSum += r * weight
                    gSum += g * weight
                    bSum += b * weight
                    count += weight
                }
            }
            if (count == 0) return null
            val avgColor = AndroidColor.rgb(
                (rSum / count).toInt().coerceIn(0, 255),
                (gSum / count).toInt().coerceIn(0, 255),
                (bSum / count).toInt().coerceIn(0, 255)
            )
            val hsv = FloatArray(3)
            AndroidColor.colorToHSV(avgColor, hsv)
            hsv[1] = hsv[1].coerceAtLeast(0.45f).coerceAtMost(0.85f)
            hsv[2] = hsv[2].coerceAtLeast(0.68f).coerceAtMost(0.95f)
            AndroidColor.HSVToColor(hsv)
        } catch (_: Exception) {
            null
        }
    }

    private fun drawableToBitmap(drawable: Drawable, sizePx: Int): ImageBitmap? {
        return try {
            val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bitmap.asImageBitmap()
        } catch (_: Exception) {
            null
        }
    }
}
