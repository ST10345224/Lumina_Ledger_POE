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
    val categories = remember { mutableStateListOf<Category>() }
    val coroutineScope = rememberCoroutineScope()
    val firestore = FirebaseFirestore.getInstance()
    val loading = remember { mutableStateOf(true) }
    val error = remember { mutableStateOf<String?>(null) }
    var showDialog by remember { mutableStateOf(false) }
    var newCategoryName by remember { mutableStateOf("") }
    var selectedIcon by rememberSaveable { mutableStateOf<String?>(null) }
    var isIconMenuExpanded by remember { mutableStateOf(false) }


    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.splashbackground),
            contentDescription = null, // Decorative image, no need for description
            contentScale = ContentScale.Crop, // Or ContentScale.FillBounds, etc.
            modifier = Modifier.fillMaxSize()
        )

        // Fetch categories from Firestore
        LaunchedEffect(coroutineScope) {
            coroutineScope.launch {
                try {
                    val categoriesSnapshot =
                        firestore.collection("Category").get().await() // Corrected collection name
                    val fetchedCategories = categoriesSnapshot.documents.map { document ->
                        Category(
                            categoryId = document.id,
                            name = document.getString("name") ?: "",
                            categoryIcon = document.getString("categoryIcon"),

                            )
                    }
                    categories.clear()
                    categories.addAll(fetchedCategories)
                    loading.value = false
                } catch (e: Exception) {
                    error.value = "Error fetching categories: ${e.message}"
                    loading.value = false
                }
            }
        }

        // Function to delete a category
        fun deleteCategory(categoryId: String) {
            coroutineScope.launch {
                try {
                    firestore.collection("Category").document(categoryId).delete()
                        .await() // Corrected collection name
                    // Update the list after successful deletion
                    categories.removeIf { it.categoryId == categoryId }
                } catch (e: Exception) {
                    error.value = "Error deleting category: ${e.message}"
                }
            }
        }

        // Function to add a new category
        fun addCategory(name: String, icon: String?) {
            coroutineScope.launch {
                try {
                    // Check if a category with the same name already exists
                    val existingCategory =
                        categories.find { it.name.equals(name, ignoreCase = true) }
                    if (existingCategory != null) {
                        error.value = "A category with this name already exists."
                        return@launch
                    }

                    val newCategoryRef =
                        firestore.collection("Category").document() //get document reference
                    val newCategory = Category(  //use the data class
                        categoryId = newCategoryRef.id,
                        name = name,
                        categoryIcon = icon
                    )

                    newCategoryRef.set(newCategory).await() //use set

                    categories.add(newCategory) //add to the list
                    newCategoryName = "" // Reset the input field
                    selectedIcon = null
                    showDialog = false
                } catch (e: Exception) {
                    error.value = "Error adding category: ${e.message}"
                }
            }
        }

        // List of available icons
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

        // Show loading indicator
        if (loading.value) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(text = "Loading categories...")
            }
        } else if (error.value != null) {
            // Show error message
            Text(text = "Error: ${error.value}")
        } else {
            // Display the list of categories
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Categories",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
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
                            Text(
                                text = category.name,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f) // Add weight to the text
                            )
                            if (category.categoryIcon != null) {
                                val icon =
                                    availableIcons.find { it.first == category.categoryIcon }?.second
                                if (icon != null) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = "Category Icon"
                                    )
                                }
                            }
                            Button(onClick = { deleteCategory(category.categoryId) }) {
                                Text("Delete")
                            }
                        }
                    }
                }

                // Button to add a new category
                Button(onClick = { showDialog = true }, modifier = Modifier.padding(top = 16.dp)) {
                    Text("Add Category")
                }
            }
        }
        //dialog to add category
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
                        Text(
                            text = "Add New Category",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        TextField(
                            value = newCategoryName,
                            onValueChange = { newCategoryName = it },
                            label = { androidx.compose.material3.Text("Category Name") },
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        // Icon selection dropdown
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Select Icon:", style = MaterialTheme.typography.bodyMedium)
                            Box {
                                Button(onClick = { isIconMenuExpanded = true }) {
                                    Text(selectedIcon ?: "Select")
                                }
                                DropdownMenu(
                                    expanded = isIconMenuExpanded,
                                    onDismissRequest = { isIconMenuExpanded = false }
                                ) {
                                    availableIcons.forEach { (name, icon) ->
                                        DropdownMenuItem(
                                            text = { Text(name) },
                                            onClick = {
                                                selectedIcon = name
                                                isIconMenuExpanded = false
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = icon,
                                                    contentDescription = name
                                                )
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Button(
                                modifier = Modifier.padding(end = 8.dp),
                                onClick = {
                                    if (newCategoryName.isNotBlank()) {
                                        addCategory(newCategoryName, selectedIcon)
                                    }
                                }) {
                                Text("Add")
                            }
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

