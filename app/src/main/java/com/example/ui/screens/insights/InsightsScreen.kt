package com.example.ui.screens.insights

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Timelapse
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.AppBrandLogo
import com.example.ui.components.FocusGuardTopBar
import com.example.ui.components.WeeklyBalanceChart
import com.example.ui.theme.FocusOnPrimary
import com.example.ui.theme.FocusOnSurface
import com.example.ui.theme.FocusOnSurfaceVariant
import com.example.ui.theme.FocusPrimary
import com.example.ui.theme.FocusSecondary
import com.example.ui.theme.FocusSurface
import com.example.ui.theme.FocusSurfaceContainer
import com.example.ui.theme.FocusSurfaceContainerHigh
import com.example.ui.theme.FocusSurfaceContainerHighest
import com.example.ui.theme.FocusSurfaceContainerLow
import com.example.ui.theme.FocusSurfaceVariant
import com.example.ui.theme.FocusTertiary
import com.example.viewmodel.FocusGuardViewModel

@Composable
fun InsightsScreen(
    viewModel: FocusGuardViewModel,
    onNavigateToChamber: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dailyStats by viewModel.dailyStats.collectAsState()
    val weeklyBalance by viewModel.weeklyBalance.collectAsState()
    val appInterventions by viewModel.appInterventions.collectAsState()
    val selectedTimeframe by viewModel.selectedTimeframe.collectAsState()

    val timeframes = listOf("Today", "This Week", "This Month")

    Scaffold(
        topBar = {
            FocusGuardTopBar(
                subtitle = "Attention Insights",
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
            // 1. Timeframe Switcher
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(FocusSurfaceContainerHigh)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    timeframes.forEach { timeframe ->
                        val isSelected = selectedTimeframe == timeframe
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isSelected) FocusPrimary else androidx.compose.ui.graphics.Color.Transparent)
                                .clickable { viewModel.selectTimeframe(timeframe) }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = timeframe,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) FocusOnPrimary else FocusOnSurfaceVariant
                            )
                        }
                    }
                }
            }

            // 2. Hero Screen Time Banner
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(FocusSurfaceContainer)
                        .padding(20.dp)
                        .testTag("insights_screen_time_card")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(FocusPrimary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Eco,
                                    contentDescription = null,
                                    tint = FocusPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "ACTIVE SCREEN TIME",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = FocusPrimary
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(FocusPrimary.copy(alpha = 0.18f))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "-${dailyStats.screenTimeReductionPercent}% vs last week",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = FocusPrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = dailyStats.activeScreenTimeText,
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-1).sp,
                        color = FocusOnSurface
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = dailyStats.screenTimeComparisonText,
                        fontSize = 13.sp,
                        color = FocusOnSurfaceVariant
                    )
                }
            }

            // 3. Tri-Metric Grid
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Deep Focus
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(FocusSurfaceContainer)
                            .padding(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(FocusPrimary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                tint = FocusPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Deep Focus", fontSize = 11.sp, color = FocusOnSurfaceVariant)
                        Text(
                            text = dailyStats.deepFocusWeeklyText,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = FocusOnSurface
                        )
                        Text("+14% vs avg", fontSize = 10.sp, color = FocusPrimary)
                    }

                    // Recovered
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(FocusSurfaceContainer)
                            .padding(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(FocusSecondary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Timelapse,
                                contentDescription = null,
                                tint = FocusSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Recovered", fontSize = 11.sp, color = FocusOnSurfaceVariant)
                        Text(
                            text = dailyStats.recoveredWeeklyText,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = FocusOnSurface
                        )
                        Text("12 traps halted", fontSize = 10.sp, color = FocusSecondary)
                    }

                    // Blocked
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(FocusSurfaceContainer)
                            .padding(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(FocusTertiary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = FocusTertiary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Blocked", fontSize = 11.sp, color = FocusOnSurfaceVariant)
                        Text(
                            text = "${dailyStats.blockedWeeklyCount}",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = FocusOnSurface
                        )
                        Text("Interceptions", fontSize = 10.sp, color = FocusTertiary)
                    }
                }
            }

            // 4. Weekly Balance Chart
            item {
                WeeklyBalanceChart(days = weeklyBalance)
            }

            // 5. Reclaimed Presence (Real World Equivalents)
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(FocusSurfaceContainer)
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Reclaimed Presence",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = FocusOnSurface
                    )
                    Text(
                        text = "Real-world activities unlocked from saved screen time",
                        fontSize = 12.sp,
                        color = FocusOnSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Card 1
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(FocusSurfaceContainerLow)
                                .padding(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoStories,
                                contentDescription = null,
                                tint = FocusPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("45 Pages Read", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = FocusOnSurface)
                            Text("Book progress", fontSize = 11.sp, color = FocusOnSurfaceVariant)
                        }

                        // Card 2
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(FocusSurfaceContainerLow)
                                .padding(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FitnessCenter,
                                contentDescription = null,
                                tint = FocusTertiary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("2 Workouts", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = FocusOnSurface)
                            Text("Physical fitness", fontSize = 11.sp, color = FocusOnSurfaceVariant)
                        }

                        // Card 3
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(FocusSurfaceContainerLow)
                                .padding(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.School,
                                contentDescription = null,
                                tint = FocusSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("3 Study Blocks", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = FocusOnSurface)
                            Text("DSA algorithms", fontSize = 11.sp, color = FocusOnSurfaceVariant)
                        }
                    }
                }
            }

            // 6. App Interventions Breakdown
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(FocusSurfaceContainer)
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Interventions by Application",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = FocusOnSurface
                    )
                    Text(
                        text = "Real-time barriers triggered before mindless browsing.",
                        fontSize = 12.sp,
                        color = FocusOnSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        appInterventions.forEach { item ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(FocusSurfaceContainerLow)
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        AppBrandLogo(appName = item.appName, size = 32)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = item.appName,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = FocusOnSurface
                                            )
                                            Text(
                                                text = item.subtext,
                                                fontSize = 11.sp,
                                                color = FocusOnSurfaceVariant
                                            )
                                        }
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = item.timeFormatted,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = FocusPrimary
                                        )
                                        Text(
                                            text = item.reductionPercentText,
                                            fontSize = 11.sp,
                                            color = FocusSecondary
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                LinearProgressIndicator(
                                    progress = { item.progressFraction },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp)),
                                    color = FocusPrimary,
                                    trackColor = FocusSurfaceContainerHighest
                                )
                            }
                        }
                    }
                }
            }

            // 7. Focus Streak Card
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(FocusSurfaceContainer)
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(FocusTertiary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = null,
                            tint = FocusTertiary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${dailyStats.streakDays}-Day Focus Streak",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = FocusOnSurface
                        )
                        Text(
                            text = "Tier I • Consistent Guardian",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = FocusTertiary
                        )
                        Text(
                            text = "${dailyStats.streakProtectedPercent}% of planned blocks protected with zero overrides.",
                            fontSize = 11.sp,
                            color = FocusOnSurfaceVariant
                        )
                    }
                }
            }

            // 8. Simone Weil Quote Card
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(FocusSurfaceVariant.copy(alpha = 0.35f))
                        .padding(16.dp)
                ) {
                    Text(
                        text = "“Attention is the rarest and purest form of generosity.”\n— Simone Weil",
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = FocusOnSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
