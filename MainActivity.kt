package com.rizqitriantomi.medtune

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.palette.graphics.Palette
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.*

class MainActivity : ComponentActivity() {
    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= 33) {
            permissions.add(Manifest.permission.READ_MEDIA_AUDIO)
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        permissionLauncher.launch(permissions.toTypedArray())

        setContent {
            LaunchedEffect(intent) { handleIntent(intent) }
            LaunchedEffect(Unit) { 
                Persistence.loadPlayerState(this@MainActivity)
                MusicPlayer.restoreSession(this@MainActivity)
            }
            
            val themeMode by PlayerState.themeMode
            MaterialTheme(
                typography = com.rizqitriantomi.medtune.ui.theme.Typography,
                colorScheme = if (themeMode == "LIGHT") lightColorScheme(background = Color(0xFFF5F5F7)) else darkColorScheme(background = Color.Black, surface = Color(0xFF121212))
            ) {
                MainRoot(onExit = { finish() })
            }
        }
    }

    private fun handleIntent(intent: Intent?) {
        when (intent?.action) {
            "com.rizqitriantomi.medtune.ACTION_SEARCH" -> PlayerState.lastSelectedTab.value = 2
            "com.rizqitriantomi.medtune.ACTION_SHUFFLE_ALL" -> {
                CoroutineScope(Dispatchers.IO).launch {
                    val songs = MediaScanner.getSongs(this@MainActivity)
                    if (songs.isNotEmpty()) {
                        withContext(Dispatchers.Main) {
                            PlayerState.isShuffle.value = true
                            MusicPlayer.playlist = songs
                            MusicPlayer.play(this@MainActivity, songs.random())
                            PlayerState.lastSelectedTab.value = 0
                        }
                    }
                }
            }
            "com.rizqitriantomi.medtune.ACTION_RESUME" -> MusicPlayer.toggle(this)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainRoot(onExit: () -> Unit) {
    val activeSubPage by PlayerState.activeSubPage
    val isSubPageOpen = activeSubPage != -1
    
    val pagerState = rememberPagerState(
        initialPage = PlayerState.lastSelectedTab.value.coerceIn(0, 1), 
        pageCount = { if (isSubPageOpen) 3 else 2 }
    )
    
    val scope = rememberCoroutineScope()
    val themeMode by PlayerState.themeMode
    val albumArtUri by PlayerState.albumArtUri
    val context = LocalContext.current
    val accentColor by PlayerState.accentColor
    val customBgUri by PlayerState.customBgUri
    val bgBlur by PlayerState.bgBlur
    val bgOpacity by PlayerState.bgOpacity
    val bgDim by PlayerState.bgDim
    
    var lastBackTime by remember { mutableLongStateOf(0L) }

    // Navigation Back Handler
    androidx.activity.compose.BackHandler {
        when (pagerState.currentPage) {
            0 -> {
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastBackTime < 2000) onExit()
                else {
                    lastBackTime = currentTime
                    Toast.makeText(context, "Press back again to exit", Toast.LENGTH_SHORT).show()
                }
            }
            1 -> scope.launch { pagerState.animateScrollToPage(0) }
            2 -> scope.launch { pagerState.animateScrollToPage(1) }
        }
    }

    // CRITICAL FIX: Trigger animation AFTER pageCount is updated
    LaunchedEffect(activeSubPage) {
        if (activeSubPage != -1 && pagerState.currentPage != 2) {
            // Wait a tiny bit for Pager to recognize the new pageCount
            delay(50) 
            pagerState.animateScrollToPage(2)
        }
    }

    // Auto-close sub-page logic
    LaunchedEffect(pagerState.currentPage, pagerState.isScrollInProgress) {
        if (!pagerState.isScrollInProgress && pagerState.currentPage < 2) {
            PlayerState.activeSubPage.value = -1
        }
        PlayerState.lastSelectedTab.value = pagerState.currentPage
        Persistence.savePlayerState(context)
    }

    // Adaptive Color Sync
    LaunchedEffect(albumArtUri) {
        if (albumArtUri == null) return@LaunchedEffect
        delay(300)
        val request = ImageRequest.Builder(context).data(albumArtUri).allowHardware(false).size(150).build()
        val result = coil.ImageLoader(context).execute(request)
        result.drawable?.let { drawable ->
            val bitmap = drawable.toBitmap()
            withContext(Dispatchers.Default) {
                val adaptive = ColorAnalyzer.analyze(bitmap, themeMode != "LIGHT")
                withContext(Dispatchers.Main) {
                    PlayerState.dominantColor.value = Color(Palette.from(bitmap).generate().getDominantColor(0))
                    if (PlayerState.adaptiveAccent.value) {
                        PlayerState.accentColor.value = adaptive.accent
                        PlayerState.subAccentColor.value = adaptive.subAccent
                    }
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(if (themeMode == "LIGHT") Color(0xFFF5F5F7) else Color.Black)) {
        // PARALLAX BACKGROUND
        val scrollOffset = pagerState.currentPage + pagerState.currentPageOffsetFraction
        Box(modifier = Modifier.fillMaxSize().graphicsLayer { 
            scaleX = 1.25f; scaleY = 1.25f
            translationX = -scrollOffset * 45f 
        }) {
            when (themeMode) {
                "TRANSPARENT", "ADAPTIVE" -> {
                    AsyncImage(
                        model = ImageRequest.Builder(context).data(albumArtUri).size(500).crossfade(true).build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().blur(25.dp).alpha(0.5f)
                    )
                    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)))
                }
                "CUSTOM" -> {
                    AsyncImage(
                        model = customBgUri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().blur(bgBlur.dp).alpha(bgOpacity)
                    )
                    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = bgDim)))
                }
            }
        }

        Column(modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars).windowInsetsPadding(WindowInsets.navigationBars)) {
            
            // ZUNE PANORAMA HEADER
            val subPageTitle = when(activeSubPage) {
                0 -> if (PlayerState.language.value == "ID") "perpustakaan" else "library"
                1 -> if (PlayerState.language.value == "ID") "artis" else "artists"
                2 -> if (PlayerState.language.value == "ID") "album" else "albums"
                3 -> if (PlayerState.language.value == "ID") "pengaturan" else "settings"
                else -> ""
            }
            val titles = listOf("", "menu", subPageTitle) 

            Box(modifier = Modifier.fillMaxWidth().height(120.dp).padding(top = 16.dp)) {
                titles.forEachIndexed { index, title ->
                    if (title.isNotEmpty()) {
                        val pageOffset = (pagerState.currentPage - index) + pagerState.currentPageOffsetFraction
                        val absOffset = Math.abs(pageOffset)
                        
                        if (absOffset < 1.1f) {
                            val alpha = (1f - absOffset).coerceIn(0f, 1f)
                            val xPos = (-pageOffset * 350).dp
                            
                            Text(
                                text = title.lowercase(),
                                modifier = Modifier
                                    .offset(x = 24.dp + xPos)
                                    .alpha(alpha)
                                    .graphicsLayer { 
                                        val s = 1f - (absOffset * 0.2f).coerceIn(0f, 0.2f)
                                        scaleX = s; scaleY = s
                                        transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0.5f) 
                                    },
                                style = TextStyle(
                                    fontSize = 48.sp,
                                    fontWeight = if (absOffset < 0.5f) FontWeight.Bold else FontWeight.Light,
                                    letterSpacing = (-4).sp,
                                    color = accentColor
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Visible
                            )
                        }
                    }
                }
            }

            HorizontalPager(
                state = pagerState, 
                modifier = Modifier.weight(1f), 
                verticalAlignment = Alignment.Top,
                userScrollEnabled = true,
                contentPadding = PaddingValues(0.dp)
            ) { page ->
                Box(modifier = Modifier.fillMaxSize()) {
                    when (page) {
                        0 -> NowPlayingScreen(onLibraryClick = { 
                            PlayerState.activeSubPage.value = 0
                        })
                        1 -> MenuScreen(onNavigate = { targetSubPage -> 
                            PlayerState.activeSubPage.value = targetSubPage
                        })
                        2 -> {
                            if (isSubPageOpen) {
                                when (activeSubPage) {
                                    0 -> LibraryScreen(onSongSelected = { scope.launch { pagerState.animateScrollToPage(0) } })
                                    1 -> ArtistsScreen(onSongSelected = { scope.launch { pagerState.animateScrollToPage(0) } })
                                    2 -> AlbumsScreen(onSongSelected = { scope.launch { pagerState.animateScrollToPage(0) } })
                                    3 -> SettingsScreen()
                                }
                            }
                        }
                    }
                }
            }

            // MINI PLAYER
            val currentTitle by PlayerState.currentTitle
            if (currentTitle != "No Song" && pagerState.currentPage != 0) {
                MiniPlayer(accentColor, albumArtUri) { scope.launch { pagerState.animateScrollToPage(0) } }
            }
        }
    }
}

