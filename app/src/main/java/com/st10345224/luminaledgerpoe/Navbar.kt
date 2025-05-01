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

// Define the Screen Data Class
data class Screen(val route: String, val title: String, val icon: ImageVector)

// Define the Screens that can be navigated to
val profileScreen = Screen("profile", "Profile", Icons.Filled.Person)
val homeScreen = Screen("home", "Home", Icons.Filled.Home)
val ledgerScreen = Screen("ledger", "Ledger", Icons.Filled.List)
val goalScreen = Screen("goals", "Goals", Icons.Filled.CheckCircle)
val newExpenseScreen = Screen("newExpense", "New Expense", Icons.Filled.Add)
val addGoalScreen = Screen("addGoal", "Add Goal", Icons.Filled.Add)
val categoryScreen = Screen("category", "Category", Icons.Filled.Settings)

// Create the List of Screens
val screens = listOf(profileScreen, homeScreen, categoryScreen, ledgerScreen, goalScreen, newExpenseScreen)
// Main App Composable with Navigation logic
@Composable
fun NavigationMap() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = { BottomAppNavigationBar(navController = navController) }
    ) { paddingValues ->  // Use paddingValues here
        // Navigation Host
        NavHost(
            navController = navController,
            startDestination = homeScreen.route, // Default destination
            modifier = Modifier.padding(paddingValues)  // Apply padding to NavHost
        ) {
            composable(route = profileScreen.route) { ProfileScreen() }
            composable(route = homeScreen.route) { HomeScreen() }
            composable(route = categoryScreen.route) { CategoriesScreen() }
            composable(route = ledgerScreen.route) { ledgerScreen() }
            composable(route = goalScreen.route) {
                GoalsScreen(onAddGoal = {
                    navController.navigate(addGoalScreen.route) // Navigate to AddGoalScreen
                })
            }
            composable(route = newExpenseScreen.route) {
                // Call CreatePostScreen and pass the callback
                AddExpenseScreen (onExpenseAdded = {
                    // Define what happens after a post is created
                    navController.navigate(ledgerScreen.route) { // Go back to feed
                        popUpTo(newExpenseScreen.route) { inclusive = true } // Remove new post screen from backstack, otherwise it will stay open
                    }
                    // Optionally show a message
                    Toast.makeText(
                        navController.context,
                        "Expense created!",
                        Toast.LENGTH_SHORT
                    ).show()
                })
            }
            composable(route = addGoalScreen.route){ //Added route for AddGoalScreen
                AddGoalScreen(onGoalAdded = {
                    navController.navigate(goalScreen.route){
                        popUpTo(addGoalScreen.route){inclusive = true}
                    }
                    Toast.makeText(
                        navController.context,
                        "Goal Added",
                        Toast.LENGTH_SHORT
                    ).show()
                })
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
            .height(80.dp)  // Reduced height for better proportions
    ) {
        screens.forEach { screen ->
            NavigationBarItem(
                modifier = Modifier
                    .weight(1f)  // Equal weight for all items
                    .padding(vertical = 8.dp),
                icon = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.height(40.dp)  // Fixed height for icon area
                    ) {
                        Icon(
                            imageVector = screen.icon,
                            contentDescription = screen.title,
                            modifier = Modifier.size(24.dp)  // Standard icon size
                        )
                    }
                },
                label = {
                    Text(
                        text = screen.title,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,  // Slightly larger font
                        maxLines = 1,  // Prevent text from wrapping
                        overflow = TextOverflow.Ellipsis  // Add ellipsis if text is too long
                    )
                },
                selected = currentRoute == screen.route,
                onClick = {
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)  // Subtle selection indicator
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


