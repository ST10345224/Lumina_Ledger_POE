package com.st10345224.luminaledgerpoe

data class User(
    val userId: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val profilePicString: String? = null // Store the profile picture URL or Base64 string
)