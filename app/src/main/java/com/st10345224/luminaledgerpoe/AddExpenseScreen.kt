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
    var expenseTitle by remember { mutableStateOf("") }
    var expenseCategory by remember { mutableStateOf("") }
    var expenseAmount by remember { mutableStateOf("") }
    var expenseDescription by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }
    var imageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var categories by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedCurrency by remember { mutableStateOf("ZAR") } // Default currency
    var expandedCategory by remember { mutableStateOf(false) }
    var expandedCurrency by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val firestore = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val userId = auth.currentUser?.uid ?: ""
    val coroutineScope = rememberCoroutineScope()
    val currencies = remember { listOf("ZAR", "USD", "EUR", "GBP") }

    // Function to create a temporary file for camera intent
    fun createImageFile(context: Context): Uri? {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "JPEG_" + timeStamp + "_"
        val storageDir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES)
        return try {
            val imageFile = File.createTempFile(
                imageFileName, /* prefix */
                ".jpg", /* suffix */
                storageDir      /* directory */
            )
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", imageFile)
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    // Camera image launcher
    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
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

    // Permission launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            val uri = createImageFile(context)
            cameraImageUri = uri
            uri?.let { takePicture.launch(it) }
        } else {
            Toast.makeText(context, "Camera permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    // Gallery image launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { selectedImageUri = it }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.splashbackground), // Replace with your image resource
            contentDescription = null, // Decorative image, no need for description
            contentScale = ContentScale.Crop, // Or ContentScale.FillBounds, etc.
            modifier = Modifier.fillMaxSize()
        )
        LaunchedEffect(selectedImageUri) {
            selectedImageUri?.let { uri ->
                imageBitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            }
        }

        // Fetch categories from Firestore
        LaunchedEffect(key1 = true) {
            firestore.collection("Category")
                .get()
                .addOnSuccessListener { querySnapshot ->
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
            Text(
                "Add New Expense",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            OutlinedTextField(
                value = expenseTitle,
                onValueChange = { expenseTitle = it },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Category Dropdown
            CategoryDropdown(
                categories = categories,
                selectedCategory = expenseCategory,
                onCategorySelected = { category -> expenseCategory = category }
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Currency Dropdown
            CurrencyDropdown(
                currencies = currencies,
                selectedCurrency = selectedCurrency,
                onCurrencySelected = { currency -> selectedCurrency = currency }
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = expenseAmount,
                onValueChange = { expenseAmount = it },
                label = { Text("Amount") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = expenseDescription,
                onValueChange = { expenseDescription = it },
                label = { Text("Description (Optional)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Button(onClick = { galleryLauncher.launch("image/*") }) {
                    Text("Select Photo")
                }
                Button(onClick = {
                    val uri = createImageFile(context)
                    cameraImageUri = uri
                    if (ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.CAMERA
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        uri?.let { takePicture.launch(it) }
                    } else {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                }) {
                    Text("Take Photo")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    val amount = expenseAmount.toDoubleOrNull()
                    if (expenseTitle.isNotBlank() && expenseCategory.isNotBlank() && amount != null) {
                        val base64Image = imageBitmap?.let {
                            val compressedBytes = compressBitmap(it) // Assuming this function exists
                            encodeToBase64(compressedBytes)         // Assuming this function exists
                        } ?: ""
                        val exId = UUID.randomUUID().toString()
                        val currentDate = Date() // Get the current date and time
                        val newExpense = Expense(
                            exID = exId,
                            UserID = userId,
                            Category = expenseCategory,
                            exAmount = amount,
                            Date = Timestamp.now(), // Convert Date to String for simplicity
                            exDescription = expenseDescription,
                            exPhotoString = base64Image,
                            Currency = selectedCurrency,
                            exTitle = expenseTitle
                        )
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
                                    // Optionally clear fields after successful addition
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

// Category dropdown button
@Composable
fun CategoryDropdown(
    categories: List<String>,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Button(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
        ) {
            Text(if (selectedCategory.isNotEmpty()) selectedCategory else "Select Category")
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth()
        ) {
            categories.forEach { category ->
                DropdownMenuItem(
                    text = { Text(category) },
                    onClick = {
                        onCategorySelected(category)
                        expanded = false
                    }
                )
            }
        }
    }
}

// Currency dropdown button
@Composable
fun CurrencyDropdown(
    currencies: List<String>,
    selectedCurrency: String,
    onCurrencySelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Button(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
        ) {
            Text(selectedCurrency)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth()
        ) {
            currencies.forEach { currency ->
                DropdownMenuItem(
                    text = { Text(currency) },
                    onClick = {
                        onCurrencySelected(currency)
                        expanded = false
                    }
                )
            }
        }
    }
}