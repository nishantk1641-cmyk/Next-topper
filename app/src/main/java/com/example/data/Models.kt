package com.example.data

enum class LanguageMode(val displayName: String, val badge: String) {
    ENGLISH("English", "EN"),
    HINGLISH("Hinglish", "HI/EN")
}

enum class StudentGrade(val label: String) {
    CLASS_8("Class 8"),
    CLASS_9("Class 9"),
    CLASS_10("Class 10 (Boards)"),
    CLASS_11("Class 11"),
    CLASS_12("Class 12 (Boards)"),
    JEE_NEET("JEE / NEET Prep")
}

enum class TutorMode(val title: String, val desc: String) {
    EXPLAIN("Simple Explainer", "Breaks down concepts using real-life analogies"),
    SOCRATIC("Socratic Coach", "Asks you guiding questions to help you think"),
    EXAM_HACKS("Topper Hacks", "Formulas, exam traps, and scoring secrets")
}

enum class QuizDifficulty(val label: String, val colorHex: Long) {
    EASY("Easy", 0xFF10B981),
    MEDIUM("Medium", 0xFFF59E0B),
    HARD("Hard", 0xFFEF4444)
}

enum class QuestionType(val label: String) {
    MCQ("Multiple Choice"),
    SHORT_ANSWER("Short Answer"),
    MIXED("Mixed Practice")
}

data class UserProfile(
    val name: String = "Aarav Sharma",
    val grade: StudentGrade = StudentGrade.CLASS_10,
    val targetScore: String = "95%+ Board Target",
    val streakDays: Int = 7,
    val overallProgressPercent: Int = 78,
    val completedLessons: Int = 24,
    val averageQuizScore: Int = 86,
    val languageMode: LanguageMode = LanguageMode.HINGLISH
)

data class StudyTask(
    val id: String,
    val subject: String,
    val title: String,
    val estimatedMinutes: Int,
    val isCompleted: Boolean = false,
    val tag: String = "Revision"
)

data class SubjectItem(
    val id: String,
    val name: String,
    val iconName: String,
    val totalChapters: Int,
    val completedChapters: Int,
    val progressPercent: Int,
    val accentColorHex: Long,
    val chapters: List<String>
)

data class UpcomingExam(
    val id: String,
    val subject: String,
    val examName: String,
    val daysLeft: Int,
    val targetDate: String,
    val priority: String = "High Weightage"
)

data class ChatMessage(
    val id: String,
    val sender: MessageSender,
    val text: String,
    val timestamp: String,
    val formulas: List<String> = emptyList(),
    val followUpQuestions: List<String> = emptyList(),
    val isThinking: Boolean = false
)

enum class MessageSender {
    USER,
    AI_TUTOR
}

data class QuizQuestion(
    val id: String,
    val question: String,
    val options: List<String> = emptyList(), // empty for short-answer
    val correctOptionIndex: Int = -1,
    val sampleAnswer: String = "", // for short answer
    val explanation: String,
    val conceptHint: String = "",
    val difficulty: QuizDifficulty = QuizDifficulty.MEDIUM
)

data class QuizResult(
    val quizTitle: String,
    val subject: String,
    val totalQuestions: Int,
    val correctAnswers: Int,
    val scorePercentage: Int,
    val timeSpentSeconds: Int,
    val questions: List<QuizQuestion>,
    val selectedAnswers: Map<Int, Int> // questionIndex -> selectedOptionIndex
)

data class HomeworkSolution(
    val coreConcept: String,
    val stepByStep: List<String>,
    val keyFormula: String,
    val commonMistakes: String,
    val finalAnswer: String,
    val practiceQuestions: List<PracticeQuestion> = emptyList()
)

data class PracticeQuestion(
    val id: String,
    val question: String,
    val hint: String,
    val solution: String
)
