package com.rizqitriantomi.medtune

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import coil.compose.AsyncImage
import com.rizqitriantomi.medtune.R
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var bahasa by PlayerState.language
    var themeMode by PlayerState.themeMode
    var gradientPreset by PlayerState.gradientPreset
    
    val contentColor = getAdaptiveContentColor()
    var showAbout by remember { mutableStateOf(false) }

    // State for main section expansions
    var librariesExpanded by remember { mutableStateOf(false) }
    var personalizationExpanded by remember { mutableStateOf(false) }
    var advancedExpanded by remember { mutableStateOf(false) }

    // State for expansions
    var locationsExpanded by remember { mutableStateOf(false) }
    var excludedExpanded by remember { mutableStateOf(false) }
    var playbackExpanded by remember { mutableStateOf(false) }

    fun t(id: String, en: String) = if (bahasa == "ID") id else en

    val treeLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            PlayerState.includedFolders.value += it.toString()
            Persistence.savePlayerState(context)
        }
    }

    val excludeLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            PlayerState.excludedFolders.value += it.toString()
            Persistence.savePlayerState(context)
        }
    }

    if (showAbout) {
        AboutPage(onBack = { showAbout = false }, t = ::t)
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // LIBRARIES SECTION
            SettingsSectionCard(
                icon = Icons.Default.LibraryMusic,
                title = t("Libraries", "Libraries"),
                expanded = librariesExpanded,
                onToggle = { librariesExpanded = !librariesExpanded }
            ) {
                SettingsItemCard(
                    icon = Icons.Default.Folder,
                    title = t("Lokasi perpustakaan musik", "Music library locations"),
                    trailing = {
                        SettingsActionButton(t("Tambah folder", "Add folder")) { treeLauncher.launch(null) }
                    },
                    isExpandable = true,
                    expanded = locationsExpanded,
                    onToggle = { locationsExpanded = !locationsExpanded }
                ) {
                    PlayerState.includedFolders.value.forEach { uri ->
                        FolderItem(uri) { 
                            PlayerState.includedFolders.value -= uri 
                            Persistence.savePlayerState(context)
                        }
                    }
                }

                SettingsItemCard(
                    icon = Icons.Default.FolderOff,
                    title = t("Perpustakaan musik dikecualikan", "Music library excluded"),
                    trailing = {
                        SettingsActionButton(t("Tambah folder", "Add folder")) { excludeLauncher.launch(null) }
                    },
                    isExpandable = true,
                    expanded = excludedExpanded,
                    onToggle = { excludedExpanded = !excludedExpanded }
                ) {
                    PlayerState.excludedFolders.value.forEach { uri ->
                        FolderItem(uri) { 
                            PlayerState.excludedFolders.value -= uri 
                            Persistence.savePlayerState(context)
                        }
                    }
                }

                SettingsItemCard(
                    icon = Icons.Default.Build,
                    title = t("Segarkan perpustakaan", "Refresh libraries"),
                    trailing = {
                        SettingsActionButton(t("Segarkan", "Refresh")) { 
                            scope.launch {
                                MediaScanner.getSongs(context, forceRefresh = true)
                                Toast.makeText(context, t("Perpustakaan diperbarui", "Library refreshed"), Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )
            }

            // PERSONALIZATION SECTION
            SettingsSectionCard(
                icon = Icons.Default.Palette,
                title = t("Personalisasi", "Personalization"),
                expanded = personalizationExpanded,
                onToggle = { personalizationExpanded = !personalizationExpanded }
            ) {
                SettingsItemCard(
                    icon = Icons.Default.Brush,
                    title = t("Tema aplikasi", "App theme"),
                    trailing = {
                        val themes = listOf("LIGHT", "DARK", "EXPERIMENTAL", "ADAPTIVE", "CUSTOM")
                        SettingsValueDropdown(selected = themeMode, options = themes, onSelected = { themeMode = it })
                    }
                )

                SettingsItemCard(
                    icon = Icons.Default.Palette,
                    title = t("Warna aksen", "Accent color"),
                    trailing = {
                        val presets = listOf("Aurora", "Ocean", "Sunset", "Neon", "AMOLED", "Rose", "Dynamic Auto")
                        SettingsValueDropdown(selected = gradientPreset, options = presets, onSelected = { 
                            gradientPreset = it
                            applyGradientPreset(it)
                        })
                    }
                )
            }

            // ADVANCED SECTION
            SettingsSectionCard(
                icon = Icons.Default.Settings,
                title = t("Lanjutan", "Advanced"),
                expanded = advancedExpanded,
                onToggle = { advancedExpanded = !advancedExpanded }
            ) {
                SettingsItemCard(
                    icon = Icons.Default.PlayCircle,
                    title = t("Pemutaran", "Playback"),
                    subtitle = t("Sleep timer dari:", "Sleep timer from:"),
                    isExpandable = true,
                    expanded = playbackExpanded,
                    onToggle = { playbackExpanded = !playbackExpanded }
                ) {
                    if (PlayerState.sleepTimerActive.value) {
                        val remaining = PlayerState.sleepTimerRemaining.value / 1000
                        val m = remaining / 60
                        val s = remaining % 60
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(String.format(Locale.getDefault(), "%02d:%02d", m, s), style = MaterialTheme.typography.titleLarge, color = contentColor)
                            Spacer(Modifier.width(16.dp))
                            Button(onClick = { PlayerState.sleepTimerActive.value = false }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) {
                                Text(t("Hentikan", "Stop"))
                            }
                        }
                    } else {
                        val timerPresets = listOf(5, 10, 15, 30)
                        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            timerPresets.forEach { min ->
                                FilterChip(
                                    selected = false, 
                                    onClick = { MusicPlayer.startSleepTimer(min, PlayerState.fadeOutBeforeStop.value) }, 
                                    label = { Text("${min}m") },
                                    colors = FilterChipDefaults.filterChipColors(labelColor = contentColor)
                                )
                            }
                        }
                    }
                    
                    Spacer(Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(t("Ingat Posisi", "Remember Position"), color = contentColor)
                            Text(t("Cocok untuk Podcast/Audiobook", "Best for Podcasts/Audiobooks"), style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        }
                        Switch(checked = PlayerState.rememberPosition.value, onCheckedChange = { PlayerState.rememberPosition.value = it })
                    }
                }

                SettingsItemCard(
                    icon = Icons.Default.Language,
                    title = t("Bahasa", "Language"),
                    subtitle = t("Pilih bahasa aplikasi", "Choose app language"),
                    trailing = {
                        val languages = listOf("Indonesia", "English")
                        val currentLabel = if (bahasa == "ID") "Indonesia" else "English"
                        SettingsValueDropdown(selected = currentLabel, options = languages, onSelected = { 
                            bahasa = if (it == "Indonesia") "ID" else "EN"
                        })
                    }
                )
            }

            // ABOUT SECTION
            SettingsSectionCard(
                icon = Icons.Default.Info,
                title = t("About", "About"),
                onToggle = { showAbout = true }
            )
            
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
fun SettingsSectionCard(
    icon: ImageVector,
    title: String,
    expanded: Boolean = false,
    onToggle: () -> Unit,
    content: @Composable (ColumnScope.() -> Unit)? = null
) {
    val contentColor = getAdaptiveContentColor()
    val themeMode by PlayerState.themeMode
    val accentColor by PlayerState.accentColor
    val cardBg = if (themeMode == "LIGHT") Color.Black.copy(alpha = 0.03f) else Color.White.copy(alpha = 0.03f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(cardBg)
            .clickable { onToggle() }
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = accentColor, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = contentColor,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            if (content != null) {
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    null,
                    tint = contentColor.copy(alpha = 0.5f)
                )
            } else {
                Icon(
                    Icons.Default.ChevronRight,
                    null,
                    tint = contentColor.copy(alpha = 0.5f)
                )
            }
        }
        
        AnimatedVisibility(
            visible = expanded && content != null,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier.padding(top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                content?.invoke(this)
            }
        }
    }
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = PlayerState.accentColor.value,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
    )
}

