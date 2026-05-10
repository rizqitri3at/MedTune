package com.rizqitriantomi.medtune

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.palette.graphics.Palette

object ColorAnalyzer {
    
    data class AdaptiveColors(
        val accent: Color,
        val subAccent: Color,
        val content: Color,
        val subtitle: Color
    )

    fun analyze(bitmap: Bitmap, isDarkTheme: Boolean): AdaptiveColors {
        val palette = Palette.from(bitmap).generate()
        
        val dominant = palette.getDominantColor(Color.Black.toArgb())
        val vibrant = palette.getVibrantColor(dominant)
        val lightVibrant = palette.getLightVibrantColor(vibrant)
        val darkVibrant = palette.getDarkVibrantColor(vibrant)
        
        val primaryAccent = if (isDarkTheme) {
            val lv = Color(lightVibrant)
            if (getLuminance(lv) > 0.4f) lv else Color(vibrant)
        } else {
            val dv = Color(darkVibrant)
            if (getLuminance(dv) < 0.6f) dv else Color(vibrant)
        }
        
        val subAccent = primaryAccent.copy(alpha = 0.4f)
        
        // Dynamic content color based on theme AND dominant background if transparent
        val contentColor = if (isDarkTheme) Color.White else Color.Black
        val subtitleColor = contentColor.copy(alpha = 0.7f)
        
        return AdaptiveColors(
            accent = primaryAccent,
            subAccent = subAccent,
            content = contentColor,
            subtitle = subtitleColor
        )
    }

    fun getLuminance(color: Color): Float {
        return (0.2126f * color.red + 0.7152f * color.green + 0.0722f * color.blue)
    }
}
