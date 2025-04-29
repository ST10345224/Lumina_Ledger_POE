package com.st10345224.luminaledgerpoe

import java.time.YearMonth

data class Goal(
    val yearMonth: YearMonth,
    val minimumSpendingGoal: Double?,
    val maximumSpendingGoal: Double?
)