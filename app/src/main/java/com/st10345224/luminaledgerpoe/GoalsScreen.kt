package com.st10345224.luminaledgerpoe


import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

// Function to convert Timestamp to YearMonth.
fun convertTimestampToYearMonth(timestamp: Timestamp): YearMonth {
    val instant = timestamp.toInstant()
    val zoneId = ZoneId.systemDefault()
    val localDateTime = instant.atZone(zoneId).toLocalDateTime()
    return YearMonth.from(localDateTime)
}

// Instance of Firebase Authentication
val auth = FirebaseAuth.getInstance()
// Get the current user's ID, or an empty string if no user is logged in
val userId = auth.currentUser?.uid ?: ""

// Function to calculate total expenses for a given month.
fun calculateExpensesForMonth(expenses: List<Expense>, yearMonth: YearMonth): Double {
    return expenses.filter { convertTimestampToYearMonth(it.Date) == yearMonth }
        .sumOf { it.exAmount }
}

@Composable
fun GoalsScreen(onAddGoal: () -> Unit) {
    // Coroutine scope for asynchronous operations
    val coroutineScope = rememberCoroutineScope()
    // State to hold the list of goals
    val goals = remember { mutableStateListOf<Goal>() }
    // State to hold the list of expenses for calculating spending
    val expenses = remember { mutableStateListOf<Expense>() }
    // State to track loading state
    val loading = remember { mutableStateOf(true) }
    // State to hold any error messages during data fetching
    val error = remember { mutableStateOf<String?>(null) }
    // Firestore instance
    val firestore = com.google.firebase.Firebase.firestore

    Box(modifier = Modifier.fillMaxSize()) {
        // Background with slight overlay for better readability
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(id = R.drawable.splashbackground),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.2f))
            )
        }

        // Fetch data
        LaunchedEffect(Unit) {
            coroutineScope.launch {
                try {
                    // Fetch Goals from Firestore
                    val goalsSnapshot = Firebase.firestore.collection("Goals").get().await()
                    goals.clear()
                    goals.addAll(goalsSnapshot.documents.mapNotNull { document ->
                        try {
                            val yearMonthString = document.getString("yearMonth") ?: ""
                            Goal(
                                // Parse yearMonth string, default to current month if empty
                                yearMonth = if (yearMonthString.isNotEmpty()) {
                                    YearMonth.parse(
                                        yearMonthString,
                                        DateTimeFormatter.ofPattern(
                                            "MMMM yyyy",
                                            Locale.getDefault()
                                        )
                                    )
                                } else YearMonth.now(),
                                minimumSpendingGoal = document.getDouble("minimumSpendingGoal"),
                                maximumSpendingGoal = document.getDouble("maximumSpendingGoal"),
                                userID = document.getString("userId") ?: ""
                            )
                        } catch (e: Exception) {
                            // Handle potential errors during goal parsing
                            null
                        }
                    })

                    // Fetch Expenses from Firestore
                    val expensesSnapshot = Firebase.firestore.collection("Expenses").get().await()
                    expenses.clear()
                    expenses.addAll(expensesSnapshot.documents.mapNotNull { document ->
                        try {
                            Expense(
                                exID = document.getString("exID") ?: "",
                                UserID = document.getString("userID") ?: "",
                                Category = document.getString("category") ?: "",
                                exAmount = document.getDouble("exAmount") ?: 0.0,
                                // Get Date as Timestamp, default to now if null
                                Date = document.get("date", Timestamp::class.java)
                                    ?: Timestamp.now(),
                                exDescription = document.getString("exDescription") ?: "",
                                exPhotoString = document.getString("exPhotoString") ?: "",
                                Currency = document.getString("currency") ?: "",
                                exTitle = document.getString("exTitle") ?: ""
                            )
                        } catch (e: Exception) {
                            // Handle potential errors during expense parsing
                            null
                        }


                    })

                    loading.value = false // Data fetching successful
                } catch (e: Exception) {
                    error.value = "Error fetching data: ${e.message}" // Store the error message
                    loading.value = false // Data fetching failed
                }
            }
        }

        when {
            loading.value -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        strokeWidth = 4.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Loading Goals...",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White
                    )
                }
            }

            error.value != null -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.exclamation_24),
                        contentDescription = "Error",
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = error.value.orEmpty(),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    // Filter goals by current user into filteredGoals
                    val filteredGoals = goals.filter { it.userID == userId }
                    // Filter expenses by current user into filteredExpenses
                    val filteredExpenses = expenses.filter { it.UserID == userId }
                    // Sort goals chronologically
                    items(filteredGoals.sortedBy { it.yearMonth }) { goal ->
                        AnimatedGoalCard(
                            goal = goal,
                            // Group expenses by month for each goal
                            monthlyExpenses = filteredExpenses.groupBy { convertTimestampToYearMonth(it.Date) },
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }

                // Button to navigate to add a new goal
                FloatingActionButton(
                    onClick = onAddGoal,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp),
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = "Add Goal",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AnimatedGoalCard(
    goal: Goal,
    monthlyExpenses: Map<YearMonth, List<Expense>>,
    modifier: Modifier = Modifier
) {
    // Calculate total spending for the goal's month
    val totalExpenses = remember(monthlyExpenses, goal.yearMonth) {
        calculateExpensesForMonth(monthlyExpenses[goal.yearMonth] ?: emptyList(), goal.yearMonth)
    }

    val maxGoal = goal.maximumSpendingGoal ?: 0.0
    val minGoal = goal.minimumSpendingGoal ?: 0.0

    // Animated progress value towards maximum goal
    val animatedProgress by animateFloatAsState(
        targetValue = when {
            maxGoal <= 0 -> 0f
            totalExpenses > maxGoal -> 1f
            else -> (totalExpenses / maxGoal).toFloat()
        },
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "progressAnimation"
    )

    // Animated color of the progress bar
    val progressColor by animateColorAsState(
        targetValue = when {
            minGoal > 0 && totalExpenses < minGoal -> MaterialTheme.colorScheme.error // Below minimum
            totalExpenses > maxGoal -> MaterialTheme.colorScheme.error // Exceeded maximum
            else -> MaterialTheme.colorScheme.primary // Within or approaching maximum
        },
        animationSpec = tween(durationMillis = 1000),
        label = "colorAnimation"
    )

    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Display month and year of the goal
            Text(
                text = goal.yearMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Display minimum and maximum spending goals
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        text = "Minimum",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        text = minGoal.currencyFormat(),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Maximum",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        text = maxGoal.currencyFormat(),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Display current spending and progress
            Column {
                // Text showing current spending
                Text(
                    text = "Current Spending: ${totalExpenses.currencyFormat()}",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                // Animated progress bar
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .clip(RoundedCornerShape(6.dp)),
                    color = progressColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )

                // Display progress percentage
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "0%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        text = "${(animatedProgress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = progressColor,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = "100%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

// Helper extension for currency formatting
fun Double.currencyFormat(): String {
    return "R${"%.2f".format(this)}" // Adjust currency symbol as needed
}