package com.st10345224.luminaledgerpoe

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await


@Composable
fun CategoriesScreen() {
    // State to hold the list of categories
    val categories = remember { mutableStateListOf<Category>() }
    // Coroutine scope for asynchronous operations
    val coroutineScope = rememberCoroutineScope()
    // Instance of Firebase Firestore
    val firestore = FirebaseFirestore.getInstance()
    // State to track loading state
    val loading = remember { mutableStateOf(true) }
    // State to hold any error messages
    val error = remember { mutableStateOf<String?>(null) }
    // State to control the visibility of the add category dialog
    var showDialog by remember { mutableStateOf(false) }
    // State to hold the name of the new category being added
    var newCategoryName by remember { mutableStateOf("") }
    // State to hold the selected icon for the new category
    var selectedIcon by rememberSaveable { mutableStateOf<String?>(null) }
    // State to control the expansion of the icon selection dropdown menu
    var isIconMenuExpanded by remember { mutableStateOf(false) }


    Box(modifier = Modifier.fillMaxSize()) {
        // Background image
        Image(
            painter = painterResource(id = R.drawable.splashbackground),
            contentDescription = null, // Decorative image, no need for description
            contentScale = ContentScale.Crop, // Or ContentScale.FillBounds, etc.
            modifier = Modifier.fillMaxSize()
        )

        // Fetch categories from Firestore when the composable is launched
        LaunchedEffect(coroutineScope) {
            coroutineScope.launch {
                try {
                    // Get all documents from the "Category" collection
                    val categoriesSnapshot =
                        firestore.collection("Category").get().await() // Corrected collection name
                    // Map each document to a Category data class
                    val fetchedCategories = categoriesSnapshot.documents.map { document ->
                        Category(
                            categoryId = document.id,
                            name = document.getString("name") ?: "",
                            categoryIcon = document.getString("categoryIcon"),

                            )
                    }
                    // Clear the existing list and add the fetched categories
                    categories.clear()
                    categories.addAll(fetchedCategories)
                    loading.value = false // Indicate that loading is complete
                } catch (e: Exception) {
                    error.value = "Error fetching categories: ${e.message}" // Store the error message
                    loading.value = false // Indicate loading failed
                }
            }
        }

        // Function to delete a category by its ID
        fun deleteCategory(categoryId: String) {
            coroutineScope.launch {
                try {
                    // Delete the document with the given ID from the "Category" collection
                    firestore.collection("Category").document(categoryId).delete()
                        .await() // Corrected collection name
                    // Update the local list of categories after successful deletion
                    categories.removeIf { it.categoryId == categoryId }
                } catch (e: Exception) {
                    error.value = "Error deleting category: ${e.message}" // Store the error message
                }
            }
        }

        // Function to add a new category to Firestore
        fun addCategory(name: String, icon: String?) {
            coroutineScope.launch {
                try {
                    // Check if a category with the same name already exists (case-insensitive)
                    val existingCategory =
                        categories.find { it.name.equals(name, ignoreCase = true) }
                    if (existingCategory != null) {
                        error.value = "A category with this name already exists." // Set error if name exists
                        return@launch
                    }

                    // Get a reference to a new document in the "Category" collection
                    val newCategoryRef =
                        firestore.collection("Category").document() //get document reference
                    // Create a new Category object with the provided name and icon
                    val newCategory = Category(  //use the data class
                        categoryId = newCategoryRef.id,
                        name = name,
                        categoryIcon = icon
                    )

                    // Set the data for the new document using the Category object
                    newCategoryRef.set(newCategory).await() //use set

                    // Add the new category to the local list
                    categories.add(newCategory) //add to the list
                    newCategoryName = "" // Reset the input field
                    selectedIcon = null // Reset the selected icon
                    showDialog = false // Dismiss the add category dialog
                } catch (e: Exception) {
                    error.value = "Error adding category: ${e.message}" // Store the error message
                }
            }
        }

        // List of available icons with their names and Material Icons
        val availableIcons = listOf(
            "Food" to Icons.Filled.ShoppingCart,
            "Housing" to Icons.Filled.Home,
            "Transport" to Icons.Filled.Place,
            "Entertainment" to Icons.Filled.PlayArrow,
            "Utilities" to Icons.Filled.Build,
            "Healthcare" to Icons.Filled.Favorite,
            "Personal" to Icons.Filled.Person,
            "Other" to Icons.Filled.Star,
            "Business" to Icons.Filled.Email
        )

        // Show loading indicator while data is being fetched
        if (loading.value) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(text = "Loading categories...")
            }
        } else if (error.value != null) {
            // Show error message if fetching failed
            Text(text = "Error: ${error.value}")
        } else {
            // Display the list of categories in a Column
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Title of the screen
                Text(
                    text = "Categories",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                // Iterate through the list of categories and display each in a Card
                categories.forEach { category ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Display the category name
                            Text(
                                text = category.name,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f) // Add weight to the text to take available space
                            )
                            // Display the category icon if it exists
                            if (category.categoryIcon != null) {
                                // Find the corresponding Material Icon for the category icon name
                                val icon =
                                    availableIcons.find { it.first == category.categoryIcon }?.second
                                if (icon != null) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = "Category Icon"
                                    )
                                }
                            }
                            // Button to delete the category
                            Button(onClick = { deleteCategory(category.categoryId) }) {
                                Text("Delete")
                            }
                        }
                    }
                }

                // Button to show the dialog for adding a new category
                Button(onClick = { showDialog = true }, modifier = Modifier.padding(top = 16.dp)) {
                    Text("Add Category")
                }
            }
        }
        // Dialog to add a new category
        if (showDialog) {
            Dialog(onDismissRequest = { showDialog = false }) {
                Surface(
                    modifier = Modifier
                        .wrapContentWidth()
                        .wrapContentHeight(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Title of the dialog
                        Text(
                            text = "Add New Category",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        // Text field to input the new category name
                        TextField(
                            value = newCategoryName,
                            onValueChange = { newCategoryName = it },
                            label = { Text("Category Name") },
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        // Row for icon selection
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Select Icon:", style = MaterialTheme.typography.bodyMedium)
                            // Box containing the icon selection button and dropdown
                            Box {
                                Button(onClick = { isIconMenuExpanded = true }) {
                                    Text(selectedIcon ?: "Select") // Display selected icon or "Select"
                                }
                                // Dropdown menu to choose an icon
                                DropdownMenu(
                                    expanded = isIconMenuExpanded,
                                    onDismissRequest = { isIconMenuExpanded = false }
                                ) {
                                    // Iterate through the available icons and create a DropdownMenuItem for each
                                    availableIcons.forEach { (name, icon) ->
                                        DropdownMenuItem(
                                            text = { Text(name) },
                                            onClick = {
                                                selectedIcon = name // Update the selected icon
                                                isIconMenuExpanded = false // Dismiss the dropdown
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = icon,
                                                    contentDescription = name // Content description for accessibility
                                                )
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        // Row for action buttons (Add and Cancel)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            // Button to add the new category
                            Button(
                                modifier = Modifier.padding(end = 8.dp),
                                onClick = {
                                    // Only add the category if the name is not blank
                                    if (newCategoryName.isNotBlank()) {
                                        addCategory(newCategoryName, selectedIcon)
                                    }
                                }) {
                                Text("Add")
                            }
                            // Button to cancel adding the category and dismiss the dialog
                            Button(onClick = { showDialog = false }) {
                                Text("Cancel")
                            }
                        }
                    }
                }
            }
        }
    }
}

