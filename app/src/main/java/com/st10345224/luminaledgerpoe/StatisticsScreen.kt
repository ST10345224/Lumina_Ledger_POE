package com.st10345224.luminaledgerpoe.ui.screens

import android.app.DatePickerDialog
import android.widget.DatePicker
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.st10345224.luminaledgerpoe.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.time.ZoneId
import java.time.temporal.ChronoUnit // For date difference

// Correct Imports for me.bytebeats.views.charts
import me.bytebeats.views.charts.line.LineChart
import me.bytebeats.views.charts.line.LineChartData
import me.bytebeats.views.charts.line.render.line.SolidLineDrawer
import me.bytebeats.views.charts.line.render.line.GradientLineShader as FillLineShader
import me.bytebeats.views.charts.line.render.point.FilledCircularPointDrawer
import me.bytebeats.views.charts.line.render.xaxis.SimpleXAxisDrawer
import me.bytebeats.views.charts.line.render.yaxis.SimpleYAxisDrawer
import me.bytebeats.views.charts.simpleChartAnimation

// Import your ViewModel and Factory from their actual package
import com.st10345224.luminaledgerpoe.viewmodels.StatisticsViewModel
import com.st10345224.luminaledgerpoe.viewmodels.StatisticsViewModelFactory


@Composable
fun StatisticsScreen(
    viewModel: StatisticsViewModel = viewModel(factory = StatisticsViewModelFactory)
) {
    val startDate by viewModel.startDate.collectAsState()
    val endDate by viewModel.endDate.collectAsState()
    val dailyExpenseData by viewModel.dailyExpenseData.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    val context = LocalContext.current

    val dateFormatter = SimpleDateFormat("MMM dd,yyyy", Locale.getDefault())
    val shortDateFormatter = SimpleDateFormat("MMM dd", Locale.getDefault()) // For X-axis labels

    Box(modifier = Modifier.fillMaxSize()) {
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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = "Expense Statistics",
                style = MaterialTheme.typography.headlineLarge,
                color = Color.Black,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "From:", color = Color.White, style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = dateFormatter.format(Date(startDate)),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .weight(1f)
                        .background(Color.White.copy(alpha = 0.8f), MaterialTheme.shapes.small)
                        .padding(8.dp)
                        .clickable {
                            val calendar = Calendar.getInstance()
                            calendar.timeInMillis = startDate
                            DatePickerDialog(
                                context,
                                { _: DatePicker, year: Int, month: Int, dayOfMonth: Int ->
                                    val selectedDate = Calendar.getInstance().apply {
                                        set(year, month, dayOfMonth, 0, 0, 0)
                                        set(Calendar.MILLISECOND, 0)
                                    }.timeInMillis
                                    viewModel.updateStartDate(selectedDate)
                                },
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH),
                                calendar.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        }
                )

                Spacer(modifier = Modifier.width(16.dp))

                Text(text = "To:", color = Color.White, style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = dateFormatter.format(Date(endDate)),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .weight(1f)
                        .background(Color.White.copy(alpha = 0.8f), MaterialTheme.shapes.small)
                        .padding(8.dp)
                        .clickable {
                            val calendar = Calendar.getInstance()
                            calendar.timeInMillis = endDate
                            DatePickerDialog(
                                context,
                                { _: DatePicker, year: Int, month: Int, dayOfMonth: Int ->
                                    val selectedDate = Calendar.getInstance().apply {
                                        set(year, month, dayOfMonth, 23, 59, 59)
                                        set(Calendar.MILLISECOND, 999)
                                    }.timeInMillis
                                    viewModel.updateEndDate(selectedDate)
                                },
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH),
                                calendar.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        }
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            // --- Line Graph Implementation with me.bytebeats.views.charts API ---
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                if (loading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                } else if (errorMessage != null) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Error: ${errorMessage!!}",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center
                        )
                    }
                } else if (dailyExpenseData.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No expense data for this period.",
                            color = Color.Gray,
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    // Calculate the number of days in the period
                    val startLocalDate = Calendar.getInstance().apply { timeInMillis = startDate }.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
                    val endLocalDate = Calendar.getInstance().apply { timeInMillis = endDate }.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
                    val totalDays = ChronoUnit.DAYS.between(startLocalDate, endLocalDate) + 1

                    // Determine the interval for showing X-axis labels
                    val maxLabels = 6 // Target maximum number of labels on X-axis
                    val labelInterval = if (totalDays > maxLabels) {
                        (totalDays / maxLabels).toInt().coerceAtLeast(1) // Ensure at least 1
                    } else {
                        1 // Show all labels if few data points
                    }

                    val lineChartPoints = dailyExpenseData.mapIndexed { index, data ->
                        val date = Date(data.date)
                        val label = if (index % labelInterval == 0 || index == dailyExpenseData.size - 1) {
                            shortDateFormatter.format(date) // Show label at interval or for the last point
                        } else {
                            "" // Hide label
                        }
                        LineChartData.Point(
                            value = data.totalAmount.toFloat(),
                            label = label
                        )
                    }

                    val lineChartData = LineChartData(
                        points = lineChartPoints,
                        startAtZero = true
                    )

                    LineChart(
                        lineChartData = lineChartData,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        animation = simpleChartAnimation(),
                        pointDrawer = FilledCircularPointDrawer(color = MaterialTheme.colorScheme.primary),
                        lineDrawer = SolidLineDrawer(
                            color = MaterialTheme.colorScheme.primary,
                            thickness = 3.dp
                        ),
                        lineShader = FillLineShader(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.0f)
                            )
                        ),
                        xAxisDrawer = SimpleXAxisDrawer(
                            labelTextColor = Color.Gray,
                            // Adjust label text size further if needed, 8.sp is often good for dense charts
                            labelTextSize = 9.sp
                        ),
                        yAxisDrawer = SimpleYAxisDrawer(
                            labelTextColor = Color.Gray,
                            labelTextSize = 9.sp, // Keep Y-axis labels consistent in size
                            labelValueFormatter = { value -> "R%.0f".format(value) }
                        ),
                        horizontalOffset = 5f
                    )
                }
            }

            // Display total expenses below the graph
            Spacer(modifier = Modifier.height(16.dp))
            val totalExpenses = dailyExpenseData.sumOf { it.totalAmount }
            Text(
                text = "Total Expenses for Period: R${"%.2f".format(totalExpenses)}",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}