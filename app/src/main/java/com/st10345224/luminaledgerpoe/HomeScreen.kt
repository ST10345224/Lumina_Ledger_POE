package com.st10345224.luminaledgerpoe

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign


@Composable
fun HomeScreen() {
    val expenses = remember { mutableStateListOf<Expense>() }
    val goals = remember { mutableStateListOf<Goal>() }
    val coroutineScope = rememberCoroutineScope()
    val firestore = FirebaseFirestore.getInstance()
    val loading = remember { mutableStateOf(true) }
    val error = remember { mutableStateOf<String?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.splashbackground), // Replace with your image resource
            contentDescription = null, // Decorative image, no need for description
            contentScale = ContentScale.Crop, // Or ContentScale.FillBounds, etc.
            modifier = Modifier.fillMaxSize()
        )

        // Fetch data from Firestore
        LaunchedEffect(coroutineScope) {
            coroutineScope.launch {
                try {
                    // Fetch Expenses for the current month
                    val currentYearMonth = YearMonth.now()
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
                    }.filter { // Filter expenses for the current month
                        val expenseYearMonth =
                            LocalDateTime.ofInstant(
                                it.Date.toDate().toInstant(),
                                ZoneId.systemDefault()
                            )
                                .toLocalDate()
                                .let { YearMonth.from(it) }
                        expenseYearMonth == currentYearMonth
                    }
                    expenses.clear()
                    expenses.addAll(fetchedExpenses)

                    // Fetch Goals
                    val goalsSnapshot = firestore.collection("Goals").get().await()
                    val fetchedGoals = goalsSnapshot.documents.map { document ->
                        val yearMonthString = document.getString("yearMonth") ?: ""
                        val yearMonth = try {
                            if (yearMonthString.isNotEmpty()) {
                                YearMonth.parse(
                                    yearMonthString,
                                    DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())
                                )
                            } else {
                                YearMonth.now()
                            }
                        } catch (e: Exception) {
                            println("Error parsing yearMonth: $yearMonthString. Using current month. Error: ${e.message}")
                            YearMonth.now()
                        }
                        Goal(
                            yearMonth = yearMonth,
                            minimumSpendingGoal = document.getDouble("minimumSpendingGoal"),
                            maximumSpendingGoal = document.getDouble("maximumSpendingGoal")
                        )
                    }
                    goals.clear()
                    goals.addAll(fetchedGoals)

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
                Text(text = "Loading data...")
            }
        } else if (error.value != null) {
            // Show error message
            Text(text = "Error: ${error.value}")
        } else {
            // Display content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Donut Chart Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Monthly Expenses",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        ExpenseDonutChart(expenses = expenses)
                    }
                }

                // Goals Progress Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Goals",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        if (goals.isNotEmpty()) {
                            goals.forEach { goal ->
                                GoalProgressBar(goal = goal, expenses = expenses)
                            }
                        } else {
                            Text(text = "No goals set for this month.")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ExpenseDonutChart(expenses: List<Expense>) {
    // Calculate total expenses and expenses per category
    val totalExpenses = expenses.sumOf { it.exAmount }
    val expensesByCategory = expenses.groupBy { it.Category }.mapValues { (_, expenses) ->
        expenses.sumOf { it.exAmount }
    }

    // Define colors for categories (you can expand this as needed)
    val categoryColors = mapOf(
        "Food" to Color(0xFFE57373),       // Red
        "Housing" to Color(0xFFF06292),    // Pink
        "Transport" to Color(0xFFBA68C8),  // Purple
        "Entertainment" to Color(0xFF9575CD), // Deep Purple
        "Utilities" to Color(0xFF7986CB),  // Indigo
        "Healthcare" to Color(0xFF64B5F6), // Blue
        "Personal" to Color(0xFF4FC3F7),    // Light Blue
        "Business" to Color(0xFF4DD0E1)      // Cyan
    )

    // Ensure that the colors list matches the number of categories
    val colorList = expensesByCategory.keys.map { category ->
        categoryColors[category] ?: Color.Gray // Default color if category not found
    }
    val categoryNameList = expensesByCategory.keys.toList()


    // Calculate percentages for each category
    val percentages = expensesByCategory.mapValues { (_, amount) ->
        (amount / totalExpenses).toFloat()
    }
    var startAngle = 0f
    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {

        Box(modifier = Modifier.size(200.dp), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(200.dp)) {
                percentages.forEach { (category, percentage) ->
                    val angle = percentage * 360f
                    val color = categoryColors[category] ?: Color.Gray
                    drawArc(
                        color = color,
                        startAngle = startAngle,
                        sweepAngle = angle,
                        useCenter = false, //  Do not connect to the center
                        style = Stroke(width = 80f, cap = StrokeCap.Butt)
                    )
                    startAngle += angle
                }
            }

        }
        // Add a legend
        Column(modifier = Modifier.padding(top = 16.dp)) { // Added padding here
            categoryNameList.forEachIndexed { index, categoryName ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Canvas(modifier = Modifier.size(16.dp)) {
                        drawCircle(color = colorList[index])
                    }
                    Text(
                        categoryName,
                        modifier = Modifier.padding(start = 4.dp),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            Text(
                "Total: ${expenses.firstOrNull()?.Currency ?: "ZAR"} ${
                    String.format(
                        "%.2f",
                        totalExpenses
                    )
                }",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun GoalProgressBar(goal: Goal, expenses: List<Expense>) {
    val monthlyExpenses = expenses.filter {
        LocalDateTime.ofInstant(it.Date.toDate().toInstant(), ZoneId.systemDefault())
            .toLocalDate()
            .let { YearMonth.from(it) } == goal.yearMonth
    }.sumOf { it.exAmount }

    val maxGoal = goal.maximumSpendingGoal ?: 0.0
    val progress = if (maxGoal > 0) (monthlyExpenses / maxGoal).coerceIn(0.0, 1.0) else 0.0f

    // Animate the progress
    val animatedProgress by animateFloatAsState(
        targetValue = progress.toFloat(),
        animationSpec = tween(durationMillis = 1000) // You can adjust the duration
    )

    Column(modifier = Modifier.padding(8.dp)) {
        Text(
            text = "Goal for ${goal.yearMonth.format(DateTimeFormatter.ofPattern("MMMM", Locale.getDefault()))}",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = animatedProgress,
            modifier = Modifier.fillMaxWidth(),
            color = if (monthlyExpenses > (goal.maximumSpendingGoal ?: Double.MAX_VALUE)) Color.Red else MaterialTheme.colorScheme.primary,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Spent: ${expenses.firstOrNull()?.Currency ?: "ZAR"} ${
                    String.format(
                        "%.2f",
                        monthlyExpenses
                    )
                }",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Max: ${expenses.firstOrNull()?.Currency ?: "ZAR"} ${
                    String.format(
                        "%.2f",
                        goal.maximumSpendingGoal ?: 0.0
                    )
                }",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