@Composable
fun SettingsItemCard(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    trailing: @Composable (() -> Unit)? = null,
    isExpandable: Boolean = false,
    expanded: Boolean = false,
    onToggle: (() -> Unit)? = null,
    content: @Composable (ColumnScope.() -> Unit)? = null
) {
    val contentColor = getAdaptiveContentColor()
    val themeMode by PlayerState.themeMode
    val cardBg = if (themeMode == "LIGHT") Color.Black.copy(alpha = 0.05f) else Color.White.copy(alpha = 0.05f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(cardBg)
            .then(if (onToggle != null) Modifier.clickable { onToggle() } else Modifier)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = contentColor.copy(alpha = 0.7f), modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = contentColor, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                if (subtitle != null) {
                    Text(subtitle, color = contentColor.copy(alpha = 0.5f), style = MaterialTheme.typography.bodySmall)
                }
            }
            if (trailing != null) {
                trailing()
            }
            if (isExpandable) {
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    null,
                    tint = contentColor.copy(alpha = 0.5f),
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
        AnimatedVisibility(visible = expanded && content != null) {
            Column {
                Spacer(Modifier.height(16.dp))
                content?.invoke(this)
            }
        }
    }
}

@Composable
fun SettingsActionButton(text: String, onClick: () -> Unit) {
    val accentColor by PlayerState.accentColor
    val themeMode by PlayerState.themeMode
    val contentColor = getAdaptiveContentColor()
    val btnBg = if (themeMode == "LIGHT") Color.Black.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.08f)
    
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = btnBg
    ) {
        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Add, null, tint = accentColor, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text(text, color = contentColor, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
fun SettingsValueDropdown(selected: String, options: List<String>, onSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val accentColor by PlayerState.accentColor
    val contentColor = getAdaptiveContentColor()
    val themeMode by PlayerState.themeMode
    
    Box {
        Surface(
            onClick = { expanded = true }, 
            shape = RoundedCornerShape(8.dp), 
            color = if (themeMode == "LIGHT") Color.Black.copy(alpha = 0.05f) else Color.White.copy(alpha = 0.05f)
        ) {
            Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(selected, color = contentColor, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.width(4.dp))
                Icon(Icons.Default.ArrowDropDown, null, tint = accentColor, modifier = Modifier.size(18.dp))
            }
        }
        DropdownMenu(
            expanded = expanded, 
            onDismissRequest = { expanded = false }, 
            modifier = Modifier.background(if (themeMode == "LIGHT") Color.White else Color(0xFF1A1A1A))
        ) {
            options.forEach { opt ->
                DropdownMenuItem(
                    text = { Text(opt, color = if (opt == selected) accentColor else contentColor) }, 
                    onClick = { onSelected(opt); expanded = false }
                )
            }
        }
    }
}

@Composable
fun FolderItem(uri: String, onDelete: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
        Icon(Icons.Default.Folder, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(Uri.parse(uri).path?.split(":")?.lastOrNull() ?: uri.takeLast(15), color = Color.Gray, fontSize = 12.sp, modifier = Modifier.weight(1f))
        IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.Close, null, tint = Color.Red, modifier = Modifier.size(16.dp)) }
    }
}

