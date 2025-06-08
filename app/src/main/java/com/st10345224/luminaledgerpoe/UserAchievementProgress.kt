package com.st10345224.luminaledgerpoe

import com.google.firebase.Timestamp

data class UserAchievementProgress(
    val achievementId: String = "", // Matches the 'id' from AchievementDefinition
    val progress: Int = 0,          // Current progress towards the achievement
    val unlockedDate: Timestamp? = null // Null if not unlocked, Timestamp if unlocked

)
