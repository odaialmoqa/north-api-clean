package com.north.mobile.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun NorthAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Color(0xFF00D4AA), // Wealthsimple teal
            secondary = Color(0xFF6B46C1), // Warm purple
            tertiary = Color(0xFF10B981), // Success green
            background = Color(0xFFF8FAFC), // Very light gray background
            surface = Color.White,
            surfaceVariant = Color(0xFFF1F5F9), // Light gray variant
            onPrimary = Color.White,
            onSecondary = Color.White,
            onBackground = Color(0xFF1F2937), // Charcoal text
            onSurface = Color(0xFF1F2937),
            onSurfaceVariant = Color(0xFF6B7280), // Medium gray
            outline = Color(0xFFE5E7EB), // Light border
            error = Color(0xFFEF4444), // Soft red
            onError = Color.White
        ),
        content = content
    )
} 