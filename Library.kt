package com.rizqitriantomi.medtune

import android.content.ContentValues
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun LibraryScreen(onSongSelected: () -> Unit = {}) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var songs by remember { mutableStateOf<List<SongData>>(emptyList()) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var sortOrder by PlayerState.songsSortOrder
    
    val listState = rememberLazyListState()
    val contentColor = getAdaptiveContentColor()
    val accentColor by PlayerState.accentColor
    val themeMode by PlayerState.themeMode

    var selectedSongForAction by remember { mutableStateOf<SongData?>(null) }
    var activeAction by remember { mutableStateOf<String?>(null) }

    fun loadSongs() {
        scope.launch(Dispatchers.IO) {
            val allSongs = MediaScanner.getSongs(context)
            val sortedSongs = when (sortOrder) {
                "Judul" -> allSongs.sortedBy { it.title }
                "Artis" -> allSongs.sortedBy { it.artist }
                "Album" -> allSongs.sortedBy { it.album }
                "Baru ditambahkan" -> allSongs.sortedByDescending { it.dateAdded }
                else -> allSongs.sortedBy { it.title }
            }
            withContext(Dispatchers.Main) { songs = sortedSongs }
        }
    }

    LaunchedEffect(sortOrder, PlayerState.includedFolders.value) { loadSongs() }

    val filteredSongs = remember(songs, searchQuery) {
        songs.filter { it.title.contains(searchQuery, true) || it.artist.contains(searchQuery, true) }
    }

    Column(modifier = Modifier.fillMaxSize().background(if (themeMode == "LIGHT") Color(0xFFF5F5F7) else Color(0xFF111111))) {
        // Search Box (Atas)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .height(40.dp)
                .background(if (themeMode == "LIGHT") Color.Black.copy(alpha = 0.05f) else Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            if (searchQuery.isEmpty()) {
                Text("Cari lagu atau artis...", color = contentColor.copy(alpha = 0.3f), fontSize = 14.sp)
            }
            androidx.compose.foundation.text.BasicTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                textStyle = TextStyle(color = contentColor, fontSize = 14.sp),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(accentColor),
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Actions and Filters
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val isShuffleActive by PlayerState.isShuffle
            var playMenuExpanded by remember { mutableStateOf(false) }

            Box {
                Button(
                    onClick = { playMenuExpanded = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE96D22),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(if (isShuffleActive) Icons.Default.Shuffle else Icons.Default.PlayArrow, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (isShuffleActive) "Acak" else "Putar semua")
                    Icon(Icons.Default.ArrowDropDown, null, modifier = Modifier.size(18.dp))
                }

                DropdownMenu(
                    expanded = playMenuExpanded,
                    onDismissRequest = { playMenuExpanded = false },
                    modifier = Modifier.background(Color(0xFF1A1A1A)).border(0.5.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                ) {
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.PlayArrow, null, tint = if (!isShuffleActive) Color(0xFFE96D22) else Color.White, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Putar semua", color = if (!isShuffleActive) Color(0xFFE96D22) else Color.White)
                            }
                        },
                        onClick = {
                            PlayerState.isShuffle.value = false
                            if (filteredSongs.isNotEmpty()) {
                                MusicPlayer.playlist = filteredSongs
                                MusicPlayer.play(context, filteredSongs.first())
                                onSongSelected()
                            }
                            playMenuExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.FormatListNumbered, null, tint = if (!isShuffleActive) Color(0xFFE96D22) else Color.White, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Putar berurutan", color = if (!isShuffleActive) Color(0xFFE96D22) else Color.White)
                            }
                        },
                        onClick = {
                            PlayerState.isShuffle.value = false
                            if (filteredSongs.isNotEmpty()) {
                                MusicPlayer.playlist = filteredSongs
                                MusicPlayer.play(context, filteredSongs.first())
                                onSongSelected()
                            }
                            playMenuExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Shuffle, null, tint = if (isShuffleActive) Color(0xFFE96D22) else Color.White, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Acak", color = if (isShuffleActive) Color(0xFFE96D22) else Color.White)
                            }
                        },
                        onClick = {
                            PlayerState.isShuffle.value = true
                            if (filteredSongs.isNotEmpty()) {
                                MusicPlayer.playlist = filteredSongs
                                MusicPlayer.play(context, filteredSongs.random())
                                onSongSelected()
                            }
                            playMenuExpanded = false
                        }
                    )
                }
            }

            ModernSortDropdown(
                currentSort = sortOrder,
                options = listOf("Judul", "Artis", "Album", "Tahun", "Baru ditambahkan"),
                onSortSelected = { sortOrder = it }
            )
        }

        // TABLE HEADER
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(Modifier.width(48.dp)) // Icon space
            
            val columns = when(sortOrder) {
                "Artis" -> listOf("Artis", "Judul", "Album")
                "Album" -> listOf("Album", "Judul", "Artis")
                else -> listOf("Judul", "Artis", "Album")
            }

            columns.forEach { col ->
                val weight = when(col) {
                    "Judul" -> 2f
                    "Artis" -> 1.5f
                    "Album" -> 1.5f
                    else -> 1f
                }
                Text(
                    text = col,
                    modifier = Modifier.weight(weight).clickable { sortOrder = col },
                    color = if (sortOrder == col) Color(0xFFE96D22) else contentColor.copy(alpha = 0.5f),
                    fontSize = 12.sp,
                    fontWeight = if (sortOrder == col) FontWeight.Bold else FontWeight.Normal
                )
            }
            Text("Durasi", modifier = Modifier.width(60.dp), color = contentColor.copy(alpha = 0.5f), fontSize = 12.sp)
        }

        LazyColumn(state = listState, modifier = Modifier.weight(1f)) {
            itemsIndexed(filteredSongs, key = { _, song -> song.uri.toString() }) { index, song ->
                ModernSongRow(
                    song = song,
                    index = index,
                    isActive = song.uri == PlayerState.currentUri.value,
                    accentColor = Color(0xFFE96D22),
                    contentColor = contentColor,
                    sortOrder = sortOrder,
                    onClick = {
                        MusicPlayer.playlist = filteredSongs
                        MusicPlayer.play(context, song)
                        onSongSelected()
                    },
                    onLongClick = { 
                        selectedSongForAction = song
                        activeAction = "Menu"
                    }
                )
            }
            item { Spacer(Modifier.height(100.dp)) }
        }
    }

    if (selectedSongForAction != null && activeAction != null) {
        if (activeAction == "Menu") {
            ZuneContextMenu(
                song = selectedSongForAction!!,
                onDismiss = { activeAction = null },
                onAction = { action ->
                    if (action == "Hapus" || action == "Ubah Nama" || action == "Ubah Metadata" || action == "Info") {
                        activeAction = action
                    } else {
                        handleSongAction(context, action, selectedSongForAction!!, filteredSongs, onSongSelected)
                        activeAction = null
                        selectedSongForAction = null
                    }
                }
            )
        } else {
            SongActionDialog(
                action = activeAction!!,
                song = selectedSongForAction!!,
                onDismiss = {
                    selectedSongForAction = null
                    activeAction = null
                },
                onConfirm = { 
                    loadSongs()
                    selectedSongForAction = null
                    activeAction = null
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ModernSongRow(
    song: SongData,
    index: Int,
    isActive: Boolean,
    accentColor: Color,
    contentColor: Color,
    sortOrder: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val bgColor = if (index % 2 == 0) Color.White.copy(alpha = 0.03f) else Color.Transparent
    val activeBgColor = accentColor.copy(alpha = 0.1f)
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isActive) activeBgColor else bgColor)
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                    onLongClick()
                }
            )
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.width(48.dp), contentAlignment = Alignment.CenterStart) {
            if (isActive && PlayerState.isPlaying.value) {
                EqualizerAnimation(accentColor)
            } else {
                Text((index + 1).toString(), color = contentColor.copy(alpha = 0.3f), fontSize = 12.sp)
            }
        }

        val columns = when(sortOrder) {
            "Artis" -> listOf("Artis", "Judul", "Album")
            "Album" -> listOf("Album", "Judul", "Artis")
            else -> listOf("Judul", "Artis", "Album")
        }

        columns.forEach { col ->
            val (text, weight) = when(col) {
                "Judul" -> song.title to 2f
                "Artis" -> song.artist to 1.5f
                "Album" -> song.album to 1.5f
                else -> "" to 1f
            }
            Text(
                text = text,
                modifier = Modifier.weight(weight),
                color = if (isActive && col == sortOrder) accentColor else if (isActive) contentColor else if (col == sortOrder) contentColor else contentColor.copy(alpha = 0.7f),
                fontSize = 14.sp,
                fontWeight = if (col == sortOrder) FontWeight.Medium else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        
        Text(formatDuration(song.duration), modifier = Modifier.width(60.dp), color = contentColor.copy(alpha = 0.5f), fontSize = 14.sp)
    }
}

