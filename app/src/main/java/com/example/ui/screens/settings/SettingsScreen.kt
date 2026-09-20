package com.example.ui.screens.settings

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.FocusGuardBrandIcon
import com.example.ui.components.FocusGuardTopBar
import com.example.ui.components.StrictSecurityDialog
import com.example.ui.theme.FocusOnPrimary
import com.example.ui.theme.FocusOnSurface
import com.example.ui.theme.FocusOnSurfaceVariant
import com.example.ui.theme.FocusOutline
import com.example.ui.theme.FocusPrimary
import com.example.ui.theme.FocusSecondary
import com.example.ui.theme.FocusSurface
import com.example.ui.theme.FocusSurfaceContainer
import com.example.ui.theme.FocusSurfaceContainerHigh
import com.example.ui.theme.FocusSurfaceContainerHighest
import com.example.ui.theme.FocusSurfaceContainerLow
import com.example.ui.theme.FocusTertiary
import com.example.viewmodel.FocusGuardViewModel

@Composable
fun SettingsScreen(
    viewModel: FocusGuardViewModel,
    onNavigateToChamber: () -> Unit,
    onNavigateToEditor: () -> Unit,
    modifier: Modifier = Modifier
) {
    val masterShield by viewModel.shortsAndReelsMasterShield.collectAsState()
    val dailyStats by viewModel.dailyStats.collectAsState()

    var isStrictModeEnabled by remember { mutableStateOf(true) }
    var isDndEnabled by remember { mutableStateOf(true) }
    var isNotificationsEnabled by remember { mutableStateOf(true) }
    var isSecurityDialogOpen by remember { mutableStateOf(false) }

    if (isSecurityDialogOpen) {
        StrictSecurityDialog(onDismiss = { isSecurityDialogOpen = false })
    }

    Scaffold(
        topBar = {
            FocusGuardTopBar(
                subtitle = "Settings & Security",
                onShieldActionClick = onNavigateToChamber
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
            // 1. User Profile Card
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(FocusSurfaceContainer)
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(FocusPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = FocusOnPrimary,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Alex Morgan",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = FocusOnSurface
                        )
                        Text(
                            text = "Guardian Tier I • ${dailyStats.streakDays}-Day Streak",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = FocusPrimary
                        )
                        Text(
                            text = "Focus Score: ${dailyStats.focusScorePercent}%",
                            fontSize = 12.sp,
                            color = FocusOnSurfaceVariant
                        )
                    }
                }
            }

            // 2. Strict Security Section
            item {
                Text(
                    text = "Security & Anti-Tamper",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = FocusOnSurfaceVariant
                )

                Spacer(modifier = Modifier.height(6.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(FocusSurfaceContainer)
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Strict Mode Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = FocusPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Strict Mode",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = FocusOnSurface
                                )
                                Text(
                                    text = "PIN lock and breathing cooldown",
                                    fontSize = 12.sp,
                                    color = FocusOnSurfaceVariant
                                )
                            }
                        }

                        Switch(
                            checked = isStrictModeEnabled,
                            onCheckedChange = { isStrictModeEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = FocusSurface,
                                checkedTrackColor = FocusPrimary
                            )
                        )
                    }

                    // Security Options Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(FocusSurfaceContainerLow)
                            .clickable { isSecurityDialogOpen = true }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = FocusTertiary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "4-Digit PIN & Cooldown Settings",
                                fontSize = 13.sp,
                                color = FocusOnSurface
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = FocusOutline,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // 3. Intervention Chamber Section
            item {
                Text(
                    text = "Intervention Chamber",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = FocusOnSurfaceVariant
                )

                Spacer(modifier = Modifier.height(6.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(FocusSurfaceContainer)
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Customize Anchor
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(FocusSurfaceContainerLow)
                            .clickable { onNavigateToEditor() }
                            .padding(12.dp)
                            .testTag("customize_intervention_settings"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.EditNote,
                                contentDescription = null,
                                tint = FocusPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Customize Personal Anchor",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = FocusOnSurface
                                )
                                Text(
                                    text = "Quote, photo, target, and countdown seconds",
                                    fontSize = 12.sp,
                                    color = FocusOnSurfaceVariant
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = FocusOutline,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Test Chamber View
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(FocusSurfaceContainerLow)
                            .clickable { onNavigateToChamber() }
                            .padding(12.dp)
                            .testTag("test_chamber_view"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Visibility,
                                contentDescription = null,
                                tint = FocusSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Simulate Intervention Screen",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = FocusOnSurface
                                )
                                Text(
                                    text = "Experience the mindful pause as a user",
                                    fontSize = 12.sp,
                                    color = FocusOnSurfaceVariant
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = FocusOutline,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // 4. Permissions & System Services
            item {
                Text(
                    text = "System Permissions",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = FocusOnSurfaceVariant
                )

                Spacer(modifier = Modifier.height(6.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(FocusSurfaceContainer)
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Accessibility Service
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Accessibility Service",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = FocusOnSurface
                            )
                            Text(
                                text = "Detects Shorts / Reels windows",
                                fontSize = 12.sp,
                                color = FocusOnSurfaceVariant
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(FocusPrimary.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("Ready", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = FocusPrimary)
                        }
                    }

                    // Usage Access
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Usage Access",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = FocusOnSurface
                            )
                            Text(
                                text = "Tracks real-time app screen times",
                                fontSize = 12.sp,
                                color = FocusOnSurfaceVariant
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(FocusPrimary.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("Granted", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = FocusPrimary)
                        }
                    }
                }
            }

            // 5. About & Philosophy
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(FocusSurfaceContainer)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    FocusGuardBrandIcon(size = 40)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "FocusGuard v1.0.0",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = FocusOnSurface
                    )
                    Text(
                        text = "Calm Bastion Edition • Hackathon Build",
                        fontSize = 12.sp,
                        color = FocusPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Crafted to safeguard human attention and eliminate compulsive digital loops.",
                        fontSize = 12.sp,
                        color = FocusOnSurfaceVariant,
                        lineHeight = 16.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
