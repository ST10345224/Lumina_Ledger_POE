package com.st10345224.luminaledgerpoe


import android.widget.Toast
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
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

@Composable
fun LoginScreen(
    onNavigateToRegister: () -> Unit, // Callback to navigate to the registration screen
    onLoginSuccess: () -> Unit // Callback to execute upon successful login
) {
    // State to hold the entered email, using `remember` to survive recompositions
    var email by remember { mutableStateOf("") }
    // State to hold the entered password, using `remember` to survive recompositions
    var password by remember { mutableStateOf("") }
    // Get the current context for displaying Toast messages
    val context = LocalContext.current
    // Initialize Firebase Authentication
    val auth = FirebaseAuth.getInstance()

    // Main container filling the entire screen
    Box(modifier = Modifier.fillMaxSize()) {
        // App background image
        Image(
            painter = painterResource(id = R.drawable.splashbackground), // Load the image from resources
            contentDescription = null, // Decorative image, no accessibility description needed
            contentScale = ContentScale.Crop, // Scale the image to fill the bounds, potentially cropping
            modifier = Modifier.fillMaxSize() // Make the image fill the entire Box
        )

        // Column to arrange login elements vertically in the center
        Column(
            modifier = Modifier
                .fillMaxSize() // Make the Column fill the entire Box
                .padding(24.dp), // Add padding around the column
            horizontalAlignment = Alignment.CenterHorizontally, // Center items horizontally
            verticalArrangement = Arrangement.Center // Center items vertically
        ) {
            // App logo, cropped to a circle with a white border
            Image(
                painter = painterResource(id = R.drawable.luminaledgerlogo), // Load the logo from resources
                contentDescription = "App Logo", // Accessibility description for the logo
                contentScale = ContentScale.Crop, // Scale the image to fill the bounds, potentially cropping
                modifier = Modifier
                    .size(120.dp) // Set the size of the logo to 120x120 dp
                    .clip(CircleShape) // Clip the logo to a circle
                    .border(1.dp, Color.White, CircleShape) // Add a 1dp white border to the circle
            )

            Spacer(modifier = Modifier.height(32.dp)) // Add vertical space of 32dp below the logo

            // Surface acting as a white card to contain login fields and buttons
            Surface(
                modifier = Modifier
                    .fillMaxWidth() // Make the Surface fill the width of its parent
                    // Add a shadow for visual depth
                    .shadow(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(24.dp) // Apply rounded corners to the shadow
                    ),
                shape = RoundedCornerShape(24.dp), // Apply rounded corners with a 24dp radius to the Surface
                color = Color.White.copy(alpha = 1f) // Set the background color to white (fully opaque)
            ) {
                // Column to arrange elements inside the white card vertically
                Column(
                    modifier = Modifier.padding(24.dp), // Add padding inside the Surface
                    horizontalAlignment = Alignment.CenterHorizontally // Center items horizontally
                ) {
                    // App title inside the login card
                    Text(
                        "Lumina Ledger",
                        style = MaterialTheme.typography.headlineSmall.copy( // Use the small headline style from the theme and modify it
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp
                        ),
                        color = Color.Black,
                        modifier = Modifier.padding(bottom = 8.dp) // Add padding below the title
                    )

                    // Subtitle for the login screen
                    Text(
                        "Sign in to continue",
                        style = MaterialTheme.typography.bodyMedium, // Use the body medium style from the theme
                        color = Color.Gray
                    )

                    Spacer(modifier = Modifier.height(24.dp)) // Add vertical space of 24dp

                    // Email input field with an email icon
                    OutlinedTextField(
                        value = email, // The current value of the input field
                        onValueChange = { email = it }, // Update the `email` state on text change
                        label = {
                            Text(
                                "Email",
                                style = MaterialTheme.typography.bodyMedium // Use the body medium style for the label
                            )
                        },
                        modifier = Modifier.fillMaxWidth(), // Make the field fill the width
                        shape = RoundedCornerShape(12.dp), // Apply rounded corners to the input field
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFFE7E7E7), // Light gray background when focused
                            unfocusedContainerColor = Color(0xFFF1F1F1), // Slightly lighter gray background when unfocused
                            focusedLabelColor = Color(0xFF000000), // Black label when focused
                            unfocusedLabelColor = Color.Gray, // Gray label when unfocused
                            focusedTextColor = Color.Black, // Black text when focused
                            unfocusedTextColor = Color.Black, // Black text when unfocused
                            focusedIndicatorColor = Color.Transparent, // No border when focused
                            unfocusedIndicatorColor = Color.Transparent // No border when unfocused
                        ),
                        singleLine = true, // Ensure only one line of text can be entered
                        leadingIcon = {
                            // Use the default email icon from Material Icons
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = "Email", // Accessibility description for the icon
                                tint = Color(0xFF000000) // Set the icon color to black
                            )
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp)) // Add vertical space of 16dp

                    // Password input field with a lock icon, masking the input
                    OutlinedTextField(
                        value = password, // The current value of the input field
                        onValueChange = { password = it }, // Update the `password` state on text change
                        label = {
                            Text(
                                "Password",
                                style = MaterialTheme.typography.bodyMedium // Use the body medium style for the label
                            )
                        },
                        visualTransformation = PasswordVisualTransformation(), // Hide the password characters
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFFE7E7E7),
                            unfocusedContainerColor = Color(0xFFF1F1F1),
                            focusedLabelColor = Color(0xFF000000),
                            unfocusedLabelColor = Color.Gray,
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        singleLine = true,
                        leadingIcon = {
                            // Use the default lock icon from Material Icons
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Password", // Accessibility description for the icon
                                tint = Color(0xFF4CAF50) // Set the icon color to green
                            )
                        }
                    )

                    Spacer(modifier = Modifier.height(24.dp)) // Add vertical space of 24dp

                    // Login button
                    Button(
                        onClick = {
                            // Check if both email and password fields are not blank
                            if (email.isNotBlank() && password.isNotBlank()) {
                                // Sign in with email and password using Firebase Authentication
                                auth.signInWithEmailAndPassword(email, password)
                                    .addOnCompleteListener { task ->
                                        // Listener for the completion of the sign-in task
                                        if (task.isSuccessful) {
                                            // If sign-in is successful
                                            Toast.makeText(
                                                context,
                                                "Login successful!",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            onLoginSuccess() // Execute the callback for successful login
                                        } else {
                                            // If sign-in fails
                                            Toast.makeText(
                                                context,
                                                "Login failed: ${task.exception?.message}", // Show the error message
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    }
                            } else {
                                // If email or password is blank, show an error message
                                Toast.makeText(
                                    context,
                                    "Please enter email and password",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth() // Make the button fill the width
                            .height(52.dp), // Set a fixed height for the button
                        shape = RoundedCornerShape(12.dp), // Apply rounded corners to the button
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50), // Set the button background color to green
                            contentColor = Color.White // Set the button text color to white
                        ),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 4.dp, // Default shadow elevation
                            pressedElevation = 8.dp // Shadow elevation when pressed
                        )
                    ) {
                        Text(
                            "LOG IN",
                            style = MaterialTheme.typography.labelLarge.copy( // Use the label large style and modify it
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp // Add some letter spacing for emphasis
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp)) // Add vertical space of 16dp

                    // Text button to navigate to the registration screen
                    TextButton(
                        onClick = onNavigateToRegister,
                        modifier = Modifier.fillMaxWidth() // Make the button fill the width
                    ) {
                        Text(
                            "Don't have an account? Register",
                            style = MaterialTheme.typography.bodyMedium.copy( // Use the body medium style and modify it
                                color = Color(0xFF4CAF50) // Set the text color to green
                            ),
                            modifier = Modifier.padding(8.dp) // Add some padding around the text
                        )
                    }
                }
            }
        }
    }
}