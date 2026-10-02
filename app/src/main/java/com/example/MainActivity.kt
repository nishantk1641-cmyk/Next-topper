package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.data.*
import com.example.ui.screens.*
import com.example.ui.theme.NextTopperTheme

enum class AppDestination(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    DASHBOARD("Dashboard", Icons.Default.Dashboard, Icons.Default.DashboardCustomize),
    TUTOR("AI Tutor", Icons.Default.Psychology, Icons.Default.PsychologyAlt),
    HOMEWORK("Homework", Icons.Default.Assignment, Icons.Default.AssignmentTurnedIn),
    QUIZ("AI Quiz", Icons.Default.Quiz, Icons.Default.HelpOutline)
}

class MainActivity : ComponentActivity() {

    private val geminiService = GeminiService()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NextTopperTheme {
                NextTopperApp(geminiService = geminiService)
            }
        }
    }
}

@Composable
fun NextTopperApp(geminiService: GeminiService) {
    var currentDestination by remember { mutableStateOf(AppDestination.DASHBOARD) }
    var userProfile by remember { mutableStateOf(SampleData.initialProfile) }
    var pendingTutorPrompt by remember { mutableStateOf<String?>(null) }
    var pendingQuizSubject by remember { mutableStateOf<String?>(null) }
    var pendingQuizChapter by remember { mutableStateOf<String?>(null) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                tonalElevation = 8.dp,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                AppDestination.values().forEach { destination ->
                    val isSelected = currentDestination == destination
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = {
                            currentDestination = destination
                        },
                        icon = {
                            Icon(
                                imageVector = if (isSelected) destination.selectedIcon else destination.unselectedIcon,
                                contentDescription = destination.title
                            )
                        },
                        label = {
                            Text(text = destination.title)
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = MaterialTheme.colorScheme.background
        ) {
            when (currentDestination) {
                AppDestination.DASHBOARD -> {
                    DashboardScreen(
                        userProfile = userProfile,
                        onUpdateLanguage = { newLang ->
                            userProfile = userProfile.copy(languageMode = newLang)
                        },
                        onUpdateGrade = { newGrade ->
                            userProfile = userProfile.copy(grade = newGrade)
                        },
                        onNavigateToTutorWithPrompt = { prompt ->
                            pendingTutorPrompt = prompt
                            currentDestination = AppDestination.TUTOR
                        },
                        onNavigateToQuizForSubject = { subject, chapter ->
                            pendingQuizSubject = subject
                            pendingQuizChapter = chapter
                            currentDestination = AppDestination.QUIZ
                        },
                        onNavigateToHomework = {
                            currentDestination = AppDestination.HOMEWORK
                        }
                    )
                }

                AppDestination.TUTOR -> {
                    TutorScreen(
                        userProfile = userProfile,
                        geminiService = geminiService,
                        initialPrompt = pendingTutorPrompt,
                        onUpdateLanguage = { newLang ->
                            userProfile = userProfile.copy(languageMode = newLang)
                        }
                    )
                }

                AppDestination.HOMEWORK -> {
                    HomeworkScreen(
                        userProfile = userProfile,
                        geminiService = geminiService
                    )
                }

                AppDestination.QUIZ -> {
                    QuizScreen(
                        userProfile = userProfile,
                        geminiService = geminiService,
                        initialSubject = pendingQuizSubject,
                        initialChapter = pendingQuizChapter,
                        onQuizCompleted = { correct, total ->
                            if (total > 0) {
                                val currentScore = (correct * 100) / total
                                val updatedAvg = ((userProfile.averageQuizScore * 3) + currentScore) / 4
                                userProfile = userProfile.copy(
                                    averageQuizScore = updatedAvg,
                                    completedLessons = userProfile.completedLessons + 1,
                                    overallProgressPercent = (userProfile.overallProgressPercent + 1).coerceAtMost(100)
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name!", modifier = modifier)
}

