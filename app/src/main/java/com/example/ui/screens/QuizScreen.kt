package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizScreen(
    userProfile: UserProfile,
    geminiService: GeminiService,
    initialSubject: String? = null,
    initialChapter: String? = null,
    onQuizCompleted: (Int, Int) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Configuration State
    var selectedSubject by remember { mutableStateOf(initialSubject ?: "Mathematics") }
    var selectedChapter by remember { mutableStateOf(initialChapter ?: "Quadratic Equations") }
    var selectedDifficulty by remember { mutableStateOf(QuizDifficulty.MEDIUM) }
    var selectedQuestionType by remember { mutableStateOf(QuestionType.MCQ) }
    var questionCount by remember { mutableStateOf(5) }

    // Quiz Session State
    var isGenerating by remember { mutableStateOf(false) }
    var isQuizActive by remember { mutableStateOf(false) }
    var questions by remember { mutableStateOf<List<QuizQuestion>>(emptyList()) }
    var currentQuestionIndex by remember { mutableStateOf(0) }
    var selectedAnswers by remember { mutableStateOf<Map<Int, Int>>(emptyMap()) } // qIndex -> optionIndex
    var shortAnswers by remember { mutableStateOf<Map<Int, String>>(emptyMap()) } // qIndex -> user text
    var revealedExplanations by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var isQuizFinished by remember { mutableStateOf(false) }

    // Update chapter options when subject changes
    val currentSubjectItem = SampleData.subjects.find { it.name == selectedSubject }
    val availableChapters = currentSubjectItem?.chapters ?: listOf("General Revision", "Important Formulas")

    LaunchedEffect(selectedSubject) {
        if (availableChapters.isNotEmpty() && !availableChapters.contains(selectedChapter)) {
            selectedChapter = availableChapters.first()
        }
    }

    fun startQuiz() {
        isGenerating = true
        coroutineScope.launch {
            val generated = geminiService.generateQuiz(
                subject = selectedSubject,
                chapter = selectedChapter,
                difficulty = selectedDifficulty,
                questionType = selectedQuestionType,
                count = questionCount,
                languageMode = userProfile.languageMode,
                grade = userProfile.grade
            )
            questions = generated
            currentQuestionIndex = 0
            selectedAnswers = emptyMap()
            shortAnswers = emptyMap()
            revealedExplanations = emptySet()
            isQuizFinished = false
            isQuizActive = true
            isGenerating = false
        }
    }

    fun finishQuiz() {
        isQuizFinished = true
        val correctCount = questions.indices.count { idx ->
            selectedAnswers[idx] == questions[idx].correctOptionIndex
        }
        onQuizCompleted(correctCount, questions.size)
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
                                imageVector = Icons.Default.Quiz,
                                contentDescription = "AI Quiz",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Next Topper AI Quiz",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = if (isQuizActive) "$selectedSubject • $selectedChapter" else "Generate MCQs & Practice Tests",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }
                },
                actions = {
                    if (isQuizActive) {
                        TextButton(
                            onClick = {
                                isQuizActive = false
                                isQuizFinished = false
                            }
                        ) {
                            Text("Exit Quiz", color = MaterialTheme.colorScheme.error)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            if (!isQuizActive) {
                // Quiz Setup View
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "1. Select Subject:",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(SampleData.subjects) { sub ->
                                        FilterChip(
                                            selected = selectedSubject == sub.name,
                                            onClick = { selectedSubject = sub.name },
                                            label = { Text(sub.name, fontSize = 12.sp) }
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    text = "2. Select Chapter / Topic:",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(availableChapters) { ch ->
                                        FilterChip(
                                            selected = selectedChapter == ch,
                                            onClick = { selectedChapter = ch },
                                            label = { Text(ch, fontSize = 12.sp) }
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    text = "3. Difficulty Level:",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    QuizDifficulty.values().forEach { diff ->
                                        FilterChip(
                                            selected = selectedDifficulty == diff,
                                            onClick = { selectedDifficulty = diff },
                                            label = { Text(diff.label, fontSize = 12.sp) },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    text = "4. Question Format:",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    QuestionType.values().forEach { qType ->
                                        FilterChip(
                                            selected = selectedQuestionType == qType,
                                            onClick = { selectedQuestionType = qType },
                                            label = { Text(qType.label, fontSize = 11.sp) },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    text = "5. Number of Questions:",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf(3, 5, 10).forEach { num ->
                                        FilterChip(
                                            selected = questionCount == num,
                                            onClick = { questionCount = num },
                                            label = { Text("$num Questions", fontSize = 12.sp) },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(20.dp))

                                Button(
                                    onClick = { startQuiz() },
                                    enabled = !isGenerating,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(vertical = 14.dp)
                                ) {
                                    if (isGenerating) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            color = Color.White,
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text("Generating AI Questions...")
                                    } else {
                                        Icon(imageVector = Icons.Default.Bolt, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Generate & Start Quiz", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    // Sample Quick Quizzes
                    item {
                        Text(
                            text = "Or try Popular High-Yield Quizzes:",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedSubject = "Mathematics"
                                    selectedChapter = "Quadratic Equations"
                                    selectedDifficulty = QuizDifficulty.MEDIUM
                                    startQuiz()
                                },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "📐", fontSize = 24.sp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Class 10 Math: Quadratic Equations",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "Discriminants, roots, and factorizations • 5 MCQs",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(imageVector = Icons.Default.ArrowForward, contentDescription = null)
                            }
                        }
                    }

                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedSubject = "Physics"
                                    selectedChapter = "Light - Reflection & Refraction"
                                    selectedDifficulty = QuizDifficulty.MEDIUM
                                    startQuiz()
                                },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "⚡", fontSize = 24.sp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Class 10 Physics: Ray Optics & Snell's Law",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "Refraction indices & mirror formulas • 5 MCQs",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(imageVector = Icons.Default.ArrowForward, contentDescription = null)
                            }
                        }
                    }
                }
            } else if (isQuizFinished) {
                // Quiz Completed Summary Screen
                val correctCount = questions.indices.count { idx ->
                    selectedAnswers[idx] == questions[idx].correctOptionIndex
                }
                val scorePercent = if (questions.isNotEmpty()) (correctCount * 100) / questions.size else 0

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = if (scorePercent >= 80) "🏆 Next Topper Score!" else "Keep Practicing! 👍",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp,
                                    color = if (scorePercent >= 80) Color(0xFFD97706) else MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Box(
                                    modifier = Modifier
                                        .size(100.dp)
                                        .clip(CircleShape)
                                        .background(if (scorePercent >= 80) Color(0xFFFEF3C7) else Color(0xFFDBEAFE)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "$scorePercent%",
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 26.sp,
                                            color = if (scorePercent >= 80) Color(0xFFB45309) else Color(0xFF1E40AF)
                                        )
                                        Text(
                                            text = "$correctCount / ${questions.size}",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = "Completed: $selectedSubject ($selectedChapter)",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            isQuizActive = false
                                            isQuizFinished = false
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("New Quiz")
                                    }
                                    Button(
                                        onClick = {
                                            selectedAnswers = emptyMap()
                                            shortAnswers = emptyMap()
                                            revealedExplanations = emptySet()
                                            currentQuestionIndex = 0
                                            isQuizFinished = false
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Retake")
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Text(
                            text = "Question Review & Explanations:",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    items(questions.indices.toList()) { idx ->
                        val q = questions[idx]
                        val userChoice = selectedAnswers[idx]
                        val isCorrect = userChoice == q.correctOptionIndex

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isCorrect) Color(0xFFF0FDF4) else Color(0xFFFEF2F2)
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isCorrect) Color(0xFF86EFAC) else Color(0xFFFECACA)
                            )
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Q${idx + 1}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (isCorrect) Color(0xFF15803D) else Color(0xFFB91C1C)
                                    )
                                    Text(
                                        text = if (isCorrect) "✓ Correct" else "✗ Incorrect",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = if (isCorrect) Color(0xFF15803D) else Color(0xFFB91C1C)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = q.question,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )

                                if (q.options.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Your Answer: ${if (userChoice != null && userChoice in q.options.indices) q.options[userChoice] else "Skipped"}",
                                        fontSize = 12.sp,
                                        color = if (isCorrect) Color(0xFF15803D) else Color(0xFFB91C1C)
                                    )
                                    if (!isCorrect && q.correctOptionIndex in q.options.indices) {
                                        Text(
                                            text = "Correct Answer: ${q.options[q.correctOptionIndex]}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF15803D)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text(
                                            text = "💡 Explanation: ${q.explanation}",
                                            fontSize = 12.sp,
                                            lineHeight = 17.sp
                                        )
                                        if (q.conceptHint.isNotBlank()) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "📌 Tip: ${q.conceptHint}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Interactive Question Taker
                if (questions.isNotEmpty() && currentQuestionIndex < questions.size) {
                    val currentQ = questions[currentQuestionIndex]
                    val userSelected = selectedAnswers[currentQuestionIndex]
                    val isAnswered = userSelected != null
                    val isRevealed = revealedExplanations.contains(currentQuestionIndex)

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            // Progress bar
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Question ${currentQuestionIndex + 1} of ${questions.size}",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(currentQ.difficulty.colorHex).copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = currentQ.difficulty.label,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(currentQ.difficulty.colorHex)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            LinearProgressIndicator(
                                progress = { (currentQuestionIndex + 1).toFloat() / questions.size },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(CircleShape),
                                color = MaterialTheme.colorScheme.primary
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Question Statement Card
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Text(
                                    text = currentQ.question,
                                    fontSize = 16.sp,
                                    lineHeight = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Options List (for MCQs)
                            if (currentQ.options.isNotEmpty()) {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    currentQ.options.forEachIndexed { optIndex, optionText ->
                                        val isChosen = userSelected == optIndex
                                        val isCorrectOption = optIndex == currentQ.correctOptionIndex

                                        val bgColor = when {
                                            !isAnswered -> MaterialTheme.colorScheme.surface
                                            isChosen && isCorrectOption -> Color(0xFFD1FAE5)
                                            isChosen && !isCorrectOption -> Color(0xFFFEE2E2)
                                            !isChosen && isCorrectOption && isAnswered -> Color(0xFFD1FAE5)
                                            else -> MaterialTheme.colorScheme.surface
                                        }

                                        val borderColor = when {
                                            !isAnswered && isChosen -> MaterialTheme.colorScheme.primary
                                            isAnswered && isCorrectOption -> Color(0xFF059669)
                                            isAnswered && isChosen && !isCorrectOption -> Color(0xFFDC2626)
                                            else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                        }

                                        Surface(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(12.dp))
                                                .border(1.dp, borderColor, RoundedCornerShape(12.dp))
                                                .clickable {
                                                    if (!isAnswered) {
                                                        selectedAnswers = selectedAnswers + (currentQuestionIndex to optIndex)
                                                        revealedExplanations = revealedExplanations + currentQuestionIndex
                                                    }
                                                },
                                            color = bgColor
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(14.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(28.dp)
                                                        .clip(CircleShape)
                                                        .background(
                                                            if (isChosen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                                        ),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = ('A' + optIndex).toString(),
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 12.sp,
                                                        color = if (isChosen) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Text(
                                                    text = optionText,
                                                    fontSize = 14.sp,
                                                    fontWeight = if (isChosen) FontWeight.Bold else FontWeight.Normal,
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Explanation Card
                            AnimatedVisibility(visible = isAnswered || isRevealed) {
                                Column(modifier = Modifier.padding(top = 14.dp)) {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                                        )
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text(
                                                text = "💡 Detailed Explanation",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = currentQ.explanation,
                                                fontSize = 12.sp,
                                                lineHeight = 17.sp
                                            )
                                            if (currentQ.conceptHint.isNotBlank()) {
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = "Topper Tip: ${currentQ.conceptHint}",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.secondary
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Bottom Navigation Buttons
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            if (currentQuestionIndex > 0) {
                                OutlinedButton(
                                    onClick = { currentQuestionIndex-- },
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Previous")
                                }
                            } else {
                                Spacer(modifier = Modifier.width(10.dp))
                            }

                            if (currentQuestionIndex < questions.size - 1) {
                                Button(
                                    onClick = { currentQuestionIndex++ },
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Next Question")
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(imageVector = Icons.Default.ArrowForward, contentDescription = null)
                                }
                            } else {
                                Button(
                                    onClick = { finishQuiz() },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    ),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Finish & View Score")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
