package com.example.ajustadordevolumen.ui.dialog

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VideogameAsset
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ajustadordevolumen.audio.VolumeRepository
import com.example.ajustadordevolumen.ui.components.AppVolumeSlider
import com.example.ajustadordevolumen.ui.components.CategoryMasterSlider
import com.example.ajustadordevolumen.ui.components.VolumeStreamSlider
import com.example.ajustadordevolumen.ui.theme.AjustadorDeVolumenTheme

class VolumeDialogActivity : ComponentActivity() {

    private lateinit var volumeRepository: VolumeRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        volumeRepository = VolumeRepository.getInstance(this)

        setContent {
            AjustadorDeVolumenTheme {
                val state by volumeRepository.mixerState.collectAsState()

                VolumeDialogOverlay(
                    state = state,
                    onDismiss = { finish() },
                    onVolumeChanged = { streamType, vol ->
                        volumeRepository.setVolume(streamType, vol)
                    },
                    onToggleMute = { streamType ->
                        volumeRepository.toggleMute(streamType)
                    },
                    onGameVolumeChanged = { vol ->
                        volumeRepository.setGameMasterVolume(vol)
                    },
                    onToggleGameMute = {
                        volumeRepository.toggleGameMasterMute()
                    },
                    onMediaVolumeChanged = { vol ->
                        volumeRepository.setMediaMasterVolume(vol)
                    },
                    onToggleMediaMute = {
                        volumeRepository.toggleMediaMasterMute()
                    },
                    onAppVolumeChanged = { pkg, vol ->
                        volumeRepository.setAppVolume(pkg, vol)
                    },
                    onToggleAppMute = { pkg ->
                        volumeRepository.toggleAppMute(pkg)
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        volumeRepository.refresh()
    }
}

@Composable
fun VolumeDialogOverlay(
    state: com.example.ajustadordevolumen.audio.AudioMixerState,
    onDismiss: () -> Unit,
    onVolumeChanged: (Int, Int) -> Unit,
    onToggleMute: (Int) -> Unit,
    onGameVolumeChanged: (Int) -> Unit,
    onToggleGameMute: () -> Unit,
    onMediaVolumeChanged: (Int) -> Unit,
    onToggleMediaMute: () -> Unit,
    onAppVolumeChanged: (String, Int) -> Unit,
    onToggleAppMute: (String) -> Unit
) {
    // Backdrop scrim
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            ),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Floating bottom sheet panel
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 24.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { /* prevent click through to scrim */ }
                ),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Mezclador de Volumen",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            if (state.isInCall) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.PhoneInTalk,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Llamada activa + Juegos / Apps",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            } else {
                                Text(
                                    text = "Juegos y Multimedia separados",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Scrollable content
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(vertical = 4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(410.dp)
                ) {
                    // SEPARATED CHANNELS: GAMES & MEDIA
                    item {
                        Text(
                            text = "Canales Separados (Juegos vs Multimedia)",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 2.dp, bottom = 2.dp)
                        )
                    }

                    // Game Master Slider
                    item {
                        CategoryMasterSlider(
                            title = "Volumen de Juegos",
                            subtitle = if (state.isGameActive) "Juego activo detectado" else "Ajuste independiente para juegos",
                            icon = Icons.Default.VideogameAsset,
                            volumePercent = state.gameVolumePercent,
                            isMuted = state.isGameMuted,
                            accentColor = Color(0xFFE65100), // Vibrant Game Amber/Orange
                            badgeText = if (state.isGameActive) "🎮 Activo" else "Juegos",
                            onVolumeChanged = onGameVolumeChanged,
                            onToggleMute = onToggleGameMute
                        )
                    }

                    // Media Master Slider
                    item {
                        CategoryMasterSlider(
                            title = "Volumen Multimedia",
                            subtitle = "Música, videos, YouTube y Spotify",
                            icon = Icons.Default.MusicNote,
                            volumePercent = state.mediaVolumePercent,
                            isMuted = state.isMediaMuted,
                            accentColor = Color(0xFF1565C0), // Media Blue
                            badgeText = "Música/Video",
                            onVolumeChanged = onMediaVolumeChanged,
                            onToggleMute = onToggleMediaMute
                        )
                    }

                    // Per-app section
                    if (state.activeApps.isNotEmpty()) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 6.dp, bottom = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Apps,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Apps Específicas Detectadas",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            }
                        }

                        items(state.activeApps, key = { it.packageName }) { app ->
                            AppVolumeSlider(
                                app = app,
                                onVolumeChanged = { newVol ->
                                    onAppVolumeChanged(app.packageName, newVol)
                                },
                                onToggleMute = {
                                    onToggleAppMute(app.packageName)
                                }
                            )
                        }
                    }

                    // System streams section (Calls, Ringtones, Alarms)
                    item {
                        Text(
                            text = "Canales del Sistema (Llamadas / Avisos)",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
                        )
                    }

                    items(state.streams.filter { !it.isMediaStream }, key = { it.streamType }) { stream ->
                        val isHighlighted = stream.isCallStream && state.isInCall

                        VolumeStreamSlider(
                            stream = stream,
                            isHighlighted = isHighlighted,
                            onVolumeChanged = { newVol ->
                                onVolumeChanged(stream.streamType, newVol)
                            },
                            onToggleMute = {
                                onToggleMute(stream.streamType)
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Listo", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
