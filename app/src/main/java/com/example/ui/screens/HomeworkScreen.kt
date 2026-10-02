package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeworkScreen(
    userProfile: UserProfile,
    geminiService: GeminiService
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var questionText by remember { mutableStateOf("") }
    var selectedSubject by remember { mutableStateOf("Mathematics") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var imageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var imageBase64 by remember { mutableStateOf<String?>(null) }

    var isSolving by remember { mutableStateOf(false) }
    var solution by remember { mutableStateOf<HomeworkSolution?>(null) }
    var isGeneratingPractice by remember { mutableStateOf(false) }
    var practiceList by remember { mutableStateOf<List<PracticeQuestion>>(emptyList()) }
    var expandedPracticeId by remember { mutableStateOf<String?>(null) }

    // Image Picker Launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            imageUri = uri
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        val originalBitmap = BitmapFactory.decodeStream(inputStream)
                        if (originalBitmap != null) {
                            // Scale down to avoid huge memory/network payloads
                            val maxDimension = 1024
                            val width = originalBitmap.width
                            val height = originalBitmap.height
                            val scale = if (width > height) {
                                maxDimension.toFloat() / width
                            } else {
                                maxDimension.toFloat() / height
                            }.coerceAtMost(1f)

                            val scaledBitmap = Bitmap.createScaledBitmap(
                                originalBitmap,
                                (width * scale).toInt(),
                                (height * scale).toInt(),
                                true
                            )
                            val outputStream = ByteArrayOutputStream()
                            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
                            val bytes = outputStream.toByteArray()
                            val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)

                            withContext(Dispatchers.Main) {
                                imageBitmap = scaledBitmap
                                imageBase64 = b64
                                Toast.makeText(context, "Question image uploaded successfully!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Could not load photo: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    fun solveHomework() {
        if (questionText.isBlank() && imageBase64 == null) {
            Toast.makeText(context, "Please type a question or upload a photo", Toast.LENGTH_SHORT).show()
            return
        }

        isSolving = true
        solution = null
        practiceList = emptyList()

        coroutineScope.launch {
            val result = geminiService.solveHomework(
                questionText = questionText,
                imageBase64 = imageBase64,
                grade = userProfile.grade,
                subject = selectedSubject,
                languageMode = userProfile.languageMode
            )
            solution = result
            practiceList = result.practiceQuestions
            isSolving = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Homework Helper",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Homework Helper AI",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Photo & Text Step-by-Step Solver",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Subject & Class Selector
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Select Subject:",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(listOf("Mathematics", "Physics", "Chemistry", "Biology", "English", "Social Science")) { sub ->
                                FilterChip(
                                    selected = selectedSubject == sub,
                                    onClick = { selectedSubject = sub },
                                    label = { Text(sub, fontSize = 12.sp) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Text Area for Question
                        OutlinedTextField(
                            value = questionText,
                            onValueChange = { questionText = it },
                            label = { Text("Type your homework question...") },
                            placeholder = { Text("e.g. Find the roots of 2x² - 7x + 3 = 0, or state Snell's law formula...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 100.dp),
                            maxLines = 5,
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Upload Question Photo Card
                        if (imageBitmap != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Image(
                                        bitmap = imageBitmap!!.asImageBitmap(),
                                        contentDescription = "Uploaded Question",
                                        modifier = Modifier
                                            .size(80.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Question Photo Attached",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            text = "AI Vision will extract and analyze the question",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(onClick = {
                                        imageBitmap = null
                                        imageBase64 = null
                                        imageUri = null
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Remove photo",
                                            tint = Color.Red
                                        )
                                    }
                                }
                            }
                        } else {
                            OutlinedButton(
                                onClick = { imagePickerLauncher.launch("image/*") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(vertical = 12.dp)
                            ) {
                                Icon(imageVector = Icons.Default.AddPhotoAlternate, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Upload Question Photo / Screenshot")
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Sample Homework Templates
                        Text(
                            text = "Or try an exam problem:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(listOf(
                                "Solve: 2x² - 7x + 3 = 0 by factorization",
                                "A 10Ω resistor connected to 220V. Find current & power.",
                                "Why does sky appear blue during midday?",
                                "Calculate molar mass of Glucose (C₆H₁₂O₆)"
                            )) { sample ->
                                SuggestionChip(
                                    onClick = { questionText = sample },
                                    label = { Text(sample, fontSize = 11.sp, maxLines = 1) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Solve Action Button
                        Button(
                            onClick = { solveHomework() },
                            enabled = !isSolving && (questionText.isNotBlank() || imageBase64 != null),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(vertical = 14.dp)
                        ) {
                            if (isSolving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("AI is Solving Step-by-Step...")
                            } else {
                                Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Get Step-by-Step Solution & Working", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Solution Presentation Section
            if (solution != null) {
                item {
                    val sol = solution!!
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Core Concept Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Lightbulb,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Core Concept",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = sol.coreConcept,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        // Step by Step Working Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "🔢 Step-by-Step Working",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Spacer(modifier = Modifier.height(10.dp))

                                sol.stepByStep.forEachIndexed { index, step ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primaryContainer),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "${index + 1}",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = step,
                                            fontSize = 13.sp,
                                            lineHeight = 19.sp,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }

                        // Key Formula Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Functions,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Formula / Rule Used",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = sol.keyFormula,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        // Common Trap / Examiner Trap Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = Color(0xFFDC2626)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Common Student Mistakes / Traps",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Color(0xFF991B1B)
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = sol.commonMistakes,
                                    fontSize = 13.sp,
                                    color = Color(0xFF7F1D1D)
                                )
                            }
                        }

                        // Final Answer Highlight
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFFD1FAE5)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "🎯 Final Verified Answer",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color(0xFF065F46)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = sol.finalAnswer,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 16.sp,
                                    color = Color(0xFF047857)
                                )
                            }
                        }

                        // Generate Practice Questions Action
                        Button(
                            onClick = {
                                isGeneratingPractice = true
                                coroutineScope.launch {
                                    val practice = geminiService.generateSimilarPractice(
                                        questionText = questionText.ifBlank { sol.coreConcept },
                                        subject = selectedSubject,
                                        grade = userProfile.grade,
                                        languageMode = userProfile.languageMode
                                    )
                                    practiceList = practice
                                    isGeneratingPractice = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary
                            )
                        ) {
                            if (isGeneratingPractice) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Generating Similar Practice Questions...")
                            } else {
                                Icon(imageVector = Icons.Default.Repeat, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Generate Similar Practice Questions to Master This!")
                            }
                        }
                    }
                }
            }

            // Generated Practice Questions List
            if (practiceList.isNotEmpty()) {
                item {
                    Text(
                        text = "Similar Practice Problems:",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                items(practiceList) { pq ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = pq.question,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = "💡 Hint: ${pq.hint}",
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(8.dp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            TextButton(
                                onClick = {
                                    expandedPracticeId = if (expandedPracticeId == pq.id) null else pq.id
                                }
                            ) {
                                Text(if (expandedPracticeId == pq.id) "Hide Solution" else "Show Solution")
                            }

                            AnimatedVisibility(visible = expandedPracticeId == pq.id) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFD1FAE5),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Solution: ${pq.solution}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF065F46),
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
