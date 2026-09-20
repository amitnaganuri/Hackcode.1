package com.example.ui.onboarding

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.FocusError
import com.example.ui.theme.FocusOnPrimary
import com.example.ui.theme.FocusOnSurface
import com.example.ui.theme.FocusOnSurfaceVariant
import com.example.ui.theme.FocusOutline
import com.example.ui.theme.FocusPrimary
import com.example.ui.theme.FocusSurface
import com.example.ui.theme.FocusSurfaceContainer
import com.example.ui.theme.FocusSurfaceContainerHigh

/**
 * Shown while the permission FocusGuard cannot work without is missing.
 *
 * Accessibility access is not a nice-to-have here: without it the service never learns
 * which app is in the foreground, so every rule silently does nothing. Presenting the
 * app as working in that state is the worst outcome, so this stands in front until the
 * permission is granted and steps aside the moment it is.
 */
@Composable
fun PermissionOnboardingScreen(
    isAccessibilityGranted: Boolean,
    isUsageAccessGranted: Boolean,
    onGrantAccessibility: () -> Unit,
    onGrantUsageAccess: () -> Unit,
    onContinueAnyway: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(FocusSurface)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(FocusPrimary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = null,
                tint = FocusPrimary,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "One step before you start",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = FocusOnSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "UltimateFocus needs one permission to see which app you have opened. " +
                "Without it, your blocks cannot be enforced.",
            fontSize = 14.sp,
            color = FocusOnSurfaceVariant
        )

        Spacer(modifier = Modifier.height(28.dp))

        PermissionRow(
            title = "Accessibility Service",
            required = true,
            granted = isAccessibilityGranted,
            explanation = "Lets UltimateFocus see which app is in the foreground so it can " +
                "step in when you open something you chose to block. It never reads, stores " +
                "or sends the content of your screen.",
            actionLabel = "Open Accessibility settings",
            onAction = onGrantAccessibility,
            testTag = "onboarding_accessibility"
        )

        Spacer(modifier = Modifier.height(14.dp))

        PermissionRow(
            title = "Usage Access",
            required = false,
            granted = isUsageAccessGranted,
            explanation = "Optional. UltimateFocus already measures screen time itself, so " +
                "you can skip this. Granting it lets future versions read system usage " +
                "figures directly.",
            actionLabel = "Open Usage access settings",
            onAction = onGrantUsageAccess,
            testTag = "onboarding_usage"
        )

        Spacer(modifier = Modifier.height(28.dp))

        if (isAccessibilityGranted) {
            Text(
                text = "All set. Taking you into the app…",
                fontSize = 13.sp,
                color = FocusPrimary,
                fontWeight = FontWeight.SemiBold
            )
        } else {
            Text(
                text = "Find UltimateFocus under Downloaded apps, then switch it on.",
                fontSize = 12.sp,
                color = FocusOutline
            )
            Spacer(modifier = Modifier.height(10.dp))
            TextButton(
                onClick = onContinueAnyway,
                modifier = Modifier.testTag("onboarding_skip")
            ) {
                Text(
                    text = "Look around first",
                    fontSize = 13.sp,
                    color = FocusOnSurfaceVariant
                )
            }
            Text(
                text = "Blocking stays off until the permission is granted.",
                fontSize = 11.sp,
                color = FocusOutline
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun PermissionRow(
    title: String,
    required: Boolean,
    granted: Boolean,
    explanation: String,
    actionLabel: String,
    onAction: () -> Unit,
    testTag: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(FocusSurfaceContainer)
            .clickable(enabled = !granted) { onAction() }
            .padding(16.dp)
            .testTag(testTag),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (granted) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                contentDescription = null,
                tint = when {
                    granted -> FocusPrimary
                    required -> FocusError
                    else -> FocusOutline
                },
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = FocusOnSurface
                )
                Text(
                    text = if (required) "Required" else "Optional",
                    fontSize = 11.sp,
                    color = if (required) FocusError else FocusOutline
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        (if (granted) FocusPrimary else FocusOnSurfaceVariant).copy(alpha = 0.15f)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = if (granted) "Granted" else "Not granted",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (granted) FocusPrimary else FocusOnSurfaceVariant
                )
            }
        }

        Text(
            text = explanation,
            fontSize = 12.sp,
            color = FocusOnSurfaceVariant
        )

        if (!granted) {
            Button(
                onClick = onAction,
                colors = ButtonDefaults.buttonColors(containerColor = FocusPrimary),
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(testTag + "_button")
            ) {
                Text(
                    text = actionLabel,
                    color = FocusOnPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }
    }
}
