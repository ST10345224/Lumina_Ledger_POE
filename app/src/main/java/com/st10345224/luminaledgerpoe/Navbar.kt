package com.st10345224.luminaledgerpoe

import android.R
import android.R.attr.padding
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.st10345224.luminaledgerpoe.ui.screens.StatisticsScreen

// Screen Data Class
data class Screen(val route: String, val title: String, val icon: ImageVector)

// --- ADDED: Define a constant for your achievements route ---
object AppDestinations {
    const val ACHIEVEMENTS_ROUTE = "achievements"
    const val STATISTICS_ROUTE = "statistics"
}
// --- END ADDED ---

// Define the Screens that can be navigated to
val profileScreen = Screen("profile", "Profile", Icons.Filled.Person)
val homeScreen = Screen("home", "Home", Icons.Filled.Home)
val ledgerScreen = Screen("ledger", "Ledger", Icons.Filled.List)
val goalScreen = Screen("goals", "Goals", Icons.Filled.CheckCircle)
val newExpenseScreen = Screen("newExpense", "New Expense", Icons.Filled.Add)
val addGoalScreen = Screen("addGoal", "Add Goal", Icons.Filled.Add)
val categoryScreen = Screen("category", "Category", Icons.Filled.Settings)

// List of Screens
val screens = listOf(profileScreen, homeScreen, categoryScreen, ledgerScreen, goalScreen, newExpenseScreen)

// Main App Composable with Navigation logic
@Composable
fun NavigationMap() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = { BottomAppNavigationBar(navController = navController) } // Set the bottom navigation bar
    ) { paddingValues ->  // Padding for the content area
        // Navigation Host to manage different screens
        NavHost(
            navController = navController,
            startDestination = homeScreen.route, // Initial screen
            modifier = Modifier.padding(paddingValues)  // Apply padding to the content
        ) {
            composable(route = profileScreen.route) { ProfileScreen() } // Define Profile screen route
            composable(route = homeScreen.route) { HomeScreen(navController = navController) } // Define Home screen route
            composable(route = categoryScreen.route) { CategoriesScreen() } // Define Categories screen route
            composable(route = ledgerScreen.route) { ledgerScreen() } // Define Ledger screen route
            composable(route = goalScreen.route) {
                GoalsScreen(onAddGoal = { // Navigate to add goal screen
                    navController.navigate(addGoalScreen.route)
                })
            }
            composable(route = newExpenseScreen.route) {
                AddExpenseScreen (onExpenseAdded = { // Handle expense creation
                    navController.navigate(ledgerScreen.route) { // Go back to ledger
                        popUpTo(newExpenseScreen.route) { inclusive = true } // Clear new expense screen from back stack
                    }
                    Toast.makeText(
                        navController.context,
                        "Expense created!",
                        Toast.LENGTH_SHORT
                    ).show()
                })
            }
            composable(route = addGoalScreen.route){ // Route for adding a new goal
                AddGoalScreen(onGoalAdded = { // Handle goal creation
                    navController.navigate(goalScreen.route){ // Go back to goals screen
                        popUpTo(addGoalScreen.route){inclusive = true} // Clear add goal screen from back stack
                    }
                    Toast.makeText(
                        navController.context,
                        "Goal Added",
                        Toast.LENGTH_SHORT
                    ).show()
                })
            }
            composable(route = AppDestinations.ACHIEVEMENTS_ROUTE) {
                AchievementsScreen()
            }
            composable(route = AppDestinations.STATISTICS_ROUTE) {
                StatisticsScreen()
            }

        }
    }
}

// Bottom Navigation Bar Composable
@Composable
fun BottomAppNavigationBar(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar(
        containerColor = Color(0xFFFFFFFF),
        tonalElevation = 8.dp,
        modifier = Modifier
            .height(80.dp)
    ) {
        screens.forEach { screen ->
            NavigationBarItem(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 8.dp),
                icon = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.height(40.dp)
                    ) {
                        Icon(
                            imageVector = screen.icon,
                            contentDescription = screen.title,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                label = {
                    Text(
                        text = screen.title,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                selected = currentRoute == screen.route, // Highlight the currently selected item
                onClick = {
                    navController.navigate(screen.route) { // Navigate to the selected screen
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = false // Save state of the popped destinations
                        }
                        launchSingleTop = true // Avoid multiple instances of the same screen
                        restoreState = true // Restore state of the navigated screen
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                )
            )
        }
    }
}
// Screen Composables - put screen functions here

@Composable
fun ProfileScreen() {
    UserProfileScreen()
}

@Composable
fun Home() {
    Text("Home Screen Content")
}


@Composable
fun ledgerScreen() {
    LedgerScreen()
}

@Composable
fun GoalScreen() {
    GoalsScreen(onAddGoal = {})
}

