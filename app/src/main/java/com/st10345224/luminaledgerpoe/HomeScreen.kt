package com.st10345224.luminaledgerpoe

import android.util.Log
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.*
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.random.Random
import androidx.navigation.NavHostController
import com.st10345224.luminaledgerpoe.AchievementsScreen
import java.text.SimpleDateFormat
import java.util.Date

@Composable
fun HomeScreen(navController: NavHostController) {
    // State to hold the list of fetched expenses for the current month
    val expenses = remember { mutableStateListOf<Expense>() }
    // State to hold the list of fetched goals
    val goals = remember { mutableStateListOf<Goal>() }
    // Coroutine scope for asynchronous operations
    val coroutineScope = rememberCoroutineScope()
    // Instance of Firebase Authentication
    val auth = FirebaseAuth.getInstance()
    // Instance of Firebase Firestore
    // Get the current user's ID, or an empty string if no user is logged in
    val userId = auth.currentUser?.uid ?: ""
    val firestore = FirebaseFirestore.getInstance()
    // State to track the loading state of data fetching
    val loading = remember { mutableStateOf(true) }
    // State to hold any error message that occurs during data fetching
    val error = remember { mutableStateOf<String?>(null) }
    // State to hold the selected currency for display (defaulting to ZAR)
    val selectedCurrency = remember { mutableStateOf("ZAR") }
    // State to remember the scroll position of the screen
    val scrollState = rememberScrollState() // Remember the scroll state

    val exchangeRates = remember { mutableStateOf<Map<String, Double>>(emptyMap()) }
    val baseCurrency = "ZAR"

    // a single "First Expense" achievement
    val firstExpenseDefinition = AchievementDefinition(
        id = "first_expense",
        name = "First Expense!",
        description = "Record your very first expense.",
        iconResId = R.drawable.ic_trophy,
        target = 1

    )
    val userCurrentProgressForFirstExpense = 1 // Assuming user has made 1 expense
    val isCompleted = (userCurrentProgressForFirstExpense >= firstExpenseDefinition.target)


    val achievementsToDisplay = listOf(
        mapOf( // Using a map to combine definition with user-specific progress
            "definition" to firstExpenseDefinition,
            "userCurrentProgress" to userCurrentProgressForFirstExpense,
            "isCompleted" to isCompleted
        )
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // Background image
        Image(
            painter = painterResource(id = R.drawable.splashbackground),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Fetch expenses and goals when the composable is first created
        LaunchedEffect(Unit) {
            coroutineScope.launch {
                try {
                    // Fetch Exchange Rates FIRST
                    val fetchedRates = CurrencyApiService.getLatestConversionRates(baseCurrency)
                    if (fetchedRates != null) {
                        exchangeRates.value = fetchedRates
                        Log.d(
                            "CurrencyApi",
                            "Fetched Exchange Rates: ${exchangeRates.value}"
                        ) // For debugging
                    } else {
                        // Handle error in fetching rates, maybe set a default or show an error
                        error.value = "Failed to fetch exchange rates. Using default rates."
                        // Fallback to a hardcoded map if API fails
                        exchangeRates.value = mapOf(
                            "ZAR" to 1.0,
                            "USD" to 0.055,
                            "EUR" to 0.051
                        )
                        Log.d(
                            "CurrencyApi",
                            "Failed to fetch exchange rates. Using default rates."
                        ) // For debugging"Falling back to default rates due to API error.") // For debugging
                    }


                    // Get the current year and month
                    val currentYearMonth = YearMonth.now()
                    // Fetch expenses from Firestore
                    val expensesSnapshot = firestore.collection("Expenses").get().await()
                    // Map the documents to Expense objects and filter for the current month
                    val fetchedExpenses = expensesSnapshot.documents.mapNotNull { doc ->
                        try {
                            Expense(
                                exID = doc.getString("exID") ?: "",
                                UserID = doc.getString("userID") ?: "",
                                Category = doc.getString("category") ?: "",
                                exAmount = doc.getDouble("exAmount") ?: 0.0,
                                Date = doc.get("date", Timestamp::class.java) ?: Timestamp.now(),
                                exDescription = doc.getString("exDescription") ?: "",
                                exPhotoString = doc.getString("exPhotoString") ?: "",
                                Currency = doc.getString("currency") ?: "",
                                exTitle = doc.getString("exTitle") ?: ""
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }.filter { it.UserID == userId } // Filter expenses by user ID
                        .filter {
                            // Filter expenses to include only those from the current year and month
                            val expenseYearMonth = LocalDateTime.ofInstant(
                                it.Date.toDate().toInstant(), ZoneId.systemDefault()
                            ).toLocalDate().let { date -> YearMonth.from(date) }
                            expenseYearMonth == currentYearMonth
                        }
                    // Clear the existing expenses and add the fetched ones
                    expenses.clear()
                    expenses.addAll(fetchedExpenses)

                    // Fetch goals from Firestore
                    val goalsSnapshot = firestore.collection("Goals").get().await()
                    // Map the documents to Goal objects
                    val fetchedGoals = goalsSnapshot.documents.mapNotNull { doc ->
                        val yearMonthString = doc.getString("yearMonth") ?: ""
                        val yearMonth = try {
                            if (yearMonthString.isNotEmpty()) {
                                YearMonth.parse(
                                    yearMonthString,
                                    DateTimeFormatter.ofPattern(buildString {
                                        append("MMMM") // Month in full text
                                    }, Locale.getDefault())
                                )
                            } else YearMonth.now()
                        } catch (e: Exception) {
                            YearMonth.now()
                        }
                        Goal(
                            yearMonth = yearMonth,
                            minimumSpendingGoal = doc.getDouble("minimumSpendingGoal"),
                            maximumSpendingGoal = doc.getDouble("maximumSpendingGoal"),
                            userID = doc.getString("userId") ?: ""

                        )
                    }.filter { it.userID == userId } // Filter goals by user ID
                    // Clear the existing goals and add the fetched ones
                    goals.clear()
                    goals.addAll(fetchedGoals)

                    loading.value = false // Data fetching is complete
                } catch (e: Exception) {
                    error.value = "Error fetching data: ${e.message}" // Store the error message
                    loading.value = false // Indicate that loading failed
                }
            }
        }

        // Display different UI based on the loading and error states
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
                // Main content of the Home Screen
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(scrollState), // Enable vertical scrolling for the content
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
                            // List of available currencies to select from
                            listOf("ZAR", "USD", "EUR").forEach { currency ->
                                DropdownMenuItem(
                                    text = { Text(currency) },
                                    onClick = {
                                        selectedCurrency.value =
                                            currency // Update the selected currency
                                        expanded = false // Close the dropdown
                                    }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    // 🔁 Expense donut chart and goal cards
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                            .clickable {
                                navController.navigate(AppDestinations.STATISTICS_ROUTE)
                            },
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Monthly Expenses",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            // Display the expense donut chart
                            ExpenseDonutChart(
                                expenses = expenses,
                                selectedCurrency = selectedCurrency.value,
                                exchangeRates = exchangeRates
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    // Card displaying the user's goals for the current month
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                            .height(200.dp) // Set a fixed height for the goals card
                            .clickable {
                                navController.navigate(goalScreen.route)
                            },
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
                            // Display the progress for each goal
                            if (goals.isNotEmpty()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .verticalScroll(rememberScrollState()), // Enable scrolling within the goals section
                                    verticalArrangement = Arrangement.spacedBy(24.dp)  // Add space between goal progress bars
                                ) {
                                    goals.forEach { goal ->
                                        GoalProgressBar(
                                            goal = goal,
                                            expenses = expenses,
                                            selectedCurrency = selectedCurrency.value,
                                            exchangeRates = exchangeRates.value
                                        )
                                    }
                                }
                            } else {
                                Text(text = "No goals set for this month.")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))


                    // Card displaying the user's achievements
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                            .height(200.dp) // Set a fixed height for the goals card
                            .clickable {
                                navController.navigate(AppDestinations.ACHIEVEMENTS_ROUTE)
                            },
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {


                        Column(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxSize()
                        ) {
                            Text(
                                text = "Achievements",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 16.dp)  // Increased bottom padding
                            )

                            if (achievementsToDisplay.isEmpty()) {
                                // This case won't be hit with a hardcoded list unless you change it
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No achievements to display.",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = Color.White
                                    )
                                }
                            } else {
                                LazyColumn {
                                    items(achievementsToDisplay) { achievementData ->
                                        // Extract definition and user-specific progress from the map
                                        val definition =
                                            achievementData["definition"] as AchievementDefinition
                                        val userProgress =
                                            achievementData["userCurrentProgress"] as Int
                                        val itemIsCompleted =
                                            achievementData["isCompleted"] as Boolean

                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 8.dp),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                        ) {
                                            Column(modifier = Modifier.padding(16.dp)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    // Display Icon
                                                    Image(
                                                        painter = painterResource(id = definition.iconResId),
                                                        contentDescription = null, // Content description for accessibility
                                                        modifier = Modifier.size(40.dp), // Adjust size as needed
                                                        contentScale = ContentScale.Fit
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    // Achievement Name
                                                    Text(
                                                        text = definition.name,
                                                        style = MaterialTheme.typography.titleMedium
                                                    )
                                                }

                                                Spacer(modifier = Modifier.height(4.dp))
                                                // Achievement Description
                                                Text(
                                                    text = definition.description,
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                                Spacer(modifier = Modifier.height(8.dp))

                                                // Progress bar
                                                val progressFraction = if (definition.target > 0) {
                                                    userProgress.toFloat() / definition.target.toFloat()
                                                } else {
                                                    0f // Avoid division by zero
                                                }
                                                LinearProgressIndicator(
                                                    progress = progressFraction,
                                                    modifier = Modifier.fillMaxWidth(),
                                                    color = MaterialTheme.colorScheme.primary,
                                                    trackColor = MaterialTheme.colorScheme.onSurface.copy(
                                                        alpha = 0.2f
                                                    )
                                                )
                                                // Progress Text
                                                Text(
                                                    text = "${userProgress} / ${definition.target}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    modifier = Modifier.align(Alignment.End)
                                                )
                                                // Completion Status
                                                if (itemIsCompleted) {
                                                    Text(
                                                        text = "Completed!",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = Color.Green,
                                                        modifier = Modifier.align(Alignment.End)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


    // Donut chart composable
    @Composable
    fun ExpenseDonutChart(
        expenses: List<Expense>,
        selectedCurrency: String,
        exchangeRates: MutableState<Map<String, Double>>
    ) {
        // Calculate the total expenses in the base currency (ZAR)
        val totalExpensesZAR = expenses.sumOf { it.exAmount }
        // Get the exchange rate for the selected currency, defaulting to 1.0 if not found
        val rate = exchangeRates.value[selectedCurrency] ?: 1.0
        // Calculate the total expenses in the selected currency
        val totalExpenses = totalExpensesZAR * rate

        // Group expenses by their category and sum the amounts for each category
        val expensesByCategory =
            expenses.groupBy { it.Category }.mapValues { it.value.sumOf { ex -> ex.exAmount } }
        // Mutable map to store category colors. Initial colors are defined, and new ones will be added if needed.
        val categoryColors = remember {
            mutableMapOf(
                "Food" to Color(0xFFE57373),
//            "Housing" to Color(0xFFF06292),
//            "Transport" to Color(0xFFBA68C8),
//            "Entertainment" to Color(0xFF9575CD),
//            "Utilities" to Color(0xFF7986CB),
//            "Healthcare" to Color(0xFF64B5F6),
//            "Personal" to Color(0xFF4FC3F7),
//            "Business" to Color(0xFF4DD0E1)
            )
        }

        // Calculate the percentage of each category's spending relative to the total spending in ZAR
        val percentages =
            expensesByCategory.mapValues { it.value.toFloat() / totalExpensesZAR.toFloat() }

        // Create a list of colors for each category. If a category doesn't have a predefined color, generates a random one.
        val colorList = remember(expensesByCategory.keys) {
            expensesByCategory.keys.map { category ->
                // Get the color for the category from the map, or put a new random color if it's not present.
                categoryColors.getOrPut(category) {
                    Color(
                        Random.nextInt(256), // Random red value
                        Random.nextInt(256), // Random green value
                        Random.nextInt(256), // Random blue value
                        255 // Alpha value (fully opaque)
                    )
                }
            }
        }

        // Variable to keep track of the starting angle for each slice of the donut chart
        var startAngle = 0f

        // Column to center the chart and its legend horizontally
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Box to draw the donut chart on a Canvas
            Box(modifier = Modifier.size(200.dp), contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.size(200.dp)) {
                    // Iterate through the spending percentages for each category
                    percentages.forEach { (category, percent) ->
                        // Calculate the sweep angle for the current category based on its percentage
                        val angle = percent * 360f
                        // Draw an arc representing the category's share of the total expenses
                        drawArc(
                            color = categoryColors[category]
                                ?: Color.Gray, // Use the category's color or gray as a fallback
                            startAngle = startAngle, // Start angle of the arc
                            sweepAngle = angle, // Angle (size) of the arc
                            useCenter = false, // Don't connect the ends of the arc to the center
                            style = Stroke(
                                width = 80f,
                                cap = StrokeCap.Butt
                            ) // Style for the arc (hollow circle)
                        )
                        // Update the starting angle for the next category's arc
                        startAngle += angle
                    }
                }
            }

            // Column to display the legend (category names and their colors)
            Column(modifier = Modifier.padding(top = 16.dp)) {
                // Iterate through the categories and their corresponding colors
                expensesByCategory.keys.forEachIndexed { index, category ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Small colored circle representing the category
                        Canvas(modifier = Modifier.size(16.dp)) {
                            drawCircle(color = colorList[index])
                        }
                        // Text displaying the category name
                        Text(
                            text = category,
                            modifier = Modifier.padding(start = 4.dp),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                // Text displaying the total expenses in the selected currency
                Text(
                    text = "Total: $selectedCurrency ${String.format("%.2f", totalExpenses)}",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }

    // Progress bar composable
    @Composable
    fun GoalProgressBar(
        goal: Goal,
        expenses: List<Expense>,
        selectedCurrency: String,
        exchangeRates: Map<String, Double>
    ) {
        // Get the exchange rate for the selected currency
        val rate = exchangeRates[selectedCurrency] ?: 1.0

        // Filter expenses for the current goal's month and calculate the total spending in the selected currency
        val monthlyExpenses = expenses.filter {
            YearMonth.from(
                LocalDateTime.ofInstant(it.Date.toDate().toInstant(), ZoneId.systemDefault())
                    .toLocalDate()
            ) == goal.yearMonth
        }.sumOf { it.exAmount } * rate

        // Get the maximum spending goal in the selected currency
        val maxGoal = (goal.maximumSpendingGoal ?: 0.0) * rate
        // Calculate the progress towards the goal (clamped between 0 and 1)
        val progress = if (maxGoal > 0) (monthlyExpenses / maxGoal).coerceIn(0.0, 1.0) else 0.0

        // Animate the progress of the progress bar
        val animatedProgress by animateFloatAsState(
            targetValue = progress.toFloat(),
            animationSpec = tween(1000)
        )

        Column(modifier = Modifier.fillMaxWidth()) {
            // Display the month for which the goal is set
            Text(
                text = "Goal for ${
                    goal.yearMonth.format(
                        DateTimeFormatter.ofPattern(
                            "MMMM",
                            Locale.getDefault()
                        )
                    )
                }",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)  // Added bottom padding
            )

            Spacer(modifier = Modifier.height(8.dp))  // Increased spacing

            // Linear progress indicator to show the progress towards the goal
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp),  // Made progress bar taller
                // Set the color to red if the spending exceeds the maximum goal
                color = if (monthlyExpenses > maxGoal) Color.Red else MaterialTheme.colorScheme.primary,

                trackColor = MaterialTheme.colorScheme.surfaceVariant // Optional: Set a track color
            )

            Spacer(modifier = Modifier.height(8.dp))  // Added spacing after progress bar

            // Row to display the amount spent and the maximum limit
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically  // Align text vertically
            ) {
                // Text displaying the amount spent in the selected currency
                Text(
                    "Spent: ${String.format("%.2f", monthlyExpenses)} $selectedCurrency",
                    style = MaterialTheme.typography.bodyMedium
                )
                // Text displaying the maximum spending limit in the selected currency
                Text(
                    "Limit: ${String.format("%.2f", maxGoal)} $selectedCurrency",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }



