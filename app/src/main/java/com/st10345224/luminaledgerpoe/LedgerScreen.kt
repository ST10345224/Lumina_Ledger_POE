package com.st10345224.luminaledgerpoe

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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

    // Currency conversion variables
    var selectedCurrency by remember { mutableStateOf("ZAR") }
    var showCurrencyDropdown by remember { mutableStateOf(false) }
    val currencies = remember { listOf("ZAR", "USD", "EUR", "GBP", "JPY", "CNY", "AUD") }
    val exchangeRates = remember {
        mapOf(
            "ZAR" to 1.0,
            "USD" to 0.055,
            "EUR" to 0.051,
            "GBP" to 0.044,
            "JPY" to 8.33,
            "CNY" to 0.40,
            "AUD" to 0.083
        )
    }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = Instant.now().toEpochMilli(),
        yearRange = 2020..2030
    )

    suspend fun fetchExpenses(yearMonth: YearMonth) {
        loading.value = true
        try {
            val snapshot = firestore.collection("Expenses").get().await()
            val allExpenses = snapshot.documents.mapNotNull {
                try {
                    Expense(
                        exID = it.getString("exID") ?: "",
                        UserID = it.getString("userID") ?: "",
                        Category = it.getString("category") ?: "Uncategorized",
                        exAmount = it.getDouble("exAmount") ?: 0.0,
                        Date = it.get("Date", Timestamp::class.java) ?: Timestamp.now(),
                        exDescription = it.getString("exDescription") ?: "",
                        exPhotoString = it.getString("exPhotoString") ?: "",
                        Currency = it.getString("currency") ?: "ZAR",
                        exTitle = it.getString("exTitle") ?: "Expense"
                    )
                } catch (e: Exception) {
                    null
                }
            }
            val filtered = allExpenses.filter {
                YearMonth.from(
                    LocalDateTime.ofInstant(it.Date.toDate().toInstant(), ZoneId.systemDefault())
                ) == yearMonth
            }
            expenses.clear()
            expenses.addAll(filtered)
            loading.value = false
        } catch (e: Exception) {
            error.value = "Error fetching expenses: ${e.message}"
            loading.value = false
        }
    }

    // Function to convert expense amount to selected currency
    fun convertCurrency(amount: Double, fromCurrency: String, toCurrency: String): Double {
        if (fromCurrency == toCurrency) return amount
        val amountInZAR = amount / (exchangeRates[fromCurrency] ?: 1.0)
        return amountInZAR * (exchangeRates[toCurrency] ?: 1.0)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Background with overlay
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
                    .background(Color.Black.copy(alpha = 0.3f))
            )
        }

        LaunchedEffect(selectedYearMonth) {
            fetchExpenses(selectedYearMonth)
        }

        if (showDatePicker) {
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    Button(
                        onClick = {
                            datePickerState.selectedDateMillis?.let {
                                selectedYearMonth = YearMonth.from(
                                    Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault())
                                )
                            }
                            showDatePicker = false
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text("Select")
                    }
                },
                dismissButton = {
                    Button(
                        onClick = { showDatePicker = false },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Text("Cancel")
                    }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }

        Column(modifier = Modifier.fillMaxSize()) {
            // Header with month selector and currency selector
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                elevation = CardDefaults.cardElevation(4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = selectedYearMonth.format(
                                DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())
                            ),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box {
                                Button(
                                    onClick = { showCurrencyDropdown = true },
                                    modifier = Modifier.height(40.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                ) {
                                    Text(
                                        text = "$",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                DropdownMenu(
                                    expanded = showCurrencyDropdown,
                                    onDismissRequest = { showCurrencyDropdown = false }
                                ) {
                                    currencies.forEach { currency ->
                                        DropdownMenuItem(
                                            text = { Text(currency) },
                                            onClick = {
                                                selectedCurrency = currency
                                                showCurrencyDropdown = false
                                            }
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp)) // Add spacing here
                            Button(
                                onClick = { showDatePicker = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                Text("Change Month")
                            }
                        }
                    }
                }
            }

            when {
                loading.value -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                error.value != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Error",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = error.value.orEmpty(),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                expenses.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No expenses for selected month",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(expenses) { expense ->
                            ExpenseCard(
                                expense = expense,
                                selectedCurrency = selectedCurrency,
                                convertCurrency = { amount, fromCurrency ->
                                    convertCurrency(amount, fromCurrency, selectedCurrency)
                                }
                            )
                        }
                    }

                    // Total expenses footer
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        elevation = CardDefaults.cardElevation(4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Total:",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "$selectedCurrency ${
                                    String.format(
                                        "%.2f",
                                        expenses.sumOf {
                                            convertCurrency(it.exAmount, it.Currency, selectedCurrency)
                                        }
                                    )
                                }",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ExpenseCard(
    expense: Expense,
    selectedCurrency: String,
    convertCurrency: (Double, String) -> Double
) {
    var expanded by remember { mutableStateOf(false) }
    var bitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var imageLoadError by remember { mutableStateOf(false) }

    LaunchedEffect(expense.exPhotoString) {
        if (expense.exPhotoString.isNotBlank()) {
            try {
                val decoded = Base64.decode(expense.exPhotoString, Base64.DEFAULT)
                val androidBitmap = BitmapFactory.decodeByteArray(decoded, 0, decoded.size)
                bitmap = androidBitmap?.asImageBitmap()
                imageLoadError = false
            } catch (e: Exception) {
                imageLoadError = true
                bitmap = null
            }
        } else {
            bitmap = null
            imageLoadError = false
        }
    }

    // Convert expense amount to selected currency
    val convertedAmount = convertCurrency(expense.exAmount, expense.Currency)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = expense.exTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = formatTimestamp(expense.Date),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        text = expense.Category,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Text(
                    text = "$selectedCurrency ${String.format("%.2f", convertedAmount)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (convertedAmount < 0) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurface
                )
            }

            // Original currency if different
            if (expense.Currency != selectedCurrency) {
                Text(
                    text = "Original: ${expense.Currency} ${String.format("%.2f", expense.exAmount)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.align(Alignment.End)
                )
            }

            // Expandable content
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    if (expense.exDescription.isNotBlank()) {
                        Text(
                            text = expense.exDescription,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    if (expense.exPhotoString.isNotBlank()) {
                        if (imageLoadError) {
                            Text(
                                text = "Could not load image",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        } else if (bitmap != null) {
                            Image(
                                bitmap = bitmap!!,
                                contentDescription = "Expense receipt",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .clip(MaterialTheme.shapes.medium)
                            )
                        } else {
                            // You might want to show a loading indicator here if needed
                            Text(
                                text = "Loading receipt...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    } else {
                        Text(
                            text = "No receipt attached",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}

fun formatTimestamp(timestamp: Timestamp): String {
    return DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm", Locale.getDefault())
        .format(timestamp.toDate().toInstant().atZone(ZoneId.systemDefault()))
}