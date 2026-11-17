package com.example.ajustadordevolumen.audio

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class VolumeRepository(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val scope = CoroutineScope(Dispatchers.Main.immediate)
    private val appVolumeManager = AppVolumeManager(context)

    private val _mixerState = MutableStateFlow(getFreshAudioState())
    val mixerState: StateFlow<AudioMixerState> = _mixerState.asStateFlow()

    private val volumeReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, intent: Intent?) {
            refresh()
        }
    }

    init {
        val filter = IntentFilter().apply {
            addAction("android.media.VOLUME_CHANGED_ACTION")
            addAction(AudioManager.RINGER_MODE_CHANGED_ACTION)
            addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(volumeReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            context.registerReceiver(volumeReceiver, filter)
        }

        // Listen for active apps volume updates
        scope.launch {
            appVolumeManager.activeApps.collect { apps ->
                _mixerState.value = _mixerState.value.copy(activeApps = apps)
            }
        }
    }

    fun refresh() {
        appVolumeManager.refresh()
        scope.launch {
            _mixerState.value = getFreshAudioState()
        }
    }

    fun setVolume(streamType: Int, volume: Int) {
        try {
            val min = getMinVolume(streamType)
            val max = audioManager.getStreamMaxVolume(streamType)
            val safeVolume = volume.coerceIn(min, max)
            audioManager.setStreamVolume(streamType, safeVolume, 0)
            refresh()
        } catch (_: Exception) {
            // Some streams might be restricted by Do Not Disturb policy
        }
    }

    fun toggleMute(streamType: Int) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val isMuted = audioManager.isStreamMute(streamType)
                val direction = if (isMuted) AudioManager.ADJUST_UNMUTE else AudioManager.ADJUST_MUTE
                audioManager.adjustStreamVolume(streamType, direction, 0)
            } else {
                val current = audioManager.getStreamVolume(streamType)
                if (current > 0) {
                    audioManager.setStreamVolume(streamType, 0, 0)
                } else {
                    val half = audioManager.getStreamMaxVolume(streamType) / 2
                    audioManager.setStreamVolume(streamType, half, 0)
                }
            }
            refresh()
        } catch (_: Exception) {
        }
    }

    fun setGameMasterVolume(volumePercent: Int) {
        appVolumeManager.setGameMasterVolume(volumePercent)
        // If a game is active or detected, adjust the media stream as target
        if (appVolumeManager.activeApps.value.any { it.isGame }) {
            val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val min = getMinVolume(AudioManager.STREAM_MUSIC)
            val target = (min + (volumePercent * (max - min) / 100f)).toInt().coerceIn(min, max)
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0)
        }
        refresh()
    }

    fun toggleGameMasterMute() {
        appVolumeManager.toggleGameMasterMute()
        if (appVolumeManager.activeApps.value.any { it.isGame }) {
            toggleMute(AudioManager.STREAM_MUSIC)
        }
        refresh()
    }

    fun setMediaMasterVolume(volumePercent: Int) {
        appVolumeManager.setMediaMasterVolume(volumePercent)
        // Set real hardware volume for STREAM_MUSIC
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val min = getMinVolume(AudioManager.STREAM_MUSIC)
        val target = (min + (volumePercent * (max - min) / 100f)).toInt().coerceIn(min, max)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0)
        refresh()
    }

    fun toggleMediaMasterMute() {
        appVolumeManager.toggleMediaMasterMute()
        toggleMute(AudioManager.STREAM_MUSIC)
        refresh()
    }

    fun setAppVolume(packageName: String, volumePercent: Int) {
        appVolumeManager.setAppVolume(packageName, volumePercent)
    }

    fun toggleAppMute(packageName: String) {
        appVolumeManager.toggleAppMute(packageName)
    }

    fun isInCall(): Boolean {
        val mode = audioManager.mode
        return mode == AudioManager.MODE_IN_CALL || mode == AudioManager.MODE_IN_COMMUNICATION
    }

    fun isMusicActive(): Boolean {
        return audioManager.isMusicActive
    }

    private fun getMinVolume(streamType: Int): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            audioManager.getStreamMinVolume(streamType)
        } else {
            0
        }
    }

    fun getFreshAudioState(): AudioMixerState {
        val inCall = isInCall()
        val musicActive = isMusicActive()

        val streamsList = listOf(
            AudioStreamInfo(
                streamType = AudioManager.STREAM_VOICE_CALL,
                category = StreamCategory.CALL,
                title = "Llamada de voz",
                subtitle = if (inCall) "Llamada activa (VoIP / Telefónica)" else "Volumen de llamadas",
                currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL),
                minVolume = getMinVolume(AudioManager.STREAM_VOICE_CALL),
                maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL),
                isMuted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) audioManager.isStreamMute(AudioManager.STREAM_VOICE_CALL) else false,
                isCallStream = true
            ),
            AudioStreamInfo(
                streamType = AudioManager.STREAM_MUSIC,
                category = StreamCategory.MEDIA,
                title = "Multimedia y Juegos",
                subtitle = if (musicActive) "Sonando en apps o juegos" else "Apps, juegos, videos y música",
                currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC),
                minVolume = getMinVolume(AudioManager.STREAM_MUSIC),
                maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC),
                isMuted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) audioManager.isStreamMute(AudioManager.STREAM_MUSIC) else false,
                isMediaStream = true
            ),
            AudioStreamInfo(
                streamType = AudioManager.STREAM_RING,
                category = StreamCategory.RING,
                title = "Tono de llamada",
                subtitle = "Alertas de llamadas entrantes",
                currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_RING),
                minVolume = getMinVolume(AudioManager.STREAM_RING),
                maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING),
                isMuted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) audioManager.isStreamMute(AudioManager.STREAM_RING) else false
            ),
            AudioStreamInfo(
                streamType = AudioManager.STREAM_NOTIFICATION,
                category = StreamCategory.NOTIFICATION,
                title = "Notificaciones",
                subtitle = "Mensajes y avisos del sistema",
                currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION),
                minVolume = getMinVolume(AudioManager.STREAM_NOTIFICATION),
                maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION),
                isMuted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) audioManager.isStreamMute(AudioManager.STREAM_NOTIFICATION) else false
            ),
            AudioStreamInfo(
                streamType = AudioManager.STREAM_ALARM,
                category = StreamCategory.ALARM,
                title = "Alarmas",
                subtitle = "Despertadores y temporizadores",
                currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM),
                minVolume = getMinVolume(AudioManager.STREAM_ALARM),
                maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM),
                isMuted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) audioManager.isStreamMute(AudioManager.STREAM_ALARM) else false
            )
        )

        val isGameActive = appVolumeManager.activeApps.value.any { it.isGame }
        val musicStream = streamsList.firstOrNull { it.streamType == AudioManager.STREAM_MUSIC }
        val systemMediaPercent = musicStream?.percentage ?: 100
        val systemMediaMuted = musicStream?.isMuted ?: false

        return AudioMixerState(
            streams = streamsList,
            activeApps = appVolumeManager.activeApps.value,
            gameVolumePercent = appVolumeManager.gameMasterVolume,
            isGameMuted = appVolumeManager.isGameMasterMuted,
            isGameActive = isGameActive,
            mediaVolumePercent = systemMediaPercent,
            isMediaMuted = systemMediaMuted,
            isInCall = inCall,
            isMusicActive = musicActive
        )
    }

    companion object {
        @Volatile
        private var instance: VolumeRepository? = null

        fun getInstance(context: Context): VolumeRepository {
            return instance ?: synchronized(this) {
                instance ?: VolumeRepository(context.applicationContext).also { instance = it }
            }
        }
    }
}
