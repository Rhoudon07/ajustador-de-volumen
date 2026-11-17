package com.example.ajustadordevolumen.audio

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.AudioPlaybackConfiguration
import android.os.Build
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.lang.reflect.Method

class AppVolumeManager(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val packageManager = context.packageManager
    private val prefs: SharedPreferences = context.getSharedPreferences("app_volume_prefs", Context.MODE_PRIVATE)

    private val _activeApps = MutableStateFlow<List<AppAudioInfo>>(emptyList())
    val activeApps: StateFlow<List<AppAudioInfo>> = _activeApps.asStateFlow()

    private var playbackCallback: AudioManager.AudioPlaybackCallback? = null

    var gameMasterVolume: Int = prefs.getInt("master_game_vol", 100)
        private set
    var isGameMasterMuted: Boolean = prefs.getBoolean("master_game_muted", false)
        private set

    var mediaMasterVolume: Int = prefs.getInt("master_media_vol", 100)
        private set
    var isMediaMasterMuted: Boolean = prefs.getBoolean("master_media_muted", false)
        private set

    init {
        initPlaybackListener()
    }

    private fun initPlaybackListener() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            playbackCallback = object : AudioManager.AudioPlaybackCallback() {
                override fun onPlaybackConfigChanged(configs: MutableList<AudioPlaybackConfiguration>?) {
                    super.onPlaybackConfigChanged(configs)
                    updateActiveApps(configs ?: emptyList())
                }
            }
            audioManager.registerAudioPlaybackCallback(playbackCallback!!, Handler(Looper.getMainLooper()))
            // Initial poll
            val initialConfigs = audioManager.activePlaybackConfigurations
            updateActiveApps(initialConfigs)
        }
    }

    fun refresh() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val configs = audioManager.activePlaybackConfigurations
            updateActiveApps(configs)
        }
    }

    private fun updateActiveApps(configs: List<AudioPlaybackConfiguration>) {
        val detectedApps = mutableMapOf<String, AppAudioInfo>()

        for (config in configs) {
            val uid = getClientUid(config)
            val sessionId = getSessionId(config)

            val packageName = if (uid > 0) {
                packageManager.getPackagesForUid(uid)?.firstOrNull()
            } else {
                null
            }

            if (packageName != null && packageName != context.packageName) {
                if (!detectedApps.containsKey(packageName)) {
                    val appName = try {
                        val appInfo = packageManager.getApplicationInfo(packageName, 0)
                        packageManager.getApplicationLabel(appInfo).toString()
                    } catch (_: Exception) {
                        packageName
                    }

                    val iconBitmap = try {
                        val drawable = packageManager.getApplicationIcon(packageName)
                        drawableToBitmap(drawable)
                    } catch (_: Exception) {
                        null
                    }

                    val isGame = isAppGame(packageName, config)
                    val defaultVol = if (isGame) gameMasterVolume else mediaMasterVolume
                    val savedVolume = prefs.getInt("vol_$packageName", defaultVol)
                    val isMuted = prefs.getBoolean("mute_$packageName", false)

                    detectedApps[packageName] = AppAudioInfo(
                        packageName = packageName,
                        appName = appName,
                        appIconBitmap = iconBitmap,
                        volumePercent = if (isMuted) 0 else savedVolume,
                        isMuted = isMuted,
                        isPlaying = true,
                        isGame = isGame,
                        audioSessionId = sessionId
                    )

                    // Apply volume scale via proxy if possible
                    val playerProxy = getPlayerProxy(config)
                    if (playerProxy != null) {
                        val targetVolume = if (isMuted) 0.0f else (savedVolume / 100f)
                        setPlayerProxyVolume(playerProxy, targetVolume)
                    }
                }
            }
        }

        // If no apps detected via system UID reflection, check if music is active
        if (detectedApps.isEmpty() && audioManager.isMusicActive) {
            val recentPackages = prefs.getStringSet("recent_media_apps", emptySet()) ?: emptySet()
            for (pkg in recentPackages) {
                try {
                    val appInfo = packageManager.getApplicationInfo(pkg, 0)
                    val appName = packageManager.getApplicationLabel(appInfo).toString()
                    val iconBitmap = drawableToBitmap(packageManager.getApplicationIcon(appInfo))
                    val isGame = isAppGame(pkg, null)
                    val defaultVol = if (isGame) gameMasterVolume else mediaMasterVolume
                    val savedVolume = prefs.getInt("vol_$pkg", defaultVol)
                    val isMuted = prefs.getBoolean("mute_$pkg", false)

                    detectedApps[pkg] = AppAudioInfo(
                        packageName = pkg,
                        appName = appName,
                        appIconBitmap = iconBitmap,
                        volumePercent = if (isMuted) 0 else savedVolume,
                        isMuted = isMuted,
                        isPlaying = true,
                        isGame = isGame
                    )
                } catch (_: Exception) {
                }
            }
        }

        _activeApps.value = detectedApps.values.toList()
    }

    @Suppress("DEPRECATION")
    fun isAppGame(packageName: String, config: AudioPlaybackConfiguration?): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && config != null) {
            try {
                if (config.audioAttributes.usage == AudioAttributes.USAGE_GAME) {
                    return true
                }
            } catch (_: Exception) {
            }
        }
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            if ((appInfo.flags and ApplicationInfo.FLAG_IS_GAME) != 0) {
                return true
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && appInfo.category == ApplicationInfo.CATEGORY_GAME) {
                return true
            }
            false
        } catch (_: Exception) {
            false
        }
    }

    fun setGameMasterVolume(volumePercent: Int) {
        val clamped = volumePercent.coerceIn(0, 100)
        gameMasterVolume = clamped
        isGameMasterMuted = clamped == 0
        prefs.edit().putInt("master_game_vol", clamped).putBoolean("master_game_muted", isGameMasterMuted).apply()

        // Apply to all active game apps
        for (app in _activeApps.value.filter { it.isGame }) {
            setAppVolume(app.packageName, clamped)
        }
    }

    fun toggleGameMasterMute() {
        isGameMasterMuted = !isGameMasterMuted
        prefs.edit().putBoolean("master_game_muted", isGameMasterMuted).apply()
        val targetVolume = if (isGameMasterMuted) 0 else gameMasterVolume

        for (app in _activeApps.value.filter { it.isGame }) {
            setAppVolume(app.packageName, targetVolume)
        }
    }

    fun setMediaMasterVolume(volumePercent: Int) {
        val clamped = volumePercent.coerceIn(0, 100)
        mediaMasterVolume = clamped
        isMediaMasterMuted = clamped == 0
        prefs.edit().putInt("master_media_vol", clamped).putBoolean("master_media_muted", isMediaMasterMuted).apply()

        // Apply to all active non-game apps
        for (app in _activeApps.value.filter { !it.isGame }) {
            setAppVolume(app.packageName, clamped)
        }
    }

    fun toggleMediaMasterMute() {
        isMediaMasterMuted = !isMediaMasterMuted
        prefs.edit().putBoolean("master_media_muted", isMediaMasterMuted).apply()
        val targetVolume = if (isMediaMasterMuted) 0 else mediaMasterVolume

        for (app in _activeApps.value.filter { !it.isGame }) {
            setAppVolume(app.packageName, targetVolume)
        }
    }

    fun setAppVolume(packageName: String, volumePercent: Int) {
        val clampedVolume = volumePercent.coerceIn(0, 100)
        prefs.edit().putInt("vol_$packageName", clampedVolume).apply()
        if (clampedVolume > 0) {
            prefs.edit().putBoolean("mute_$packageName", false).apply()
        }

        // Save to recent list
        val recent = (prefs.getStringSet("recent_media_apps", emptySet()) ?: emptySet()).toMutableSet()
        recent.add(packageName)
        prefs.edit().putStringSet("recent_media_apps", recent).apply()

        // Apply via player proxy if active
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val configs = audioManager.activePlaybackConfigurations
            for (config in configs) {
                val uid = getClientUid(config)
                val pkg = if (uid > 0) packageManager.getPackagesForUid(uid)?.firstOrNull() else null
                if (pkg == packageName) {
                    val playerProxy = getPlayerProxy(config)
                    if (playerProxy != null) {
                        setPlayerProxyVolume(playerProxy, clampedVolume / 100f)
                    }
                }
            }
        }

        // Update local state
        _activeApps.value = _activeApps.value.map { app ->
            if (app.packageName == packageName) {
                app.copy(volumePercent = clampedVolume, isMuted = clampedVolume == 0)
            } else {
                app
            }
        }
    }

    fun toggleAppMute(packageName: String) {
        val current = _activeApps.value.find { it.packageName == packageName } ?: return
        val newMuted = !current.isMuted
        prefs.edit().putBoolean("mute_$packageName", newMuted).apply()

        val newVolume = if (newMuted) 0 else prefs.getInt("vol_$packageName", 100)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val configs = audioManager.activePlaybackConfigurations
            for (config in configs) {
                val uid = getClientUid(config)
                val pkg = if (uid > 0) packageManager.getPackagesForUid(uid)?.firstOrNull() else null
                if (pkg == packageName) {
                    val playerProxy = getPlayerProxy(config)
                    if (playerProxy != null) {
                        setPlayerProxyVolume(playerProxy, if (newMuted) 0.0f else (newVolume / 100f))
                    }
                }
            }
        }

        _activeApps.value = _activeApps.value.map { app ->
            if (app.packageName == packageName) {
                app.copy(volumePercent = newVolume, isMuted = newMuted)
            } else {
                app
            }
        }
    }

    private fun getClientUid(config: AudioPlaybackConfiguration): Int {
        return try {
            val method: Method = config.javaClass.getMethod("getClientUid")
            method.invoke(config) as? Int ?: -1
        } catch (_: Exception) {
            -1
        }
    }

    private fun getSessionId(config: AudioPlaybackConfiguration): Int {
        return try {
            val method = config.javaClass.methods.firstOrNull {
                it.name == "getSessionId" || it.name == "getClientAudioSessionId"
            }
            method?.invoke(config) as? Int ?: 0
        } catch (_: Exception) {
            0
        }
    }

    private fun getPlayerProxy(config: AudioPlaybackConfiguration): Any? {
        return try {
            val method: Method = config.javaClass.getMethod("getPlayerProxy")
            method.invoke(config)
        } catch (_: Exception) {
            null
        }
    }

    private fun setPlayerProxyVolume(playerProxy: Any, volume: Float) {
        try {
            val method: Method = playerProxy.javaClass.getMethod("setVolume", Float::class.javaPrimitiveType)
            method.invoke(playerProxy, volume)
        } catch (_: Exception) {
        }
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            return drawable.bitmap
        }
        val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 96
        val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 96
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }
}
