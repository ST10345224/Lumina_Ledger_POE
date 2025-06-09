package com.st10345224.luminaledgerpoe

import androidx.annotation.DrawableRes // Import for resource IDs

// This data class defines what an achievement IS
data class AchievementDefinition(
    val id: String,          // Unique ID for the achievement
    val name: String,        // Display name
    val description: String, // How to achieve it
    @DrawableRes val iconResId: Int, // Local drawable resource ID
    val target: Int          // The target value to unlock it
)

// This object holds all the achievement definitions
object AppAchievements {
    val ALL_ACHIEVEMENTS = listOf(
        AchievementDefinition(
            id = "first_expense",
            name = "First Spender",
            description = "Log your very first expense.",
            iconResId = R.drawable.ic_first_spend,
            target = 1
        ),
        AchievementDefinition(
            id = "no_spend_day",
            name = "No Spend Day",
            description = "Log no expenses for a day.",
            iconResId = R.drawable.ic_no_spend,
            target = 1
        ),
        AchievementDefinition(
            id = "monthly_maven",
            name = "Monthly Maven",
            description = "Log expenses for a full calendar month",
            iconResId = R.drawable.ic_calendar,
            target = 30
        )

    )

    // Helper function to easily get an achievement definition by its ID
    fun getDefinitionById(id: String): AchievementDefinition? {
        return ALL_ACHIEVEMENTS.find { it.id == id }
    }
}