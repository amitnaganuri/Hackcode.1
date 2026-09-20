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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.Image
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.data.model.ContentCatalog
import com.example.data.repository.InstalledApp
import com.example.ui.theme.FocusError
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
    onSaveBlock: (
        appName: String,
        packageName: String,
        mode: BlockMode,
        filterLabel: String,
        schedule: String,
        allowanceMinutes: Int,
        blockedContentIds: List<String>,
        contentAllowanceMinutes: Int
    ) -> Unit,
    installedApps: List<InstalledApp> = emptyList(),
    isLoadingApps: Boolean = false,
    /** Packages that already have a rule, so the same app cannot be added twice. */
    alreadyBlockedPackages: Set<String> = emptySet(),
    /** Saves the rule and starts a capture window for the screen the user opens next. */
    onTeachScreen: (
        appName: String,
        packageName: String,
        mode: BlockMode,
        filterLabel: String,
        schedule: String,
        blockedContentIds: List<String>,
        contentAllowanceMinutes: Int
    ) -> Unit = { _, _, _, _, _, _, _ -> },
    existingRule: BlockedAppRule? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    // Keyed on the rule so reopening the sheet for a different rule re-seeds the fields.
    var selectedMode by remember(existingRule) { mutableStateOf(existingRule?.blockMode) }
    // Keyed on the loaded list too, so an edited rule snaps to its real entry (with
    // icon) once PackageManager has finished enumerating.
    var selectedApp by remember(existingRule, installedApps) {
        mutableStateOf(
            existingRule?.let { rule ->
                installedApps.firstOrNull { it.packageName == rule.packageName }
                    ?: InstalledApp(rule.packageName, rule.appName)
            }
        )
    }
    var appSearchQuery by remember(existingRule) { mutableStateOf("") }
    // Daily-allowance budget in minutes. Previously there was no way to choose this,
    // so every limit rule was saved with a hard-coded value.
    var allowanceMinutes by remember(existingRule) {
        mutableIntStateOf(existingRule?.totalAllowedMinutes?.takeIf { it > 0 } ?: 30)
    }
    // A rule being edited whose budget is not one of the presets must open on the
    // custom fields, otherwise editing it would silently round the value to a preset.
    var isCustomAllowance by remember(existingRule) {
        mutableStateOf(
            existingRule?.totalAllowedMinutes?.let {
                it > 0 && it !in ALLOWANCE_OPTIONS_MINUTES
            } ?: false
        )
    }
    var customHours by remember(existingRule) {
        mutableStateOf(((existingRule?.totalAllowedMinutes ?: 0) / 60).toString())
    }
    var customMinutes by remember(existingRule) {
        mutableStateOf(((existingRule?.totalAllowedMinutes ?: 0) % 60).toString())
    }

    // Which surfaces inside the app this content rule covers. Seeded from the rule when
    // editing, otherwise the app's short-form feeds.
    var selectedContentIds by remember(existingRule, selectedApp) {
        mutableStateOf(
            existingRule?.blockedContentIds?.takeIf { it.isNotEmpty() }?.toSet()
                ?: selectedApp?.packageName
                    ?.let { ContentCatalog.defaultTargetIdsFor(it).toSet() }
                ?: emptySet()
        )
    }
    // Zero means block on sight, which is how content blocking behaved before budgets.
    var contentBudgetMinutes by remember(existingRule) {
        mutableIntStateOf(existingRule?.contentAllowanceMinutes ?: 0)
    }
    var isCustomContentBudget by remember(existingRule) {
        mutableStateOf(
            existingRule?.contentAllowanceMinutes?.let {
                it > 0 && it !in ALLOWANCE_OPTIONS_MINUTES
            } ?: false
        )
    }
    var contentCustomHours by remember(existingRule) {
        mutableStateOf(((existingRule?.contentAllowanceMinutes ?: 0) / 60).toString())
    }
    var contentCustomMinutes by remember(existingRule) {
        mutableStateOf(((existingRule?.contentAllowanceMinutes ?: 0) % 60).toString())
    }

    val availableTargets = selectedApp?.packageName
        ?.let { ContentCatalog.targetsFor(it) }
        .orEmpty()

    val effectiveContentBudget = if (isCustomContentBudget) {
        ((contentCustomHours.toIntOrNull() ?: 0) * 60 +
            (contentCustomMinutes.toIntOrNull() ?: 0)).coerceAtLeast(0)
    } else {
        contentBudgetMinutes
    }

    val effectiveAllowanceMinutes = if (isCustomAllowance) {
        val hours = customHours.toIntOrNull() ?: 0
        val minutes = customMinutes.toIntOrNull() ?: 0
        (hours * 60 + minutes).coerceAtLeast(0)
    } else {
        allowanceMinutes
    }
    // There is no schedule picker in this sheet yet, so every new rule inherits this
    // default. A weekday-only default means a rule the user just created silently does
    // nothing at weekends, so a newly created block defaults to always-on; the Rule
    // Summary below shows exactly what will be saved.
    var customSchedule by remember(existingRule) {
        mutableStateOf(existingRule?.scheduleText ?: "All Day • Daily")
    }

    val isEditing = existingRule != null
    val filteredApps = remember(installedApps, appSearchQuery) {
        if (appSearchQuery.isBlank()) installedApps
        else installedApps.filter {
            it.label.contains(appSearchQuery, ignoreCase = true) ||
                it.packageName.contains(appSearchQuery, ignoreCase = true)
        }
    }

    // The app this sheet is editing must stay selectable even though it is, of course,
    // already blocked.
    fun isAlreadyBlocked(packageName: String): Boolean =
        packageName in alreadyBlockedPackages && packageName != existingRule?.packageName

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = FocusSurfaceContainerHigh,
        contentColor = FocusOnSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                // The sheet can outgrow the screen once the app list, the duration row
                // and the custom hour/minute fields are all present, and the numeric
                // keyboard pushes it further. Without scrolling and IME padding the
                // Save button becomes unreachable.
                .verticalScroll(rememberScrollState())
                .imePadding()
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
                        isEditing -> "Edit Block: " + (selectedApp?.label ?: "")
                        else -> "Configure Block: " + (selectedApp?.label ?: "")
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
                    if (isEditing) {
                        // Editing an existing rule: the app is already decided, so the
                        // picker is noise. Show what is being edited and nothing else.
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(FocusSurfaceContainer)
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                                .testTag("editing_app_header"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(9.dp))
                                    .background(FocusSurfaceContainerHighest),
                                contentAlignment = Alignment.Center
                            ) {
                                val editingIcon = selectedApp?.icon
                                if (editingIcon != null) {
                                    Image(
                                        bitmap = editingIcon,
                                        contentDescription = null,
                                        modifier = Modifier.size(26.dp)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Shield,
                                        contentDescription = null,
                                        tint = FocusPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = selectedApp?.label ?: existingRule?.appName.orEmpty(),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = FocusOnSurface,
                                    maxLines = 1
                                )
                                Text(
                                    text = selectedApp?.packageName
                                        ?: existingRule?.packageName.orEmpty(),
                                    fontSize = 11.sp,
                                    color = FocusOutline,
                                    maxLines = 1
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "Choose Application:",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = FocusOnSurfaceVariant
                        )

                        OutlinedTextField(
                            value = appSearchQuery,
                            onValueChange = { appSearchQuery = it },
                            singleLine = true,
                            placeholder = {
                                Text("Search your apps", fontSize = 13.sp, color = FocusOutline)
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = FocusOnSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = FocusOnSurface,
                                unfocusedTextColor = FocusOnSurface,
                                focusedBorderColor = FocusPrimary,
                                unfocusedBorderColor = FocusSurfaceContainerHighest
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("app_search_field")
                        )

                        when {
                            isLoadingApps && installedApps.isEmpty() -> {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 18.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(
                                        color = FocusPrimary,
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Reading your installed apps\u2026",
                                        fontSize = 13.sp,
                                        color = FocusOnSurfaceVariant
                                    )
                                }
                            }

                            filteredApps.isEmpty() -> {
                                Text(
                                    text = "No installed app matches that search.",
                                    fontSize = 13.sp,
                                    color = FocusOnSurfaceVariant,
                                    modifier = Modifier.padding(vertical = 18.dp)
                                )
                            }

                            else -> {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("installed_app_list")
                                ) {
                                    // Capped so the sheet stays manageable; search narrows it.
                                    filteredApps.take(MAX_LISTED_APPS).forEach { app ->
                                        val isChosen = selectedApp?.packageName == app.packageName
                                        val alreadyBlocked = isAlreadyBlocked(app.packageName)
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(
                                                    if (isChosen) FocusPrimary.copy(alpha = 0.18f)
                                                    else FocusSurfaceContainer
                                                )
                                                .clickable(enabled = !alreadyBlocked) { selectedApp = app }
                                                .padding(horizontal = 10.dp, vertical = 8.dp)
                                                .testTag("app_option_" + app.packageName),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(34.dp)
                                                    .clip(RoundedCornerShape(9.dp))
                                                    .background(FocusSurfaceContainerHighest),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                val appIcon = app.icon
                                                if (appIcon != null) {
                                                    Image(
                                                        bitmap = appIcon,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(26.dp)
                                                    )
                                                } else {
                                                    Icon(
                                                        imageVector = Icons.Default.Shield,
                                                        contentDescription = null,
                                                        tint = FocusPrimary,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = app.label,
                                                    fontSize = 14.sp,
                                                    fontWeight = if (isChosen) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (alreadyBlocked) FocusOutline else FocusOnSurface,
                                                    maxLines = 1
                                                )
                                                Text(
                                                    text = if (alreadyBlocked) {
                                                        "Already has a rule \u2022 edit it instead"
                                                    } else {
                                                        app.packageName
                                                    },
                                                    fontSize = 11.sp,
                                                    color = FocusOutline,
                                                    maxLines = 1
                                                )
                                            }
                                            if (isChosen) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = "Selected",
                                                    tint = FocusPrimary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }

                                    if (filteredApps.size > MAX_LISTED_APPS) {
                                        Text(
                                            text = "+" + (filteredApps.size - MAX_LISTED_APPS) +
                                                " more \u2022 use search to narrow the list",
                                            fontSize = 12.sp,
                                            color = FocusOutline,
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (selectedMode == BlockMode.CONTENT_LEVEL && selectedApp != null) {
                        if (availableTargets.isEmpty()) {
                            Text(
                                text = "UltimateFocus does not know the inner sections of " +
                                    (selectedApp?.label ?: "this app") +
                                    " yet. Use \"Block Entire Application\" or a daily limit instead.",
                                fontSize = 12.sp,
                                color = FocusError
                            )
                        } else {
                            Text(
                                text = "What to block inside " + (selectedApp?.label ?: "") + ":",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = FocusOnSurfaceVariant
                            )
                            Column(
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("content_target_list")
                            ) {
                                availableTargets.forEach { target ->
                                    val isOn = target.id in selectedContentIds
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                if (isOn) FocusPrimary.copy(alpha = 0.18f)
                                                else FocusSurfaceContainer
                                            )
                                            .clickable {
                                                selectedContentIds =
                                                    if (isOn) selectedContentIds - target.id
                                                    else selectedContentIds + target.id
                                            }
                                            .padding(horizontal = 12.dp, vertical = 10.dp)
                                            .testTag("content_target_" + target.id),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = target.label,
                                                fontSize = 14.sp,
                                                fontWeight = if (isOn) FontWeight.Bold else FontWeight.Medium,
                                                color = FocusOnSurface
                                            )
                                            Text(
                                                text = target.description,
                                                fontSize = 11.sp,
                                                color = FocusOnSurfaceVariant
                                            )
                                        }
                                        Icon(
                                            imageVector = if (isOn) {
                                                Icons.Default.CheckCircle
                                            } else {
                                                Icons.Default.RadioButtonUnchecked
                                            },
                                            contentDescription = null,
                                            tint = if (isOn) FocusPrimary else FocusOutline,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }

                            if (selectedContentIds.isEmpty() &&
                                existingRule?.learnedViewIds.isNullOrEmpty()
                            ) {
                                Text(
                                    text = "Pick at least one section to block.",
                                    fontSize = 12.sp,
                                    color = FocusError
                                )
                            }

                            Text(
                                text = "Daily limit for this content:",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = FocusOnSurfaceVariant
                            )
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("content_budget_row")
                            ) {
                                item {
                                    val isChosen = !isCustomContentBudget && contentBudgetMinutes == 0
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(
                                                if (isChosen) FocusPrimary else FocusSurfaceContainerHighest
                                            )
                                            .clickable {
                                                isCustomContentBudget = false
                                                contentBudgetMinutes = 0
                                            }
                                            .padding(horizontal = 14.dp, vertical = 8.dp)
                                            .testTag("content_budget_none")
                                    ) {
                                        Text(
                                            text = "Block on sight",
                                            fontSize = 13.sp,
                                            fontWeight = if (isChosen) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isChosen) FocusOnPrimary else FocusOnSurface
                                        )
                                    }
                                }
                                items(ALLOWANCE_OPTIONS_MINUTES) { minutes ->
                                    val isChosen = !isCustomContentBudget && contentBudgetMinutes == minutes
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(
                                                if (isChosen) FocusPrimary else FocusSurfaceContainerHighest
                                            )
                                            .clickable {
                                                isCustomContentBudget = false
                                                contentBudgetMinutes = minutes
                                            }
                                            .padding(horizontal = 14.dp, vertical = 8.dp)
                                            .testTag("content_budget_" + minutes)
                                    ) {
                                        Text(
                                            text = formatAllowance(minutes),
                                            fontSize = 13.sp,
                                            fontWeight = if (isChosen) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isChosen) FocusOnPrimary else FocusOnSurface
                                        )
                                    }
                                }
                                item {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(
                                                if (isCustomContentBudget) FocusPrimary
                                                else FocusSurfaceContainerHighest
                                            )
                                            .clickable { isCustomContentBudget = true }
                                            .padding(horizontal = 14.dp, vertical = 8.dp)
                                            .testTag("content_budget_custom")
                                    ) {
                                        Text(
                                            text = "Custom",
                                            fontSize = 13.sp,
                                            fontWeight = if (isCustomContentBudget) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isCustomContentBudget) FocusOnPrimary else FocusOnSurface
                                        )
                                    }
                                }
                            }

                            if (isCustomContentBudget) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = contentCustomHours,
                                        onValueChange = {
                                            contentCustomHours = clampDigits(it, 23)
                                        },
                                        singleLine = true,
                                        label = { Text("Hours", fontSize = 12.sp) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = FocusOnSurface,
                                            unfocusedTextColor = FocusOnSurface,
                                            focusedBorderColor = FocusPrimary,
                                            unfocusedBorderColor = FocusSurfaceContainerHighest,
                                            focusedLabelColor = FocusPrimary,
                                            unfocusedLabelColor = FocusOnSurfaceVariant
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("content_budget_hours")
                                    )
                                    OutlinedTextField(
                                        value = contentCustomMinutes,
                                        onValueChange = {
                                            contentCustomMinutes = clampDigits(it, 59)
                                        },
                                        singleLine = true,
                                        label = { Text("Minutes", fontSize = 12.sp) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = FocusOnSurface,
                                            unfocusedTextColor = FocusOnSurface,
                                            focusedBorderColor = FocusPrimary,
                                            unfocusedBorderColor = FocusSurfaceContainerHighest,
                                            focusedLabelColor = FocusPrimary,
                                            unfocusedLabelColor = FocusOnSurfaceVariant
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("content_budget_minutes")
                                    )
                                }
                            }
                        }
                    }

                    // Teaching works for any app, including ones with no catalogue entry,
                    // which is the only reliable way to block a screen whose internal ids
                    // are unknown on this device.
                    if (selectedMode == BlockMode.CONTENT_LEVEL && selectedApp != null) {
                        val learnedCount = existingRule?.learnedViewIds?.size ?: 0
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(FocusSurfaceContainer)
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = if (learnedCount > 0) {
                                    "Screen taught on this device (" + learnedCount + " signals)"
                                } else {
                                    "Not blocking the right screen?"
                                },
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = FocusOnSurface
                            )
                            Text(
                                text = "Tap below, then open " +
                                    (selectedApp?.label ?: "the app") +
                                    " and go to the exact screen you want blocked. " +
                                    "FocusGuard watches for 20 seconds and learns it.",
                                fontSize = 11.sp,
                                color = FocusOnSurfaceVariant
                            )
                            Button(
                                onClick = {
                                    val app = selectedApp ?: return@Button
                                    onTeachScreen(
                                        app.label,
                                        app.packageName,
                                        BlockMode.CONTENT_LEVEL,
                                        filterLabelForTeach(
                                            availableTargets
                                                .filter { it.id in selectedContentIds }
                                                .map { it.label },
                                            effectiveContentBudget
                                        ),
                                        customSchedule,
                                        availableTargets
                                            .filter { it.id in selectedContentIds }
                                            .map { it.id },
                                        effectiveContentBudget
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = FocusPrimary),
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("teach_screen_button")
                            ) {
                                Text(
                                    text = if (learnedCount > 0) "Teach again" else "Teach this screen",
                                    color = FocusOnPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }

                    if (selectedMode == BlockMode.DAILY_ALLOWANCE) {
                        Text(
                            text = "Daily Time Limit:",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = FocusOnSurfaceVariant
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("allowance_duration_row")
                        ) {
                            items(ALLOWANCE_OPTIONS_MINUTES) { minutes ->
                                val isChosen = !isCustomAllowance && allowanceMinutes == minutes
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(
                                            if (isChosen) FocusPrimary else FocusSurfaceContainerHighest
                                        )
                                        .clickable {
                                            isCustomAllowance = false
                                            allowanceMinutes = minutes
                                        }
                                        .padding(horizontal = 14.dp, vertical = 8.dp)
                                        .testTag("allowance_option_" + minutes)
                                ) {
                                    Text(
                                        text = formatAllowance(minutes),
                                        fontSize = 13.sp,
                                        fontWeight = if (isChosen) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isChosen) FocusOnPrimary else FocusOnSurface
                                    )
                                }
                            }
                            item {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(
                                            if (isCustomAllowance) FocusPrimary
                                            else FocusSurfaceContainerHighest
                                        )
                                        .clickable { isCustomAllowance = true }
                                        .padding(horizontal = 14.dp, vertical = 8.dp)
                                        .testTag("allowance_option_custom")
                                ) {
                                    Text(
                                        text = "Custom",
                                        fontSize = 13.sp,
                                        fontWeight = if (isCustomAllowance) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isCustomAllowance) FocusOnPrimary else FocusOnSurface
                                    )
                                }
                            }
                        }

                        if (isCustomAllowance) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = customHours,
                                    onValueChange = { input ->
                                        // Digits only, capped so a stray keypress cannot
                                        // produce an absurd budget.
                                        customHours = input.filter { it.isDigit() }
                                            .take(2)
                                            .let { digits ->
                                                val value = digits.toIntOrNull()
                                                when {
                                                    digits.isEmpty() -> ""
                                                    value == null -> ""
                                                    value > 23 -> "23"
                                                    else -> digits
                                                }
                                            }
                                    },
                                    singleLine = true,
                                    label = { Text("Hours", fontSize = 12.sp) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = FocusOnSurface,
                                        unfocusedTextColor = FocusOnSurface,
                                        focusedBorderColor = FocusPrimary,
                                        unfocusedBorderColor = FocusSurfaceContainerHighest,
                                        focusedLabelColor = FocusPrimary,
                                        unfocusedLabelColor = FocusOnSurfaceVariant
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("allowance_custom_hours")
                                )
                                OutlinedTextField(
                                    value = customMinutes,
                                    onValueChange = { input ->
                                        customMinutes = input.filter { it.isDigit() }
                                            .take(2)
                                            .let { digits ->
                                                val value = digits.toIntOrNull()
                                                when {
                                                    digits.isEmpty() -> ""
                                                    value == null -> ""
                                                    value > 59 -> "59"
                                                    else -> digits
                                                }
                                            }
                                    },
                                    singleLine = true,
                                    label = { Text("Minutes", fontSize = 12.sp) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = FocusOnSurface,
                                        unfocusedTextColor = FocusOnSurface,
                                        focusedBorderColor = FocusPrimary,
                                        unfocusedBorderColor = FocusSurfaceContainerHighest,
                                        focusedLabelColor = FocusPrimary,
                                        unfocusedLabelColor = FocusOnSurfaceVariant
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("allowance_custom_minutes")
                                )
                            }
                            if (effectiveAllowanceMinutes <= 0) {
                                Text(
                                    text = "Enter a limit of at least 1 minute.",
                                    fontSize = 12.sp,
                                    color = FocusError
                                )
                            }
                        }
                    }

                    val filterLabel = when (selectedMode) {
                        BlockMode.CONTENT_LEVEL -> buildContentLabel(
                            labels = availableTargets
                                .filter { it.id in selectedContentIds }
                                .map { it.label },
                            budgetMinutes = effectiveContentBudget
                        )
                        BlockMode.HARD_BLOCK -> "Entire App Blocked"
                        BlockMode.DAILY_ALLOWANCE ->
                            formatAllowance(effectiveAllowanceMinutes) + " / day allowance"
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
                                text = (selectedApp?.label ?: "Select an app") + " • " + filterLabel,
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
                                val app = selectedApp ?: return@Button
                                val needsBudget = selectedMode == BlockMode.DAILY_ALLOWANCE
                                if (needsBudget && effectiveAllowanceMinutes <= 0) return@Button
                                val hasLearned = !existingRule?.learnedViewIds.isNullOrEmpty()
                                if (selectedMode == BlockMode.CONTENT_LEVEL &&
                                    selectedContentIds.isEmpty() && !hasLearned
                                ) {
                                    return@Button
                                }
                                onSaveBlock(
                                    app.label,
                                    app.packageName,
                                    selectedMode ?: BlockMode.HARD_BLOCK,
                                    filterLabel,
                                    customSchedule,
                                    effectiveAllowanceMinutes,
                                    availableTargets
                                        .filter { it.id in selectedContentIds }
                                        .map { it.id },
                                    effectiveContentBudget
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

/** How many apps to render before asking the user to search. */
private const val MAX_LISTED_APPS = 40

/** Budgets offered for a daily time limit. */
private val ALLOWANCE_OPTIONS_MINUTES = listOf(5, 10, 15, 30, 45, 60, 90, 120)

private fun formatAllowance(minutes: Int): String = when {
    minutes < 60 -> minutes.toString() + " min"
    minutes % 60 == 0 -> (minutes / 60).toString() + " hr"
    else -> (minutes / 60).toString() + "h " + (minutes % 60).toString() + "m"
}

/** Keeps a numeric field to digits only and within [max]. */
private fun clampDigits(input: String, max: Int): String {
    val digits = input.filter { it.isDigit() }.take(2)
    val value = digits.toIntOrNull() ?: return ""
    return if (value > max) max.toString() else digits
}

/** Summary text for a content rule, e.g. "Shorts, Stories - 30 min/day". */
private fun buildContentLabel(labels: List<String>, budgetMinutes: Int): String {
    val what = if (labels.isEmpty()) "Shorts & Reels" else labels.joinToString(", ")
    return if (budgetMinutes > 0) {
        what + " \u2022 " + formatAllowance(budgetMinutes) + "/day"
    } else {
        what
    }
}

/** Label used when a rule is saved as part of teaching a screen. */
private fun filterLabelForTeach(labels: List<String>, budgetMinutes: Int): String =
    buildContentLabel(labels, budgetMinutes)
