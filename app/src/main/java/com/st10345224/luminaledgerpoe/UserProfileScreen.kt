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
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.ByteArrayOutputStream

@Composable
fun UserProfileScreen() {
    // Context
    val context = LocalContext.current

    // Firebase instances
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()

    // Current User
    val currentUser = auth.currentUser

    // State variables to hold user data.  These use remember to survive recompositions.
    var user by remember { mutableStateOf<User?>(null) }
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var profilePicBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isEditing by remember { mutableStateOf(false) }  // Track if the user is in edit mode
    var loading by remember { mutableStateOf(true) }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.splashbackground), // Replace with your image resource
            contentDescription = null, // Decorative image, no need for description
            contentScale = ContentScale.Crop, // Or ContentScale.FillBounds, etc.
            modifier = Modifier.fillMaxSize()
        )

        // Fetch user data from Firestore when the screen is loaded or when the user changes.
        LaunchedEffect(currentUser?.uid) {
            if (currentUser != null) {
                loading = true
                firestore.collection("Users").document(currentUser.uid)
                    .get()
                    .addOnSuccessListener { document ->
                        if (document.exists()) {
                            // Map the document data to the User data class.
                            user = User(
                                userId = document.getString("userId") ?: currentUser.uid,
                                firstName = document.getString("firstName") ?: "",
                                lastName = document.getString("lastName") ?: "",
                                email = document.getString("email") ?: currentUser.email ?: "",
                                profilePicString = document.getString("profilePicString"),
                            )
                            // Initialize the state variables with the fetched data.
                            firstName = user?.firstName ?: ""
                            lastName = user?.lastName ?: ""
                            email = user?.email ?: ""
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
                            // If the user document doesn't exist, create a default User object.
                            user = User(
                                userId = currentUser.uid,
                                firstName = "",
                                lastName = "",
                                email = currentUser.email ?: "", // Use email from Firebase Auth
                                profilePicString = null
                            )
                            // Initialize state variables.
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

        // Activity launcher for selecting an image from the gallery
        val galleryLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == android.app.Activity.RESULT_OK && result.data != null) {
                val imageUri = result.data?.data
                try {
                    // Load the image into a Bitmap.  Consider resizing here to save memory.
                    val inputStream = imageUri?.let { context.contentResolver.openInputStream(it) }
                    profilePicBitmap = BitmapFactory.decodeStream(inputStream)
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(context, "Failed to load image", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Function to handle saving the user data to Firestore.
        fun saveUserData() {
            if (currentUser != null) {
                val userRef = firestore.collection("Users").document(currentUser.uid)

                // Prepare the data to be updated.
                val updates = hashMapOf<String, Any>(
                    "firstName" to firstName,
                    "lastName" to lastName,
                    "email" to email, //update email in firestore
                )

                // Handle profile picture upload if a new one is selected
                if (profilePicBitmap != null) {
                    val baos = ByteArrayOutputStream()
                    profilePicBitmap?.compress(
                        Bitmap.CompressFormat.JPEG,
                        80,
                        baos
                    ) // Compress the image.
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
                            isEditing = false // Exit edit mode after successful save.
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
                    // If no new profile picture, just update the text fields.
                    userRef.update(updates)
                        .addOnSuccessListener {
                            Toast.makeText(
                                context,
                                "Profile updated successfully!",
                                Toast.LENGTH_SHORT
                            ).show()
                            isEditing = false // Exit edit mode after successful save.
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

        if (loading) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()
                Text("Loading profile...")
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()), // Make the column scrollable
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Profile Picture
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (profilePicBitmap != null) {
                        Image(
                            bitmap = profilePicBitmap!!.asImageBitmap(),
                            contentDescription = "Profile Picture",
                            modifier = Modifier
                                .size(120.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop,
                        )
                    } else {
                        Image(
                            painter = painterResource(id = R.drawable.ic_launcher_foreground), //changed to ic_launcher_foreground,
                            contentDescription = "Profile Picture",
                            modifier = Modifier
                                .size(120.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    }
                    if (isEditing) {
                        Spacer(modifier = Modifier.width(16.dp))
                        Button(onClick = {
                            // Launch gallery to select a new profile picture.
                            val intent = Intent(
                                Intent.ACTION_PICK,
                                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                            )
                            galleryLauncher.launch(intent)
                        }) {
                            Text("Change Picture")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // First Name
                OutlinedTextField(
                    value = firstName,
                    onValueChange = { firstName = it },
                    label = { Text("First Name") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = isEditing,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
                    ),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Last Name
                OutlinedTextField(
                    value = lastName,
                    onValueChange = { lastName = it },
                    label = { Text("Last Name") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = isEditing,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
                    ),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Email
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = false, // Email is not editable here,  //make email uneditable
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Done
                    ),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(16.dp))

                // enable Edit/Save Button
                if (isEditing) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(onClick = {
                            // Save the changes
                            saveUserData()
                        }) {
                            Text("Save")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedButton(onClick = {
                            // Cancel editing, revert to original data.
                            isEditing = false
                            firstName = user?.firstName ?: ""
                            lastName = user?.lastName ?: ""
                            email = user?.email ?: ""
                            profilePicBitmap = null // Clear the selected image.
                        }) {
                            Text("Cancel")
                        }
                    }
                } else {
                    Button(onClick = {
                        // Enter edit mode
                        isEditing = true
                    }, modifier = Modifier.fillMaxWidth()) {
                        Text("Edit Profile")
                    }
                }
            }
        }
    }
}
