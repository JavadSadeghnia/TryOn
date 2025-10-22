package com.tryon.virtualfit.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.rememberAsyncImagePainter
import com.tryon.virtualfit.R
import com.tryon.virtualfit.data.TryOnResult
import com.tryon.virtualfit.network.ApiConfig
import com.tryon.virtualfit.utils.FileUtils
import com.tryon.virtualfit.viewmodel.TryOnViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: TryOnViewModel) {
    val context = LocalContext.current
    val bodyImageUri by viewModel.bodyImageUri.collectAsState()
    val clothingImageUri by viewModel.clothingImageUri.collectAsState()
    val tryOnResult by viewModel.tryOnResult.collectAsState()
    val apiUrl by viewModel.apiUrl.collectAsState()

    var showApiDialog by remember { mutableStateOf(false) }
    var showImagePickerDialog by remember { mutableStateOf(false) }
    var showCategorySamplesDialog by remember { mutableStateOf(false) }
    var showErrorDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("") }
    var isSelectingBodyImage by remember { mutableStateOf(true) }

    // Camera URI for capturing photos
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }

    // Watch for errors and show dialog
    LaunchedEffect(tryOnResult) {
        if (tryOnResult is TryOnResult.Error) {
            errorMessage = (tryOnResult as TryOnResult.Error).message
            showErrorDialog = true
        }
    }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val cameraGranted = permissions[Manifest.permission.CAMERA] ?: false
        if (!cameraGranted) {
            Toast.makeText(context, "Camera permission required", Toast.LENGTH_SHORT).show()
        }
    }

    // Gallery picker
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            if (isSelectingBodyImage) {
                viewModel.setBodyImage(it)
            } else {
                viewModel.setClothingImage(it)
            }
        }
    }

    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            tempCameraUri?.let { uri ->
                if (isSelectingBodyImage) {
                    viewModel.setBodyImage(uri)
                } else {
                    viewModel.setClothingImage(uri)
                }
            }
        }
    }

    // Check permissions
    fun checkAndRequestPermissions() {
        val permissions = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.CAMERA)
        }

        if (permissions.isNotEmpty()) {
            permissionLauncher.launch(permissions.toTypedArray())
        }
    }

    // Launch camera
    fun launchCamera() {
        checkAndRequestPermissions()
        val photoFile = FileUtils.createImageFile(context)
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            photoFile
        )
        tempCameraUri = uri
        cameraLauncher.launch(uri)
    }

    // API Dialog
    if (showApiDialog) {
        ApiUrlDialog(
            currentUrl = apiUrl,
            onDismiss = { showApiDialog = false },
            onSave = { url ->
                viewModel.setApiUrl(url)
                showApiDialog = false
                Toast.makeText(context, "API URL saved", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Image Picker Dialog
    if (showImagePickerDialog) {
        ImagePickerDialog(
            onDismiss = { showImagePickerDialog = false },
            onCamera = {
                showImagePickerDialog = false
                launchCamera()
            },
            onGallery = {
                showImagePickerDialog = false
                galleryLauncher.launch("image/*")
            }
        )
    }

    // Category Samples Dialog
    if (showCategorySamplesDialog) {
        CategorySamplesDialog(
            category = selectedCategory,
            onDismiss = { showCategorySamplesDialog = false },
            onSampleSelected = { resourceId ->
                showCategorySamplesDialog = false
                viewModel.setClothingImageFromResource(context, resourceId)
            }
        )
    }

    // Error Dialog
    if (showErrorDialog) {
        ErrorDialog(
            errorMessage = errorMessage,
            onDismiss = {
                showErrorDialog = false
                viewModel.resetResult()
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Virtual Try-On") },
                actions = {
                    // Only show settings if API configuration is allowed
                    if (ApiConfig.ALLOW_USER_API_CONFIG) {
                        IconButton(onClick = { showApiDialog = true }) {
                            Icon(Icons.Default.Settings, "Settings")
                        }
                    }
                }
            )
        }
    ) { padding ->
        // Check if we should show only the result
        val showOnlyResult = tryOnResult is TryOnResult.Success
        val isGenerating = tryOnResult is TryOnResult.Loading

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(10.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Show input sections only if not showing result
            if (!showOnlyResult) {
                // Body Image Upload
                ImageUploadCard(
                    title = "Body Photo",
                    imageUri = bodyImageUri,
                    onClick = {
                        isSelectingBodyImage = true
                        showImagePickerDialog = true
                    }
                )

                // Clothing Image Upload
                ImageUploadCard(
                    title = "Clothing Item",
                    imageUri = clothingImageUri,
                    onClick = {
                        isSelectingBodyImage = false
                        showImagePickerDialog = true
                    }
                )

                // Category Selector (always visible)
                ClothingCategorySelector(
                    onCategorySelected = { category ->
                        if (category == "Upload") {
                            isSelectingBodyImage = false
                            showImagePickerDialog = true
                        } else {
                            selectedCategory = category
                            showCategorySamplesDialog = true
                        }
                    }
                )

                // Generate Button
                Button(
                    onClick = { if (!isGenerating) viewModel.generateTryOn(context) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = (bodyImageUri != null && clothingImageUri != null && apiUrl.isNotEmpty() && !isGenerating)
                ) {
                    if (isGenerating) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "Generating...",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    } else {
                        Text(
                            stringResource(R.string.generate_tryon),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }

            // Result Display
            when (val result = tryOnResult) {
                is TryOnResult.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.padding(32.dp))
                    Text("Generating try-on image...")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "This may take 3-5 minutes (using free CPU)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                is TryOnResult.Success -> {
                    ResultCard(
                        imageData = result.imageData,
                        onShare = {
                            shareImage(context, result.imageData)
                        },
                        onSave = {
                            saveImage(context, result.imageData)
                        },
                        onClose = {
                            viewModel.resetResult()
                        }
                    )
                }
                else -> {}
            }
        }
    }
}

@Composable
fun ImageUploadCard(
    title: String,
    imageUri: Uri?,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (imageUri != null) {
                Image(
                    painter = rememberAsyncImagePainter(imageUri),
                    contentDescription = title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFE0E0E0)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Add image",
                        modifier = Modifier.size(48.dp),
                        tint = Color(0xFF9E9E9E)
                    )
                }
            }
        }
    }
}