@Composable
fun MiniPlayer(accentColor: Color, albumArtUri: Uri?, onClick: () -> Unit) {
    val context = LocalContext.current
    val currentTitle by PlayerState.currentTitle
    val currentArtist by PlayerState.currentArtist
    val isPlaying by PlayerState.isPlaying
    val isShuffle by PlayerState.isShuffle
    val isRepeat by PlayerState.isRepeat
    val progress by PlayerState.currentPosition
    val duration by PlayerState.duration
    
    val themeMode by PlayerState.themeMode
    val bgColor = if (themeMode == "LIGHT") Color.White else Color(0xFF1A1A1A)
    val contentColor = if (themeMode == "LIGHT") Color.Black else Color.White

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(84.dp) // Slightly taller to accommodate time
            .clickable { onClick() },
        color = bgColor
    ) {
        Column {
            // Progress Bar at the very top
            val progressPercent = if (duration > 0) progress.toFloat() / duration.toFloat() else 0f
            Box(modifier = Modifier.fillMaxWidth().height(4.dp).background(Color.Gray.copy(alpha = 0.3f))) {
                Box(modifier = Modifier.fillMaxWidth(progressPercent).height(4.dp).background(accentColor))
                // Progress Handle (Orange dot)
                Box(
                    modifier = Modifier
                        .offset(x = (LocalContext.current.resources.displayMetrics.widthPixels.dp / LocalContext.current.resources.displayMetrics.density * progressPercent) - 6.dp)
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(accentColor)
                        .align(Alignment.CenterStart)
                )
            }

            Spacer(Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        formatMiniPlayerTime(progress), 
                        color = contentColor, 
                        fontSize = 11.sp, 
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    AsyncImage(
                        model = ImageRequest.Builder(context).data(albumArtUri).crossfade(true).build(),
                        contentDescription = null,
                        modifier = Modifier.size(48.dp).clip(RoundedCornerShape(4.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
                
                Spacer(Modifier.width(16.dp))
                
                // Title and Artist
                Column(modifier = Modifier.weight(1f)) {
                    Text(currentTitle, color = contentColor, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(currentArtist, color = contentColor.copy(alpha = 0.6f), fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                
                // Controls (Shuffle, Prev, Play/Pause, Next, Repeat)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(onClick = { PlayerState.isShuffle.value = !isShuffle }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Shuffle, null, tint = if (isShuffle) accentColor else contentColor.copy(alpha = 0.5f), modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = { MusicPlayer.prev(context) }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.SkipPrevious, null, tint = contentColor, modifier = Modifier.size(24.dp))
                    }
                    
                    // Play/Pause with circular border
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .border(2.dp, accentColor, CircleShape)
                            .padding(4.dp)
                            .clip(CircleShape)
                            .clickable { MusicPlayer.toggle(context) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, 
                            null, 
                            tint = contentColor,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    
                    IconButton(onClick = { MusicPlayer.next(context) }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.SkipNext, null, tint = contentColor, modifier = Modifier.size(24.dp))
                    }
                    IconButton(onClick = { PlayerState.isRepeat.value = !isRepeat }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Repeat, null, tint = if (isRepeat) accentColor else contentColor.copy(alpha = 0.5f), modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

private fun formatMiniPlayerTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(java.util.Locale.getDefault(), "%d:%02d:%02d", minutes / 60, minutes % 60, seconds)
}




@Composable
fun MenuScreen(onNavigate: (Int) -> Unit) {
    val currentLang by PlayerState.language
    val accentColor by PlayerState.accentColor
    fun t(id: String, en: String): String = if (currentLang == "ID") id else en

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp).padding(top = 40.dp),
        verticalArrangement = Arrangement.Top
    ) {
        val menuItems = listOf(
            t("Perpustakaan", "Library") to 0,
            t("Artis", "Artists") to 1,
            t("Album", "Albums") to 2,
            t("Pengaturan", "Settings") to 3
        )

        menuItems.forEach { (label, target) ->
            ZuneMenuItem(label = label, onClick = { onNavigate(target) }, accentColor = accentColor)
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun ZuneMenuItem(label: String, onClick: () -> Unit, accentColor: Color) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    // Smooth Metro scaling and bold transition
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 1.15f else 1f, 
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
        label = "zune_scale"
    )
    val textColor = accentColor

    Text(
        text = label.lowercase(),
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { 
                scaleX = scale
                scaleY = scale
                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0.5f)
            }
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(vertical = 4.dp),
        style = TextStyle(
            fontSize = 58.sp,
            fontWeight = if (isPressed) FontWeight.ExtraBold else FontWeight.ExtraLight,
            letterSpacing = (-4).sp,
            color = if (isPressed) textColor else textColor.copy(alpha = 0.8f)
        ),
        maxLines = 1,
        overflow = TextOverflow.Visible
    )
}
