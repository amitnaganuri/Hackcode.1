package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.data.model.BlockMode
import com.example.data.model.BlockedAppRule
import com.example.data.model.InterventionConfig
import com.example.data.repository.FocusGuardRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class FocusGuardViewModel(
    private val repository: FocusGuardRepository = FocusGuardRepository()
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

    fun addNewRule(
        appName: String,
        mode: BlockMode,
        filterLabel: String,
        schedule: String,
        category: String = "Social"
    ) {
        val newRule = BlockedAppRule(
            id = "rule_${System.currentTimeMillis()}",
            appName = appName,
            packageName = "com.${appName.lowercase().replace(" ", "")}.android",
            blockMode = mode,
            filterLabel = filterLabel,
            scheduleText = schedule,
            isEnabled = true,
            category = category
        )
        repository.addRule(newRule)
        _isAddBlockSheetOpen.value = false
    }

    fun updateInterventionConfig(config: InterventionConfig) {
        repository.updateInterventionConfig(config)
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return FocusGuardViewModel() as T
            }
        }
    }
}
