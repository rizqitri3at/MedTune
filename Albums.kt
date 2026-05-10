package com.rizqitriantomi.medtune

import android.net.Uri
import android.provider.MediaStore
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AlbumItem(val name: String, val songs: List<SongData>, val albumId: Long)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumsScreen(onSongSelected: () -> Unit = {}, showHeader: Boolean = true) {
    val context = LocalContext.current
    var albums by remember { mutableStateOf<List<AlbumItem>>(emptyList()) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedAlbum by remember { mutableStateOf<AlbumItem?>(null) }
    var sortOrder by PlayerState.albumsSortOrder
    var columns by PlayerState.albumsGridColumns
    var viewMode by PlayerState.albumsViewMode // "GRID" or "LIST"
    
    val contentColor = getAdaptiveContentColor()
    val accentColor by PlayerState.accentColor

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val list = MediaScanner.getSongs(context)
            val albumNames = mutableMapOf<Long, String>()
            val cursor = context.contentResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, arrayOf(MediaStore.Audio.Media.ALBUM_ID, MediaStore.Audio.Media.ALBUM), null, null, null)
            cursor?.use {
                val idCol = it.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)
                val nameCol = it.getColumnIndex(MediaStore.Audio.Media.ALBUM)
                while (it.moveToNext()) { albumNames[it.getLong(idCol)] = it.getString(nameCol) ?: "Unknown Album" }
            }
            val loaded = list.groupBy { it.albumId }.map { AlbumItem(albumNames[it.key] ?: "Unknown Album", it.value, it.key) }
            withContext(Dispatchers.Main) { albums = loaded }
        }
    }

    val sortedAlbums = remember(albums, sortOrder) {
        when (sortOrder) {
            "A -> Z" -> albums.sortedBy { it.name }
            "Z -> A" -> albums.sortedByDescending { it.name }
            else -> albums.sortedBy { it.name }
        }
    }

    if (selectedAlbum != null) {
        AlbumSongsScreen(album = selectedAlbum!!, onBack = { selectedAlbum = null }, onSongSelected = onSongSelected)
    } else {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
            if (showHeader) {
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("search albums...", color = contentColor.copy(alpha = 0.3f), fontSize = 24.sp, fontWeight = FontWeight.Light) },
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
                    
                    // Toggle View Mode (List/Grid)
                    IconButton(onClick = { 
                        viewMode = if (viewMode == "GRID") "LIST" else "GRID"
                    }) { 
                        Icon(if (viewMode == "GRID") Icons.Default.List else Icons.Default.GridView, null, tint = contentColor) 
                    }
                    
                    if (viewMode == "GRID") {
                        IconButton(onClick = { 
                            columns = when(columns) {
                                2 -> 3
                                3 -> 4
                                4 -> 5
                                else -> 2
                            }
                        }) { Icon(Icons.Default.ViewModule, null, tint = contentColor) }
                    }
                    
                    IconButton(onClick = { 
                        sortOrder = if (sortOrder == "A -> Z") "Z -> A" else "A -> Z"
                    }) { Icon(Icons.Default.SortByAlpha, null, tint = contentColor) }
                }
            } else {
                Spacer(Modifier.height(16.dp))
            }


            val filtered = sortedAlbums.filter { it.name.contains(searchQuery, true) }
            
            if (viewMode == "GRID") {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columns),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 100.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filtered) { album ->
                        AlbumTile(album, columns, contentColor, onClick = { selectedAlbum = album })
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 100.dp)
                ) {
                    items(filtered) { album ->
                        AlbumListItem(album, contentColor, accentColor, onClick = { selectedAlbum = album })
                    }
                }
            }
        }
    }
}

@Composable
fun AlbumTile(album: AlbumItem, columns: Int, contentColor: Color, onClick: () -> Unit) {
    val artUri = Uri.parse("content://media/external/audio/albumart/${album.albumId}")
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.95f else 1f, label = "tile_scale")

    Column(
        modifier = Modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
    ) {
        Box(modifier = Modifier.aspectRatio(1f).clip(RoundedCornerShape(4.dp)).background(Color.White.copy(alpha = 0.05f))) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current).data(artUri).size(400).crossfade(true).build(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        if (columns <= 3) {
            Spacer(Modifier.height(8.dp))
            Text(album.name.lowercase(), color = contentColor, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${album.songs.size} songs", color = contentColor.copy(alpha = 0.5f), fontSize = 11.sp, fontWeight = FontWeight.Light)
        }
    }
}

@Composable
fun AlbumListItem(album: AlbumItem, contentColor: Color, accentColor: Color, onClick: () -> Unit) {
    val artUri = Uri.parse("content://media/external/audio/albumart/${album.albumId}")
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 1.05f else 1f, label = "list_scale")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale; transformOrigin = TransformOrigin(0f, 0.5f) }
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(64.dp).clip(RoundedCornerShape(4.dp)).background(Color.White.copy(alpha = 0.05f))) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current).data(artUri).size(150).crossfade(true).build(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        Spacer(Modifier.width(16.dp))
        Column {
            Text(
                text = album.name.lowercase(),
                color = if (isPressed) accentColor else contentColor,
                fontSize = 24.sp,
                fontWeight = if (isPressed) FontWeight.Bold else FontWeight.Light,
                letterSpacing = (-1).sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${album.songs.size} songs",
                color = contentColor.copy(alpha = 0.6f),
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraLight
            )
        }
    }
}

@Composable
fun AlbumSongsScreen(album: AlbumItem, onBack: () -> Unit, onSongSelected: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val accentColor by PlayerState.accentColor
    val contentColor = getAdaptiveContentColor()
    BackHandler { onBack() }

    var selectedSongForAction by remember { mutableStateOf<SongData?>(null) }
    var activeAction by remember { mutableStateOf<String?>(null) }
    var songs by remember { mutableStateOf(album.songs) }

    fun refreshSongs() {
        scope.launch(Dispatchers.IO) {
            val allSongs = MediaScanner.getSongs(context)
            val updatedSongs = allSongs.filter { it.albumId == album.albumId }
            withContext(Dispatchers.Main) { songs = updatedSongs }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = contentColor) }
            Spacer(modifier = Modifier.width(16.dp))
            val artUri = Uri.parse("content://media/external/audio/albumart/${album.albumId}")
            AsyncImage(model = artUri, contentDescription = null, modifier = Modifier.size(64.dp).clip(RoundedCornerShape(4.dp)))
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(album.name.lowercase(), fontSize = 28.sp, fontWeight = FontWeight.Bold, color = accentColor, letterSpacing = (-1).sp, maxLines = 1)
                Text("${songs.size} songs", fontSize = 16.sp, color = contentColor.copy(alpha = 0.6f))
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
