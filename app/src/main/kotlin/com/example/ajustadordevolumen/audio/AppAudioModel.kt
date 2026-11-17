package com.example.ajustadordevolumen.audio

import android.graphics.Bitmap

data class AppAudioInfo(
    val packageName: String,
    val appName: String,
    val appIconBitmap: Bitmap? = null,
    val volumePercent: Int = 100,
    val isMuted: Boolean = false,
    val isPlaying: Boolean = true,
    val audioSessionId: Int = 0
)
