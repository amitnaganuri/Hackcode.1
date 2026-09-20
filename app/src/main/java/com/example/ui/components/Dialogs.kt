package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.ui.theme.FocusError
import com.example.ui.theme.FocusOnPrimary
import com.example.ui.theme.FocusOnSurface
import com.example.ui.theme.FocusOnSurfaceVariant
import com.example.ui.theme.FocusPrimary
import com.example.ui.theme.FocusSurfaceContainerHigh
import com.example.ui.theme.FocusSurfaceContainerHighest

@Composable
fun EditGoalDialog(
    initialGoal: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var goalText by remember { mutableStateOf(initialGoal) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = FocusSurfaceContainerHigh,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Flag,
                    contentDescription = null,
                    tint = FocusPrimary
                )
                Text(
                    text = "Edit Today's Goal",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = FocusOnSurface
                )
            }
        },
        text = {
            Column {
                Text(
                    text = "Your daily anchor reminds you why your focus matters when distractions tempt you.",
                    fontSize = 13.sp,
                    color = FocusOnSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = goalText,
                    onValueChange = { goalText = it },
                    label = { Text("Daily Focus Goal") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = FocusOnSurface,
                        unfocusedTextColor = FocusOnSurface,
                        focusedBorderColor = FocusPrimary,
                        unfocusedBorderColor = FocusSurfaceContainerHighest,
                        focusedLabelColor = FocusPrimary,
                        unfocusedLabelColor = FocusOnSurfaceVariant
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("goal_input_field")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(goalText) },
                colors = ButtonDefaults.buttonColors(containerColor = FocusPrimary),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.testTag("save_goal_button")
            ) {
                Text("Save Anchor", color = FocusOnPrimary, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = FocusOnSurfaceVariant)
            }
        }
    )
}

@Composable
fun StrictSecurityDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = FocusSurfaceContainerHigh,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = FocusPrimary
                )
                Text(
                    text = "Strict Mode & Tamper Guard",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = FocusOnSurface
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Strict Mode prevents impulsive rule modification during scheduled deep focus windows.",
                    fontSize = 13.sp,
                    color = FocusOnSurfaceVariant
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(FocusSurfaceContainerHighest)
                        .padding(10.dp)
                ) {
                    Text(
                        text = "While a focus session is running:\n• Blocks cannot be switched off\n• Rules cannot be edited or deleted\n• New rules cannot be added\n\nEverything unlocks as soon as the session ends.",
                        fontSize = 12.sp,
                        color = FocusOnSurface
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = FocusPrimary),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text("Understood", color = FocusOnPrimary, fontWeight = FontWeight.Bold)
            }
        }
    )
}

/**
 * Confirmation for a destructive rule delete. Styled to match [EditGoalDialog] so the
 * existing visual language is preserved.
 */
@Composable
fun DeleteRuleDialog(
    appName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = FocusSurfaceContainerHigh,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = null,
                    tint = FocusError
                )
                Text(
                    text = "Remove Block?",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = FocusOnSurface
                )
            }
        },
        text = {
            Text(
                text = "$appName will no longer be shielded. You can add the block back at any time.",
                fontSize = 13.sp,
                color = FocusOnSurfaceVariant
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = FocusError),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.testTag("confirm_delete_rule_button")
            ) {
                Text("Remove Block", color = FocusSurfaceContainerHigh, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Keep Block", color = FocusOnSurfaceVariant)
            }
        }
    )
}

/** Lets the user set the name shown on the Settings profile card. */
@Composable
fun EditProfileNameDialog(
    initialName: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var nameText by remember { mutableStateOf(initialName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = FocusSurfaceContainerHigh,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = FocusPrimary
                )
                Text(
                    text = "Your Name",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = FocusOnSurface
                )
            }
        },
        text = {
            OutlinedTextField(
                value = nameText,
                onValueChange = { nameText = it },
                singleLine = true,
                label = { Text("Display name") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = FocusOnSurface,
                    unfocusedTextColor = FocusOnSurface,
                    focusedBorderColor = FocusPrimary,
                    unfocusedBorderColor = FocusSurfaceContainerHighest,
                    focusedLabelColor = FocusPrimary,
                    unfocusedLabelColor = FocusOnSurfaceVariant
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("profile_name_field")
            )
        },
        confirmButton = {
            Button(
                onClick = { onSave(nameText) },
                colors = ButtonDefaults.buttonColors(containerColor = FocusPrimary),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.testTag("save_profile_name_button")
            ) {
                Text("Save", color = FocusOnPrimary, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = FocusOnSurfaceVariant)
            }
        }
    )
}