@Composable
fun ResultCard(
    imageData: ByteArray,
    onShare: () -> Unit,
    onSave: () -> Unit,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Result Image
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Image(
                    painter = rememberAsyncImagePainter(imageData),
                    contentDescription = "Try-on result",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(500.dp),
                    contentScale = ContentScale.Fit
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Share Button
                Button(
                    onClick = onShare,
                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                ) {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = "Share",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Share")
                }

                // Save Button
                Button(
                    onClick = onSave,
                    modifier = Modifier.weight(1f).padding(start = 8.dp)
                ) {
                    Icon(
                        Icons.Default.Save,
                        contentDescription = "Save",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Close Button
            OutlinedButton(
                onClick = onClose,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Close",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Try Another")
            }
        }
    }
}

@Composable
fun ApiUrlDialog(
    currentUrl: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var url by remember { mutableStateOf(currentUrl) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("API Configuration") },
        text = {
            Column {
                Text("Enter your Gradio API URL:")
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("API URL") },
                    placeholder = { Text("https://xxxxx.gradio.live") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(url) },
                enabled = url.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ImagePickerDialog(
    onDismiss: () -> Unit,
    onCamera: () -> Unit,
    onGallery: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Image") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onCamera,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.take_photo))
                }
                Button(
                    onClick = onGallery,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.choose_from_gallery))
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
fun ClothingCategorySelector(
    onCategorySelected: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "No outfit image? Try these instead!",
                style = MaterialTheme.typography.titleMedium,
                color = Color.Black,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                CategoryButton(
                    label = "Jackets",
                    icon = "🧥",
                    onClick = { onCategorySelected("Jackets") }
                )
                CategoryButton(
                    label = "T-Shirts",
                    icon = "👕",
                    onClick = { onCategorySelected("T-Shirts") }
                )
                CategoryButton(
                    label = "Dress",
                    icon = "👗",
                    onClick = { onCategorySelected("Dress") }
                )
                CategoryButton(
                    label = "Shirts",
                    icon = "👔",
                    onClick = { onCategorySelected("Shirts") }
                )
                CategoryButton(
                    label = "Upload",
                    icon = "+",
                    onClick = { onCategorySelected("Upload") }
                )
            }
        }
    }
}

@Composable
fun CategoryButton(
    label: String,
    icon: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFE0E0E0)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = icon,
                style = MaterialTheme.typography.headlineMedium,
                color = Color.Black
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.Black
        )
    }
}

@Composable
fun CategorySamplesDialog(
    category: String,
    onDismiss: () -> Unit,
    onSampleSelected: (Int) -> Unit
) {
    val samples = when (category) {
        "Jackets" -> listOf(R.drawable.sample_jacket_1, R.drawable.sample_jacket_2)
        "T-Shirts" -> listOf(R.drawable.sample_tshirt_1, R.drawable.sample_tshirt_2)
        "Dress" -> listOf(R.drawable.sample_dress_1, R.drawable.sample_dress_2)
        "Shirts" -> listOf(R.drawable.sample_shirt_1, R.drawable.sample_shirt_2)
        else -> emptyList()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Select $category",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                samples.forEach { resourceId ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                            .clickable { onSampleSelected(resourceId) },
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Image(
                            painter = rememberAsyncImagePainter(resourceId),
                            contentDescription = category,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ErrorDialog(
    errorMessage: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Error",
                    tint = Color(0xFFC62828),
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "Error",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFC62828)
                )
            }
        },
        text = {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
            ) {
                Text(
                    errorMessage,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF424242)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFC62828)
                )
            ) {
                Text("Close", color = Color.White)
            }
        },
        dismissButton = {}
    )
}

/**
 * Share image using Android share intent
 */
private fun shareImage(context: android.content.Context, imageData: ByteArray) {
    try {
        // Save image to cache
        val file = java.io.File(context.cacheDir, "tryon_result_${System.currentTimeMillis()}.jpg")
        file.outputStream().use { it.write(imageData) }

        // Get URI using FileProvider
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        // Create share intent
        val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Try-On Result"))
    } catch (e: Exception) {
        Toast.makeText(context, "Failed to share image: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

/**
 * Save image to device gallery
 */
private fun saveImage(context: android.content.Context, imageData: ByteArray) {
    try {
        val contentValues = android.content.ContentValues().apply {
            put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, "TryOn_${System.currentTimeMillis()}.jpg")
            put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                put(android.provider.MediaStore.Images.Media.RELATIVE_PATH, android.os.Environment.DIRECTORY_PICTURES + "/VirtualTryOn")
            }
        }

        val uri = context.contentResolver.insert(
            android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        )

        uri?.let {
            context.contentResolver.openOutputStream(it)?.use { outputStream ->
                outputStream.write(imageData)
            }
            Toast.makeText(context, "Image saved to gallery!", Toast.LENGTH_SHORT).show()
        } ?: run {
            Toast.makeText(context, "Failed to save image", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Failed to save image: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}