@Composable
fun ModernSortDropdown(currentSort: String, options: List<String>, onSortSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val contentColor = getAdaptiveContentColor()
    val accentColor by PlayerState.accentColor

    Box {
        Row(
            verticalAlignment = Alignment.CenterVertically, 
            modifier = Modifier.clickable { expanded = true }
        ) {
            Text("Urutkan: ", color = contentColor.copy(alpha = 0.5f), fontSize = 14.sp)
            Text(currentSort, color = accentColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Icon(Icons.Default.ArrowDropDown, null, tint = contentColor.copy(alpha = 0.5f))
        }

        val filteredOptions = options.filter { it != "Tahun" }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .background(Color(0xFF1A1A1A))
                .border(0.5.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                .padding(vertical = 4.dp)
        ) {
            filteredOptions.forEach { option ->
                val isSelected = currentSort == option
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isSelected) {
                                Box(modifier = Modifier.width(3.dp).height(16.dp).background(accentColor, RoundedCornerShape(1.dp)))
                                Spacer(Modifier.width(8.dp))
                            } else {
                                Spacer(Modifier.width(11.dp))
                            }
                            Text(
                                text = option, 
                                color = if (isSelected) accentColor else Color.White.copy(alpha = 0.7f),
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    },
                    onClick = { onSortSelected(option); expanded = false },
                    modifier = Modifier.background(if (isSelected) Color.White.copy(alpha = 0.05f) else Color.Transparent)
                )
            }
        }
    }
}
