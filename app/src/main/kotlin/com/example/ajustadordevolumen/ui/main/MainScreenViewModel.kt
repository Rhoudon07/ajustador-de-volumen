package com.example.ajustadordevolumen.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.ajustadordevolumen.audio.AudioMixerState
import com.example.ajustadordevolumen.audio.VolumeRepository
import kotlinx.coroutines.flow.StateFlow

class MainScreenViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = VolumeRepository.getInstance(application)

    val mixerState: StateFlow<AudioMixerState> = repository.mixerState

    fun setVolume(streamType: Int, volume: Int) {
        repository.setVolume(streamType, volume)
    }

    fun toggleMute(streamType: Int) {
        repository.toggleMute(streamType)
    }

    fun setGameMasterVolume(volume: Int) {
        repository.setGameMasterVolume(volume)
    }

    fun toggleGameMasterMute() {
        repository.toggleGameMasterMute()
    }

    fun setMediaMasterVolume(volume: Int) {
        repository.setMediaMasterVolume(volume)
    }

    fun toggleMediaMasterMute() {
        repository.toggleMediaMasterMute()
    }

    fun setAppVolume(packageName: String, volume: Int) {
        repository.setAppVolume(packageName, volume)
    }

    fun toggleAppMute(packageName: String) {
        repository.toggleAppMute(packageName)
    }

    fun refresh() {
        repository.refresh()
    }
}
