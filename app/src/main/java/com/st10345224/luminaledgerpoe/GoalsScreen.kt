package com.st10345224.luminaledgerpoe


import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.lerp
import com.google.firebase.Timestamp
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.time.ZoneId
import java.time.LocalDateTime
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import kotlinx.coroutines.launch
import java.lang.Exception
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon

// Function to convert Timestamp to YearMonth.
fun convertTimestampToYearMonth(timestamp: Timestamp): YearMonth {
    val instant = timestamp.toInstant()
    val zoneId = ZoneId.systemDefault()
    val localDateTime = instant.atZone(zoneId).toLocalDateTime()
    return YearMonth.from(localDateTime)
}

// Function to calculate total expenses for a given month.
fun calculateExpensesForMonth(expenses: List<Expense>, yearMonth: YearMonth): Double {
    return expenses.filter { convertTimestampToYearMonth(it.Date) == yearMonth }.sumOf { it.exAmount }
}

@Composable
fun GoalsScreen(onAddGoal: () -> Unit) {
    val coroutineScope = rememberCoroutineScope()

    // State for goals, expenses, loading, and errors
    val goals = remember { mutableStateListOf<Goal>() }
    val expenses = remember { mutableStateListOf<Expense>() }
    val loading = remember { mutableStateOf(true) }
    val error: MutableState<String?> = remember { mutableStateOf(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.splashbackground), // Replace with your image resource
            contentDescription = null, // Decorative image, no need for description
            contentScale = ContentScale.Crop, // Or ContentScale.FillBounds, etc.
            modifier = Modifier.fillMaxSize()
        )
        // Fetch goals and expenses from Firestore
        LaunchedEffect(coroutineScope) {
            coroutineScope.launch {
                try {
                    // Fetch Goals
                    val goalsCollection = Firebase.firestore.collection("Goals")
                    val goalsSnapshot = goalsCollection.get().await()
                    val fetchedGoals = goalsSnapshot.documents.map { document ->
                        // Changed to handle string and parse to YearMonth
                        val yearMonthString = document.getString("yearMonth") ?: ""
                        val yearMonth: YearMonth = try {
                            if (yearMonthString.isNotEmpty()) {
                                YearMonth.parse(
                                    yearMonthString,
                                    DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())
                                )
                            } else {
                                YearMonth.now()
                            }
                        } catch (e: Exception) {
                            // Log the error or use a default value
                            println("Error parsing yearMonth: $yearMonthString. Using current month. Error: ${e.message}")
                            YearMonth.now() // Or handle the error as appropriate for your app
                        }
                        Goal(
                            yearMonth = yearMonth,
                            minimumSpendingGoal = document.getDouble("minimumSpendingGoal"),
                            maximumSpendingGoal = document.getDouble("maximumSpendingGoal")
                        )
                    }
                    goals.clear()
                    goals.addAll(fetchedGoals)

                    // Fetch Expenses
                    val expensesCollection = Firebase.firestore.collection("Expenses")
                    val expensesSnapshot = expensesCollection.get().await()
                    val fetchedExpenses = expensesSnapshot.documents.map { document ->
                        Expense(
                            exID = document.getString("exID") ?: "",
                            UserID = document.getString("UserID") ?: "",
                            Category = document.getString("Category") ?: "",
                            exAmount = document.getDouble("exAmount") ?: 0.0,
                            Date = document.get("Date", Timestamp::class.java)
                                ?: Timestamp.now(), // Ensure Date is fetched as Timestamp
                            exDescription = document.getString("exDescription") ?: "",
                            exPhotoString = document.getString("exPhotoString") ?: "",
                            Currency = document.getString("Currency") ?: "",
                            exTitle = document.getString("exTitle") ?: ""
                        )
                    }
                    expenses.clear()
                    expenses.addAll(fetchedExpenses)

                    loading.value = false
                } catch (e: Exception) {
                    error.value = "Error fetching data: ${e.message}"
                    loading.value = false
                }
            }
        }

        // Show loading indicator
        if (loading.value) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()
                Text(text = "Loading Goals and Expenses...")
            }
        } else if (error.value != null) {
            // Show error message
            Text(text = "Error: ${error.value}")
        } else {
            // Display the list of goals, passing in the fetched expenses
            LazyColumn(modifier = Modifier.padding(16.dp)) {
                items(goals) { goal ->
                    GoalCard(
                        goal = goal,
                        monthlyExpenses = expenses.groupBy { convertTimestampToYearMonth(it.Date) })
                }
            }
            // FloatingActionButton to add a new goal
            FloatingActionButton(
                onClick = onAddGoal, // Use the onAddGoal callback
                modifier = Modifier
                    .align(Alignment.BottomEnd) // Position at bottom right
                    .padding(16.dp), // Add some padding
                containerColor = MaterialTheme.colorScheme.primary, // Customize color if needed
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add Goal")
            }
        }
    }
}

@Composable
fun GoalCard(goal: Goal, monthlyExpenses: Map<YearMonth, List<Expense>>) {
    val totalExpenses = remember(monthlyExpenses, goal.yearMonth) {
        calculateExpensesForMonth(monthlyExpenses[goal.yearMonth] ?: emptyList(), goal.yearMonth)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        val formattedMonth = goal.yearMonth.format(DateTimeFormatter.ofPattern("MMMM ", Locale.getDefault()))

        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Goal for $formattedMonth",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Min: ${goal.minimumSpendingGoal?.let { String.format("%.2f", it) } ?: "N/A"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
                Text(
                    text = "Max: ${goal.maximumSpendingGoal?.let { String.format("%.2f", it) } ?: "N/A"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            val progress = when {
                goal.maximumSpendingGoal == null -> 0f
                totalExpenses > goal.maximumSpendingGoal -> 1f
                else -> (totalExpenses / goal.maximumSpendingGoal).toFloat()
            }
            val progressColor = when {
                goal.minimumSpendingGoal != null && totalExpenses < goal.minimumSpendingGoal -> Color.Red
                totalExpenses > (goal.maximumSpendingGoal ?: Double.MAX_VALUE) -> Color.Red
                else -> lerp(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary, progress)
            }

            Text(
                text = "Expenses: ${String.format("%.2f", totalExpenses)}",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            LinearProgressIndicator(
                progress = progress,
                color = progressColor,
                modifier = Modifier.fillMaxWidth(),
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Text(
                text = "${(progress * 100).toInt()}% of Max",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
