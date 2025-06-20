package com.st10345224.luminaledgerpoe

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.ByteArrayOutputStream

@Composable
fun UserProfileScreen() {
    // Context for Toast messages
    val context = LocalContext.current

    // Firebase Authentication and Firestore instances
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()

    // Get the currently logged-in user
    val currentUser = auth.currentUser

    // States for compose
    var user by remember { mutableStateOf<User?>(null) }
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var profilePicBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isEditing by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }

    Box(modifier = Modifier.fillMaxSize()) {
        // Background image
        Image(
            painter = painterResource(id = R.drawable.splashbackground),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Semi-transparent overlay
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Black.copy(alpha = 0.3f)
        ) {}

        // Fetch user data when the composable is first created or when the user changes
        LaunchedEffect(currentUser?.uid) {
            if (currentUser != null) {
                loading = true
                firestore.collection("Users").document(currentUser.uid)
                    .get()
                    .addOnSuccessListener { document ->
                        if (document.exists()) {
                            // Map Firestore document to User data class
                            user = User(
                                userId = document.getString("userId") ?: currentUser.uid,
                                firstName = document.getString("firstName") ?: "",
                                lastName = document.getString("lastName") ?: "",
                                email = document.getString("email") ?: currentUser.email ?: "",
                                profilePicString = document.getString("profilePicString"),
                            )
                            firstName = user?.firstName ?: ""
                            lastName = user?.lastName ?: ""
                            email = user?.email ?: ""
                            // Decode base64 string to Bitmap for profile picture
                            user?.profilePicString?.let {
                                try {
                                    val imageBytes = decodeBase64(it)
                                    profilePicBitmap = bytesToBitmap(imageBytes)
                                } catch (e: Exception) {
                                    Log.e(
                                        "ProfileScreen",
                                        "Error decoding profile picture: ${e.message}"
                                    )
                                    Toast.makeText(
                                        context,
                                        "Error loading profile picture",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        } else {
                            // Create default User object if document doesn't exist
                            user = User(
                                userId = currentUser.uid,
                                firstName = "",
                                lastName = "",
                                email = currentUser.email ?: "",
                                profilePicString = null
                            )
                            firstName = ""
                            lastName = ""
                            email = currentUser.email ?: ""
                        }
                        loading = false
                    }
                    .addOnFailureListener { e ->
                        Log.e("UserProfileScreen", "Error fetching user data: ${e.message}")
                        Toast.makeText(
                            context,
                            "Failed to load user data: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                        loading = false
                    }
            }
        }

        // Activity launcher to pick an image from the gallery
        val galleryLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == android.app.Activity.RESULT_OK && result.data != null) {
                val imageUri = result.data?.data
                try {
                    val inputStream = imageUri?.let { context.contentResolver.openInputStream(it) }
                    profilePicBitmap = BitmapFactory.decodeStream(inputStream) // Load selected image
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(context, "Failed to load image", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Function to save user data to Firestore
        fun saveUserData() {
            if (currentUser != null) {
                val userRef = firestore.collection("Users").document(currentUser.uid)
                val updates = hashMapOf<String, Any>(
                    "firstName" to firstName,
                    "lastName" to lastName,
                    "email" to email, // Update email in Firestore
                )

                // Handle profile picture upload if a new one is selected
                if (profilePicBitmap != null) {
                    val baos = ByteArrayOutputStream()
                    profilePicBitmap?.compress(Bitmap.CompressFormat.JPEG, 80, baos)
                    val data = baos.toByteArray()
                    val profilePicString = Base64.encodeToString(data, Base64.DEFAULT)
                    updates["profilePicString"] = profilePicString
                    userRef.update(updates)
                        .addOnSuccessListener {
                            Toast.makeText(
                                context,
                                "Profile updated successfully!",
                                Toast.LENGTH_SHORT
                            ).show()
                            isEditing = false // Exit edit mode
                            user = user?.copy(
                                firstName = firstName,
                                lastName = lastName,
                                email = email,
                                profilePicString = profilePicString
                            )
                        }
                        .addOnFailureListener { e ->
                            Log.e("UserProfileScreen", "Error updating profile: ${e.message}")
                            Toast.makeText(
                                context,
                                "Failed to update profile: ${e.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                } else {
                    // Update text fields if no new profile picture
                    userRef.update(updates)
                        .addOnSuccessListener {
                            Toast.makeText(
                                context,
                                "Profile updated successfully!",
                                Toast.LENGTH_SHORT
                            ).show()
                            isEditing = false // Exit edit mode
                            user = user?.copy(
                                firstName = firstName,
                                lastName = lastName,
                                email = email
                            )
                        }
                        .addOnFailureListener { e ->
                            Log.e("UserProfileScreen", "Error updating profile: ${e.message}")
                            Toast.makeText(
                                context,
                                "Failed to update profile: ${e.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                }
            }
        }

        // Show loading indicator while data is being fetched
        if (loading) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(60.dp),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Loading profile...",
                    color = Color.White,
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp
                )
            }
        } else {
            // Main profile content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(32.dp))

                // Profile title
                Text(
                    "User Profile",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                // Profile information card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(8.dp, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Profile Picture Section
                        Box(
                            modifier = Modifier
                                .padding(bottom = 20.dp)
                                .size(140.dp)
                        ) {
                            // Display profile picture with border
                            Surface(
                                modifier = Modifier
                                    .size(140.dp)
                                    .clip(CircleShape)
                                    .shadow(6.dp, CircleShape),
                                shape = CircleShape,
                                border = BorderStroke(4.dp, MaterialTheme.colorScheme.primary)
                            ) {
                                if (profilePicBitmap != null) {
                                    Image(
                                        bitmap = profilePicBitmap!!.asImageBitmap(),
                                        contentDescription = "Profile Picture",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Image(
                                        painter = painterResource(id = R.drawable.ic_launcher_foreground),
                                        contentDescription = "Profile Picture",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }

                            // Button to change profile picture in edit mode
                            if (isEditing) {
                                FloatingActionButton(
                                    onClick = {
                                        val intent = Intent(
                                            Intent.ACTION_PICK,
                                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                                        )
                                        galleryLauncher.launch(intent)
                                    },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .align(Alignment.BottomEnd),
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text("+", fontSize = 24.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Input field for first name
                        OutlinedTextField(
                            value = firstName,
                            onValueChange = { firstName = it },
                            label = { Text("First Name") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            enabled = isEditing, // Only enabled in edit mode
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Next
                            ),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Input field for last name
                        OutlinedTextField(
                            value = lastName,
                            onValueChange = { lastName = it },
                            label = { Text("Last Name") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            enabled = isEditing, // Only enabled in edit mode
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Next
                            ),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Display for email (not editable)
                        OutlinedTextField(
                            value = email,
                            onValueChange = { },
                            label = { Text("Email") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            enabled = false, // Disabled for editing
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Done
                            ),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                disabledTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Buttons to either edit or save/cancel profile
                        if (isEditing) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                // Cancel edit button
                                OutlinedButton(
                                    onClick = {
                                        isEditing = false
                                        firstName = user?.firstName ?: ""
                                        lastName = user?.lastName ?: ""
                                        email = user?.email ?: ""
                                        profilePicBitmap = null // Clear any selected new image
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                                ) {
                                    Text(
                                        "Cancel",
                                        modifier = Modifier.padding(vertical = 4.dp),
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                // Save changes button
                                Button(
                                    onClick = {
                                        saveUserData()
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Text(
                                        "Save",
                                        modifier = Modifier.padding(vertical = 4.dp),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        } else {
                            // Edit profile button
                            Button(
                                onClick = {
                                    isEditing = true
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Text(
                                    "Edit Profile",
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}