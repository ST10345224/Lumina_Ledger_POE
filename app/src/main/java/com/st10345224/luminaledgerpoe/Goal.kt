package com.st10345224.luminaledgerpoe

import java.time.YearMonth

data class Goal(
    val userID: String,
    val yearMonth: YearMonth,
    val minimumSpendingGoal: Double?,
    val maximumSpendingGoal: Double?
)