fun applyGradientPreset(preset: String) {
    val colors = when(preset) {
        "Aurora" -> listOf(Color(0xFF00D2FF), Color(0xFF3A7BD5))
        "Ocean" -> listOf(Color(0xFF2193B0), Color(0xFF6DD5ED))
        "Sunset" -> listOf(Color(0xFFEE0979), Color(0xFFFF6A00))
        "Neon" -> listOf(Color(0xFF00F260), Color(0xFF0575E6))
        "AMOLED" -> listOf(Color.Black, Color(0xFF121212))
        "Rose" -> listOf(Color(0xFFF093FB), Color(0xFFF5576C))
        else -> null
    }
    colors?.let {
        PlayerState.accentColor.value = it[0]
        PlayerState.subAccentColor.value = it[1]
    }
}

@Composable
fun AboutPage(onBack: () -> Unit, t: (String, String) -> String) {
    val accentColor by PlayerState.accentColor
    val contentColor = getAdaptiveContentColor()
    BackHandler { onBack() }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        IconButton(onClick = onBack, modifier = Modifier.align(Alignment.Start)) { 
            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = contentColor) 
        }
        
        Text("MedTune", style = MaterialTheme.typography.headlineMedium, color = contentColor, fontWeight = FontWeight.Bold)
        Text("V2.5 Architectural Overhaul", style = MaterialTheme.typography.labelLarge, color = accentColor)
        
        Spacer(Modifier.height(24.dp))
        
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Text(t("Informasi Pengembang", "Developer Info"), fontWeight = FontWeight.Bold, color = accentColor, fontSize = 14.sp)
            Spacer(Modifier.height(8.dp))
            Text("Rizqi Tri Antomi S.AB", color = contentColor, fontWeight = FontWeight.Medium)
            Text("develoft01@gmail.com", color = Color.Gray, fontSize = 12.sp)
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = contentColor.copy(alpha = 0.1f))
            
            Text(t("Teknologi", "Engine & Tech"), fontWeight = FontWeight.Bold, color = accentColor, fontSize = 14.sp)
            Spacer(Modifier.height(4.dp))
            Text("Engine: Jetpack Compose (Modern Toolkit)", color = Color.Gray, fontSize = 13.sp)
            Text("Language: Kotlin 1.9.x", color = Color.Gray, fontSize = 13.sp)
            Text("Reactive: Kotlin Coroutines & Flow", color = Color.Gray, fontSize = 13.sp)
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = contentColor.copy(alpha = 0.1f))
            
            Text(t("Fitur Utama", "Key Features"), fontWeight = FontWeight.Bold, color = accentColor, fontSize = 14.sp)
            val features = remember {
                listOf(
                    "MediaPlay Inspired UI",
                    "Adaptive Color Engine",
                    "Dynamic Pager Navigation",
                    "Smart Media Scanning",
                    "Advanced Folder Filtering",
                    "Sleep Timer with Fade-out"
                )
            }
            features.forEach { feature ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                    Icon(Icons.Default.Check, null, tint = accentColor, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(feature, color = Color.Gray, fontSize = 13.sp)
                }
            }
        }
        
        Spacer(Modifier.height(24.dp))
        Text("©2026 MedTune by Rizqi. All Rights Reserved.", color = Color.Gray, fontSize = 10.sp)
    }
}
