package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material.icons.filled.Timelapse
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BlockMode
import com.example.data.model.BlockedAppRule
import com.example.ui.theme.FocusError
import com.example.ui.theme.FocusErrorContainer
import com.example.ui.theme.FocusOnPrimary
import com.example.ui.theme.FocusOnSurface
import com.example.ui.theme.FocusOnSurfaceVariant
import com.example.ui.theme.FocusOutline
import com.example.ui.theme.FocusPrimary
import com.example.ui.theme.FocusSecondary
import com.example.ui.theme.FocusSecondaryContainer
import com.example.ui.theme.FocusSecondaryFixed
import com.example.ui.theme.FocusSurface
import com.example.ui.theme.FocusSurfaceContainer
import com.example.ui.theme.FocusSurfaceContainerHigh
import com.example.ui.theme.FocusSurfaceContainerHighest
import com.example.ui.theme.FocusSurfaceContainerLow
import com.example.ui.theme.FocusSurfaceContainerLowest
import com.example.ui.theme.FocusSurfaceVariant
import com.example.ui.theme.FocusTertiary
import com.example.ui.theme.FocusTertiaryContainer
import com.example.ui.theme.FocusTertiaryFixed
import com.example.ui.theme.InstagramOrange
import com.example.ui.theme.InstagramPurple
import com.example.ui.theme.InstagramRed
import com.example.ui.theme.YouTubeRed

@Composable
fun AppBrandLogo(
    appName: String,
    size: Int = 44,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(RoundedCornerShape(12.dp))
            .then(
                when {
                    appName.contains("Instagram", ignoreCase = true) -> {
                        Modifier.background(
                            Brush.linearGradient(
                                listOf(InstagramOrange, InstagramRed, InstagramPurple)
                            )
                        )
                    }
                    appName.contains("YouTube", ignoreCase = true) -> {
                        Modifier.background(YouTubeRed)
                    }
                    appName.contains("TikTok", ignoreCase = true) -> {
                        Modifier.background(FocusSurfaceContainerLowest)
                    }
                    appName.contains("Twitter", ignoreCase = true) || appName.contains("X", ignoreCase = true) -> {
                        Modifier.background(Color.Black)
                    }
                    else -> Modifier.background(FocusSurfaceContainerHighest)
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        when {
            appName.contains("Instagram", ignoreCase = true) -> {
                Icon(
                    imageVector = Icons.Default.PhotoCamera,
                    contentDescription = "Instagram",
                    tint = Color.White,
                    modifier = Modifier.size((size * 0.55f).dp)
                )
            }
            appName.contains("YouTube", ignoreCase = true) -> {
                Icon(
                    imageVector = Icons.Default.SmartDisplay,
                    contentDescription = "YouTube",
                    tint = Color.White,
                    modifier = Modifier.size((size * 0.55f).dp)
                )
            }
            appName.contains("TikTok", ignoreCase = true) -> {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = "TikTok",
                    tint = FocusPrimary,
                    modifier = Modifier.size((size * 0.55f).dp)
                )
            }
            appName.contains("Twitter", ignoreCase = true) || appName.contains("X", ignoreCase = true) -> {
                Text(
                    text = "𝕏",
                    color = Color.White,
                    fontSize = (size * 0.45f).sp,
                    fontWeight = FontWeight.Bold
                )
            }
            else -> {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = appName,
                    tint = FocusPrimary,
                    modifier = Modifier.size((size * 0.55f).dp)
                )
            }
        }
    }
}

