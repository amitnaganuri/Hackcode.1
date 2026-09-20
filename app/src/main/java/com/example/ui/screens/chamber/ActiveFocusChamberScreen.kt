package com.example.ui.screens.chamber

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.components.FocusGuardTopBar
import com.example.ui.theme.FocusError
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
import com.example.viewmodel.FocusGuardViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ActiveFocusChamberScreen(
    viewModel: FocusGuardViewModel,
    onReturnToFocus: () -> Unit,
    onNavigateToEditor: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interventionConfig by viewModel.interventionConfig.collectAsState()
    val dailyStats by viewModel.dailyStats.collectAsState()

    var remainingSeconds by remember { mutableIntStateOf(interventionConfig.countdownSeconds) }
    var isReflectionDialogOpen by remember { mutableStateOf(false) }
    var selectedReflectionReason by remember { mutableStateOf<String?>(null) }
    var isEmergencyDialogOpen by remember { mutableStateOf(false) }

    // Countdown timer
    LaunchedEffect(Unit) {
        while (remainingSeconds > 0) {
            delay(1000)
            remainingSeconds -= 1
        }
    }

    val progress = if (interventionConfig.countdownSeconds > 0) {
        remainingSeconds.toFloat() / interventionConfig.countdownSeconds
    } else 0f

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 500),
        label = "countdown_anim"
    )

    // Reflection Dialog
    if (isReflectionDialogOpen) {
        AlertDialog(
            onDismissRequest = { isReflectionDialogOpen = false },
            containerColor = FocusSurfaceContainerHigh,
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = null,
                        tint = FocusPrimary
                    )
                    Text(
                        text = "Mindful Check-in",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = FocusOnSurface
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Why were you opening ${interventionConfig.targetApp} right now?",
                        fontSize = 13.sp,
                        color = FocusOnSurfaceVariant
                    )

                    val reflectionReasons = listOf(
                        "Boredom / Restless",
                        "Habitual reflex",
                        "Stress relief",
                        "Avoiding hard problem",
                        "Legitimate quick task"
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        reflectionReasons.forEach { reason ->
                            val isSelected = selectedReflectionReason == reason
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        if (isSelected) FocusPrimary else FocusSurfaceContainerHighest
                                    )
                                    .clickable { selectedReflectionReason = reason }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = reason,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) FocusOnPrimary else FocusOnSurface
                                )
                            }
                        }
                    }

                    if (selectedReflectionReason != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Awareness is the cure to mindless scrolling. Take a slow breath.",
                            fontSize = 12.sp,
                            color = FocusPrimary
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isReflectionDialogOpen = false
                        onReturnToFocus()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FocusPrimary),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text("Close & Return to Focus", color = FocusOnPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { isReflectionDialogOpen = false }) {
                    Text("Back", color = FocusOnSurfaceVariant)
                }
            }
        )
    }

    // Emergency 30s Pass Dialog
    if (isEmergencyDialogOpen) {
        AlertDialog(
            onDismissRequest = { isEmergencyDialogOpen = false },
            containerColor = FocusSurfaceContainerHigh,
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = FocusTertiary
                    )
                    Text(
                        text = "Emergency 30s Pass",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = FocusOnSurface
                    )
                }
            },
            text = {
                Text(
                    text = "Strict Mode limits emergency overrides to 1 time per day. You will have exactly 30 seconds before the barrier locks again.",
                    fontSize = 13.sp,
                    color = FocusOnSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        isEmergencyDialogOpen = false
                        onReturnToFocus()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FocusTertiary),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text("Use 30s Pass", color = FocusSurface, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { isEmergencyDialogOpen = false }) {
                    Text("Cancel", color = FocusOnSurfaceVariant)
                }
            }
        )
    }

    Scaffold(
        topBar = {
            FocusGuardTopBar(
                title = "FOCUS CHAMBER",
                subtitle = "Intervention Pause",
                showBackButton = true,
                onBackClick = onReturnToFocus,
                onShieldActionClick = onReturnToFocus
            )
        },
        containerColor = FocusSurface,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // 1. Header: Active Focus Chamber & Paused App Pill
            Text(
                text = "ACTIVE FOCUS CHAMBER",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                color = FocusPrimary
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(FocusSurfaceContainerHigh)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Pause,
                    contentDescription = null,
                    tint = FocusPrimary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${interventionConfig.targetApp} is paused",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = FocusOnSurface
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Personal Anchor Card with Custom Photo & Inspiring Quote
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(FocusSurfaceContainer)
                    .testTag("personal_anchor_card")
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                ) {
                    AsyncImage(
                        model = interventionConfig.anchorImageUrl,
                        contentDescription = "Personal Anchor Photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
                    )
                    // Dark gradient scrim
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Transparent, FocusSurfaceContainer)
                                )
                            )
                    )
                    // Anchor Label & Edit Button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(FocusSurface.copy(alpha = 0.8f))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = interventionConfig.anchorLabel.uppercase(),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = FocusPrimary
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(FocusSurface.copy(alpha = 0.85f))
                                .clickable { onNavigateToEditor() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Anchor",
                                tint = FocusOnSurface,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 14.dp)
                ) {
                    Text(
                        text = interventionConfig.customQuote,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = FocusOnSurface,
                        lineHeight = 24.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = interventionConfig.subMessage,
                        fontSize = 13.sp,
                        color = FocusOnSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 3. Mindful Pause Circular Countdown Dial
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Mindful Pause",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = FocusOnSurface
                )
                Text(
                    text = "Hold the calm inside",
                    fontSize = 13.sp,
                    color = FocusOnSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                val ringSize = 160.dp
                val strokeWidth = 8.dp

                Box(
                    modifier = Modifier.size(ringSize),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(ringSize)) {
                        val strokePx = strokeWidth.toPx()
                        val radius = (ringSize.toPx() - strokePx) / 2f
                        val centerOffset = Offset(ringSize.toPx() / 2f, ringSize.toPx() / 2f)

                        // Background dashed track
                        drawCircle(
                            color = FocusSurfaceVariant.copy(alpha = 0.5f),
                            radius = radius,
                            center = centerOffset,
                            style = Stroke(
                                width = strokePx * 0.8f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 14f), 0f)
                            )
                        )

                        // Countdown arc
                        val sweep = 360f * animatedProgress
                        drawArc(
                            color = FocusPrimary,
                            startAngle = -90f,
                            sweepAngle = sweep,
                            useCenter = false,
                            topLeft = Offset(strokePx / 2f, strokePx / 2f),
                            size = Size(ringSize.toPx() - strokePx, ringSize.toPx() - strokePx),
                            style = Stroke(
                                width = strokePx,
                                cap = StrokeCap.Round
                            )
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "%02d".format(remainingSeconds),
                            fontSize = 44.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-1).sp,
                            color = FocusOnSurface,
                            lineHeight = 46.sp
                        )
                        Text(
                            text = "SECONDS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = FocusPrimary
                        )
                        Text(
                            text = if (remainingSeconds > 0) "until option unlocks" else "pause complete",
                            fontSize = 11.sp,
                            color = FocusOnSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 4. Active Target Ribbon
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(FocusSurfaceContainer)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(FocusPrimary)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "ACTIVE TARGET",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = FocusOutline
                        )
                        Text(
                            text = interventionConfig.activeTargetName,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = FocusOnSurface
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(FocusPrimary.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = interventionConfig.activeTargetBadge,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = FocusPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 5. Action Buttons
            Button(
                onClick = onReturnToFocus,
                colors = ButtonDefaults.buttonColors(containerColor = FocusPrimaryContainer),
                shape = RoundedCornerShape(26.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("return_to_focus_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = FocusOnPrimaryContainer,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Return to Focus",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = FocusOnPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = { isReflectionDialogOpen = true },
                shape = RoundedCornerShape(26.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("reflection_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Psychology,
                    contentDescription = null,
                    tint = FocusOnSurface,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Why am I opening this?",
                    fontSize = 14.sp,
                    color = FocusOnSurface
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Emergency 30s Pass
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clickable { isEmergencyDialogOpen = true }
                    .padding(8.dp)
                    .testTag("emergency_pass_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = FocusTertiary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Emergency 30s Pass (Strict Mode: 1 left today)",
                    fontSize = 12.sp,
                    color = FocusTertiary
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
