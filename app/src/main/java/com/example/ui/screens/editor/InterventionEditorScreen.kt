package com.example.ui.screens.editor

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.FocusGuardTopBar
import com.example.ui.theme.FocusOnPrimary
import com.example.ui.theme.FocusOnPrimaryContainer
import com.example.ui.theme.FocusOnSurface
import com.example.ui.theme.FocusOnSurfaceVariant
import com.example.ui.theme.FocusPrimary
import com.example.ui.theme.FocusPrimaryContainer
import com.example.ui.theme.FocusSurface
import com.example.ui.theme.FocusSurfaceContainer
import com.example.ui.theme.FocusSurfaceContainerHighest
import com.example.viewmodel.FocusGuardViewModel

@Composable
fun InterventionEditorScreen(
    viewModel: FocusGuardViewModel,
    onBack: () -> Unit,
    onPreviewChamber: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentConfig by viewModel.interventionConfig.collectAsState()

    var quoteText by remember { mutableStateOf(currentConfig.customQuote) }
    var subMessageText by remember { mutableStateOf(currentConfig.subMessage) }
    var anchorLabelText by remember { mutableStateOf(currentConfig.anchorLabel) }
    var activeTargetText by remember { mutableStateOf(currentConfig.activeTargetName) }
    var countdownSeconds by remember { mutableFloatStateOf(currentConfig.countdownSeconds.toFloat()) }

    Scaffold(
        topBar = {
            FocusGuardTopBar(
                title = "INTERVENTION EDITOR",
                subtitle = "Customize Pause Screen",
                showBackButton = true,
                onBackClick = onBack
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Tailor Your Reflection Chamber",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = FocusOnSurface
            )
            Text(
                text = "When an intercepted app is opened, this mindful barrier will remind you of your core commitment.",
                fontSize = 13.sp,
                color = FocusOnSurfaceVariant
            )

            // Quote field
            OutlinedTextField(
                value = quoteText,
                onValueChange = { quoteText = it },
                label = { Text("Inspiring Anchor Quote") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.FormatQuote,
                        contentDescription = null,
                        tint = FocusPrimary
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = FocusOnSurface,
                    unfocusedTextColor = FocusOnSurface,
                    focusedBorderColor = FocusPrimary,
                    unfocusedBorderColor = FocusSurfaceContainerHighest
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("quote_input_field")
            )

            // Submessage field
            OutlinedTextField(
                value = subMessageText,
                onValueChange = { subMessageText = it },
                label = { Text("Sub-Message / Reminder") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = FocusOnSurface,
                    unfocusedTextColor = FocusOnSurface,
                    focusedBorderColor = FocusPrimary,
                    unfocusedBorderColor = FocusSurfaceContainerHighest
                ),
                modifier = Modifier.fillMaxWidth()
            )

            // Active Target field
            OutlinedTextField(
                value = activeTargetText,
                onValueChange = { activeTargetText = it },
                label = { Text("Active Target Title") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = FocusOnSurface,
                    unfocusedTextColor = FocusOnSurface,
                    focusedBorderColor = FocusPrimary,
                    unfocusedBorderColor = FocusSurfaceContainerHighest
                ),
                modifier = Modifier.fillMaxWidth()
            )

            // Countdown slider
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
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
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = FocusPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Mindful Pause Duration",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = FocusOnSurface
                        )
                    }
                    Text(
                        text = "${countdownSeconds.toInt()} seconds",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = FocusPrimary
                    )
                }

                Slider(
                    value = countdownSeconds,
                    onValueChange = { countdownSeconds = it },
                    valueRange = 3f..20f,
                    steps = 16,
                    colors = SliderDefaults.colors(
                        thumbColor = FocusPrimary,
                        activeTrackColor = FocusPrimary,
                        inactiveTrackColor = FocusSurfaceContainerHighest
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action Buttons
            Button(
                onClick = {
                    viewModel.updateInterventionConfig(
                        currentConfig.copy(
                            customQuote = quoteText,
                            subMessage = subMessageText,
                            anchorLabel = anchorLabelText,
                            activeTargetName = activeTargetText,
                            countdownSeconds = countdownSeconds.toInt()
                        )
                    )
                    onBack()
                },
                colors = ButtonDefaults.buttonColors(containerColor = FocusPrimaryContainer),
                shape = RoundedCornerShape(26.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_intervention_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = FocusOnPrimaryContainer,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Save Changes",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = FocusOnPrimaryContainer
                )
            }

            OutlinedButton(
                onClick = {
                    viewModel.updateInterventionConfig(
                        currentConfig.copy(
                            customQuote = quoteText,
                            subMessage = subMessageText,
                            anchorLabel = anchorLabelText,
                            activeTargetName = activeTargetText,
                            countdownSeconds = countdownSeconds.toInt()
                        )
                    )
                    onPreviewChamber()
                },
                shape = RoundedCornerShape(26.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("preview_chamber_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Visibility,
                    contentDescription = null,
                    tint = FocusOnSurface,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Preview Chamber Live",
                    fontSize = 14.sp,
                    color = FocusOnSurface
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
