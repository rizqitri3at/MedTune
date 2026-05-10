package com.rizqitriantomi.medtune

import android.content.ContentValues
import android.net.Uri
import android.provider.MediaStore
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ZuneSongItem(
    song: SongData,
    isActive: Boolean,
    isPlaying: Boolean,
    accentColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val finalTitleColor = if (isActive) accentColor else accentColor.copy(alpha = 0.9f)
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                }
            )
            .padding(vertical = 12.dp)
    ) {
        Text(
            text = song.title,
            color = finalTitleColor,
            fontSize = 22.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = song.artist.lowercase(),
            color = if (isActive) accentColor.copy(alpha = 0.7f) else contentColor.copy(alpha = 0.5f),
            fontSize = 16.sp,
            fontWeight = FontWeight.Light,
            maxLines = 1
        )
    }
}

fun formatDuration(ms: Long): String {
    val s = ms / 1000
    val m = s / 60
    return String.format(java.util.Locale.getDefault(), "%02d:%02d", m, s % 60)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZuneContextMenu(song: SongData, onDismiss: () -> Unit, onAction: (String) -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color(0xFF111111)) {
        Column(modifier = Modifier.padding(24.dp).padding(bottom = 32.dp)) {
            Text(song.title.lowercase(), color = PlayerState.accentColor.value, fontSize = 28.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 24.dp))
            listOf("Putar", "Putar Berikutnya", "Tambah ke Antrean", "Ubah Nama", "Ubah Metadata", "Hapus", "Info").forEach { option ->
                Text(
                    text = option.lowercase(), 
                    color = Color.White, 
                    fontSize = 24.sp, 
                    fontWeight = FontWeight.Light, 
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onAction(option) }
                        .padding(vertical = 12.dp)
                )
            }
        }
    }
}

@Composable
fun SongActionDialog(action: String, song: SongData, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    val context = LocalContext.current
    when (action) {
        "Ubah Nama" -> {
            var newName by remember { mutableStateOf(song.title) }
            AlertDialog(
                onDismissRequest = onDismiss,
                containerColor = Color(0xFF111111),
                title = { Text("ubah nama lagu", color = Color.White, fontWeight = FontWeight.Bold) },
                text = { TextField(value = newName, onValueChange = { newName = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, colors = TextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)) },
                confirmButton = { TextButton(onClick = {
                    val values = ContentValues().apply { put(MediaStore.Audio.Media.TITLE, newName) }
                    context.contentResolver.update(song.uri, values, null, null)
                    onConfirm()
                }) { Text("oke", color = PlayerState.accentColor.value) } },
                dismissButton = { TextButton(onClick = onDismiss) { Text("batal", color = Color.Gray) } }
            )
        }
        "Hapus" -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                containerColor = Color(0xFF111111),
                title = { Text("hapus lagu?", color = Color.White, fontWeight = FontWeight.Bold) },
                text = { Text("ini akan menghapus file secara permanen dari perangkat anda.", color = Color.Gray) },
                confirmButton = { TextButton(onClick = {
                    context.contentResolver.delete(song.uri, null, null)
                    onConfirm()
                }) { Text("hapus", color = Color.Red) } },
                dismissButton = { TextButton(onClick = onDismiss) { Text("batal", color = Color.Gray) } }
            )
        }
        "Ubah Metadata" -> {
            var title by remember { mutableStateOf(song.title) }
            var artist by remember { mutableStateOf(song.artist) }
            var album by remember { mutableStateOf(song.album) }
            
            AlertDialog(
                onDismissRequest = onDismiss,
                containerColor = Color(0xFF111111),
                title = { Text("ubah metadata", color = Color.White, fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        TextField(value = title, onValueChange = { title = it }, label = { Text("Judul") }, modifier = Modifier.fillMaxWidth())
                        TextField(value = artist, onValueChange = { artist = it }, label = { Text("Artis") }, modifier = Modifier.fillMaxWidth())
                        TextField(value = album, onValueChange = { album = it }, label = { Text("Album") }, modifier = Modifier.fillMaxWidth())
                    }
                },
                confirmButton = { TextButton(onClick = {
                    val values = ContentValues().apply {
                        put(MediaStore.Audio.Media.TITLE, title)
                        put(MediaStore.Audio.Media.ARTIST, artist)
                        put(MediaStore.Audio.Media.ALBUM, album)
                    }
                    context.contentResolver.update(song.uri, values, null, null)
                    onConfirm()
                }) { Text("simpan", color = PlayerState.accentColor.value) } },
                dismissButton = { TextButton(onClick = onDismiss) { Text("batal", color = Color.Gray) } }
            )
        }
        "Info" -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                containerColor = Color(0xFF111111),
                title = { Text("info lagu", color = Color.White, fontWeight = FontWeight.Bold) },
                text = {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        val artUri = Uri.parse("content://media/external/audio/albumart/${song.albumId}")
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(artUri)
                                .crossfade(true)
                                .build(),
                            contentDescription = null,
                            modifier = Modifier
                                .size(140.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.05f))
                                .align(Alignment.CenterHorizontally),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(Modifier.height(24.dp))
                        InfoRow("judul", song.title)
                        InfoRow("artis", song.artist)
                        InfoRow("album", song.album)
                        InfoRow("format", song.data.substringAfterLast(".", "tidak diketahui").uppercase())
                        InfoRow("lokasi", song.data)
                    }
                },
                confirmButton = { TextButton(onClick = onDismiss) { Text("tutup", color = PlayerState.accentColor.value) } }
            )
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(label.lowercase(), color = PlayerState.accentColor.value, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text(value, color = Color.White, fontSize = 16.sp)
    }
}

