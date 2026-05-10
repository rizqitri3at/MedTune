package com.rizqitriantomi.medtune

import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

@Composable
fun NowPlayingScreen(onLibraryClick: () -> Unit = {}) {
    val title by PlayerState.currentTitle
    val artist by PlayerState.currentArtist
    val isPlaying by PlayerState.isPlaying
    val albumArtUri by PlayerState.albumArtUri
    val isLyricsVisible by PlayerState.isLyricsVisible
    val currentPos by PlayerState.currentPosition
    val duration by PlayerState.duration
    val accentColor by PlayerState.accentColor
    val playbackSpeed by PlayerState.playbackSpeed
    
    val contentColor = getAdaptiveContentColor()
    val subtitleColor = getAdaptiveSubtitleColor()
    val context = LocalContext.current

    var showInfoDialog by remember { mutableStateOf(false) }
    val currentSong = remember(PlayerState.currentUri.value) {
        MusicPlayer.playlist.getOrNull(MusicPlayer.index)
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Album Art / Lyrics Container
        Box(
            modifier = Modifier
                .fillMaxWidth().aspectRatio(1f)
                .shadow(32.dp, RoundedCornerShape(24.dp), spotColor = accentColor)
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White.copy(alpha = 0.05f))
        ) {
            AnimatedContent(targetState = isLyricsVisible, label = "lyric_fade") { showLyrics ->
                if (showLyrics) {
                    LyricsView(currentPos)
                } else {
                    AsyncImage(model = albumArtUri, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                }
            }
        }

        Spacer(Modifier.height(32.dp))

        // Metadata (Authentic Zune/WP8 Style)
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = title, 
                color = contentColor, 
                fontSize = 42.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-2).sp,
                lineHeight = 44.sp,
                maxLines = 2, 
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = artist.lowercase(), 
                color = accentColor, 
                fontSize = 28.sp,
                fontWeight = FontWeight.Light,
                letterSpacing = (-1).sp,
                maxLines = 1
            )
        }

        Spacer(Modifier.height(24.dp))

        // Progress
        val progress = if (duration > 0) currentPos.toFloat() / duration else 0f
        Slider(
            value = progress,
            onValueChange = { MusicPlayer.seekTo((it * duration).toLong()) },
            colors = SliderDefaults.colors(thumbColor = accentColor, activeTrackColor = accentColor, inactiveTrackColor = contentColor.copy(alpha = 0.15f))
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatTime(currentPos), color = subtitleColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(formatTime(duration), color = subtitleColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.weight(1f))

        // Main Controls
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { MusicPlayer.toggleShuffle() }) {
                Icon(Icons.Default.Shuffle, null, tint = if (PlayerState.isShuffle.value) accentColor else contentColor.copy(alpha = 0.3f))
            }
            IconButton(onClick = { MusicPlayer.prev(context) }) {
                Icon(Icons.Default.SkipPrevious, null, tint = contentColor, modifier = Modifier.size(44.dp))
            }
            
            // Play/Pause Zune Style
            Surface(
                onClick = { MusicPlayer.toggle(context) },
                shape = CircleShape,
                color = Color.Transparent,
                border = BorderStroke(3.dp, accentColor),
                modifier = Modifier.size(84.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, 
                        null, 
                        tint = accentColor, 
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            IconButton(onClick = { MusicPlayer.next(context) }) {
                Icon(Icons.Default.SkipNext, null, tint = contentColor, modifier = Modifier.size(44.dp))
            }
            IconButton(onClick = { MusicPlayer.toggleRepeat() }) {
                Icon(if (PlayerState.isRepeat.value) Icons.Default.RepeatOne else Icons.Default.Repeat, null, tint = if (PlayerState.isRepeat.value) accentColor else contentColor.copy(alpha = 0.3f))
            }
        }

        // Bottom Bar
        Row(Modifier.fillMaxWidth().padding(bottom = 24.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { PlayerState.isLyricsVisible.value = !isLyricsVisible }) {
                Icon(Icons.Default.KeyboardArrowUp, null, tint = if (isLyricsVisible) accentColor else contentColor.copy(alpha = 0.4f), modifier = Modifier.size(32.dp))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                SpeedButton(playbackSpeed) { MusicPlayer.setPlaybackSpeed(it) }
                IconButton(onClick = { showInfoDialog = true }) {
                    Icon(Icons.Default.Info, null, tint = contentColor.copy(alpha = 0.4f))
                }
            }
            IconButton(onClick = onLibraryClick) {
                Icon(Icons.Default.QueueMusic, null, tint = contentColor.copy(alpha = 0.4f))
            }
        }
    }

    if (showInfoDialog && currentSong != null) {
        SongActionDialog(
            action = "Info",
            song = currentSong,
            onDismiss = { showInfoDialog = false },
            onConfirm = { showInfoDialog = false }
        )
    }
}

@Composable
fun SpeedButton(currentSpeed: Float, onSelected: (Float) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { expanded = true }) {
            Text("${currentSpeed}x", color = PlayerState.accentColor.value, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            listOf(0.5f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { speed ->
                DropdownMenuItem(text = { Text("${speed}x") }, onClick = { onSelected(speed); expanded = false })
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val s = (ms / 1000) % 60
    val m = (ms / 1000) / 60
    return String.format("%02d:%02d", m, s)
}

@Composable
fun LyricsView(currentPos: Long) {
    val lyricsText by PlayerState.currentLyrics
    val lyrics = remember(lyricsText) { LyricParser.parse(lyricsText) }
    val listState = rememberLazyListState()
    val contentColor = getAdaptiveContentColor()
    
    LaunchedEffect(currentPos) {
        val idx = lyrics.indexOfLast { it.time <= currentPos }
        if (idx != -1) listState.animateScrollToItem(idx)
    }

    LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(vertical = 120.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        itemsIndexed(lyrics) { index, line ->
            val isCurrent = index == lyrics.indexOfLast { it.time <= currentPos }
            Text(
                line.text,
                color = if (isCurrent) contentColor else contentColor.copy(alpha = 0.3f),
                style = MaterialTheme.typography.headlineSmall.copy(textAlign = TextAlign.Center, fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Light, letterSpacing = (-1).sp),
                modifier = Modifier.padding(16.dp).graphicsLayer(scaleX = if (isCurrent) 1.1f else 1f, scaleY = if (isCurrent) 1.1f else 1f)
            )
        }
    }
}
