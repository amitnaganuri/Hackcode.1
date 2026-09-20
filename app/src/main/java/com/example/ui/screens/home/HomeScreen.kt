package com.example.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material.icons.filled.Timelapse
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.EditGoalDialog
import com.example.ui.components.FocusGuardTopBar
import com.example.ui.components.FocusRing
import com.example.ui.theme.FocusError
import com.example.ui.theme.FocusErrorContainer
import com.example.ui.theme.FocusOnErrorContainer
import com.example.ui.theme.FocusOnPrimary
import com.example.ui.theme.FocusOnPrimaryContainer
import com.example.ui.theme.FocusOnSurface
import com.example.ui.theme.FocusOnSurfaceVariant
import com.example.ui.theme.FocusOutline
import com.example.ui.theme.FocusPrimary
import com.example.ui.theme.FocusPrimaryContainer
import com.example.ui.theme.FocusSecondary
import com.example.ui.theme.FocusSurface
import com.example.ui.theme.FocusSurfaceContainer
import com.example.ui.theme.FocusSurfaceContainerHigh
import com.example.ui.theme.FocusSurfaceContainerHighest
import com.example.ui.theme.FocusSurfaceContainerLow
import com.example.ui.theme.FocusSurfaceVariant
import com.example.ui.theme.FocusTertiary
import com.example.ui.theme.InstagramOrange
import com.example.ui.theme.InstagramPurple
import com.example.ui.theme.InstagramRed
import com.example.ui.theme.YouTubeRed
import com.example.viewmodel.FocusGuardViewModel

