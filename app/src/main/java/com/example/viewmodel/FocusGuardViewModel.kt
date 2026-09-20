package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import com.example.FocusGuardApplication
import com.example.data.model.AppCatalog
import com.example.data.model.BlockMode
import com.example.data.model.BlockedAppRule
import com.example.data.analytics.FocusAnalytics
import com.example.data.model.InsightsStats
import com.example.data.model.InterventionConfig
import com.example.data.model.Timeframe
import com.example.data.repository.FocusGuardRepository
import com.example.data.repository.InstalledApp
import com.example.data.repository.InstalledAppsProvider
import com.example.service.ScreenLearner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FocusGuardViewModel(
    private val repository: FocusGuardRepository,
    private val installedAppsProvider: InstalledAppsProvider,
    private val screenLearner: ScreenLearner
) : ViewModel() {

    val rules = repository.rules
    val shortsAndReelsMasterShield = repository.shortsAndReelsMasterShield
    val activeSession = repository.activeSession
    val interventionConfig = repository.interventionConfig
    val dailyStats = repository.dailyStats
    val weeklyBalance = repository.weeklyBalance
    val appInterventions = repository.appInterventions
    val isStrictModeEnabled = repository.isStrictModeEnabled
    val profileName = repository.profileName

    // UI state
    private val _selectedDurationMinutes = MutableStateFlow(25)
    val selectedDurationMinutes: StateFlow<Int> = _selectedDurationMinutes.asStateFlow()

    private val _selectedTimeframe = MutableStateFlow(Timeframe.THIS_WEEK.label)
    val selectedTimeframe: StateFlow<String> = _selectedTimeframe.asStateFlow()

    /**
     * Everything Insights shows, recomputed whenever the logs or the selected
     * timeframe change. Combining here keeps the screen a pure reader.
     */
    val insightsStats: StateFlow<InsightsStats> = combine(
        repository.distractionAttempts,
        repository.sessionLog,
        repository.usageHistory,
        repository.activeSession,
        _selectedTimeframe
    ) { attempts, sessions, usage, active, timeframeLabel ->
        val runningSeconds = active
            ?.let { (it.totalDurationSeconds - it.remainingSeconds).coerceAtLeast(0) }
            ?: 0
        FocusAnalytics.insights(
            timeframe = Timeframe.fromLabel(timeframeLabel),
            attempts = attempts,
            sessions = sessions,
            usageHistory = usage,
            runningSessionSeconds = runningSeconds,
            nowMillis = System.currentTimeMillis()
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InsightsStats())

    // Dialogs & Sheets
    private val _isAddBlockSheetOpen = MutableStateFlow(false)
    val isAddBlockSheetOpen: StateFlow<Boolean> = _isAddBlockSheetOpen.asStateFlow()

    private val _isEditGoalDialogOpen = MutableStateFlow(false)
    val isEditGoalDialogOpen: StateFlow<Boolean> = _isEditGoalDialogOpen.asStateFlow()

    private val _isSecurityDialogOpen = MutableStateFlow(false)
    val isSecurityDialogOpen: StateFlow<Boolean> = _isSecurityDialogOpen.asStateFlow()

    private val _isProfileNameDialogOpen = MutableStateFlow(false)
    val isProfileNameDialogOpen: StateFlow<Boolean> = _isProfileNameDialogOpen.asStateFlow()

    private val _isPreferencesDialogOpen = MutableStateFlow(false)
    val isPreferencesDialogOpen: StateFlow<Boolean> = _isPreferencesDialogOpen.asStateFlow()

    /**
     * The rule the Add/Edit sheet is currently editing. Null means the sheet is in
     * "create" mode. Keeping this in the ViewModel (rather than in the sheet) is what
     * makes "Edit Rule" open a prefilled sheet instead of a blank one.
     */
    private val _ruleBeingEdited = MutableStateFlow<BlockedAppRule?>(null)
    val ruleBeingEdited: StateFlow<BlockedAppRule?> = _ruleBeingEdited.asStateFlow()

    /** Every launchable app on the device, for the block picker. */
    private val _installedApps = MutableStateFlow<List<InstalledApp>>(emptyList())
    val installedApps: StateFlow<List<InstalledApp>> = _installedApps.asStateFlow()

    private val _isLoadingInstalledApps = MutableStateFlow(false)
    val isLoadingInstalledApps: StateFlow<Boolean> = _isLoadingInstalledApps.asStateFlow()

    /** Rule pending delete confirmation, so a destructive tap is never one-click. */
    private val _rulePendingDelete = MutableStateFlow<BlockedAppRule?>(null)
    val rulePendingDelete: StateFlow<BlockedAppRule?> = _rulePendingDelete.asStateFlow()

    init {
        loadInstalledApps()
    }

    private fun loadInstalledApps() {
        if (_installedApps.value.isNotEmpty()) return
        viewModelScope.launch {
            _isLoadingInstalledApps.value = true
            _installedApps.value = installedAppsProvider.loadInstalledApps()
            _isLoadingInstalledApps.value = false
        }
    }

    fun selectDuration(minutes: Int) {
        _selectedDurationMinutes.value = minutes
    }

    fun selectTimeframe(timeframe: String) {
        _selectedTimeframe.value = timeframe
    }

    fun startDeepFocus() {
        repository.startDeepFocus(_selectedDurationMinutes.value)
    }

    fun endSession() {
        repository.endActiveSession()
    }

    fun togglePauseSession() {
        repository.togglePauseSession()
    }

    fun toggleRule(ruleId: String) {
        if (repository.areRuleChangesLocked()) return
        repository.toggleRule(ruleId)
    }

    fun setStrictMode(enabled: Boolean) {
        repository.setStrictMode(enabled)
    }

    fun setProfileName(name: String) {
        repository.setProfileName(name)
        _isProfileNameDialogOpen.value = false
    }

    fun setProfileNameDialogOpen(open: Boolean) {
        _isProfileNameDialogOpen.value = open
    }

    /** Strict Mode blocks rule edits while a session runs; surfaced so the UI can say so. */
    fun areRuleChangesLocked(): Boolean = repository.areRuleChangesLocked()

    fun toggleShortsAndReelsMasterShield() {
        repository.toggleShortsAndReelsMasterShield()
    }

    fun updateGoal(newGoal: String) {
        repository.updateGoal(newGoal)
        _isEditGoalDialogOpen.value = false
    }

    fun setAddBlockSheetOpen(open: Boolean) {
        _isAddBlockSheetOpen.value = open
        if (!open) _ruleBeingEdited.value = null
    }

    fun setEditGoalDialogOpen(open: Boolean) {
        _isEditGoalDialogOpen.value = open
    }

    fun setSecurityDialogOpen(open: Boolean) {
        _isSecurityDialogOpen.value = open
    }

    fun setPreferencesDialogOpen(open: Boolean) {
        _isPreferencesDialogOpen.value = open
    }

    // ----------------------------------------------------------- rule editing

    /** Opens the sheet in "create" mode. */
    fun startCreatingRule() {
        if (repository.areRuleChangesLocked()) return
        _ruleBeingEdited.value = null
        _isAddBlockSheetOpen.value = true
    }

    /** Opens the sheet prefilled with an existing rule. */
    fun startEditingRule(ruleId: String) {
        if (repository.areRuleChangesLocked()) return
        _ruleBeingEdited.value = repository.ruleById(ruleId)
        _isAddBlockSheetOpen.value = true
    }

    /**
     * Single save path for the Add/Edit sheet. Creates a new rule or updates the one
     * being edited, preserving its id and accumulated counters.
     */
    fun saveRule(
        appName: String,
        packageName: String,
        mode: BlockMode,
        filterLabel: String,
        schedule: String,
        allowanceMinutes: Int = DEFAULT_ALLOWANCE_MINUTES,
        blockedContentIds: List<String> = emptyList(),
        contentAllowanceMinutes: Int = 0
    ): String? {
        val existing = _ruleBeingEdited.value
        // The picker supplies the real package name straight from PackageManager; the
        // catalogue is only consulted for a nicer category label on apps we know.
        val resolvedPackage = packageName.ifBlank { existing?.packageName.orEmpty() }
        if (resolvedPackage.isBlank()) return null
        val category = AppCatalog.categoryFor(appName)
        // Only an allowance rule carries a budget; a zero budget would also render a
        // NaN progress bar, so it is clamped to something sane.
        val resolvedAllowance = if (mode == BlockMode.DAILY_ALLOWANCE) {
            allowanceMinutes.takeIf { it > 0 } ?: DEFAULT_ALLOWANCE_MINUTES
        } else 0

        val ruleId = existing?.id ?: ("rule_" + System.currentTimeMillis())

        if (existing == null) {
            repository.addRule(
                BlockedAppRule(
                    id = ruleId,
                    appName = appName,
                    packageName = resolvedPackage,
                    blockMode = mode,
                    filterLabel = filterLabel,
                    scheduleText = schedule,
                    isEnabled = true,
                    totalAllowedMinutes = resolvedAllowance,
                    category = category,
                    blockedContentIds = blockedContentIds,
                    contentAllowanceMinutes = contentAllowanceMinutes
                )
            )
        } else {
            repository.updateRule(
                existing.copy(
                    appName = appName,
                    packageName = resolvedPackage,
                    blockMode = mode,
                    filterLabel = filterLabel,
                    scheduleText = schedule,
                    totalAllowedMinutes = resolvedAllowance,
                    category = category,
                    blockedContentIds = blockedContentIds,
                    contentAllowanceMinutes = contentAllowanceMinutes
                )
            )
        }
        _isAddBlockSheetOpen.value = false
        _ruleBeingEdited.value = null
        return ruleId
    }

    /**
     * Saves the rule, then starts a capture window so the user can open the app and
     * navigate to the screen they want blocked.
     *
     * The rule must be saved first because the captured signature is written back to it
     * by id from the accessibility service once the window closes.
     */
    fun teachScreen(
        appName: String,
        packageName: String,
        mode: BlockMode,
        filterLabel: String,
        schedule: String,
        blockedContentIds: List<String>,
        contentAllowanceMinutes: Int
    ): Long {
        val ruleId = saveRule(
            appName = appName,
            packageName = packageName,
            mode = mode,
            filterLabel = filterLabel,
            schedule = schedule,
            blockedContentIds = blockedContentIds,
            contentAllowanceMinutes = contentAllowanceMinutes
        ) ?: return 0L

        screenLearner.start(ruleId, packageName, TEACH_WINDOW_MILLIS)
        return TEACH_WINDOW_MILLIS
    }

    fun requestDeleteRule(ruleId: String) {
        if (repository.areRuleChangesLocked()) return
        _rulePendingDelete.value = repository.ruleById(ruleId)
    }

    fun cancelDeleteRule() {
        _rulePendingDelete.value = null
    }

    fun confirmDeleteRule() {
        _rulePendingDelete.value?.let { repository.deleteRule(it.id) }
        _rulePendingDelete.value = null
    }

    fun updateInterventionConfig(config: InterventionConfig) {
        repository.updateInterventionConfig(config)
    }

    companion object {
        private const val DEFAULT_ALLOWANCE_MINUTES = 30

        /** How long the user has to reach the screen they want captured. */
        const val TEACH_WINDOW_MILLIS = 20_000L

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as FocusGuardApplication
                FocusGuardViewModel(
                    repository = app.container.repository,
                    installedAppsProvider = app.container.installedAppsProvider,
                    screenLearner = app.container.screenLearner
                )
            }
        }
    }
}
