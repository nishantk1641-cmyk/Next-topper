package com.example.data

object SampleData {
    val initialProfile = UserProfile(
        name = "Aarav Sharma",
        grade = StudentGrade.CLASS_10,
        targetScore = "95%+ Board Target",
        streakDays = 7,
        overallProgressPercent = 78,
        completedLessons = 28,
        averageQuizScore = 88,
        languageMode = LanguageMode.HINGLISH
    )

    val subjects = listOf(
        SubjectItem(
            id = "math",
            name = "Mathematics",
            iconName = "Calculate",
            totalChapters = 14,
            completedChapters = 11,
            progressPercent = 82,
            accentColorHex = 0xFF2563EB,
            chapters = listOf(
                "Real Numbers",
                "Polynomials",
                "Linear Equations in 2 Variables",
                "Quadratic Equations",
                "Arithmetic Progressions",
                "Triangles & Similarity",
                "Coordinate Geometry",
                "Introduction to Trigonometry",
                "Applications of Trigonometry",
                "Circles",
                "Surface Areas and Volumes",
                "Statistics & Probability"
            )
        ),
        SubjectItem(
            id = "physics",
            name = "Physics",
            iconName = "Bolt",
            totalChapters = 6,
            completedChapters = 4,
            progressPercent = 75,
            accentColorHex = 0xFF0D9488,
            chapters = listOf(
                "Light - Reflection & Refraction",
                "The Human Eye & Colourful World",
                "Electricity & Ohm's Law",
                "Magnetic Effects of Electric Current",
                "Sources of Energy"
            )
        ),
        SubjectItem(
            id = "chemistry",
            name = "Chemistry",
            iconName = "Science",
            totalChapters = 5,
            completedChapters = 4,
            progressPercent = 80,
            accentColorHex = 0xFF7C3AED,
            chapters = listOf(
                "Chemical Reactions & Equations",
                "Acids, Bases and Salts",
                "Metals and Non-Metals",
                "Carbon and Its Compounds",
                "Periodic Classification of Elements"
            )
        ),
        SubjectItem(
            id = "biology",
            name = "Biology",
            iconName = "Biotech",
            totalChapters = 6,
            completedChapters = 5,
            progressPercent = 88,
            accentColorHex = 0xFF059669,
            chapters = listOf(
                "Life Processes (Nutrition & Respiration)",
                "Control and Coordination",
                "How Do Organisms Reproduce?",
                "Heredity and Evolution",
                "Our Environment & Sustainable Management"
            )
        ),
        SubjectItem(
            id = "social",
            name = "Social Science",
            iconName = "Public",
            totalChapters = 12,
            completedChapters = 8,
            progressPercent = 67,
            accentColorHex = 0xFFD97706,
            chapters = listOf(
                "The Rise of Nationalism in Europe",
                "Nationalism in India",
                "Resources and Development",
                "Federalism & Power Sharing",
                "Money and Credit",
                "Globalization & The Indian Economy"
            )
        ),
        SubjectItem(
            id = "english",
            name = "English Language",
            iconName = "MenuBook",
            totalChapters = 10,
            completedChapters = 7,
            progressPercent = 70,
            accentColorHex = 0xFFE11D48,
            chapters = listOf(
                "A Letter to God",
                "Nelson Mandela: Long Walk to Freedom",
                "Direct & Indirect Speech Rules",
                "Active & Passive Voice Mastery",
                "Analytical Paragraph Writing"
            )
        )
    )

    val todayTasks = listOf(
        StudyTask(
            id = "task_1",
            subject = "Mathematics",
            title = "Solve 5 Quadratic Equation word problems",
            estimatedMinutes = 25,
            isCompleted = false,
            tag = "Numericals"
        ),
        StudyTask(
            id = "task_2",
            subject = "Physics",
            title = "Revise Snell's Law & Ray Diagrams for Concave Mirrors",
            estimatedMinutes = 20,
            isCompleted = true,
            tag = "Concept"
        ),
        StudyTask(
            id = "task_3",
            subject = "Chemistry",
            title = "Memorize Reactivity Series & Displacement reactions",
            estimatedMinutes = 15,
            isCompleted = false,
            tag = "Formula Trap"
        ),
        StudyTask(
            id = "task_4",
            subject = "Biology",
            title = "Take 5-min AI Quiz on Nephron & Kidney filtration",
            estimatedMinutes = 10,
            isCompleted = false,
            tag = "AI Quiz"
        )
    )

    val upcomingExams = listOf(
        UpcomingExam(
            id = "exam_1",
            subject = "Science (Physics + Chem)",
            examName = "CBSE Class 10 Pre-Board 1",
            daysLeft = 5,
            targetDate = "Thursday, Nov 18",
            priority = "High Priority"
        ),
        UpcomingExam(
            id = "exam_2",
            subject = "Standard Mathematics",
            examName = "Mid-Term Assessment",
            daysLeft = 14,
            targetDate = "Tuesday, Nov 27",
            priority = "Target 95%+"
        ),
        UpcomingExam(
            id = "exam_3",
            subject = "Social Science",
            examName = "Unit Test: History & Civics",
            daysLeft = 21,
            targetDate = "Monday, Dec 4",
            priority = "Revision Due"
        )
    )