fun handleSongAction(context: android.content.Context, action: String, song: SongData, currentSongs: List<SongData>, onSongSelected: () -> Unit) {
    when (action) {
        "Putar", "Play" -> { MusicPlayer.playlist = currentSongs; MusicPlayer.play(context, song); onSongSelected() }
        "Putar Berikutnya", "Play Next" -> {
            val currentPlaylist = MusicPlayer.playlist.toMutableList()
            if (currentPlaylist.isNotEmpty()) {
                currentPlaylist.add(MusicPlayer.index + 1, song)
                MusicPlayer.playlist = currentPlaylist
            } else { MusicPlayer.playlist = listOf(song); MusicPlayer.play(context, song) }
        }
        "Tambah ke Antrean", "Add to Queue" -> { MusicPlayer.playlist = MusicPlayer.playlist + song }
    }
}

@Composable
fun getAdaptiveContentColor(): Color {
    val themeMode by PlayerState.themeMode
    val dominantColor by PlayerState.dominantColor
    
    return when (themeMode) {
        "LIGHT" -> Color(0xFF111111) // High contrast dark for Light Theme
        "DARK", "EXPERIMENTAL", "TRANSPARENT" -> Color.White // Force white for dark themes
        "ADAPTIVE", "CUSTOM" -> {
            // Check background luminance: if dark, use white; if light, use dark
            if (isColorDark(dominantColor)) Color.White else Color(0xFF111111)
        }
        else -> Color.White
    }
}

@Composable
fun getAdaptiveSubtitleColor(): Color {
    val themeMode by PlayerState.themeMode
    return if (themeMode == "LIGHT") Color(0xFF666666) else getAdaptiveContentColor().copy(alpha = 0.65f)
}

fun isColorDark(color: Color): Boolean {
    val luminance = 0.2126 * color.red + 0.7152 * color.green + 0.0722 * color.blue
    return luminance < 0.45 // Slightly adjusted threshold for better readability
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val themeMode by PlayerState.themeMode
    val dominantColor by PlayerState.dominantColor
    
    val backgroundColor = when (themeMode) {
        "LIGHT" -> Color.White.copy(alpha = 0.85f)
        "TRANSPARENT", "ADAPTIVE", "CUSTOM" -> dominantColor.copy(alpha = 0.15f)
        else -> Color(0xFF1A1A1A).copy(alpha = 0.85f)
    }

    val borderColor = if (themeMode == "LIGHT") Color.Black.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.08f)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(backgroundColor)
            .then(if (themeMode == "LIGHT") Modifier.shadow(12.dp, RoundedCornerShape(20.dp), spotColor = Color.Black.copy(alpha = 0.08f)) else Modifier)
            .border(0.5.dp, borderColor, RoundedCornerShape(20.dp))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            content()
        }
    }
}

