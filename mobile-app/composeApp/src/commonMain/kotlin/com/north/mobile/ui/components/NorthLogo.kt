package com.north.mobile.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun NorthLogo(
    size: Dp = 32.dp,
    primaryColor: Color = Color(0xFF00D4AA),
    secondaryColor: Color = Color(0xFF10B981)
) {
    Canvas(modifier = Modifier.size(size)) {
        val center = Offset(this.size.width / 2, this.size.height / 2)
        val radius = this.size.minDimension / 3
        
        // Draw the North star/diamond shape
        drawNorthStar(
            center = center,
            radius = radius,
            primaryColor = primaryColor,
            secondaryColor = secondaryColor
        )
    }
}

private fun DrawScope.drawNorthStar(
    center: Offset,
    radius: Float,
    primaryColor: Color,
    secondaryColor: Color
) {
    // Create the four-pointed star (diamond) shape like in the logo
    val path = Path()
    
    // Top point
    val topPoint = Offset(center.x, center.y - radius * 1.5f)
    // Right point  
    val rightPoint = Offset(center.x + radius, center.y)
    // Bottom point
    val bottomPoint = Offset(center.x, center.y + radius * 1.5f)
    // Left point
    val leftPoint = Offset(center.x - radius, center.y)
    
    // Draw the main diamond shape
    path.moveTo(topPoint.x, topPoint.y)
    path.lineTo(rightPoint.x, rightPoint.y)
    path.lineTo(bottomPoint.x, bottomPoint.y)
    path.lineTo(leftPoint.x, leftPoint.y)
    path.close()
    
    // Fill with primary color
    drawPath(path, primaryColor)
    
    // Draw inner highlight (lighter part)
    val innerPath = Path()
    val innerRadius = radius * 0.6f
    
    val innerTop = Offset(center.x, center.y - innerRadius)
    val innerRight = Offset(center.x + innerRadius * 0.7f, center.y)
    val innerBottom = Offset(center.x, center.y + innerRadius)
    val innerLeft = Offset(center.x - innerRadius * 0.7f, center.y)
    
    innerPath.moveTo(innerTop.x, innerTop.y)
    innerPath.lineTo(innerRight.x, innerRight.y)
    innerPath.lineTo(innerBottom.x, innerBottom.y)
    innerPath.lineTo(innerLeft.x, innerLeft.y)
    innerPath.close()
    
    // Fill inner part with lighter color
    drawPath(innerPath, Color.White.copy(alpha = 0.3f))
}