package com.example.ajustadordevolumen.audio

import android.media.AudioManager

enum class StreamCategory {
    CALL,
    MEDIA,
    RING,
    NOTIFICATION,
    ALARM,
    SYSTEM
}

data class AudioStreamInfo(
    val streamType: Int,
    val category: StreamCategory,
    val title: String,
    val subtitle: String,
    val currentVolume: Int,
    val minVolume: Int,
    val maxVolume: Int,
    val isMuted: Boolean,
    val isCallStream: Boolean = false,
    val isMediaStream: Boolean = false
) {
    val percentage: Int
        get() {
            val range = maxVolume - minVolume
            if (range <= 0) return 0
            val normalized = (currentVolume - minVolume).coerceAtLeast(0)
            return ((normalized.toFloat() / range.toFloat()) * 100).toInt()
        }
}

data class AudioMixerState(
    val streams: List<AudioStreamInfo> = emptyList(),
    val activeApps: List<AppAudioInfo> = emptyList(),
    val isInCall: Boolean = false,
    val isMusicActive: Boolean = false
)