@Composable
fun MetroTitle(
    text: String,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    progress: Float = if (isSelected) 1f else 0f
) {
    val accentColor by PlayerState.accentColor
    val contentColor = getAdaptiveContentColor()
    val themeMode by PlayerState.themeMode
    
    // Smooth Metro scaling
    val fontSize = lerp(32f, 44f, progress)
    val fontWeight = if (progress > 0.8f) FontWeight.ExtraBold else FontWeight.Light
    
    val inactiveColor = if (themeMode == "LIGHT") Color(0xFF777777) else contentColor.copy(alpha = 0.3f)
    val finalColor = lerp(inactiveColor, accentColor, progress)
    
    // Adaptive font scaling for long text
    val adaptiveScale = if (text.length > 8) 0.85f else 1f
    
    Text(
        text = text.uppercase(),
        modifier = modifier.padding(end = 28.dp).scale(adaptiveScale),
        style = TextStyle(
            fontSize = fontSize.sp,
            fontWeight = fontWeight,
            letterSpacing = (-2).sp,
            color = finalColor
        ),
        maxLines = 1,
        overflow = TextOverflow.Visible
    )
}

private fun lerp(start: Float, stop: Float, fraction: Float): Float {
    return (1 - fraction) * start + fraction * stop
}

private fun lerp(start: Color, stop: Color, fraction: Float): Color {
    return androidx.compose.ui.graphics.lerp(start, stop, fraction)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PremiumListItem(
    title: String,
    subtitle: String,
    imageUri: Uri?,
    isActive: Boolean = false,
    isPlaying: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {}
) {
    val accentColor by PlayerState.accentColor
    val contentColor = getAdaptiveContentColor()
    val subtitleColor = getAdaptiveSubtitleColor()
    
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.98f else 1f, label = "scale")
    
    val backgroundColor = if (isActive) accentColor.copy(alpha = 0.08f) else Color.Transparent

    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .scale(scale)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = rememberRipple(color = accentColor),
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(contentAlignment = Alignment.Center) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(imageUri)
                        .size(120) // Smaller for list
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(52.dp).clip(RoundedCornerShape(10.dp))
                )
                if (isActive && isPlaying) {
                    Box(
                        modifier = Modifier.size(52.dp).background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        EqualizerAnimation(accentColor)
                    }
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                AdaptiveText(
                    text = title,
                    color = if (isActive) accentColor else contentColor,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                AdaptiveText(
                    text = subtitle,
                    color = if (isActive) accentColor.copy(alpha = 0.8f) else subtitleColor,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            if (isActive) {
                Icon(Icons.Default.MusicNote, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun AdaptiveText(
    text: String,
    color: Color,
    style: TextStyle,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip
) {
    val themeMode by PlayerState.themeMode
    val shadow = if (themeMode != "LIGHT") {
        Shadow(color = Color.Black.copy(alpha = 0.4f), offset = Offset(1f, 1.5f), blurRadius = 3f)
    } else null

    Text(
        text = text,
        color = color,
        style = style.copy(shadow = shadow),
        modifier = modifier,
        maxLines = maxLines,
        overflow = overflow
    )
}

@Composable
fun EqualizerAnimation(color: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "eq")
    @Composable
    fun Bar(duration: Int) {
        val h by infiniteTransition.animateFloat(
            initialValue = 0.3f, targetValue = 0.9f,
            animationSpec = infiniteRepeatable(tween(duration, easing = LinearEasing), RepeatMode.Reverse),
            label = "bar"
        )
        Box(modifier = Modifier.width(3.dp).fillMaxHeight(h).background(color, RoundedCornerShape(1.dp)))
    }
    Row(modifier = Modifier.size(20.dp), horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.Bottom) {
        Bar(500); Bar(700); Bar(600); Bar(800)
    }
}

@Composable
fun SortingDropdown(currentSort: String, options: List<String>, onSortSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val contentColor = getAdaptiveContentColor()
    val accentColor by PlayerState.accentColor

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Default.Sort, contentDescription = "Sort", tint = contentColor)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(Color(0xFF1A1A1A)).border(0.5.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option, color = if (currentSort == option) accentColor else Color.White) },
                    onClick = { onSortSelected(option); expanded = false }
                )
            }
        }
    }
}

@Composable
fun AlbumGridDensityButton() {
    var cols by PlayerState.albumsGridColumns
    val contentColor = getAdaptiveContentColor()
    
    IconButton(onClick = {
        cols = when(cols) {
            2 -> 3
            3 -> 4
            4 -> 2
            else -> 2
        }
    }) {
        val icon = when(cols) {
            2 -> Icons.Default.GridView
            3 -> Icons.Default.ViewModule
            4 -> Icons.Default.ViewComfy
            else -> Icons.Default.GridView
        }
        Icon(icon, contentDescription = "Columns", tint = contentColor)
    }
}
