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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
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
import com.example.data.model.AppCatalog
import com.example.data.model.BlockMode
import com.example.data.model.BlockedAppRule
import com.example.ui.theme.FocusOnPrimary
import com.example.ui.theme.FocusOnSurface
import com.example.ui.theme.FocusOnSurfaceVariant
import com.example.ui.theme.FocusOutline
import com.example.ui.theme.FocusPrimary
import com.example.ui.theme.FocusSecondary
import com.example.ui.theme.FocusSecondaryContainer
import com.example.ui.theme.FocusSurfaceContainer
import com.example.ui.theme.FocusSurfaceContainerHigh
import com.example.ui.theme.FocusSurfaceContainerHighest
import com.example.ui.theme.FocusTertiary
import com.example.ui.theme.FocusTertiaryContainer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBlockBottomSheet(
    onDismiss: () -> Unit,
    onSaveBlock: (appName: String, mode: BlockMode, filterLabel: String, schedule: String) -> Unit,
    existingRule: BlockedAppRule? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    // Keyed on the rule so reopening the sheet for a different rule re-seeds the fields.
    var selectedMode by remember(existingRule) { mutableStateOf(existingRule?.blockMode) }
    var selectedApp by remember(existingRule) { mutableStateOf(existingRule?.appName ?: "Reddit") }
    // There is no schedule picker in this sheet yet, so every new rule inherits this
    // default. A weekday-only default means a rule the user just created silently does
    // nothing at weekends, so a newly created block defaults to always-on; the Rule
    // Summary below shows exactly what will be saved.
    var customSchedule by remember(existingRule) {
        mutableStateOf(existingRule?.scheduleText ?: "All Day • Daily")
    }

    val isEditing = existingRule != null
    // Only apps with a known real package can be blocked; a fabricated package name
    // would make the rule silently never match the foreground app.
    val candidateApps = AppCatalog.selectableAppNames()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = FocusSurfaceContainerHigh,
        contentColor = FocusOnSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when {
                        selectedMode == null -> "Select Protection Type"
                        isEditing -> "Edit Block: $selectedApp"
                        else -> "Configure Block: $selectedApp"
                    },
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = FocusOnSurface
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = FocusOnSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (selectedMode == null) {
                // Step 1: Select Protection Type
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Option 1: Shorts / Reels
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(FocusSurfaceContainer)
                            .clickable { selectedMode = BlockMode.CONTENT_LEVEL }
                            .padding(14.dp)
                            .testTag("option_block_shorts_reels"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(FocusSecondaryContainer.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCut,
                                contentDescription = null,
                                tint = FocusSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Block Shorts / Reels",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = FocusOnSurface
                            )
                            Text(
                                text = "Keep the core utility of apps, silence dopamine feeds.",
                                fontSize = 12.sp,
                                color = FocusOnSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = FocusOutline,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Option 2: Full App Block
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(FocusSurfaceContainer)
                            .clickable { selectedMode = BlockMode.HARD_BLOCK }
                            .padding(14.dp)
                            .testTag("option_block_entire_app"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(FocusPrimary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = FocusPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Block Entire Application",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = FocusOnSurface
                            )
                            Text(
                                text = "Completely locks launcher access on a custom schedule.",
                                fontSize = 12.sp,
                                color = FocusOnSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = FocusOutline,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Option 3: Daily Limit
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(FocusSurfaceContainer)
                            .clickable { selectedMode = BlockMode.DAILY_ALLOWANCE }
                            .padding(14.dp)
                            .testTag("option_block_daily_limit"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(FocusTertiaryContainer.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.HourglassTop,
                                contentDescription = null,
                                tint = FocusTertiary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Set Daily Time Limit",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = FocusOnSurface
                            )
                            Text(
                                text = "Allocate a mindful budget per category or individual app.",
                                fontSize = 12.sp,
                                color = FocusOnSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = FocusOutline,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            } else {
                // Step 2: Configure & Select App
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        text = "Choose Application:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = FocusOnSurfaceVariant
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(candidateApps) { app ->
                            val isChosen = selectedApp == app
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (isChosen) FocusPrimary else FocusSurfaceContainerHighest)
                                    .clickable { selectedApp = app }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = app,
                                    fontSize = 13.sp,
                                    fontWeight = if (isChosen) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isChosen) FocusOnPrimary else FocusOnSurface
                                )
                            }
                        }
                    }

                    val filterLabel = when {
                        existingRule != null && selectedMode == existingRule.blockMode ->
                            existingRule.filterLabel
                        selectedMode == BlockMode.CONTENT_LEVEL -> "Shorts & Feed Only"
                        selectedMode == BlockMode.HARD_BLOCK -> "Entire App Blocked"
                        selectedMode == BlockMode.DAILY_ALLOWANCE -> "30 min / day allowance"
                        else -> ""
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(FocusSurfaceContainer)
                            .padding(12.dp)
                    ) {
                        Column {
                            Text(
                                text = "Rule Summary",
                                fontSize = 12.sp,
                                color = FocusOutline,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "$selectedApp • $filterLabel",
                                fontSize = 14.sp,
                                color = FocusOnSurface,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Schedule: $customSchedule",
                                fontSize = 12.sp,
                                color = FocusOnSurfaceVariant
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { selectedMode = null },
                            colors = ButtonDefaults.buttonColors(containerColor = FocusSurfaceContainerHighest),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Back", color = FocusOnSurface)
                        }

                        Button(
                            onClick = {
                                onSaveBlock(
                                    selectedApp,
                                    selectedMode ?: BlockMode.HARD_BLOCK,
                                    filterLabel,
                                    customSchedule
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = FocusPrimary),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("save_block_rule_button")
                        ) {
                            Text(
                                text = if (isEditing) "Update Rule" else "Save Rule",
                                color = FocusOnPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
