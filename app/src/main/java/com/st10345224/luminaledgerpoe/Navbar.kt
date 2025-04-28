package com.st10345224.luminaledgerpoe

import android.widget.Toast
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

// Define the Screen Data Class
data class Screen(val route: String, val title: String, val icon: ImageVector)

// Define the Screens that can be navigated to
val profileScreen = Screen("profile", "Profile", Icons.Filled.Person)
val homeScreen = Screen("feed", "Feed", Icons.Filled.Home)
val ledgerScreen = Screen("settings", "Settings", Icons.Filled.List)
val newExpenseScreen = Screen("newPost", "New Post", Icons.Filled.Add)

// Create the List of Screens
val screens = listOf(profileScreen, homeScreen, ledgerScreen, newExpenseScreen)
// Main App Composable with Navigation logic
@Composable
fun NavigationMap() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = { BottomNavigationBar(navController = navController) }
    ) { paddingValues ->  // Use paddingValues here
        // Navigation Host
        NavHost(
            navController = navController,
            startDestination = homeScreen.route, // Default destination
            modifier = Modifier.padding(paddingValues)  // Apply padding to NavHost
        ) {
            composable(route = profileScreen.route) { ProfileScreen() }
            composable(route = homeScreen.route) { FeedScreen() }
            composable(route = ledgerScreen.route) { SettingsScreen() } 
            composable(route = newExpenseScreen.route) {
                // Call CreatePostScreen and pass the callback
//                CreatePostScreen(onPostCreated = {
//                    // Define what happens after a post is created
//                    navController.navigate(feedScreen.route) { // Go back to feed
//                        popUpTo(newPostScreen.route) { inclusive = true } // Remove new post screen from backstack, otherwise it will stay open
//                    }
//                    // Optionally show a message
//                    Toast.makeText(
//                        navController.context,
//                        "Post created!",
//                        Toast.LENGTH_SHORT
//                    ).show()
//                })
            }
        }
    }
}

// Bottom Navigation Bar Composable
@Composable
fun BottomNavigationBar(navController: NavHostController) {
    NavigationBar { // Use NavigationBar instead of BottomNavigation
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        screens.forEach { screen ->
            NavigationBarItem( // Use NavigationBarItem instead of BottomNavigationItem
                icon = { Icon(imageVector = screen.icon, contentDescription = screen.title) },
                label = { Text(screen.title) },
                selected = currentRoute == screen.route,
                onClick = {
                    navController.navigate(screen.route) {
                        // Avoid multiple copies of the same destination when reselecting the same item
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                        // Restore state when reselecting a previously selected item
                        restoreState = true
                        //launchSingleTop = true
                    }
                }
            )
        }
    }
}

// Screen Composables - put screen functions here
@Composable
fun ProfileScreen() {
   // UserProfileScreen()
}

@Composable
fun FeedScreen() {
   // SocialMediaFeed()
}

// No settings screen yet, just displays message
@Composable
fun SettingsScreen() {
    Text("Settings Screen Content")
}
