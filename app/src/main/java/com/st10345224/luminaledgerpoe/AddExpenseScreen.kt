package com.st10345224.luminaledgerpoe

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.KeyboardType
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@Composable
fun AddExpenseScreen(onExpenseAdded: (Expense) -> Unit) {
    // State to hold the title of the expense
    var expenseTitle by remember { mutableStateOf("") }
    // State to hold the selected category of the expense
    var expenseCategory by remember { mutableStateOf("") }
    // State to hold the amount of the expense
    var expenseAmount by remember { mutableStateOf("") }
    // State to hold the description of the expense (optional)
    var expenseDescription by remember { mutableStateOf("") }
    // State to hold the URI of the image selected from the gallery
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    // State to hold the URI of the image captured by the camera
    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }
    // State to hold the Bitmap of the selected/captured image
    var imageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    // State to hold the list of available categories fetched from Firestore
    var categories by remember { mutableStateOf<List<String>>(emptyList()) }
    // State to hold the selected currency for the expense (defaulting to ZAR)
    var selectedCurrency by remember { mutableStateOf("ZAR") }
    // State to control the expansion of the category dropdown
    // State to control the expansion of the currency dropdown
    // Context of the current composable
    val context = LocalContext.current
    // Instance of Firebase Firestore
    val firestore = FirebaseFirestore.getInstance()
    // Instance of Firebase Authentication
    val auth = FirebaseAuth.getInstance()
    // Get the current user's ID, or an empty string if no user is logged in
    val userId = auth.currentUser?.uid ?: ""
    // Coroutine scope for asynchronous operations
    val coroutineScope = rememberCoroutineScope()
    // List of available currency options
    val currencies = remember { listOf("ZAR", "USD", "EUR", "GBP") }

    // Function to create a temporary file for saving camera captured images
    fun createImageFile(context: Context): Uri? {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "JPEG_" + timeStamp + "_"
        val storageDir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES)
        return try {
            val imageFile = File.createTempFile(
                imageFileName, /* prefix */
                ".jpg", /* suffix (extension) */
                storageDir      /* directory */
            )
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", imageFile)
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    // Activity result launcher for taking a picture with the camera
    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            // If the picture was taken successfully, load the image into the imageBitmap
            cameraImageUri?.let { uri ->
                try {
                    imageBitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                } catch (e: IOException) {
                    e.printStackTrace()
                    Toast.makeText(context, "Failed to load image from camera", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Activity result launcher for requesting camera permission
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // If camera permission is granted, create a file and launch the camera intent
            val uri = createImageFile(context)
            cameraImageUri = uri
            uri?.let { takePicture.launch(it) }
        } else {
            Toast.makeText(context, "Camera permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    // Activity result launcher for selecting an image from the gallery
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        // If an image URI is returned from the gallery, update the selectedImageUri
        uri?.let { selectedImageUri = it }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Background image
        Image(
            painter = painterResource(id = R.drawable.splashbackground), // Replace with your image resource
            contentDescription = null, // Decorative image, no need for description
            contentScale = ContentScale.Crop, // Or ContentScale.FillBounds, etc.
            modifier = Modifier.fillMaxSize()
        )
        // Load the selected image from URI into Bitmap whenever selectedImageUri changes
        LaunchedEffect(selectedImageUri) {
            selectedImageUri?.let { uri ->
                imageBitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            }
        }

        // Fetch categories from Firestore when the composable is launched
        LaunchedEffect(key1 = true) {
            firestore.collection("Category")
                .get()
                .addOnSuccessListener { querySnapshot ->
                    // Map the documents to a list of category names
                    val categoryList = querySnapshot.documents.mapNotNull { it.getString("name") }
                    categories = categoryList
                }
                .addOnFailureListener { e ->
                    Log.e("AddExpenseScreen", "Error fetching categories: ${e.message}")
                    Toast.makeText(context, "Failed to load categories.", Toast.LENGTH_SHORT).show()
                }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Title of the screen
            Text(
                "Add New Expense",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Input field for the expense title
            OutlinedTextField(
                value = expenseTitle,
                onValueChange = { expenseTitle = it },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Category Dropdown Composable
            CategoryDropdown(
                categories = categories,
                selectedCategory = expenseCategory,
                onCategorySelected = { category -> expenseCategory = category }
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Currency Dropdown Composable
            CurrencyDropdown(
                currencies = currencies,
                selectedCurrency = selectedCurrency,
                onCurrencySelected = { currency -> selectedCurrency = currency }
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Input field for the expense amount
            OutlinedTextField(
                value = expenseAmount,
                onValueChange = { expenseAmount = it },
                label = { Text("Amount") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Input field for the expense description (optional)
            OutlinedTextField(
                value = expenseDescription,
                onValueChange = { expenseDescription = it },
                label = { Text("Description (Optional)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Display the selected image if available
            if (imageBitmap != null) {
                Image(
                    bitmap = imageBitmap!!.asImageBitmap(),
                    contentDescription = "Expense Photo",
                    modifier = Modifier
                        .size(120.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Buttons to select or take a photo
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Button(onClick = { galleryLauncher.launch("image/*") }) {
                    Text("Select Photo")
                }
                Button(onClick = {
                    // Create a URI for the new image file
                    val uri = createImageFile(context)
                    cameraImageUri = uri
                    // Check if camera permission is granted
                    if (ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.CAMERA
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        // If granted, launch the camera intent
                        uri?.let { takePicture.launch(it) }
                    } else {
                        // If not granted, request camera permission
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                }) {
                    Text("Take Photo")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Button to add the new expense
            Button(
                onClick = {
                    // Validate if required fields are filled
                    val amount = expenseAmount.toDoubleOrNull()
                    if (expenseTitle.isNotBlank() && expenseCategory.isNotBlank() && amount != null) {
                        // Convert the image Bitmap to a Base64 string for storage
                        val base64Image = imageBitmap?.let {
                            val compressedBytes = compressBitmap(it) // Assuming this function exists to compress the bitmap
                            encodeToBase64(compressedBytes)         // Assuming this function exists to encode bytes to Base64
                        } ?: ""
                        // Generate a unique ID for the expense
                        val exId = UUID.randomUUID().toString()
                        // Get the current timestamp
                        Date()
                        // Create a new Expense object
                        val newExpense = Expense(
                            exID = exId,
                            UserID = userId,
                            Category = expenseCategory,
                            exAmount = amount,
                            Date = Timestamp.now(), // Use Firebase Timestamp for date
                            exDescription = expenseDescription,
                            exPhotoString = base64Image,
                            Currency = selectedCurrency,
                            exTitle = expenseTitle
                        )
                        // Add the new expense to the "Expenses" collection in Firestore
                        coroutineScope.launch {
                            firestore.collection("Expenses")
                                .add(newExpense)
                                .addOnSuccessListener { documentReference ->
                                    Log.d(
                                        "AddExpenseScreen",
                                        "Expense added with ID: ${documentReference.id}"
                                    )
                                    Toast.makeText(
                                        context,
                                        "Expense added successfully!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    // Optionally clear input fields and image after successful addition
                                    expenseTitle = ""
                                    expenseCategory = ""
                                    expenseAmount = ""
                                    expenseDescription = ""
                                    selectedImageUri = null
                                    imageBitmap = null
                                }
                                .addOnFailureListener { e ->
                                    Log.e("AddExpenseScreen", "Error adding expense: ${e.message}")
                                    Toast.makeText(
                                        context,
                                        "Failed to add expense.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                        }
                        // Notify the parent composable that an expense has been added
                        onExpenseAdded(newExpense)
                    } else {
                        Toast.makeText(
                            context,
                            "Please fill in all required details.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Add Expense")
            }
        }
    }
}

// Composable for the category dropdown
@Composable
fun CategoryDropdown(
    categories: List<String>,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit
) {
    // State to control the expansion of the dropdown
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        // Button to open the dropdown
        Button(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
        ) {
            // Display the selected category or a default text if none is selected
            Text(if (selectedCategory.isNotEmpty()) selectedCategory else "Select Category")
        }
        // Dropdown menu that appears when expanded is true
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }, // Close the dropdown when clicked outside
            modifier = Modifier.fillMaxWidth()
        ) {
            // Iterate through the list of categories and create a MenuItem for each
            categories.forEach { category ->
                DropdownMenuItem(
                    text = { Text(category) }, // Display the category name
                    onClick = {
                        onCategorySelected(category) // Callback when a category is selected
                        expanded = false // Close the dropdown
                    }
                )
            }
        }
    }
}

// Composable for the currency dropdown
@Composable
fun CurrencyDropdown(
    currencies: List<String>,
    selectedCurrency: String,
    onCurrencySelected: (String) -> Unit
) {
    // State to control the expansion of the dropdown
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        // Button to open the dropdown
        Button(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
        ) {
            // Display the currently selected currency
            Text(selectedCurrency)
        }
        // Dropdown menu that appears when expanded is true
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }, // Close the dropdown when clicked outside
            modifier = Modifier.fillMaxWidth()
        ) {
            // Iterate through the list of currencies and create a MenuItem for each
            currencies.forEach { currency ->
                DropdownMenuItem(
                    text = { Text(currency) }, // Display the currency code
                    onClick = {
                        onCurrencySelected(currency) // Callback when a currency is selected
                        expanded = false // Close the dropdown
                    }
                )
            }
        }
    }
}