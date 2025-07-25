package com.north.mobile.gamification

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
// Removed material icons imports - using emoji/text instead
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun FinancialHealthScore(score: Int, modifier: Modifier = Modifier) {
    val animatedProgress by animateFloatAsState(
        targetValue = score / 100f,
        animationSpec = tween(1000, easing = FastOutSlowInEasing),
        label = "scoreAnimation"
    )
    
    val scoreColor = when {
        score >= 80 -> Color(0xFF4CAF50) // Green
        score >= 60 -> Color(0xFFFFA000) // Amber
        else -> Color(0xFFF44336) // Red
    }
    
    // Simple vertical layout instead of overlapping elements
    Column(
        modifier = modifier.padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Large score number with colored background
        Box(
            modifier = Modifier
                .size(60.dp)
                .background(
                    color = scoreColor.copy(alpha = 0.1f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = score.toString(),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = scoreColor
            )
        }
        
        // Progress bar below the score
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            LinearProgressIndicator(
                progress = animatedProgress,
                modifier = Modifier
                    .width(80.dp)
                    .height(4.dp),
                color = scoreColor,
                trackColor = Color(0xFFE0E0E0)
            )
            Text(
                text = "Health Score",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun AchievementBadge(
    achievement: Achievement,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when (achievement.level) {
        AchievementLevel.BRONZE -> Color(0xFFCD7F32)
        AchievementLevel.SILVER -> Color(0xFFC0C0C0)
        AchievementLevel.GOLD -> Color(0xFFFFD700)
        AchievementLevel.PLATINUM -> Color(0xFFE5E4E2)
    }
    
    Column(
        modifier = modifier.width(100.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(
                    color = backgroundColor.copy(alpha = if (achievement.unlocked) 1f else 0.3f),
                    shape = CircleShape
                )
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = achievement.icon,
                fontSize = 32.sp,
                color = if (achievement.unlocked) 
                    Color.White else Color.White.copy(alpha = 0.5f)
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = achievement.name,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun StreakIndicator(
    currentStreak: Int,
    bestStreak: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = "Current Streak",
                fontSize = 12.sp
            )
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🔥",
                    fontSize = 20.sp
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "$currentStreak days",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "Best Streak",
                fontSize = 12.sp
            )
            Text(
                text = "$bestStreak days",
                fontSize = 16.sp
            )
        }
    }
}

@Composable
fun ChallengeCard(
    challenge: Challenge,
    onAccept: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = challenge.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                
                SuggestionChip(
                    onClick = { },
                    label = { 
                        Text(
                            text = challenge.difficulty.name,
                            color = Color.White,
                            fontSize = 12.sp
                        )
                    },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = when (challenge.difficulty) {
                            ChallengeDifficulty.EASY -> Color(0xFF4CAF50)
                            ChallengeDifficulty.MEDIUM -> Color(0xFFFFA000)
                            ChallengeDifficulty.HARD -> Color(0xFFF44336)
                        }
                    )
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = challenge.description,
                fontSize = 14.sp
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            LinearProgressIndicator(
                progress = challenge.progress,
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row {
                    Text(
                        text = "🏆",
                        fontSize = 20.sp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${challenge.rewardPoints} points",
                        fontSize = 14.sp
                    )
                }
                
                Button(
                    onClick = onAccept,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(if (challenge.accepted) "View Details" else "Accept Challenge")
                }
            }
        }
    }
}

@Composable
fun LevelProgressBar(
    userLevel: UserLevel,
    modifier: Modifier = Modifier
) {
    val progress = userLevel.currentPoints.toFloat() / userLevel.pointsToNextLevel.toFloat()
    
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Level ${userLevel.level}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Level ${userLevel.level + 1}",
                fontSize = 14.sp
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = "${userLevel.currentPoints} / ${userLevel.pointsToNextLevel} points",
            fontSize = 12.sp,
            modifier = Modifier.align(Alignment.End)
        )
    }
}