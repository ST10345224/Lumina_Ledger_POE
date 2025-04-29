package com.st10345224.luminaledgerpoe

import android.util.Base64
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.Instant
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LedgerScreen() {
    val expenses = remember { mutableStateListOf<Expense>() }
    val coroutineScope = rememberCoroutineScope()
    val firestore = Firebase.firestore
    val loading = remember { mutableStateOf(true) }
    val error = remember { mutableStateOf<String?>(null) }
    var selectedYearMonth by remember { mutableStateOf(YearMonth.now()) }
    var showDatePicker by remember { mutableStateOf(false) }

    // Date Picker State
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = Instant.now().toEpochMilli(),
        yearRange = 2020..2030 // Example year range
    )

    // Function to fetch expenses from Firestore
    suspend fun fetchExpenses(yearMonth: YearMonth) {
        loading.value = true
        try {
            // Fetch expenses from Firestore
            val expensesSnapshot = firestore.collection("Expenses").get().await()
            val fetchedExpenses = expensesSnapshot.documents.map { document ->
                Expense(
                    exID = document.getString("exID") ?: "",
                    UserID = document.getString("userID") ?: "",
                    Category = document.getString("category") ?: "",
                    exAmount = document.getDouble("exAmount") ?: 0.0,
                    Date = document.get("Date", Timestamp::class.java) ?: Timestamp.now(),
                    exDescription = document.getString("exDescription") ?: "",
                    exPhotoString = document.getString("exPhotoString") ?: "",
                    Currency = document.getString("currency") ?: "",
                    exTitle = document.getString("exTitle") ?: ""
                )
            }
            expenses.clear()
            // Filter expenses by the selected year and month
            val filteredExpenses = fetchedExpenses.filter {
                val expenseYearMonth =
                    LocalDateTime.ofInstant(it.Date.toDate().toInstant(), ZoneId.systemDefault())
                        .toLocalDate()
                        .let { YearMonth.from(it) }
                expenseYearMonth == yearMonth
            }
            expenses.addAll(filteredExpenses)
            loading.value = false
        } catch (e: Exception) {
            error.value = "Error fetching expenses: ${e.message}"
            loading.value = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.splashbackground), // Replace with your image resource
            contentDescription = null, // Decorative image, no need for description
            contentScale = ContentScale.Crop, // Or ContentScale.FillBounds, etc.
            modifier = Modifier.fillMaxSize()
        )

        // Fetch expenses on initial load and when the selected month changes
        LaunchedEffect(coroutineScope, selectedYearMonth) {
            coroutineScope.launch {
                fetchExpenses(selectedYearMonth)
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
                            selectedYearMonth = YearMonth.from(selectedDate)
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

        Column(modifier = Modifier.fillMaxSize()) {
            // Top bar with Month selection
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Expenses: ${
                        selectedYearMonth.format(
                            DateTimeFormatter.ofPattern(
                                "MMMM yyyy",
                                Locale.getDefault()
                            )
                        )
                    }",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Button(onClick = { showDatePicker = true }) {
                    Text("Select Month")
                }
            }

            if (loading.value) {
                // Show loading indicator
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Loading Expenses...")
                }
            } else if (error.value != null) {
                // Show error message
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Error: ${error.value}")
                }
            } else {
                // Display the list of expenses
                LazyColumn(
                    modifier = Modifier
                        .padding(16.dp)
                        .weight(1f) // Make LazyColumn take up remaining space
                ) {
                    items(expenses) { expense ->
                        ExpenseCard(expense = expense)
                    }
                }
                // Calculate and display total expenses
                val totalExpenses = remember(expenses) {
                    expenses.sumOf { it.exAmount }
                }
                Text(
                    text = "Total: ${expenses.firstOrNull()?.Currency ?: "ZAR"} ${
                        String.format(
                            "%.2f",
                            totalExpenses
                        )
                    }",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

@Composable
fun ExpenseCard(expense: Expense) {
    var expanded by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { expanded = !expanded }, // Make card clickable
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = expense.exTitle,
                    style = MaterialTheme.typography.bodySmall, // Reduced font size
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f) // Title takes up available space
                )
                Text(
                    text = "${expense.Currency} ${String.format("%.2f", expense.exAmount)}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                // Show thumbnail if image available and not expanded
                if (expense.exPhotoString.isNotBlank() && !expanded) {
                    val decodedBytes = Base64.decode(expense.exPhotoString, Base64.DEFAULT)
                    val imageBitmap =
                        android.graphics.BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                    if (imageBitmap != null) {
                        Image(
                            bitmap = imageBitmap.asImageBitmap(),
                            contentDescription = "Expense Photo",
                            modifier = Modifier
                                .size(50.dp) // Further reduced size for thumbnail
                                .clip(MaterialTheme.shapes.medium),
                            contentScale = ContentScale.Crop,
                        )
                    }
                }
            }

            // Use AnimatedVisibility to expand/collapse additional content
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Category: ${expense.Category}", style = MaterialTheme.typography.bodyMedium)
                    Text(text = "Date: ${formatTimestamp(expense.Date)}", style = MaterialTheme.typography.bodySmall)
                    Text(text = "Description: ${expense.exDescription}", style = MaterialTheme.typography.bodyMedium)

                    if (expense.exPhotoString.isNotBlank()) {
                        val decodedBytes = Base64.decode(expense.exPhotoString, Base64.DEFAULT)
                        val imageBitmap =
                            android.graphics.BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                        if (imageBitmap != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Image(
                                bitmap = imageBitmap.asImageBitmap(),
                                contentDescription = "Expense Photo",
                                modifier = Modifier
                                    .fillMaxWidth() // Use full width when expanded
                                    .height(200.dp)  // Increased height for expanded image
                                    .clip(MaterialTheme.shapes.medium),
                                contentScale = ContentScale.Crop,
                            )
                        } else {
                            Text(
                                text = "Invalid Image Data",
                                color = Color.Red,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    } else {
                        Text(
                            text = "No Image Available",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

fun formatTimestamp(timestamp: Timestamp): String {
    val instant = timestamp.toDate().toInstant()
    val zonedDateTime = instant.atZone(ZoneId.systemDefault())
    val formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy, HH:mm", Locale.getDefault())
    return zonedDateTime.format(formatter)
}
