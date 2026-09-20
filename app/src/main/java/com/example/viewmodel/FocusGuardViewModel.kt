package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import com.example.FocusGuardApplication
import com.example.data.model.AppCatalog
import com.example.data.model.BlockMode
import com.example.data.model.BlockedAppRule
import com.example.data.model.InterventionConfig
import com.example.data.repository.FocusGuardRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FocusGuardViewModel(
    private val repository: FocusGuardRepository
) : ViewModel() {

    val rules = repository.rules
    val shortsAndReelsMasterShield = repository.shortsAndReelsMasterShield
    val activeSession = repository.activeSession
    val interventionConfig = repository.interventionConfig
    val dailyStats = repository.dailyStats
    val weeklyBalance = repository.weeklyBalance
    val appInterventions = repository.appInterventions

    // UI state
    private val _selectedDurationMinutes = MutableStateFlow(25)
    val selectedDurationMinutes: StateFlow<Int> = _selectedDurationMinutes.asStateFlow()

    private val _selectedTimeframe = MutableStateFlow("This Week")
    val selectedTimeframe: StateFlow<String> = _selectedTimeframe.asStateFlow()

    // Dialogs & Sheets
    private val _isAddBlockSheetOpen = MutableStateFlow(false)
    val isAddBlockSheetOpen: StateFlow<Boolean> = _isAddBlockSheetOpen.asStateFlow()

    private val _isEditGoalDialogOpen = MutableStateFlow(false)
    val isEditGoalDialogOpen: StateFlow<Boolean> = _isEditGoalDialogOpen.asStateFlow()

    private val _isSecurityDialogOpen = MutableStateFlow(false)
    val isSecurityDialogOpen: StateFlow<Boolean> = _isSecurityDialogOpen.asStateFlow()

    private val _isPreferencesDialogOpen = MutableStateFlow(false)
    val isPreferencesDialogOpen: StateFlow<Boolean> = _isPreferencesDialogOpen.asStateFlow()

    /**
     * The rule the Add/Edit sheet is currently editing. Null means the sheet is in
     * "create" mode. Keeping this in the ViewModel (rather than in the sheet) is what
     * makes "Edit Rule" open a prefilled sheet instead of a blank one.
     */
    private val _ruleBeingEdited = MutableStateFlow<BlockedAppRule?>(null)
    val ruleBeingEdited: StateFlow<BlockedAppRule?> = _ruleBeingEdited.asStateFlow()

    /** Rule pending delete confirmation, so a destructive tap is never one-click. */
    private val _rulePendingDelete = MutableStateFlow<BlockedAppRule?>(null)
    val rulePendingDelete: StateFlow<BlockedAppRule?> = _rulePendingDelete.asStateFlow()

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
        repository.toggleRule(ruleId)
    }

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
        _ruleBeingEdited.value = null
        _isAddBlockSheetOpen.value = true
    }

    /** Opens the sheet prefilled with an existing rule. */
    fun startEditingRule(ruleId: String) {
        _ruleBeingEdited.value = repository.ruleById(ruleId)
        _isAddBlockSheetOpen.value = true
    }

    /**
     * Single save path for the Add/Edit sheet. Creates a new rule or updates the one
     * being edited, preserving its id and accumulated counters.
     */
    fun saveRule(
        appName: String,
        mode: BlockMode,
        filterLabel: String,
        schedule: String
    ) {
        val existing = _ruleBeingEdited.value
        // Package names must be real or the blocking engine will never match the rule.
        val packageName = AppCatalog.packageNameFor(appName)
            ?: existing?.packageName
            ?: return
        val category = AppCatalog.categoryFor(appName)
        // An allowance rule with a zero budget renders a NaN progress bar, so give it
        // the budget its filter label advertises.
        val allowanceMinutes = if (mode == BlockMode.DAILY_ALLOWANCE) {
            existing?.totalAllowedMinutes?.takeIf { it > 0 } ?: DEFAULT_ALLOWANCE_MINUTES
        } else 0

        if (existing == null) {
            repository.addRule(
                BlockedAppRule(
                    id = "rule_" + System.currentTimeMillis(),
                    appName = appName,
                    packageName = packageName,
                    blockMode = mode,
                    filterLabel = filterLabel,
                    scheduleText = schedule,
                    isEnabled = true,
                    totalAllowedMinutes = allowanceMinutes,
                    category = category
                )
            )
        } else {
            repository.updateRule(
                existing.copy(
                    appName = appName,
                    packageName = packageName,
                    blockMode = mode,
                    filterLabel = filterLabel,
                    scheduleText = schedule,
                    totalAllowedMinutes = allowanceMinutes,
                    category = category
                )
            )
        }
        _isAddBlockSheetOpen.value = false
        _ruleBeingEdited.value = null
    }

    fun requestDeleteRule(ruleId: String) {
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

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as FocusGuardApplication
                FocusGuardViewModel(app.container.repository)
            }
        }
    }
}
