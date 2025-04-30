package com.st10345224.luminaledgerpoe


import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip

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
    val goals = remember { mutableStateListOf<Goal>() }
    val expenses = remember { mutableStateListOf<Expense>() }
    val loading = remember { mutableStateOf(true) }
    val error = remember { mutableStateOf<String?>(null) }

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
                    // Fetch Goals
                    val goalsSnapshot = Firebase.firestore.collection("Goals").get().await()
                    goals.clear()
                    goals.addAll(goalsSnapshot.documents.mapNotNull { document ->
                        try {
                            val yearMonthString = document.getString("yearMonth") ?: ""
                            Goal(
                                yearMonth = if (yearMonthString.isNotEmpty()) {
                                    YearMonth.parse(
                                        yearMonthString,
                                        DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()))
                                } else YearMonth.now(),
                                minimumSpendingGoal = document.getDouble("minimumSpendingGoal"),
                                maximumSpendingGoal = document.getDouble("maximumSpendingGoal")
                            )
                        } catch (e: Exception) {
                            null
                        }
                    })

                    // Fetch Expenses
                    val expensesSnapshot = Firebase.firestore.collection("Expenses").get().await()
                    expenses.clear()
                    expenses.addAll(expensesSnapshot.documents.mapNotNull { document ->
                        try {
                            Expense(
                                exID = document.getString("exID") ?: "",
                                UserID = document.getString("UserID") ?: "",
                                Category = document.getString("Category") ?: "",
                                exAmount = document.getDouble("exAmount") ?: 0.0,
                                Date = document.get("Date", Timestamp::class.java) ?: Timestamp.now(),
                                exDescription = document.getString("exDescription") ?: "",
                                exPhotoString = document.getString("exPhotoString") ?: "",
                                Currency = document.getString("Currency") ?: "",
                                exTitle = document.getString("exTitle") ?: ""
                            )
                        } catch (e: Exception) {
                            null
                        }
                    })

                    loading.value = false
                } catch (e: Exception) {
                    error.value = "Error fetching data: ${e.message}"
                    loading.value = false
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
                    items(goals.sortedBy { it.yearMonth }) { goal ->
                        AnimatedGoalCard(
                            goal = goal,
                            monthlyExpenses = expenses.groupBy { convertTimestampToYearMonth(it.Date) },
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }

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
    val totalExpenses = remember(monthlyExpenses, goal.yearMonth) {
        calculateExpensesForMonth(monthlyExpenses[goal.yearMonth] ?: emptyList(), goal.yearMonth)
    }

    val maxGoal = goal.maximumSpendingGoal ?: 0.0
    val minGoal = goal.minimumSpendingGoal ?: 0.0

    // Animated progress value
    val animatedProgress by animateFloatAsState(
        targetValue = when {
            maxGoal <= 0 -> 0f
            totalExpenses > maxGoal -> 1f
            else -> (totalExpenses / maxGoal).toFloat()
        },
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "progressAnimation"
    )

    // Color animation
    val progressColor by animateColorAsState(
        targetValue = when {
            minGoal > 0 && totalExpenses < minGoal -> MaterialTheme.colorScheme.error
            totalExpenses > maxGoal -> MaterialTheme.colorScheme.error
            else -> MaterialTheme.colorScheme.primary
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
            // Header with month/year
            Text(
                text = goal.yearMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Goals row
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

            // Progress section
            Column {
                // Current spending
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

                // Progress percentage with animation
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