@Composable
fun AppBlockCard(
    rule: BlockedAppRule,
    onToggle: () -> Unit,
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(FocusSurfaceContainer)
            .padding(16.dp)
            .testTag("app_block_card_${rule.appName}")
    ) {
        // Top row: App Icon, Name, Badge, Switch
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                AppBrandLogo(appName = rule.appName, size = 44)
                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = rule.appName,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = FocusOnSurface
                        )
                        // Status indicator dot
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(
                                    if (rule.blockMode == BlockMode.DAILY_ALLOWANCE) FocusTertiary
                                    else if (rule.isEnabled) FocusPrimary else FocusOutline
                                )
                        )
                        if (rule.blockMode == BlockMode.HARD_BLOCK) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(FocusErrorContainer.copy(alpha = 0.4f))
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "HARD BLOCK",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = FocusError
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(3.dp))

                    // Badge pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                when (rule.blockMode) {
                                    BlockMode.CONTENT_LEVEL -> FocusSecondaryContainer.copy(alpha = 0.2f)
                                    BlockMode.HARD_BLOCK -> FocusSurfaceContainerHighest
                                    BlockMode.DAILY_ALLOWANCE -> FocusTertiaryContainer.copy(alpha = 0.3f)
                                }
                            )
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = when (rule.blockMode) {
                                    BlockMode.CONTENT_LEVEL -> if (rule.appName.contains("YouTube")) Icons.Default.FilterAlt else Icons.Default.AutoStories
                                    BlockMode.HARD_BLOCK -> Icons.Default.Shield
                                    BlockMode.DAILY_ALLOWANCE -> Icons.Default.Timelapse
                                },
                                contentDescription = null,
                                tint = when (rule.blockMode) {
                                    BlockMode.CONTENT_LEVEL -> FocusSecondaryFixed
                                    BlockMode.HARD_BLOCK -> FocusError
                                    BlockMode.DAILY_ALLOWANCE -> FocusTertiaryFixed
                                },
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = rule.filterLabel,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = when (rule.blockMode) {
                                    BlockMode.CONTENT_LEVEL -> FocusSecondaryFixed
                                    BlockMode.HARD_BLOCK -> FocusOnSurfaceVariant
                                    BlockMode.DAILY_ALLOWANCE -> FocusTertiaryFixed
                                }
                            )
                        }
                    }
                }
            }

            // Custom Switch
            Switch(
                checked = rule.isEnabled,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = FocusSurface,
                    checkedTrackColor = FocusPrimary,
                    uncheckedThumbColor = FocusOutline,
                    uncheckedTrackColor = FocusSurfaceContainerHighest
                ),
                thumbContent = {
                    Icon(
                        imageVector = if (rule.isEnabled) Icons.Default.Check else Icons.Default.Close,
                        contentDescription = null,
                        tint = if (rule.isEnabled) FocusPrimary else FocusOutline,
                        modifier = Modifier.size(12.dp)
                    )
                },
                modifier = Modifier.testTag("switch_${rule.appName}")
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Allowance progress bar if Daily Limit
        if (rule.blockMode == BlockMode.DAILY_ALLOWANCE) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(FocusSurfaceContainerLow.copy(alpha = 0.6f))
                    .padding(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Usage Today: ${rule.usedMinutes}m used",
                        fontSize = 12.sp,
                        color = FocusOnSurfaceVariant
                    )
                    Text(
                        text = "${rule.totalAllowedMinutes - rule.usedMinutes}m left",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = FocusTertiary
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { (rule.usedMinutes.toFloat() / rule.totalAllowedMinutes).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = FocusTertiary,
                    trackColor = FocusSurfaceContainerHighest
                )
            }
        } else {
            // Schedule strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(FocusSurfaceContainerLow.copy(alpha = 0.5f))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (rule.scheduleText.contains("Weekdays")) Icons.Default.CalendarToday else Icons.Default.Schedule,
                        contentDescription = null,
                        tint = FocusOutline,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = rule.scheduleText,
                        fontSize = 12.sp,
                        color = FocusOnSurfaceVariant,
                        maxLines = 1
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(FocusSurfaceContainerHigh)
                        .clickable { onEditClick() }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .testTag("edit_rule_${rule.appName}")
                ) {
                    Text(
                        text = "Edit Rule",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = FocusOnSurface
                    )
                }
            }
        }
    }
}
