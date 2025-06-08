package com.st10345224.luminaledgerpoe.datamodels

data class DailyExpenseData(
    val date: Long, // Epoch milliseconds for the start of the day
    val totalAmount: Double
)