@Composable
fun HomeScreen(
    viewModel: FocusGuardViewModel,
    onNavigateToChamber: () -> Unit,
    onNavigateToBlocks: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeSession by viewModel.activeSession.collectAsState()
    val dailyStats by viewModel.dailyStats.collectAsState()
    val appInterventions by viewModel.appInterventions.collectAsState()
    val selectedDuration by viewModel.selectedDurationMinutes.collectAsState()
    val isEditGoalOpen by viewModel.isEditGoalDialogOpen.collectAsState()

    if (isEditGoalOpen) {
        EditGoalDialog(
            initialGoal = dailyStats.todayGoalText,
            onDismiss = { viewModel.setEditGoalDialogOpen(false) },
            onSave = { viewModel.updateGoal(it) }
        )
    }

    Scaffold(
        topBar = {
            FocusGuardTopBar(
                subtitle = "Home Dashboard",
                onShieldActionClick = onNavigateToChamber,
                onProfileClick = onNavigateToSettings
            )
        },
        containerColor = FocusSurface,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Greeting & Daily Status
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = todayLabel(),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp,
                        color = FocusPrimary
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(FocusSurfaceContainerHigh)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(FocusPrimary)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Shield Active • Strict",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = FocusPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = greetingForNow(),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.5).sp,
                    color = FocusOnSurface
                )
                Text(
                    text = "Protect your attention today.",
                    fontSize = 14.sp,
                    color = FocusOnSurfaceVariant
                )
            }

            // 2. Hero Focus Ring Card
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(FocusSurfaceContainer)
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        FocusRing(
                            progress = dailyStats.focusScorePercent / 100f,
                            scoreLabel = "FOCUS SCORE ${dailyStats.focusScorePercent}%",
                            timeText = formatMinutes(dailyStats.focusedTodayMinutes),
                            subLabel = "Focused today",
                            size = 190.dp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.TrendingUp,
                                contentDescription = null,
                                tint = FocusPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "You’re doing better than yesterday",
                                fontSize = 13.sp,
                                color = FocusOnSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Stat Pills below dial
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(FocusSurfaceContainerHigh)
                                    .padding(vertical = 8.dp, horizontal = 12.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Timelapse,
                                    contentDescription = null,
                                    tint = FocusPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${dailyStats.recoveredMinutesToday}m",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = FocusPrimary
                                )
                                Text(
                                    text = " recovered",
                                    fontSize = 12.sp,
                                    color = FocusOnSurface
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(FocusSurfaceContainerHigh)
                                    .padding(vertical = 8.dp, horizontal = 12.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = FocusTertiary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${dailyStats.blockedAttemptsToday}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = FocusTertiary
                                )
                                Text(
                                    text = " blocked",
                                    fontSize = 12.sp,
                                    color = FocusOnSurface
                                )
                            }
                        }
                    }
                }
            }

            // 3. Quick Block Duration & Start Deep Focus Action
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Quick Block Duration",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = FocusOnSurfaceVariant
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable { onNavigateToSettings() }
                                .padding(4.dp)
                        ) {
                            Text(
                                text = "Preferences",
                                fontSize = 12.sp,
                                color = FocusPrimary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "Preferences",
                                tint = FocusPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    val durations = listOf(
                        25 to "25 min",
                        60 to "1 hour",
                        120 to "2 hours",
                        180 to "Custom..."
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(durations) { (mins, label) ->
                            val isSelected = selectedDuration == mins
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(
                                        if (isSelected) FocusPrimary else FocusSurfaceContainerHigh
                                    )
                                    .clickable { viewModel.selectDuration(mins) }
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                                    .testTag("duration_chip_$mins")
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) FocusOnPrimary else FocusOnSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Button(
                        onClick = {
                            viewModel.startDeepFocus()
                            onNavigateToChamber()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = FocusPrimaryContainer),
                        shape = RoundedCornerShape(26.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("start_deep_focus_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = FocusOnPrimaryContainer,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Start Deep Focus",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = FocusOnPrimaryContainer
                        )
                    }
                }
            }

            // 4. Live Active Session Card
            item {
                val session = activeSession
                if (session != null && session.isRunning) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                Brush.verticalGradient(
                                    listOf(FocusSurfaceContainerHigh, FocusSurfaceContainer)
                                )
                            )
                            .padding(16.dp)
                            .testTag("active_session_card")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(FocusPrimary)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Focus session active",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = FocusOnSurface
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(FocusPrimary.copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = session.title,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = FocusPrimary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = session.formattedRemainingTime,
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = FocusOnSurface,
                                    letterSpacing = (-1).sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "remaining",
                                    fontSize = 14.sp,
                                    color = FocusOnSurfaceVariant,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                            }
                            Text(
                                text = "Ends ${session.endTimeText}",
                                fontSize = 12.sp,
                                color = FocusOnSurfaceVariant,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Blocked App Badges in Active Session
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Instagram
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(FocusSurfaceContainerHighest)
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PhotoCamera,
                                    contentDescription = null,
                                    tint = FocusTertiary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Instagram", fontSize = 11.sp, color = FocusOnSurface)
                                Text(" (Reels)", fontSize = 11.sp, color = FocusOnSurfaceVariant)
                            }

                            // YouTube
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(FocusSurfaceContainerHighest)
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SmartDisplay,
                                    contentDescription = null,
                                    tint = FocusError,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("YouTube", fontSize = 11.sp, color = FocusOnSurface)
                                Text(" (Shorts)", fontSize = 11.sp, color = FocusOnSurfaceVariant)
                            }

                            // TikTok
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(FocusSurfaceContainerHighest)
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = FocusSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("TikTok", fontSize = 11.sp, color = FocusOnSurface)
                                Text(" (Full)", fontSize = 11.sp, color = FocusOnSurfaceVariant)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Manage & End Session Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = onNavigateToBlocks,
                                colors = ButtonDefaults.buttonColors(containerColor = FocusSurfaceContainerHighest),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .testTag("manage_active_session_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = null,
                                    tint = FocusOnSurface,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Manage", fontSize = 13.sp, color = FocusOnSurface)
                            }

                            Button(
                                onClick = { viewModel.endSession() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = FocusErrorContainer.copy(alpha = 0.4f)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .testTag("end_session_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LockReset,
                                    contentDescription = null,
                                    tint = FocusOnErrorContainer,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("End Session", fontSize = 13.sp, color = FocusOnErrorContainer)
                            }
                        }
                    }
                }
            }

            // 5. Blocked Attempts Card
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(FocusSurfaceContainer)
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = FocusPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Blocked Attempts",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = FocusOnSurface
                            )
                        }
                        Text(
                            text = dailyStats.totalBlockedAllTime.toString() + " total",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = FocusPrimary
                        )
                    }

                    Text(
                        text = "Interceptions executed smoothly before apps opened.",
                        fontSize = 12.sp,
                        color = FocusOnSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                    )

                    // Apps List, driven by the real interception log
                    if (appInterventions.isEmpty()) {
                        Text(
                            text = "No interceptions yet. Blocked apps you open will appear here.",
                            fontSize = 12.sp,
                            color = FocusOnSurfaceVariant,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            appInterventions.forEach { stat ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(FocusSurfaceContainerHigh)
                                        .padding(10.dp)
                                        .testTag("intercept_row_" + stat.appName),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(FocusSurfaceVariant),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Shield,
                                                contentDescription = null,
                                                tint = FocusTertiary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = stat.appName,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = FocusOnSurface
                                            )
                                            Text(
                                                text = stat.subtext,
                                                fontSize = 12.sp,
                                                color = FocusOnSurfaceVariant
                                            )
                                        }
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = stat.attemptsCount.toString(),
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = FocusOnSurface
                                        )
                                        Text(
                                            text = "attempts",
                                            fontSize = 11.sp,
                                            color = FocusOnSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 6. Reclaimed Energy Card
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(FocusSurfaceContainer)
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "RECLAIMED ENERGY",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = FocusPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = dailyStats.recoveredMinutesToday.toString(),
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = FocusOnSurface
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "minutes",
                                fontSize = 18.sp,
                                color = FocusOnSurfaceVariant,
                                modifier = Modifier.padding(bottom = 3.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "That’s enough time for one deep DSA practice session.",
                            fontSize = 13.sp,
                            color = FocusOnSurfaceVariant
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(FocusPrimary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SelfImprovement,
                            contentDescription = null,
                            tint = FocusPrimary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }

            // 7. Today's Goal Card
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(FocusSurfaceContainer)
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Flag,
                                contentDescription = null,
                                tint = FocusPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "TODAY’S GOAL",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = FocusOnSurface
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = FocusOnSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Locked Mode",
                                fontSize = 11.sp,
                                color = FocusOnSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "“${dailyStats.todayGoalText}”",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = FocusOnSurface,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { viewModel.setEditGoalDialogOpen(true) },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(FocusSurfaceContainerHigh)
                                .testTag("edit_goal_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit goal",
                                tint = FocusOnSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // 8. Mindful Shift Detected Card
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(FocusSurfaceContainerHigh)
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(FocusPrimary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Insights,
                            contentDescription = null,
                            tint = FocusPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Mindful Shift Detected",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = FocusOnSurface
                        )
                        Text(
                            text = mindfulShiftMessage(appInterventions),
                            fontSize = 12.sp,
                            color = FocusOnSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}


/** Today's date, matching the dashboard's uppercase styling. */
private fun todayLabel(): String = java.text.SimpleDateFormat("EEEE, MMM d", java.util.Locale.getDefault())
    .format(java.util.Date())
    .uppercase(java.util.Locale.getDefault())

/** Greeting chosen from the time of day rather than fixed at "morning". */
private fun greetingForNow(): String {
    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    return when {
        hour < 12 -> "Good morning"
        hour < 17 -> "Good afternoon"
        else -> "Good evening"
    }
}

private fun formatMinutes(minutes: Int): String =
    com.example.data.analytics.FocusAnalytics.formatMinutes(minutes)

/**
 * Message for the "Mindful Shift" card.
 *
 * Built from the app with the most interceptions so it reports something real, and
 * falls back to an invitation rather than a fabricated percentage when nothing has
 * been intercepted yet.
 */
private fun mindfulShiftMessage(
    interventions: List<com.example.data.model.AppInterventionStat>
): String {
    val top = interventions.maxByOrNull { it.attemptsCount }
        ?: return "Once FocusGuard intercepts a blocked app, your progress shows up here."
    val saved = com.example.data.analytics.FocusAnalytics.formatMinutes(
        top.attemptsCount * com.example.data.analytics.FocusAnalytics.MINUTES_RECOVERED_PER_INTERCEPTION
    )
    return "FocusGuard stopped " + top.appName + " " + top.attemptsCount +
        " times, saving about " + saved + "."
}
