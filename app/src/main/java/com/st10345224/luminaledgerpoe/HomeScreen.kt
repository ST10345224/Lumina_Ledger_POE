package com.st10345224.luminaledgerpoe

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HomeScreen() {
    val expenses = remember { mutableStateListOf<Expense>() }
    val goals = remember { mutableStateListOf<Goal>() }
    val coroutineScope = rememberCoroutineScope()
    val firestore = FirebaseFirestore.getInstance()
    val loading = remember { mutableStateOf(true) }
    val error = remember { mutableStateOf<String?>(null) }
    val selectedCurrency = remember { mutableStateOf("ZAR") }
    val scrollState = rememberScrollState() // Remember the scroll state

    // Currency conversion rates from ZAR to others (for demo)
    val exchangeRates = mapOf(
        "ZAR" to 1.0,
        "USD" to 0.055,
        "EUR" to 0.051
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.splashbackground),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        LaunchedEffect(Unit) {
            coroutineScope.launch {
                try {
                    val currentYearMonth = YearMonth.now()
                    val expensesSnapshot = firestore.collection("Expenses").get().await()
                    val fetchedExpenses = expensesSnapshot.documents.mapNotNull { doc ->
                        try {
                            Expense(
                                exID = doc.getString("exID") ?: "",
                                UserID = doc.getString("userID") ?: "",
                                Category = doc.getString("category") ?: "",
                                exAmount = doc.getDouble("exAmount") ?: 0.0,
                                Date = doc.get("Date", Timestamp::class.java) ?: Timestamp.now(),
                                exDescription = doc.getString("exDescription") ?: "",
                                exPhotoString = doc.getString("exPhotoString") ?: "",
                                Currency = doc.getString("currency") ?: "",
                                exTitle = doc.getString("exTitle") ?: ""
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }.filter {
                        val expenseYearMonth = LocalDateTime.ofInstant(
                            it.Date.toDate().toInstant(), ZoneId.systemDefault()
                        ).toLocalDate().let { date -> YearMonth.from(date) }
                        expenseYearMonth == currentYearMonth
                    }
                    expenses.clear()
                    expenses.addAll(fetchedExpenses)

                    val goalsSnapshot = firestore.collection("Goals").get().await()
                    val fetchedGoals = goalsSnapshot.documents.mapNotNull { doc ->
                        val yearMonthString = doc.getString("yearMonth") ?: ""
                        val yearMonth = try {
                            if (yearMonthString.isNotEmpty()) {
                                YearMonth.parse(
                                    yearMonthString,
                                    DateTimeFormatter.ofPattern(buildString {
                                        append("MMMMisPartOfTheYear")
                                    }, Locale.getDefault())
                                )
                            } else YearMonth.now()
                        } catch (e: Exception) {
                            YearMonth.now()
                        }
                        Goal(
                            yearMonth = yearMonth,
                            minimumSpendingGoal = doc.getDouble("minimumSpendingGoal"),
                            maximumSpendingGoal = doc.getDouble("maximumSpendingGoal")
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

        when {
            loading.value -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(text = "Loading data...")
                }
            }
            error.value != null -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(text = "Error: ${error.value}")
                }
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(scrollState), // Apply vertical scroll to the entire screen
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 🔻 Currency selector dropdown
                    var expanded by remember { mutableStateOf(false) }
                    Box {
                        Button(onClick = { expanded = true }) {
                            Text("Currency: ${selectedCurrency.value}")
                        }
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            listOf("ZAR", "USD", "EUR").forEach { currency ->
                                DropdownMenuItem(
                                    text = { Text(currency) },
                                    onClick = {
                                        selectedCurrency.value = currency
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    // 🔁 Expense donut and goal cards
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Monthly Expenses",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            ExpenseDonutChart(
                                expenses = expenses,
                                selectedCurrency = selectedCurrency.value,
                                exchangeRates = exchangeRates
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                            .height(300.dp),
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxSize()
                        ) {
                            Text(
                                text = "Goals",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 16.dp)  // Increased bottom padding
                            )
                            if (goals.isNotEmpty()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.spacedBy(24.dp)  // Add space between items
                                ) {
                                    goals.forEach { goal ->
                                        GoalProgressBar(
                                            goal = goal,
                                            expenses = expenses,
                                            selectedCurrency = selectedCurrency.value,
                                            exchangeRates = exchangeRates
                                        )
                                    }
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
}

@Composable
fun ExpenseDonutChart(
    expenses: List<Expense>,
    selectedCurrency: String,
    exchangeRates: Map<String, Double>
) {
    val totalExpensesZAR = expenses.sumOf { it.exAmount }
    val rate = exchangeRates[selectedCurrency] ?: 1.0
    val totalExpenses = totalExpensesZAR * rate

    val expensesByCategory = expenses.groupBy { it.Category }.mapValues { it.value.sumOf { ex -> ex.exAmount } }
    val categoryColors = mapOf(
        "Food" to Color(0xFFE57373),
        "Housing" to Color(0xFFF06292),
        "Transport" to Color(0xFFBA68C8),
        "Entertainment" to Color(0xFF9575CD),
        "Utilities" to Color(0xFF7986CB),
        "Healthcare" to Color(0xFF64B5F6),
        "Personal" to Color(0xFF4FC3F7),
        "Business" to Color(0xFF4DD0E1)
    )

    val percentages = expensesByCategory.mapValues { it.value.toFloat() / totalExpensesZAR.toFloat() }
    val colorList = expensesByCategory.keys.map { categoryColors[it] ?: Color.Gray }

    var startAngle = 0f

    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.size(200.dp), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(200.dp)) {
                percentages.forEach { (category, percent) ->
                    val angle = percent * 360f
                    drawArc(
                        color = categoryColors[category] ?: Color.Gray,
                        startAngle = startAngle,
                        sweepAngle = angle,
                        useCenter = false,
                        style = Stroke(width = 80f, cap = StrokeCap.Butt)
                    )
                    startAngle += angle
                }
            }
        }

        Column(modifier = Modifier.padding(top = 16.dp)) {
            expensesByCategory.keys.forEachIndexed { index, category ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Canvas(modifier = Modifier.size(16.dp)) {
                        drawCircle(color = colorList[index])
                    }
                    Text(
                        text = category,
                        modifier = Modifier.padding(start = 4.dp),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            Text(
                text = "Total: $selectedCurrency ${String.format("%.2f", totalExpenses)}",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
fun GoalProgressBar(
    goal: Goal,
    expenses: List<Expense>,
    selectedCurrency: String,
    exchangeRates: Map<String, Double>
) {
    val rate = exchangeRates[selectedCurrency] ?: 1.0

    val monthlyExpenses = expenses.filter {
        YearMonth.from(
            LocalDateTime.ofInstant(it.Date.toDate().toInstant(), ZoneId.systemDefault()).toLocalDate()
        ) == goal.yearMonth
    }.sumOf { it.exAmount } * rate

    val maxGoal = (goal.maximumSpendingGoal ?: 0.0) * rate
    val progress = if (maxGoal > 0) (monthlyExpenses / maxGoal).coerceIn(0.0, 1.0) else 0.0

    val animatedProgress by animateFloatAsState(
        targetValue = progress.toFloat(),
        animationSpec = tween(1000)
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Goal for ${goal.yearMonth.format(DateTimeFormatter.ofPattern("MMMM", Locale.getDefault()))}",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)  // Added bottom padding
        )

        Spacer(modifier = Modifier.height(8.dp))  // Increased spacing

        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp),  // Made progress bar taller
            color = if (monthlyExpenses > maxGoal) Color.Red else MaterialTheme.colorScheme.primary,

        )

        Spacer(modifier = Modifier.height(8.dp))  // Added spacing after progress bar

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically  // Align text vertically
        ) {
            Text(
                "Spent: ${String.format("%.2f", monthlyExpenses)} $selectedCurrency",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                "Limit: ${String.format("%.2f", maxGoal)} $selectedCurrency",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}