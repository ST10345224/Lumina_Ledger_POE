package com.st10345224.luminaledgerpoe

import android.util.Log // Import Android's standard Log class
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch


data class UserAchievement(
    val definition: AchievementDefinition,
    val currentProgress: Int,
    val isUnlocked: Boolean,
    val unlockedDate: Timestamp?
)

class AchievementsViewModel(
    private val repository: AchievementsRepository
) : ViewModel() {

    private val TAG = "AchievementsVM" // Tag for logging

    private val _userAchievements = MutableStateFlow<List<UserAchievement>>(emptyList())
    val userAchievements: StateFlow<List<UserAchievement>> = _userAchievements

    init {
        loadAndInitializeAchievements()
    }

    private fun loadAndInitializeAchievements() {
        viewModelScope.launch {
            repository.getAllUserAchievementProgress().collect { progressList ->
                if (progressList.isEmpty() && AppAchievements.ALL_ACHIEVEMENTS.isNotEmpty()) {
                    Log.d(TAG, "No achievement progress found for user. Initializing all achievements.")
                    initializeAllAchievements()
                }

                val combinedList = AppAchievements.ALL_ACHIEVEMENTS.map { definition ->
                    val userProgress = progressList.find { it.achievementId == definition.id }

                    UserAchievement(
                        definition = definition,
                        currentProgress = userProgress?.progress ?: 0,
                        isUnlocked = userProgress?.unlockedDate != null,
                        unlockedDate = userProgress?.unlockedDate
                    )
                }
                _userAchievements.value = combinedList
                incrementAchievementProgress("first_expense")
            }
        }
    }

    private fun initializeAllAchievements() {
        viewModelScope.launch {
            AppAchievements.ALL_ACHIEVEMENTS.forEach { definition ->
                val initialProgress = UserAchievementProgress(
                    achievementId = definition.id,
                    progress = 0,
                    unlockedDate = null
                )
                repository.updateUserAchievementProgress(initialProgress)
                Log.d(TAG, "Initialized achievement: ${definition.name}")
            }
        }
    }

    /**
     * This function now handles updating progress for different types of achievements.
     * For "first_expense", it counts actual expenses. For others, it might just increment.
     * 'incrementBy' is now optional and mainly for non-count based achievements.
     */
    fun incrementAchievementProgress(achievementId: String, incrementBy: Int = 1) { // incrementBy might not be used for all achievements
        viewModelScope.launch {
            val definition = AppAchievements.getDefinitionById(achievementId)

            if (definition == null) {
                Log.w(TAG, "Attempted to update progress for unknown achievement: $achievementId")
                return@launch
            }

            // Get current achievement progress from Firestore
            val currentProgress = repository.getUserAchievementProgress(achievementId).first()
            var newProgressValue = currentProgress?.progress ?: 0
            var newUnlockedDate: Timestamp? = currentProgress?.unlockedDate

            // --- Logic for "First Spender" (counting actual expenses) ---
            if (achievementId == "first_expense") {
                val expenseCount = repository.countUserExpenses() // Get the actual count from Firestore
                Log.d(TAG, "Checking 'first_expense' for user. Expense count: $expenseCount")

                // Update progress based on the count, but only if it's less than the target
                if (expenseCount > newProgressValue) { // If actual count is higher than recorded progress
                    newProgressValue = expenseCount.toInt() // Update progress to the actual count
                }

                // Check for unlock if not already unlocked and target is met
                if (newUnlockedDate == null && newProgressValue >= definition.target) {
                    newUnlockedDate = Timestamp.now()
                    Log.d(TAG, "Achievement unlocked: ${definition.name}")

                }
            }
            // --- End Logic for "First Spender" ---


            // If the progress value or unlocked date has changed, update Firestore
            if (newProgressValue != (currentProgress?.progress ?: 0) || newUnlockedDate != currentProgress?.unlockedDate) {
                val updatedProgress = UserAchievementProgress(
                    achievementId = achievementId,
                    progress = newProgressValue,
                    unlockedDate = newUnlockedDate
                )
                repository.updateUserAchievementProgress(updatedProgress)
                Log.d(TAG, "Achievement '$achievementId' updated in DB.")
            } else {
                Log.d(TAG, "Achievement '$achievementId' progress unchanged. No DB update needed.")
            }
        }
    }
}

// Simple ViewModel factory
object AchievementsViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        if (modelClass.isAssignableFrom(AchievementsViewModel::class.java)) {
            // Get instances of Firestore and FirebaseAuth
            // These will be used to create the AchievementsRepository
            val firestore = FirebaseFirestore.getInstance()
            val auth = FirebaseAuth.getInstance()

            // Create the AchievementsRepository instance
            val repository = AchievementsRepository(firestore, auth)

            // Create and return the AchievementsViewModel, injecting the repository
            @Suppress("UNCHECKED_CAST") // Suppress the unchecked cast warning
            return AchievementsViewModel(repository) as T
        }
        // If the requested ViewModel class is not AchievementsViewModel, throw an exception
        throw IllegalArgumentException("Unknown ViewModel class requested by factory")
    }
}