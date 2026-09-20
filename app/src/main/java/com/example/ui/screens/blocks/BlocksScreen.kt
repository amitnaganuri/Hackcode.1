package com.example.ui.screens.blocks

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import com.example.ui.components.AddBlockBottomSheet
import com.example.ui.components.AppBlockCard
import com.example.ui.components.FocusGuardTopBar
import com.example.ui.components.StrictSecurityDialog
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
import com.example.viewmodel.FocusGuardViewModel

@Composable
fun BlocksScreen(
    viewModel: FocusGuardViewModel,
    onNavigateToChamber: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rules by viewModel.rules.collectAsState()
    val masterShield by viewModel.shortsAndReelsMasterShield.collectAsState()
    val isAddSheetOpen by viewModel.isAddBlockSheetOpen.collectAsState()
    val isSecurityDialogOpen by viewModel.isSecurityDialogOpen.collectAsState()

    val activeCount = rules.count { it.isEnabled }

    if (isAddSheetOpen) {
        AddBlockBottomSheet(
            onDismiss = { viewModel.setAddBlockSheetOpen(false) },
            onSaveBlock = { appName, mode, filterLabel, schedule ->
                viewModel.addNewRule(appName, mode, filterLabel, schedule)
            }
        )
    }

    if (isSecurityDialogOpen) {
        StrictSecurityDialog(
            onDismiss = { viewModel.setSecurityDialogOpen(false) }
        )
    }

    Scaffold(
        topBar = {
            FocusGuardTopBar(
                subtitle = "Shield Blocks",
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
            // 1. Header with Active Counter
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ACTIVE BLOCKS: $activeCount",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = FocusPrimary
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(FocusSurfaceContainerHigh)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Enforcing Rules",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = FocusOnSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "App & Content Shields",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.5).sp,
                    color = FocusOnSurface
                )
                Text(
                    text = "Configured rules protect your time silently in the background.",
                    fontSize = 13.sp,
                    color = FocusOnSurfaceVariant
                )
            }

            // 2. Content-Level Block "Shorts & Reels Shield" Featured Spotlight
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(FocusSurfaceContainer)
                        .padding(18.dp)
                        .testTag("shorts_reels_master_card")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "CONTENT-LEVEL BLOCK",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = FocusSecondary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Shorts & Reels Shield",
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                color = FocusOnSurface
                            )
                        }

                        Switch(
                            checked = masterShield,
                            onCheckedChange = { viewModel.toggleShortsAndReelsMasterShield() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = FocusSurface,
                                checkedTrackColor = FocusPrimary,
                                uncheckedThumbColor = FocusOutline,
                                uncheckedTrackColor = FocusSurfaceContainerHighest
                            ),
                            thumbContent = {
                                Icon(
                                    imageVector = if (masterShield) Icons.Default.Check else Icons.Default.Close,
                                    contentDescription = null,
                                    tint = if (masterShield) FocusPrimary else FocusOutline,
                                    modifier = Modifier.size(12.dp)
                                )
                            },
                            modifier = Modifier.testTag("master_shield_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Silently terminates short-form dopamine loops while keeping messaging, search, and work utility accessible.",
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = FocusOnSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Comparison pills
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Allowed
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(FocusSurfaceContainerLow)
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = FocusPrimary,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = "Normal Feed",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = FocusOnSurface
                                )
                                Text(
                                    text = "Allowed",
                                    fontSize = 11.sp,
                                    color = FocusPrimary
                                )
                            }
                        }

                        // Shielded
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(FocusSurfaceContainerLow)
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = FocusSecondary,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = "Endless Scroll",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = FocusOnSurface
                                )
                                Text(
                                    text = "Shielded",
                                    fontSize = 11.sp,
                                    color = FocusSecondary
                                )
                            }
                        }
                    }
                }
            }

            // 3. Configured Apps Section Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Configured Apps",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = FocusOnSurface
                    )
                    Text(
                        text = "${rules.size} total rules",
                        fontSize = 12.sp,
                        color = FocusOnSurfaceVariant
                    )
                }
            }

            // 4. Configured Apps List
            items(rules, key = { it.id }) { rule ->
                AppBlockCard(
                    rule = rule,
                    onToggle = { viewModel.toggleRule(rule.id) },
                    onEditClick = { viewModel.setAddBlockSheetOpen(true) }
                )
            }

            // 5. Strict Mode Locked Banner
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(FocusSurfaceContainer)
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(FocusSurfaceContainerHighest),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = FocusPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Strict Mode is Active",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = FocusOnSurface
                        )
                        Text(
                            text = "Modifications require your 4-digit PIN or 60-second breathing cooldown.",
                            fontSize = 12.sp,
                            color = FocusOnSurfaceVariant
                        )
                    }
                    Button(
                        onClick = { viewModel.setSecurityDialogOpen(true) },
                        colors = ButtonDefaults.buttonColors(containerColor = FocusSurfaceContainerHigh),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("security_options_button")
                    ) {
                        Text("Options", fontSize = 12.sp, color = FocusOnSurface)
                    }
                }
            }

            // 6. Add New Block Rule Button
            item {
                Button(
                    onClick = { viewModel.setAddBlockSheetOpen(true) },
                    colors = ButtonDefaults.buttonColors(containerColor = FocusPrimaryContainer),
                    shape = RoundedCornerShape(26.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("add_block_rule_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = FocusOnPrimaryContainer,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "+ Add New Block Rule",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = FocusOnPrimaryContainer
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}
