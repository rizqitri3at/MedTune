package com.rizqitriantomi.medtune

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ArtistData(val name: String, val songs: List<SongData>)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistsScreen(onSongSelected: () -> Unit = {}, showHeader: Boolean = true) {
    val context = LocalContext.current
    var artists by remember { mutableStateOf<List<ArtistData>>(emptyList()) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedArtist by remember { mutableStateOf<ArtistData?>(null) }
    var sortOrder by PlayerState.artistsSortOrder
    
    val contentColor = getAdaptiveContentColor()
    val accentColor by PlayerState.accentColor

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val list = MediaScanner.getSongs(context)
            val loaded = list.groupBy { it.artist }.map { ArtistData(it.key, it.value) }
            withContext(Dispatchers.Main) { artists = loaded }
        }
    }

    val sortedArtists = remember(artists, sortOrder) {
        when (sortOrder) {
            "A -> Z" -> artists.sortedBy { it.name }
            "Z -> A" -> artists.sortedByDescending { it.name }
            else -> artists.sortedBy { it.name }
        }
    }

    if (selectedArtist != null) {
        ArtistSongsScreen(artist = selectedArtist!!, onBack = { selectedArtist = null }, onSongSelected = onSongSelected)
    } else {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
            if (showHeader) {
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("search artists...", color = contentColor.copy(alpha = 0.3f), fontSize = 24.sp, fontWeight = FontWeight.Light) },
                        modifier = Modifier.weight(1f),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = accentColor.copy(alpha = 0.5f),
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = contentColor
                        ),
                        textStyle = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Light),
                        singleLine = true
                    )

                    IconButton(onClick = { 
                        sortOrder = if (sortOrder == "A -> Z") "Z -> A" else "A -> Z"
                    }) { Icon(Icons.Default.SortByAlpha, null, tint = contentColor) }
                }
            } else {
                Spacer(Modifier.height(16.dp))
            }


            val filtered = sortedArtists.filter { it.name.contains(searchQuery, true) }
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(filtered) { artist ->
                    val interactionSource = remember { MutableInteractionSource() }
                    val isPressed by interactionSource.collectIsPressedAsState()
                    val scale by animateFloatAsState(if (isPressed) 1.05f else 1f, label = "artist_scale")
                    
                    Text(
                        text = artist.name.lowercase(),
                        color = if (isPressed) accentColor else contentColor,
                        fontSize = 32.sp,
                        fontWeight = if (isPressed) FontWeight.Bold else FontWeight.Light,
                        letterSpacing = (-2).sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer { scaleX = scale; scaleY = scale }
                            .clickable(interactionSource = interactionSource, indication = null) { selectedArtist = artist }
                            .padding(vertical = 16.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                item { Spacer(Modifier.height(100.dp)) }
            }
        }
    }
}

@Composable
fun ArtistSongsScreen(artist: ArtistData, onBack: () -> Unit, onSongSelected: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val accentColor by PlayerState.accentColor
    val contentColor = getAdaptiveContentColor()
    BackHandler { onBack() }

    var selectedSongForAction by remember { mutableStateOf<SongData?>(null) }
    var activeAction by remember { mutableStateOf<String?>(null) }
    var songs by remember { mutableStateOf(artist.songs) }

    fun refreshSongs() {
        scope.launch(Dispatchers.IO) {
            val allSongs = MediaScanner.getSongs(context)
            val updatedSongs = allSongs.filter { it.artist == artist.name }
            withContext(Dispatchers.Main) { songs = updatedSongs }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = contentColor) }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(artist.name.lowercase(), fontSize = 36.sp, fontWeight = FontWeight.Bold, color = accentColor, letterSpacing = (-2).sp)
                Text("${songs.size} songs", fontSize = 18.sp, color = contentColor.copy(alpha = 0.6f), fontWeight = FontWeight.Light)
            }
        }
        
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(songs) { song ->
                ZuneSongItem(
                    song = song,
                    isActive = song.uri == PlayerState.currentUri.value,
                    isPlaying = PlayerState.isPlaying.value,
                    accentColor = accentColor,
                    contentColor = contentColor,
                    onClick = {
                        MusicPlayer.playlist = songs
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
                        handleSongAction(context, action, selectedSongForAction!!, songs, onSongSelected)
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
                    refreshSongs()
                    selectedSongForAction = null
                    activeAction = null
                }
            )
        }
    }
}
