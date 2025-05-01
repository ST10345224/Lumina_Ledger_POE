package com.st10345224.luminaledgerpoe


import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun RegisterScreen(
    onNavigateToLogin: () -> Unit, // Callback function to navigate to the login screen
    onRegistrationSuccess: () -> Unit // Callback function to execute after successful registration (currently not directly used in the success path)
) {
    // State variables to hold the input values for registration
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    // Get the current context for displaying Toast messages
    val context = LocalContext.current

    // Initialize Firebase Authentication and Firestore instances
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()

    // Main container for the entire screen
    Box(modifier = Modifier.fillMaxSize()) {
        // Background image
        Image(
            painter = painterResource(id = R.drawable.splashbackground), // Replace with your actual image resource ID
            contentDescription = null, // Decorative image, so no accessibility description needed
            contentScale = ContentScale.Crop, // Scale the image to fill the bounds, potentially cropping
            modifier = Modifier.fillMaxSize()
        )
        // Column to arrange elements vertically in the center of the screen
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp), // Add some padding around the column
            horizontalAlignment = Alignment.CenterHorizontally, // Center items horizontally
            verticalArrangement = Arrangement.Center // Center items vertically
        ) {
            // App title
            Text(
                "Lumina Ledger",
                style = MaterialTheme.typography.headlineLarge, // Use a large headline style from the theme
                modifier = Modifier.padding(16.dp), // Add padding around the text
                color = Color.Black,
                fontWeight = FontWeight.Bold, // Make the text bold
                fontSize = 30.sp // Set a specific font size
            )
            Spacer(modifier = Modifier.height(16.dp)) // Add vertical space

            // App logo
            Image(
                painter = painterResource(id = R.drawable.luminaledgerlogo), // Replace with your actual logo resource ID
                contentDescription = "App Logo", // Provide a description for accessibility
                contentScale = ContentScale.Crop, // Scale the image to fill the bounds, potentially cropping
                modifier = Modifier
                    .size(120.dp) // Set the size of the logo
                    .clip(CircleShape) // Clip the image to a circle shape
                    .border(1.dp, Color.White, CircleShape) // Add a white border to the circle
            )
            Spacer(modifier = Modifier.height(16.dp)) // Add vertical space

            // Surface to group the registration form elements with a background and shadow
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp), // Apply rounded corners to the surface
                color = Color.White.copy(alpha = 0.8f), // Set a semi-transparent white background
                shadowElevation = 8.dp // Add a shadow to give depth
            ) {
                // Column to arrange the registration form elements vertically within the Surface
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp), // Add padding inside the Surface
                    horizontalAlignment = Alignment.CenterHorizontally, // Center items horizontally
                    verticalArrangement = Arrangement.Center // Center items vertically
                ) {
                    // Title for the registration section
                    Text(
                        "Create an Account",
                        style = MaterialTheme.typography.headlineMedium, // Use a medium headline style
                        modifier = Modifier.padding(bottom = 16.dp), // Add padding below the text
                        color = Color.Black,
                        fontWeight = FontWeight.Bold // Make the text bold
                    )

                    // Input field for the first name
                    OutlinedTextField(
                        value = firstName,
                        onValueChange = { firstName = it }, // Update the firstName state when the text changes
                        label = { Text("First Name") }, // Label for the input field
                        modifier = Modifier.fillMaxWidth(), // Make the field fill the width of its parent
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.Black, // Set the text color when the field is focused
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp)) // Add vertical space

                    // Input field for the last name
                    OutlinedTextField(
                        value = lastName,
                        onValueChange = { lastName = it }, // Update the lastName state
                        label = { Text("Last Name") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.Black,
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp)) // Add vertical space

                    // Input field for the email address
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it }, // Update the email state
                        label = { Text("Email") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.Black,
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp)) // Add vertical space

                    // Input field for the password
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it }, // Update the password state
                        label = { Text("Password") },
                        visualTransformation = PasswordVisualTransformation(), // Hide the password characters
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.Black,
                        )
                    )
                    Spacer(modifier = Modifier.height(16.dp)) // Add vertical space

                    // Button to trigger the registration process
                    Button(
                        onClick = {
                            // Check if email and password fields are not blank
                            if (email.isNotBlank() && password.isNotBlank()) {
                                // Create a new user with the provided email and password using Firebase Authentication
                                auth.createUserWithEmailAndPassword(email, password)
                                    .addOnCompleteListener { task ->
                                        // Listen for the completion of the registration task
                                        if (task.isSuccessful) {
                                            // If registration is successful, get the current user
                                            val user = auth.currentUser
                                            user?.let {
                                                // Create a User data object to store additional user information
                                                val userData = User(
                                                    userId = it.uid, // Get the unique user ID from Firebase Auth
                                                    firstName = firstName,
                                                    lastName = lastName,
                                                    email = email
                                                )

                                                // Add the user data to the "Users" collection in Firestore
                                                firestore.collection("Users")
                                                    .document(it.uid) // Use the user's UID as the document ID
                                                    .set(userData) // Set the user data in the document
                                                    .addOnSuccessListener {
                                                        // If saving user data to Firestore is successful
                                                        Toast.makeText(
                                                            context,
                                                            "Registration successful!",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                        onNavigateToLogin() // Navigate to the login screen
                                                    }
                                                    .addOnFailureListener { e ->
                                                        // If there's an error saving user data to Firestore
                                                        Log.e(
                                                            "Registration",
                                                            "Error adding user data to Firestore",
                                                            e // Log the error for debugging
                                                        )
                                                        Toast.makeText(
                                                            context,
                                                            "Registration successful, but failed to save user data.",
                                                            Toast.LENGTH_LONG
                                                        ).show()
                                                        onNavigateToLogin() // Still navigate to login, but inform the user about the data saving issue
                                                    }
                                            }
                                        } else {
                                            // If registration in Firebase Authentication fails
                                            Toast.makeText(
                                                context,
                                                "Registration failed: ${task.exception?.message}", // Show the error message to the user
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    }
                            } else {
                                // If email or password fields are blank, show an error message
                                Toast.makeText(
                                    context,
                                    "Please enter email and password",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50), // Set the button background color to green
                            contentColor = Color.White // Set the button text color to white
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(elevation = 2.dp, shape = RoundedCornerShape(8.dp)), // Add a small shadow to the button
                        shape = RoundedCornerShape(8.dp) // Apply rounded corners to the button
                    ) {
                        Text("Register") // Text displayed on the button
                    }
                    Spacer(modifier = Modifier.height(8.dp)) // Add vertical space

                    // Text button to navigate to the login screen
                    TextButton(onClick = onNavigateToLogin) {
                        Text("Already have an account? Log in here!") // Text displayed on the button
                    }
                }
            }
        }
    }
}