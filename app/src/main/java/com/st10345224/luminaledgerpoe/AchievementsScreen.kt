package com.st10345224.luminaledgerpoe

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel // Import for viewModel()
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AchievementsScreen(
    // Get the ViewModel instance using the factory
    viewModel: AchievementsViewModel = viewModel(factory = AchievementsViewModelFactory)
) {
    // Collect the StateFlow from the ViewModel as Compose State
    val userAchievements by viewModel.userAchievements.collectAsState()

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

        // --- Achievement Content Starts Here ---
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Achievements",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Show a loading indicator if achievements are not yet loaded
            if (userAchievements.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color.White)
                }
            } else {
                LazyColumn {
                    items(userAchievements) { userAchievement ->
                        AchievementItem(userAchievement = userAchievement)
                    }
                }
            }
        }
        // --- Achievement Content Ends Here ---
    }
}

@Composable
fun AchievementItem(userAchievement: UserAchievement) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.9f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Achievement Icon
            Image(
                painter = painterResource(id = userAchievement.definition.iconResId),
                contentDescription = userAchievement.definition.name,
                modifier = Modifier.size(48.dp),
                // Tint the icon grey if not unlocked, otherwise show original color
                colorFilter = if (userAchievement.isUnlocked) null else ColorFilter.tint(Color.Gray)
            )

            Spacer(modifier = Modifier.size(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = userAchievement.definition.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.Black
                )
                Text(
                    text = userAchievement.definition.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.DarkGray
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Display based on unlocked status
                if (userAchievement.isUnlocked) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Done,
                            contentDescription = "Unlocked",
                            tint = Color(0xFF4CAF50), // Green color
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.size(4.dp))
                        Text(
                            text = "Unlocked!",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color(0xFF4CAF50)
                        )
                        // Display unlock date if available
                        userAchievement.unlockedDate?.let { timestamp ->
                            val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                            val dateString = dateFormat.format(Date(timestamp.toDate().time))
                            Text(
                                text = " on $dateString",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                    }
                } else {
                    // Display progress for not-yet-unlocked achievements
                    Text(
                        text = "Progress: ${userAchievement.currentProgress} / ${userAchievement.definition.target}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.Gray
                    )
                    LinearProgressIndicator(
                        progress = userAchievement.currentProgress.toFloat() / userAchievement.definition.target.toFloat(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .padding(top = 4.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    )
                }
            }
        }
    }
}