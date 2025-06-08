package com.st10345224.luminaledgerpoe.viewmodels // Ensure this package is correct

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.st10345224.luminaledgerpoe.Expense
import com.st10345224.luminaledgerpoe.ExpensesRepository
import com.st10345224.luminaledgerpoe.datamodels.DailyExpenseData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Calendar
import java.util.Date

class StatisticsViewModel(
    private val expensesRepository: ExpensesRepository
) : ViewModel() {

    private val _dailyExpenseData = MutableStateFlow<List<DailyExpenseData>>(emptyList())
    val dailyExpenseData: StateFlow<List<DailyExpenseData>> = _dailyExpenseData

    // Reintroduce startDate and endDate
    private val _startDate = MutableStateFlow(getDefaultStartDate())
    val startDate: StateFlow<Long> = _startDate

    private val _endDate = MutableStateFlow(getDefaultEndDate())
    val endDate: StateFlow<Long> = _endDate

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    init {
        // Combine startDate and endDate to trigger fetching when either changes
        viewModelScope.launch {
            combine(_startDate, _endDate) { start, end ->
                Pair(start, end)
            }.collectLatest { (start, end) ->
                fetchAndProcessExpenses(start, end)
            }
        }
    }

    fun updateStartDate(newDateMillis: Long) {
        _startDate.value = newDateMillis
    }

    fun updateEndDate(newDateMillis: Long) {
        _endDate.value = newDateMillis
    }

    private suspend fun fetchAndProcessExpenses(startMillis: Long, endMillis: Long) {
        _loading.value = true
        _errorMessage.value = null
        Log.d("StatisticsVM", "Fetching expenses for range: ${Date(startMillis)} to ${Date(endMillis)}")

        try {
            // Use the new function to get all user expenses
            expensesRepository.getAllExpensesForCurrentUser()
                .collect { allUserExpenses ->
                    Log.d("StatisticsVM", "Received ${allUserExpenses.size} expenses from repository. Processing for graph.")

                    // Client-side filtering for the selected date range
                    val filteredExpensesForPeriod = allUserExpenses.filter { expense ->
                        val expenseDate: Date? = expense.Date?.toDate()
                        if (expenseDate != null) {
                            // Ensure expenseDate is within the startMillis and endMillis
                            expenseDate.time >= startMillis && expenseDate.time <= endMillis
                        } else {
                            false // Exclude expenses without a valid date
                        }
                    }

                    processDailyExpenseData(filteredExpensesForPeriod, startMillis, endMillis)
                    _loading.value = false
                }
        } catch (e: Exception) {
            _errorMessage.value = "Error fetching statistics: ${e.message}"
            _loading.value = false
            Log.e("StatisticsVM", "Error fetching statistics: ${e.message}", e)
        }
    }

    private fun processDailyExpenseData(expenses: List<Expense>, startMillis: Long, endMillis: Long) {
        val dailyTotals = mutableMapOf<Long, Double>() // Epoch millis (start of day) to total amount

        // Initialize all days in the range to 0
        val calendar = Calendar.getInstance().apply { timeInMillis = startMillis }
        val endCalendar = Calendar.getInstance().apply { timeInMillis = endMillis }

        while (calendar.timeInMillis <= endCalendar.timeInMillis) {
            // Normalize to start of day for consistent keys
            val startOfDayMillis = calendar.apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            dailyTotals[startOfDayMillis] = 0.0
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }

        expenses.forEach { expense ->
            val expenseDate: Date? = expense.Date?.toDate()
            if (expenseDate != null) {
                val expenseCalendar = Calendar.getInstance().apply { time = expenseDate }
                val startOfDayMillis = expenseCalendar.apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                dailyTotals[startOfDayMillis] = (dailyTotals[startOfDayMillis] ?: 0.0) + expense.exAmount
            }
        }

        val dataPoints = dailyTotals.entries.sortedBy { it.key }.map { (dateMillis, total) ->
            DailyExpenseData(date = dateMillis, totalAmount = total)
        }

        Log.d("StatisticsVM", "Processed daily expense data: ${dataPoints.size} points. First point: ${dataPoints.firstOrNull()}, Last point: ${dataPoints.lastOrNull()}")
        _dailyExpenseData.value = dataPoints
    }

    // Helper functions for default start and end dates (e.g., last 30 days)
    private fun getDefaultEndDate(): Long {
        val calendar = Calendar.getInstance()
        // Set to end of current day
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        return calendar.timeInMillis
    }

    private fun getDefaultStartDate(): Long {
        val calendar = Calendar.getInstance()
        // Set to start of day 30 days ago
        calendar.add(Calendar.DAY_OF_YEAR, -30) // Last 30 days
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
}

// ViewModel Factory for StatisticsViewModel remains largely the same
object StatisticsViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        if (modelClass.isAssignableFrom(StatisticsViewModel::class.java)) {
            val firestore = FirebaseFirestore.getInstance()
            val auth = FirebaseAuth.getInstance()
            val expensesRepository = ExpensesRepository(firestore, auth)
            @Suppress("UNCHECKED_CAST")
            return StatisticsViewModel(expensesRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}