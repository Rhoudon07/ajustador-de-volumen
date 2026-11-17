package com.example.ajustadordevolumen.ui.main

import android.content.Intent
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.SwipeDown
import androidx.compose.material.icons.filled.VideogameAsset
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ajustadordevolumen.ui.components.AppVolumeSlider
import com.example.ajustadordevolumen.ui.components.CategoryMasterSlider
import com.example.ajustadordevolumen.ui.components.VolumeStreamSlider
import com.example.ajustadordevolumen.ui.dialog.VolumeDialogActivity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    viewModel: MainScreenViewModel = viewModel()
) {
    val state by viewModel.mixerState.collectAsState()
    val context = LocalContext.current

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Ajustador de Volumen",
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Status Card
            item {
                StatusBanner(
                    isInCall = state.isInCall,
                    isGameActive = state.isGameActive,
                    isMusicActive = state.isMusicActive,
                    activeAppsCount = state.activeApps.size
                )
            }

            // Quick Settings Tutorial Card
            item {
                QuickSettingsGuideCard(
                    onTestDialogClick = {
                        val intent = Intent(context, VolumeDialogActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(intent)
                    }
                )
            }

            // SEPARATED CHANNELS SECTION: GAMES & MULTIMEDIA
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Canales Separados",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Juegos vs Multimedia",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Game Master Slider
            item {
                CategoryMasterSlider(
                    title = "Volumen de Juegos",
                    subtitle = if (state.isGameActive) "Juego en ejecución detectado" else "Nivel exclusivo para juegos",
                    icon = Icons.Default.VideogameAsset,
                    volumePercent = state.gameVolumePercent,
                    isMuted = state.isGameMuted,
                    accentColor = Color(0xFFE65100),
                    badgeText = if (state.isGameActive) "🎮 Activo" else "Juegos",
                    onVolumeChanged = { viewModel.setGameMasterVolume(it) },
                    onToggleMute = { viewModel.toggleGameMasterMute() }
                )
            }

            // Media Master Slider
            item {
                CategoryMasterSlider(
                    title = "Volumen Multimedia",
                    subtitle = "Música, videos, YouTube y reproductores",
                    icon = Icons.Default.MusicNote,
                    volumePercent = state.mediaVolumePercent,
                    isMuted = state.isMediaMuted,
                    accentColor = Color(0xFF1565C0),
                    badgeText = "Música/Video",
                    onVolumeChanged = { viewModel.setMediaMasterVolume(it) },
                    onToggleMute = { viewModel.toggleMediaMasterMute() }
                )
            }

            // Per-App Volume Section Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Apps,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Apps Específicas Detectadas",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (state.activeApps.isNotEmpty()) {
                        Text(
                            text = "${state.activeApps.size} activa(s)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.tertiary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Active Apps Sliders
            if (state.activeApps.isNotEmpty()) {
                items(state.activeApps, key = { it.packageName }) { app ->
                    AppVolumeSlider(
                        app = app,
                        onVolumeChanged = { newVol ->
                            viewModel.setAppVolume(app.packageName, newVol)
                        },
                        onToggleMute = {
                            viewModel.toggleAppMute(app.packageName)
                        }
                    )
                }
            } else {
                item {
                    NoActiveAppsCard()
                }
            }

            // Sliders Section Header (System streams: Calls, Ringtones, Alarms)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Canales del Sistema (Llamadas / Avisos)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Ajuste en vivo",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Volume Sliders (System streams excluding duplicate media)
            items(state.streams.filter { !it.isMediaStream }, key = { it.streamType }) { stream ->
                val isHighlighted = stream.isCallStream && state.isInCall

                VolumeStreamSlider(
                    stream = stream,
                    isHighlighted = isHighlighted,
                    onVolumeChanged = { newVol ->
                        viewModel.setVolume(stream.streamType, newVol)
                    },
                    onToggleMute = {
                        viewModel.toggleMute(stream.streamType)
                    }
                )
            }

            // Info Card about Audio Streams and Separation
            item {
                InfoAudioChannelsCard()
            }
        }
    }
}

@Composable
private fun StatusBanner(
    isInCall: Boolean,
    isGameActive: Boolean,
    isMusicActive: Boolean,
    activeAppsCount: Int
) {
    val containerColor = if (isInCall) {
        MaterialTheme.colorScheme.primaryContainer
    } else if (isGameActive) {
        Color(0xFFFFE0B2) // Light amber
    } else {
        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
    }

    val contentColor = if (isInCall) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else if (isGameActive) {
        Color(0xFFE65100) // Deep amber
    } else {
        MaterialTheme.colorScheme.onSecondaryContainer
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(contentColor.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                val icon = if (isInCall) {
                    Icons.Default.PhoneInTalk
                } else if (isGameActive) {
                    Icons.Default.VideogameAsset
                } else {
                    Icons.Default.Layers
                }
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                val title = if (isInCall) {
                    "Llamada Activa Detectada"
                } else if (isGameActive) {
                    "Juego en Ejecución"
                } else {
                    "Canales de Audio Listos"
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                )
                val description = if (isInCall) {
                    "Puedes regular la llamada, el juego y la música por separado."
                } else if (isGameActive) {
                    "Regula el juego independientemente de tu música o videos."
                } else if (activeAppsCount > 0) {
                    "$activeAppsCount aplicación(es) detectada(s) con audio activo."
                } else if (isMusicActive) {
                    "Audio multimedia activo. Abre el juego o video para calibrar."
                } else {
                    "Juegos y multimedia ahora están en canales separados."
                }
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor.copy(alpha = 0.85f)
                )
            }
        }
    }
}

@Composable
private fun NoActiveAppsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Apps,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Detección Inteligente de Apps",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Los juegos se identificarán con 🎮 y las apps de música/video con 🎵 al empezar a reproducir audio.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun QuickSettingsGuideCard(onTestDialogClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.SwipeDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Cómo añadir al Panel Superior",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            InstructionStep(
                number = "1",
                text = "Desliza dos veces hacia abajo la barra superior de tu teléfono para abrir los Ajustes Rápidos."
            )
            Spacer(modifier = Modifier.height(8.dp))
            InstructionStep(
                number = "2",
                text = "Toca el icono del lápiz ✏️ (Editar botones) en la parte superior del panel."
            )
            Spacer(modifier = Modifier.height(8.dp))
            InstructionStep(
                number = "3",
                text = "Busca el botón 'Mezclador Vol.' y arrástralo a tus accesos activos."
            )

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = onTestDialogClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Probar Mezclador Flotante ahora")
            }
        }
    }
}

@Composable
private fun InstructionStep(number: String, text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(MaterialTheme.colorScheme.primary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun InfoAudioChannelsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Separación Inteligente: La app distingue juegos (AudioAttributes.USAGE_GAME / CATEGORY_GAME) de multimedia (USAGE_MEDIA), permitiendo ajustar los juegos sin afectar tu música o videos de fondo.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
