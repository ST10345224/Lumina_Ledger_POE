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

    val coroutineScope = rememberCoroutineScope()
    val firestore = Firebase.firestore

    // Date Picker State
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = Instant.now().toEpochMilli(),
        yearRange = 2020..2030 // Example year range
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.splashbackground), // Replace with your image resource
            contentDescription = null, // Decorative image, no need for description
            contentScale = ContentScale.Crop, // Or ContentScale.FillBounds, etc.
            modifier = Modifier.fillMaxSize()
        )
        // Function to handle saving the goal to Firestore
        fun saveGoal() {
            coroutineScope.launch {
                val goalData = hashMapOf(
                    "yearMonth" to yearMonth.format(
                        DateTimeFormatter.ofPattern(
                            "MMMM yyyy",
                            Locale.getDefault()
                        )
                    ), // Store as "Month Year"
                    "minimumSpendingGoal" to minimumSpendingGoal.toDoubleOrNull(),
                    "maximumSpendingGoal" to maximumSpendingGoal.toDoubleOrNull()
                )

                firestore.collection("Goals")
                    .add(goalData)
                    .addOnSuccessListener {
                        onGoalAdded() // Notify the parent composable
                    }
                    .addOnFailureListener { e ->
                        // Handle the error appropriately, e.g., show a Toast
                        println("Error adding goal: ${e.message}")
                    }
            }
        }

        // Show Date Picker Dialog
        if (showDatePicker) {
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    Button(onClick = {
                        datePickerState.selectedDateMillis?.let {
                            val selectedDate =
                                LocalDateTime.ofInstant(
                                    Instant.ofEpochMilli(it),
                                    ZoneId.systemDefault()
                                )
                            yearMonth = YearMonth.from(selectedDate)
                        }
                        showDatePicker = false
                    }) {
                        Text("Confirm")
                    }
                },
                dismissButton = {
                    Button(onClick = { showDatePicker = false }) {
                        Text("Cancel")
                    }
                }
            ) {
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
            // YearMonth selection using a DatePickerDialog
            Button(onClick = { showDatePicker = true }) {
                Text("Select Year and Month")
            }
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

            // Minimum Spending Goal Input
            OutlinedTextField(
                value = minimumSpendingGoal,
                onValueChange = { minimumSpendingGoal = it },
                label = { Text("Minimum Spending Goal") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), // Ensure numeric keyboard
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Maximum Spending Goal Input
            OutlinedTextField(
                value = maximumSpendingGoal,
                onValueChange = { maximumSpendingGoal = it },
                label = { Text("Maximum Spending Goal") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), // Ensure numeric keyboard
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(32.dp))

            // Button to Save the Goal
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
