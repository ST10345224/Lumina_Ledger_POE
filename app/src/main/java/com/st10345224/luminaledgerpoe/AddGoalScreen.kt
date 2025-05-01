package com.st10345224.luminaledgerpoe

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.text.input.KeyboardType
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.rememberDatePickerState
import java.time.Instant
import java.time.ZoneId
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.google.firebase.auth.FirebaseAuth
import java.time.LocalDateTime
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddGoalScreen(onGoalAdded: () -> Unit) {
    // Use rememberSaveable to survive configuration changes
    var yearMonth by rememberSaveable { mutableStateOf(YearMonth.now()) }
    var minimumSpendingGoal by rememberSaveable { mutableStateOf("") }
    var maximumSpendingGoal by rememberSaveable { mutableStateOf("") }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }

    // Instance of Firebase Authentication
    val auth = FirebaseAuth.getInstance()
    // Get the current user's ID, or an empty string if no user is logged in
    val userId = auth.currentUser?.uid ?: ""

    // Coroutine scope for asynchronous operations
    val coroutineScope = rememberCoroutineScope()
    // Instance of Firebase Firestore
    val firestore = Firebase.firestore

    // Date Picker State for selecting the year and month
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = Instant.now().toEpochMilli(),
        yearRange = 2020..2030 // Example year range for the date picker
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // Background image
        Image(
            painter = painterResource(id = R.drawable.splashbackground), // Replace with your image resource
            contentDescription = null, // Decorative image, no need for description
            contentScale = ContentScale.Crop, // Or ContentScale.FillBounds, etc.
            modifier = Modifier.fillMaxSize()
        )
        // Function to handle saving the goal data to Firestore
        fun saveGoal() {
            coroutineScope.launch {
                // Create a HashMap to store the goal data
                val goalData = hashMapOf(
                    // Store the userId
                    "userId" to userId,
                    // Format the YearMonth to a String for storage
                    "yearMonth" to yearMonth.format(
                        DateTimeFormatter.ofPattern(
                            "MMMM yyyy",
                            Locale.getDefault()
                        )
                    ), // Store as "Month Year"
                    // Convert the minimum spending goal to Double if not null
                    "minimumSpendingGoal" to minimumSpendingGoal.toDoubleOrNull(),
                    // Convert the maximum spending goal to Double if not null
                    "maximumSpendingGoal" to maximumSpendingGoal.toDoubleOrNull()
                )

                // Add a new document to the "Goals" collection with the goal data
                firestore.collection("Goals")
                    .add(goalData)
                    .addOnSuccessListener {
                        // Notify the parent composable that the goal has been added successfully
                        onGoalAdded()
                    }
                    .addOnFailureListener { e ->
                        // Handle the error appropriately, e.g., log the error
                        println("Error adding goal: ${e.message}")
                        // Optionally, show a user-friendly error message
                    }
            }
        }

        // Show Date Picker Dialog when showDatePicker is true
        if (showDatePicker) {
            DatePickerDialog(
                // Callback when the dialog is dismissed (e.g., by pressing back)
                onDismissRequest = { showDatePicker = false },
                // Button to confirm the selected date
                confirmButton = {
                    Button(onClick = {
                        // Get the selected date in milliseconds
                        datePickerState.selectedDateMillis?.let {
                            // Convert milliseconds to LocalDateTime
                            val selectedDate =
                                LocalDateTime.ofInstant(
                                    Instant.ofEpochMilli(it),
                                    ZoneId.systemDefault()
                                )
                            // Extract YearMonth from the selected LocalDateTime
                            yearMonth = YearMonth.from(selectedDate)
                        }
                        // Dismiss the date picker dialog
                        showDatePicker = false
                    }) {
                        Text("Confirm")
                    }
                },
                // Button to cancel the date selection
                dismissButton = {
                    Button(onClick = { showDatePicker = false }) {
                        Text("Cancel")
                    }
                }
            ) {
                // The actual DatePicker composable
                DatePicker(state = datePickerState)
            }
        }


        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Button to trigger the display of the DatePickerDialog
            Button(onClick = { showDatePicker = true }) {
                Text("Select Year and Month")
            }
            // Display the currently selected YearMonth
            Text(
                text = "Selected Month: ${
                    yearMonth.format(
                        DateTimeFormatter.ofPattern(
                            "MMMM yyyy",
                            Locale.getDefault()
                        )
                    )
                }",
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Input field for the minimum spending goal
            OutlinedTextField(
                value = minimumSpendingGoal,
                onValueChange = { minimumSpendingGoal = it },
                label = { Text("Minimum Spending Goal") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), // Ensure numeric keyboard
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Input field for the maximum spending goal
            OutlinedTextField(
                value = maximumSpendingGoal,
                onValueChange = { maximumSpendingGoal = it },
                label = { Text("Maximum Spending Goal") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), // Ensure numeric keyboard
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(32.dp))

            // Button to save the entered goal
            Button(
                onClick = {
                    saveGoal()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Goal")
            }
        }
    }
}