    val starterTutorMessages = listOf(
        ChatMessage(
            id = "welcome_1",
            sender = MessageSender.AI_TUTOR,
            text = "Namaste Aarav! 🙏 Main hoon aapka personal AI Topper Tutor. Aaj hum kaunsa concept master karne wale hain? \n\nKuch bhi pooch sakte ho — whether it's Math proofs, Physics numericals, ya Chemistry reactions! Main simple language mein explain karunga aur questions bhi poochunga taaki concepts dimag mein permanently set ho jayein!",
            timestamp = "10:00 AM",
            formulas = listOf("Tips: You can switch between English & Hinglish anytime!"),
            followUpQuestions = listOf(
                "Why is (a+b)² = a² + 2ab + b² geometrically?",
                "Explain Snell's law with simple analogy",
                "Difference between Exothermic and Endothermic?",
                "Test my knowledge on Quadratic Equations"
            )
        )
    )

    val quickQuestionsList = listOf(
        "Explain Ohm's Law with water pipe analogy (Hinglish)",
        "How to balance chemical equations step-by-step?",
        "Why do stars twinkle but planets do not?",
        "Quadratic formula proof: x = (-b ± √(b²-4ac)) / 2a",
        "Explain Photosynthesis light & dark reactions in simple English",
        "How does Kidney nephron filter blood?"
    )

    val sampleQuizQuestions = listOf(
        QuizQuestion(
            id = "q_1",
            question = "If the discriminant of a quadratic equation ax² + bx + c = 0 is greater than zero (D > 0), the roots are:",
            options = listOf(
                "Real and unequal (distinct)",
                "Real and equal",
                "No real roots (imaginary)",
                "Both roots are zero"
            ),
            correctOptionIndex = 0,
            explanation = "When D = b² - 4ac > 0, the square root √D is a non-zero real number, resulting in two distinct real roots: x = (-b + √D)/2a and x = (-b - √D)/2a.",
            conceptHint = "Remember: D > 0 means 2 distinct real roots, D = 0 means 2 equal roots, D < 0 means no real roots!",
            difficulty = QuizDifficulty.EASY
        ),
        QuizQuestion(
            id = "q_2",
            question = "A light ray travels from water (optically denser) into air (optically rarer). What happens to the ray?",
            options = listOf(
                "Bends towards the normal and slows down",
                "Bends away from the normal and speeds up",
                "Travels straight without deviation",
                "Reflects totally in all angles"
            ),
            correctOptionIndex = 1,
            explanation = "When light moves from a denser medium to a rarer medium, its speed increases. Consequently, according to Snell's law, it bends AWAY from the normal line.",
            conceptHint = "Denser to Rarer = Speed UP & Away from Normal!",
            difficulty = QuizDifficulty.MEDIUM
        ),
        QuizQuestion(
            id = "q_3",
            question = "Which substance is oxidized in the reaction: CuO + H₂ → Cu + H₂O?",
            options = listOf(
                "CuO is oxidized",
                "H₂ is oxidized",
                "Cu is oxidized",
                "H₂O is oxidized"
            ),
            correctOptionIndex = 1,
            explanation = "Oxidation means gain of oxygen or loss of electrons. Here, H₂ gains oxygen to form H₂O, so H₂ is oxidized! Meanwhile, CuO loses oxygen and gets reduced to Cu.",
            conceptHint = "Gain of Oxygen = Oxidation. H₂ became H₂O!",
            difficulty = QuizDifficulty.MEDIUM
        ),
        QuizQuestion(
            id = "q_4",
            question = "In the human digestive system, bile juice secreted by the liver is stored in the:",
            options = listOf(
                "Pancreas",
                "Stomach",
                "Gall bladder",
                "Small intestine"
            ),
            correctOptionIndex = 2,
            explanation = "Bile is produced continuously by the liver, but is stored and concentrated in the gall bladder until needed for emulsifying fats in the duodenum (small intestine).",
            conceptHint = "Liver produces bile; Gall bladder stores it!",
            difficulty = QuizDifficulty.EASY
        ),
        QuizQuestion(
            id = "q_5",
            question = "A wire of resistance R is cut into 5 equal parts. These parts are then connected in parallel. What is the equivalent resistance R'?",
            options = listOf(
                "R / 5",
                "5 R",
                "R / 25",
                "25 R"
            ),
            correctOptionIndex = 2,
            explanation = "Each piece has resistance r = R / 5. When 5 identical resistors each of resistance r are placed in parallel: 1/R' = 5 * (1/r) = 5 * (5/R) = 25/R. Therefore, R' = R / 25.",
            conceptHint = "Classic board question trap! Cutting divides R, parallel divides again by n!",
            difficulty = QuizDifficulty.HARD
        )
    )
}
