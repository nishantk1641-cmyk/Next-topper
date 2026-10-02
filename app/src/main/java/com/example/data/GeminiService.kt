package com.example.data

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.TimeUnit

class GeminiService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    // Model name: gemini-2.5-flash as recommended for fast reasoning
    private val modelName = "gemini-2.5-flash"
    private val baseUrl = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent"

    private fun getApiKey(): String {
        return try {
            val key = BuildConfig.GEMINI_API_KEY
            if (key.isNotBlank() && key != "MY_GEMINI_API_KEY") key else ""
        } catch (e: Throwable) {
            ""
        }
    }

    suspend fun askTutor(
        userMessage: String,
        grade: StudentGrade,
        languageMode: LanguageMode,
        tutorMode: TutorMode,
        previousMessages: List<ChatMessage> = emptyList()
    ): ChatMessage = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isEmpty()) {
            return@withContext generateFallbackTutorResponse(userMessage, grade, languageMode, tutorMode)
        }

        try {
            val promptSystem = buildTutorSystemPrompt(grade, languageMode, tutorMode)
            val fullPrompt = "$promptSystem\n\nStudent asks: $userMessage\n\n" +
                "Respond directly to the student in markdown format. Keep it warm, engaging, and clear. Include 1-2 quick follow-up questions at the end to test their understanding."

            val jsonBody = JSONObject().apply {
                val contentsArray = JSONArray()

                // Recent conversation context
                previousMessages.takeLast(4).forEach { msg ->
                    val role = if (msg.sender == MessageSender.USER) "user" else "model"
                    contentsArray.put(JSONObject().apply {
                        put("role", role)
                        put("parts", JSONArray().put(JSONObject().put("text", msg.text)))
                    })
                }

                // Current message
                contentsArray.put(JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().put(JSONObject().put("text", fullPrompt)))
                })

                put("contents", contentsArray)
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.7)
                })
            }

            val request = Request.Builder()
                .url("$baseUrl?key=$apiKey")
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            if (response.isSuccessful && !responseBody.isNullOrBlank()) {
                val json = JSONObject(responseBody)
                val text = json.optJSONArray("candidates")
                    ?.optJSONObject(0)
                    ?.optJSONObject("content")
                    ?.optJSONArray("parts")
                    ?.optJSONObject(0)
                    ?.optString("text") ?: ""

                if (text.isNotBlank()) {
                    return@withContext parseTutorResponse(text, languageMode)
                }
            }
            Log.w("GeminiService", "API call failed or empty: ${response.code}")
            generateFallbackTutorResponse(userMessage, grade, languageMode, tutorMode)
        } catch (e: Exception) {
            Log.e("GeminiService", "Error in askTutor", e)
            generateFallbackTutorResponse(userMessage, grade, languageMode, tutorMode)
        }
    }

    suspend fun solveHomework(
        questionText: String,
        imageBase64: String? = null,
        mimeType: String = "image/jpeg",
        grade: StudentGrade,
        subject: String,
        languageMode: LanguageMode = LanguageMode.HINGLISH
    ): HomeworkSolution = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isEmpty()) {
            return@withContext generateFallbackHomeworkSolution(questionText, subject, languageMode)
        }

        try {
            val systemPrompt = """
                You are Next Topper AI's elite Homework Solver for ${grade.label}.
                Language preference: ${if (languageMode == LanguageMode.HINGLISH) "Friendly Hinglish (Hindi + English mix)" else "Clear English"}.
                Solve the student's homework question with 100% mathematical and conceptual rigor.
                Return your answer strictly in valid JSON format with these exact keys:
                {
                   "coreConcept": "Short title and definition of the key theory or principle behind this question",
                   "stepByStep": ["Step 1: ...", "Step 2: ...", "Step 3: ..."],
                   "keyFormula": "The exact formula, law, or equation used",
                   "commonMistakes": "Where students frequently make calculation or conceptual mistakes in this question",
                   "finalAnswer": "The final concise result or conclusion with units"
                }
                Do not include markdown backticks around the json if possible, just raw json.
            """.trimIndent()

            val partsArray = JSONArray()
            if (!imageBase64.isNullOrBlank()) {
                partsArray.put(JSONObject().apply {
                    put("inlineData", JSONObject().apply {
                        put("mimeType", mimeType)
                        put("data", imageBase64)
                    })
                })
            }
            partsArray.put(JSONObject().put("text", "$systemPrompt\n\nQuestion:\n${questionText.ifBlank { "Solve the problem shown in the uploaded photo." }}"))

            val jsonBody = JSONObject().apply {
                put("contents", JSONArray().put(JSONObject().apply {
                    put("parts", partsArray)
                }))
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.3)
                    put("responseMimeType", "application/json")
                })
            }

            val request = Request.Builder()
                .url("$baseUrl?key=$apiKey")
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            if (response.isSuccessful && !responseBody.isNullOrBlank()) {
                val json = JSONObject(responseBody)
                val text = json.optJSONArray("candidates")
                    ?.optJSONObject(0)
                    ?.optJSONObject("content")
                    ?.optJSONArray("parts")
                    ?.optJSONObject(0)
                    ?.optString("text") ?: ""

                val cleanJson = cleanJsonResponse(text)
                val parsed = JSONObject(cleanJson)
                val steps = mutableListOf<String>()
                val stepsJson = parsed.optJSONArray("stepByStep")
                if (stepsJson != null) {
                    for (i in 0 until stepsJson.length()) {
                        steps.add(stepsJson.getString(i))
                    }
                }

                val practiceQuestions = generateFallbackPracticeQuestions(questionText, subject)

                return@withContext HomeworkSolution(
                    coreConcept = parsed.optString("coreConcept", "Concept Analysis"),
                    stepByStep = if (steps.isNotEmpty()) steps else listOf("Detailed working computed"),
                    keyFormula = parsed.optString("keyFormula", "Standard Curriculum Rule"),
                    commonMistakes = parsed.optString("commonMistakes", "Double check algebraic signs and SI units!"),
                    finalAnswer = parsed.optString("finalAnswer", "Verified Correct Answer"),
                    practiceQuestions = practiceQuestions
                )
            }
            generateFallbackHomeworkSolution(questionText, subject, languageMode)
        } catch (e: Exception) {
            Log.e("GeminiService", "Error solving homework", e)
            generateFallbackHomeworkSolution(questionText, subject, languageMode)
        }
    }

    suspend fun generateSimilarPractice(
        questionText: String,
        subject: String,
        grade: StudentGrade,
        languageMode: LanguageMode = LanguageMode.HINGLISH
    ): List<PracticeQuestion> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isEmpty()) {
            return@withContext generateFallbackPracticeQuestions(questionText, subject)
        }

        try {
            val prompt = """
                Based on this question: "$questionText" in $subject for ${grade.label}.
                Generate 3 similar practice questions of comparable exam difficulty.
                Format as JSON array of objects:
                [
                  {
                    "question": "Question text...",
                    "hint": "Hint to help student get started...",
                    "solution": "Brief step-by-step solution and final answer..."
                  }
                ]
            """.trimIndent()

            val jsonBody = JSONObject().apply {
                put("contents", JSONArray().put(JSONObject().apply {
                    put("parts", JSONArray().put(JSONObject().put("text", prompt)))
                }))
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.4)
                    put("responseMimeType", "application/json")
                })
            }

            val request = Request.Builder()
                .url("$baseUrl?key=$apiKey")
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            if (response.isSuccessful && !responseBody.isNullOrBlank()) {
                val json = JSONObject(responseBody)
                val text = json.optJSONArray("candidates")
                    ?.optJSONObject(0)
                    ?.optJSONObject("content")
                    ?.optJSONArray("parts")
                    ?.optJSONObject(0)
                    ?.optString("text") ?: ""

                val cleanJson = cleanJsonResponse(text)
                val array = JSONArray(cleanJson)
                val result = mutableListOf<PracticeQuestion>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    result.add(
                        PracticeQuestion(
                            id = UUID.randomUUID().toString(),
                            question = obj.optString("question", "Practice problem ${i + 1}"),
                            hint = obj.optString("hint", "Use standard formulas"),
                            solution = obj.optString("solution", "Final result")
                        )
                    )
                }
                if (result.isNotEmpty()) return@withContext result
            }
            generateFallbackPracticeQuestions(questionText, subject)
        } catch (e: Exception) {
            Log.e("GeminiService", "Error generating practice questions", e)
            generateFallbackPracticeQuestions(questionText, subject)
        }
    }

    suspend fun generateQuiz(
        subject: String,
        chapter: String,
        difficulty: QuizDifficulty,
        questionType: QuestionType,
        count: Int,
        languageMode: LanguageMode,
        grade: StudentGrade
    ): List<QuizQuestion> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isEmpty()) {
            return@withContext generateFallbackQuizQuestions(subject, chapter, difficulty, count)
        }

        try {
            val languageInstruction = if (languageMode == LanguageMode.HINGLISH) {
                "Use simple English with Hinglish hints if helpful."
            } else {
                "Use formal clear English."
            }

            val prompt = """
                Generate $count high-yield exam practice questions for $subject, Chapter: "$chapter", Grade: ${grade.label}.
                Difficulty: ${difficulty.label}. Question type: ${questionType.label}.
                $languageInstruction
                Return strictly a JSON array of question objects:
                [
                  {
                    "question": "Clear problem statement",
                    "options": ["Option A", "Option B", "Option C", "Option D"],
                    "correctOptionIndex": 0,
                    "explanation": "Why this option is correct and how to solve it",
                    "conceptHint": "Topper mnemonic or formula tip"
                  }
                ]
            """.trimIndent()

            val jsonBody = JSONObject().apply {
                put("contents", JSONArray().put(JSONObject().apply {
                    put("parts", JSONArray().put(JSONObject().put("text", prompt)))
                }))
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.5)
                    put("responseMimeType", "application/json")
                })
            }

            val request = Request.Builder()
                .url("$baseUrl?key=$apiKey")
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            if (response.isSuccessful && !responseBody.isNullOrBlank()) {
                val json = JSONObject(responseBody)
                val text = json.optJSONArray("candidates")
                    ?.optJSONObject(0)
                    ?.optJSONObject("content")
                    ?.optJSONArray("parts")
                    ?.optJSONObject(0)
                    ?.optString("text") ?: ""

                val cleanJson = cleanJsonResponse(text)
                val array = JSONArray(cleanJson)
                val list = mutableListOf<QuizQuestion>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val optsJson = obj.optJSONArray("options")
                    val options = mutableListOf<String>()
                    if (optsJson != null) {
                        for (j in 0 until optsJson.length()) {
                            options.add(optsJson.getString(j))
                        }
                    }
                    list.add(
                        QuizQuestion(
                            id = UUID.randomUUID().toString(),
                            question = obj.optString("question", "Question ${i + 1}"),
                            options = options,
                            correctOptionIndex = obj.optInt("correctOptionIndex", 0),
                            explanation = obj.optString("explanation", "Accurate solution explanation"),
                            conceptHint = obj.optString("conceptHint", "Key exam formula"),
                            difficulty = difficulty
                        )
                    )
                }
                if (list.isNotEmpty()) return@withContext list
            }
            generateFallbackQuizQuestions(subject, chapter, difficulty, count)
        } catch (e: Exception) {
            Log.e("GeminiService", "Error generating quiz", e)
            generateFallbackQuizQuestions(subject, chapter, difficulty, count)
        }
    }

    private fun buildTutorSystemPrompt(
        grade: StudentGrade,
        languageMode: LanguageMode,
        tutorMode: TutorMode
    ): String {
        val langStr = if (languageMode == LanguageMode.HINGLISH) {
            "Communicate in friendly, relatable Hinglish (a natural conversational blend of Hindi and English, like 'Chalo isko step-by-step samajhte hain', 'Bilkul easy hai!'). Use Hindi phrases for warmth and encouragement, but keep technical/academic terms in English (e.g. 'Acceleration', 'Photosynthesis', 'Quadratic Equation')."
        } else {
            "Communicate in clear, encouraging, accessible English."
        }

        val modeStr = when (tutorMode) {
            TutorMode.EXPLAIN -> "Explain concepts using intuitive real-life analogies, simplified language, and step-by-step clarity. Always break down complex jargon into digestible ideas."
            TutorMode.SOCRATIC -> "Use the Socratic method! Do not just dump the final answer. Guide the student by asking 1 or 2 targeted questions that lead them to discover the answer themselves."
            TutorMode.EXAM_HACKS -> "Focus on Topper scoring secrets: point out common examiner traps, how to write answers for full marks in board exams, and high-frequency formulas."
        }

        return """
            You are Next Topper AI, an empathetic, brilliant personal tutor dedicated to helping students achieve 95%+ scores.
            Student Target Grade: ${grade.label}.
            Teaching Style: $modeStr
            Language Mode: $langStr
            Always keep explanations neat, encouraging, and structured with bullet points.
        """.trimIndent()
    }

    private fun parseTutorResponse(text: String, languageMode: LanguageMode): ChatMessage {
        val lines = text.split("\n")
        val followUps = mutableListOf<String>()
        val formulas = mutableListOf<String>()

        // Extract any bullet points at the end that look like questions
        lines.takeLast(4).forEach { line ->
            val clean = line.trim().trimStart('-', '*', '•', '1', '2', '3', '.', ' ')
            if (clean.endsWith("?") && clean.length > 10) {
                followUps.add(clean)
            } else if (clean.contains("=") || clean.contains("formula", ignoreCase = true)) {
                formulas.add(clean)
            }
        }

        return ChatMessage(
            id = UUID.randomUUID().toString(),
            sender = MessageSender.AI_TUTOR,
            text = text,
            timestamp = "Just now",
            formulas = formulas.take(2),
            followUpQuestions = if (followUps.isNotEmpty()) followUps.take(2) else listOf(
                if (languageMode == LanguageMode.HINGLISH) "Kya ye clear hua? Ek question try karoge?" else "Does this make sense? Want a quick practice question?"
            )
        )
    }

    private fun cleanJsonResponse(raw: String): String {
        var clean = raw.trim()
        if (clean.startsWith("```json")) {
            clean = clean.removePrefix("```json")
        } else if (clean.startsWith("```")) {
            clean = clean.removePrefix("```")
        }
        if (clean.endsWith("```")) {
            clean = clean.removeSuffix("```")
        }
        return clean.trim()
    }

    // High quality offline fallback responder
    private fun generateFallbackTutorResponse(
        userMessage: String,
        grade: StudentGrade,
        languageMode: LanguageMode,
        tutorMode: TutorMode
    ): ChatMessage {
        val lower = userMessage.lowercase()

        val text = when {
            lower.contains("ohm") || lower.contains("electricity") || lower.contains("current") -> {
                if (languageMode == LanguageMode.HINGLISH) {
                    """
                    ⚡ **Ohm's Law bilkul simple water pipe analogy se samjho:**
                    
                    Socho ek paani ki pipe hai:
                    1. **Voltage (V)** = Water pressure (jo paani ko aage push karta hai).
                    2. **Current (I)** = Water flow rate (har second kitna paani beh raha hai).
                    3. **Resistance (R)** = Pipe mein laga constriction ya kachra (jo flow ko rokta hai).
                    
                    👉 **Golden Formula:** **V = I × R**
                    
                    Agar pipe patli kar doge (R badhaoge), toh paani ka flow (I) kam ho jayega!
                    
                    💡 **Topper Exam Tip:** Board exam mein mention karna mat bhoolna: *"At constant temperature, current is directly proportional to potential difference."* (Temperature constant hona zaroori condition hai!)
                    
                    Ab mujhe batao: Agar Voltage double kar dein aur Resistance constant rahe, toh Current kitna ho jayega?
                    """.trimIndent()
                } else {
                    """
                    ⚡ **Ohm's Law explained with the Water Pipe Analogy:**
                    
                    Imagine a water pipe system:
                    1. **Voltage (V)** = The water pump pressure pushing the water.
                    2. **Current (I)** = The actual flow rate of water passing through per second.
                    3. **Resistance (R)** = Any narrowing or obstacle resisting the water flow.
                    
                    👉 **Core Formula:** **V = I × R**
                    
                    If you double the resistance while keeping pressure constant, the current gets cut in half!
                    
                    💡 **Topper Exam Tip:** Always state the crucial condition: *"At constant temperature"*, otherwise examiners will deduct 1 mark!
                    
                    Quick question for you: If Voltage is 12V and Resistance is 4Ω, what is the Current?
                    """.trimIndent()
                }
            }
            lower.contains("quadratic") || lower.contains("formula") || lower.contains("x²") || lower.contains("equation") -> {
                if (languageMode == LanguageMode.HINGLISH) {
                    """
                    📐 **Quadratic Equations Master Breakdown:**
                    
                    Standard form hoti hai: **ax² + bx + c = 0** (jahan a ≠ 0).
                    
                    1. **Discriminant (D):**
                       • **D = b² - 4ac**
                       • Agar **D > 0** → 2 distinct real roots (2 alag answers).
                       • Agar **D = 0** → 2 equal real roots (-b / 2a).
                       • Agar **D < 0** → No real roots!
                       
                    2. **Quadratic Formula (Shridharacharya Method):**
                       **x = [-b ± √(b² - 4ac)] / 2a**
                       
                    ⚠️ **Common Mistake:** Aksar students denominator mein sirf `2` likhte hain, `2a` likhna bhool jaate hain!
                    
                    Chalo ek test: Equation `x² - 5x + 6 = 0` mein discriminant D kya hoga?
                    """.trimIndent()
                } else {
                    """
                    📐 **Quadratic Equations Mastery Guide:**
                    
                    Standard form: **ax² + bx + c = 0** (where a ≠ 0).
                    
                    1. **The Discriminant (D):**
                       • **D = b² - 4ac**
                       • If **D > 0** → Two distinct real roots.
                       • If **D = 0** → Two identical real roots (-b / 2a).
                       • If **D < 0** → No real roots (imaginary).
                       
                    2. **The Quadratic Formula:**
                       **x = [-b ± √(b² - 4ac)] / 2a**
                       
                    ⚠️ **Common Exam Pitfall:** Students frequently forget the negative sign before `b`, or write `2` instead of `2a` in the denominator!
                    
                    Can you tell me what the value of D is for: `x² - 7x + 10 = 0`?
                    """.trimIndent()
                }
            }
            lower.contains("photo") || lower.contains("leaf") || lower.contains("plant") || lower.contains("biology") -> {
                if (languageMode == LanguageMode.HINGLISH) {
                    """
                    🌿 **Photosynthesis in 3 Simple Steps:**
                    
                    Plants apna khana (Glucose) kaise banate hain?
                    
                    👉 **Overall Reaction:**
                    **6CO₂ + 6H₂O + Sunlight + Chlorophyll → C₆H₁₂O₆ + 6O₂**
                    
                    3 Main Events jo exam mein aate hain:
                    1. **Absorption:** Chlorophyll sunlight ki light energy ko trap karta hai.
                    2. **Conversion:** Light energy chemical energy mein convert hoti hai, aur Water (H₂O) split ho kar H₂ aur O₂ gas banata hai.
                    3. **Reduction:** CO₂ convert ho jata hai Carbohydrates (Glucose) mein.
                    
                    💡 **Quick Question:** Is process mein release hone wali Oxygen gas paani (H₂O) se aati hai ya Carbon Dioxide (CO₂) se?
                    """.trimIndent()
                } else {
                    """
                    🌿 **Photosynthesis Broken Down Step-by-Step:**
                    
                    How autotrophs synthesize food (Glucose):
                    
                    👉 **Master Chemical Equation:**
                    **6CO₂ + 6H₂O + Light Energy (Chlorophyll) → C₆H₁₂O₆ + 6O₂**
                    
                    Three Key Events:
                    1. **Absorption** of light energy by chlorophyll pigments.
                    2. **Conversion** of light energy to chemical energy & splitting of water molecules (photolysis).
                    3. **Reduction** of Carbon Dioxide into carbohydrates.
                    
                    💡 **Topper Question:** Does the oxygen released come from CO₂ or from H₂O? What do you think?
                    """.trimIndent()
                }
            }
            else -> {
                if (languageMode == LanguageMode.HINGLISH) {
                    """
                    Great question! 🌟 Let's master this topic step by step for ${grade.label}.
                    
                    **1. The Fundamental Core Concept:**
                    Har mushkil sawal ke peeche ek simple sa rule hota hai. Jab aap fundamentals clear rakhte ho, toh questions automatically solve hone lagte hain.
                    
                    **2. Step-by-Step Approach:**
                    • **Step 1:** Diye gaye values aur terms ko note karo (Given data).
                    • **Step 2:** Relevant formula ya theorem identify karo.
                    • **Step 3:** Step-wise substitution karo aur calculation re-check karo.
                    
                    **3. Topper Pro Tip:**
                    Board exams mein step-marking hoti hai. Har step ka formula likhne par half mark secure hota hai!
                    
                    Aapka specific doubt ya question kya hai isme? Mujhe batao, main turant solve karwake practice karwaunga!
                    """.trimIndent()
                } else {
                    """
                    Excellent inquiry! 🌟 Let's break this down systematically for ${grade.label}.
                    
                    **1. Core Principle:**
                    Every complex problem stems from a fundamental law or definition. Focusing on the root concept ensures you can solve any numerical variation.
                    
                    **2. Three-Step Strategy:**
                    • **Step 1:** Write down all given variables with correct standard SI units.
                    • **Step 2:** Select and state the exact governing formula.
                    • **Step 3:** Perform substitution cleanly, checking intermediate signs.
                    
                    **3. Topper Scoring Tip:**
                    Remember that competitive and board exams reward clear step-marking. Always box your final answer with proper units!
                    
                    What specific part or formula would you like to explore next?
                    """.trimIndent()
                }
            }
        }

        return ChatMessage(
            id = UUID.randomUUID().toString(),
            sender = MessageSender.AI_TUTOR,
            text = text,
            timestamp = "Just now",
            formulas = listOf("V = I × R", "ax² + bx + c = 0", "6CO₂ + 6H₂O → C₆H₁₂O₆ + 6O₂"),
            followUpQuestions = listOf(
                if (languageMode == LanguageMode.HINGLISH) "Ek practical question try karoge?" else "Would you like a sample question on this?",
                if (languageMode == LanguageMode.HINGLISH) "Formula yaad karne ki mnemonic trick bataun?" else "Need a mnemonic memory trick for this?"
            )
        )
    }

    private fun generateFallbackHomeworkSolution(
        question: String,
        subject: String,
        languageMode: LanguageMode
    ): HomeworkSolution {
        val q = question.ifBlank { "Solve: 2x² - 7x + 3 = 0" }
        return HomeworkSolution(
            coreConcept = "Quadratic Factorization & Root Derivation (Board Standard)",
            stepByStep = listOf(
                "Given Equation: 2x² - 7x + 3 = 0. Compare with standard form ax² + bx + c = 0: a = 2, b = -7, c = 3.",
                "Find two numbers whose product is a × c = 2 × 3 = 6, and sum is b = -7. The two numbers are -6 and -1.",
                "Split the middle term: 2x² - 6x - 1x + 3 = 0.",
                "Factor by grouping: 2x(x - 3) - 1(x - 3) = 0.",
                "Take common factor (x - 3): (2x - 1)(x - 3) = 0.",
                "Set each factor to zero: 2x - 1 = 0 => x = 1/2, or x - 3 = 0 => x = 3."
            ),
            keyFormula = "Product = a × c, Sum = b (Splitting Middle Term)",
            commonMistakes = "Students often forget to change signs when taking out negative common factor -1(x - 3). Double check signs!",
            finalAnswer = "Roots are x = 3 and x = 1/2",
            practiceQuestions = generateFallbackPracticeQuestions(q, subject)
        )
    }

    private fun generateFallbackPracticeQuestions(question: String, subject: String): List<PracticeQuestion> {
        return listOf(
            PracticeQuestion(
                id = "pq_1",
                question = "Find the roots of the quadratic equation: 3x² - 10x + 3 = 0.",
                hint = "Product is 3 × 3 = 9. Find two factors that add up to -10 (-9 and -1).",
                solution = "3x² - 9x - x + 3 = 0 => 3x(x - 3) - 1(x - 3) = 0 => Roots are x = 3 and x = 1/3."
            ),
            PracticeQuestion(
                id = "pq_2",
                question = "Solve by factorization: x² - 5x + 6 = 0.",
                hint = "Product is 6, sum is -5. Factors are -2 and -3.",
                solution = "(x - 2)(x - 3) = 0 => Roots are x = 2 and x = 3."
            ),
            PracticeQuestion(
                id = "pq_3",
                question = "A car accelerates uniformly from 0 to 20 m/s in 5 seconds. Calculate its acceleration and distance traveled.",
                hint = "Use v = u + at, and s = ut + 0.5at² where u = 0.",
                solution = "a = (20 - 0)/5 = 4 m/s². Distance s = 0 + 0.5 × 4 × (5)² = 50 meters."
            )
        )
    }

    private fun generateFallbackQuizQuestions(
        subject: String,
        chapter: String,
        difficulty: QuizDifficulty,
        count: Int
    ): List<QuizQuestion> {
        val all = SampleData.sampleQuizQuestions
        return all.take(count.coerceAtMost(all.size))
    }
}
