package com.st10345224.luminaledgerpoe

import com.google.firebase.Timestamp

data class Expense(
    val exID: String,
    val UserID: String,
    val Category: String,
    val exAmount: Double,
    val Date: Timestamp,
    val exDescription: String,
    val exPhotoString: String,
    val Currency: String,
    val exTitle: String

)
