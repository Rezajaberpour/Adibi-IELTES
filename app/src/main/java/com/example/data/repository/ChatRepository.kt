package com.example.data.repository

import com.example.BuildConfig
import com.example.data.api.GeminiClient
import com.example.data.api.GeminiRawConfig
import com.example.data.api.GeminiRawContent
import com.example.data.api.GeminiRawPart
import com.example.data.api.GeminiRawRequest
import com.example.data.local.ChatDao
import com.example.data.model.ChatMessage
import com.example.data.model.GeminiAiResponse
import com.example.data.model.KeyWord
import com.example.data.model.PracticeScenario
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.Flow
import org.json.JSONObject

class ChatRepository(private val chatDao: ChatDao) {

    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val adapter = moshi.adapter(GeminiAiResponse::class.java)

    fun getMessagesForScenario(scenarioId: String): Flow<List<ChatMessage>> =
        chatDao.getMessagesForScenario(scenarioId)

    fun getUserMessageCount(): Flow<Int> = chatDao.getUserMessageCount()

    suspend fun saveMessage(message: ChatMessage): Long = chatDao.insertMessage(message)

    suspend fun getMessageCountForScenario(scenarioId: String): Int = chatDao.getMessageCountForScenario(scenarioId)

    suspend fun clearHistory(scenarioId: String) = chatDao.clearMessagesForScenario(scenarioId)

    suspend fun sendUserMessageAndGetAiResponse(
        scenario: PracticeScenario,
        userMessageText: String,
        history: List<ChatMessage>
    ): GeminiAiResponse {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return generateFallbackResponse(userMessageText, scenario, history)
        }

        val systemInstructionText = """
            You are Adibi IELTS AI, an extraordinarily warm, engaging, intelligent, and encouraging language tutor.
            Current Practice Scenario: ${scenario.titleEnglish} (${scenario.titlePersian})
            Target Language: ${scenario.targetLanguage} (Language code: ${scenario.targetLanguageCode})
            Target Learner Level: ${scenario.difficultyLevel}
            Role: ${scenario.systemPromptRole}
            Supervisor & Methodology Lead: Ms. Zahra Adibi (خانم زهرا ادیبی - مدرس کانال یوتیوب @adibilearnskills)
            Official Educational Channel: https://youtube.com/@adibilearnskills?si=e9kQ2rrem3921gXT
            Instructional Base: Strictly follow the IELTS lessons, vocabulary series, and conversational methods from the @adibilearnskills YouTube tutorials.

            User's native language is Persian (Farsi).
            Rules:
            1. 'replyText': Respond in ${scenario.targetLanguage} naturally, warmly, and authentically at the user's level (1-3 sentences). Be friendly, ask interactive questions, and encourage them.
            2. 'translationPersian': Provide fluently translated Persian text of your response.
            3. 'grammarCorrection': Evaluate user's latest input ("$userMessageText"). If there are grammar or vocabulary mistakes, explain them gently in Persian with the corrected phrase. If error-free, return "".
            4. 'suggestedReplies': Provide 3 realistic next responses in ${scenario.targetLanguage} for the user to tap and learn.
            5. 'keyVocabulary': Extract 1 to 3 key words/idioms from your reply with phonetic, Persian meaning, and an example sentence.

            You MUST respond ONLY with a single JSON object with these keys:
            "replyText", "translationPersian", "grammarCorrection", "suggestedReplies", "keyVocabulary".
        """.trimIndent()

        // Build conversation history parts
        val contentsList = mutableListOf<GeminiRawContent>()
        history.takeLast(6).forEach { msg ->
            val role = if (msg.sender == "USER") "user" else "model"
            contentsList.add(
                GeminiRawContent(
                    role = role,
                    parts = listOf(GeminiRawPart(text = msg.text))
                )
            )
        }
        // Add current user message
        contentsList.add(
            GeminiRawContent(
                role = "user",
                parts = listOf(GeminiRawPart(text = userMessageText))
            )
        )

        val request = GeminiRawRequest(
            contents = contentsList,
            generationConfig = GeminiRawConfig(
                temperature = 0.7f,
                responseMimeType = "application/json"
            ),
            systemInstruction = GeminiRawContent(
                parts = listOf(GeminiRawPart(text = systemInstructionText))
            )
        )

        return kotlinx.coroutines.withTimeoutOrNull(8000L) {
            try {
                val response = try {
                    GeminiClient.apiService.generateContent("gemini-2.5-flash", apiKey, request)
                } catch (ex: Exception) {
                    GeminiClient.apiService.generateContent("gemini-1.5-flash", apiKey, request)
                }
                val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (!jsonText.isNullOrBlank()) {
                    parseGeminiJson(jsonText)
                } else {
                    generateFallbackResponse(userMessageText, scenario, history)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                generateFallbackResponse(userMessageText, scenario, history)
            }
        } ?: generateFallbackResponse(userMessageText, scenario, history)
    }

    private fun parseGeminiJson(jsonText: String): GeminiAiResponse {
        return try {
            // Clean markdown blocks if present (```json ... ```)
            val cleanJson = jsonText.trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()
            adapter.fromJson(cleanJson) ?: GeminiAiResponse(replyText = jsonText)
        } catch (e: Exception) {
            // Manual fallback parsing if JSON adapter fails
            try {
                val obj = JSONObject(jsonText)
                val reply = obj.optString("replyText", "That sounds interesting!")
                val trans = obj.optString("translationPersian", "این جالب به نظر می‌رسد!")
                val gram = obj.optString("grammarCorrection", "")
                val suggestions = mutableListOf<String>()
                val arr = obj.optJSONArray("suggestedReplies")
                if (arr != null) {
                    for (i in 0 until arr.length()) {
                        suggestions.add(arr.getString(i))
                    }
                }
                GeminiAiResponse(
                    replyText = reply,
                    translationPersian = trans,
                    grammarCorrection = gram,
                    suggestedReplies = suggestions
                )
            } catch (ex: Exception) {
                GeminiAiResponse(
                    replyText = jsonText,
                    translationPersian = "ترجمه در دسترس نیست",
                    suggestedReplies = listOf("Tell me more!", "That's great.", "Can you explain?")
                )
            }
        }
    }

    private fun generateFallbackResponse(
        userText: String,
        scenario: PracticeScenario,
        history: List<ChatMessage>
    ): GeminiAiResponse {
        val lower = userText.lowercase()
        val userTurnCount = history.count { it.sender == "USER" } + 1
        val isCoffee = scenario.id.contains("cafe")
        val isInterview = scenario.id.contains("interview")

        val (reply, translation, suggestions, keyVocab) = when {
            isCoffee -> when (userTurnCount % 5) {
                1 -> Tuple4(
                    "Great! What size would you like for your coffee today?",
                    "عالی است! چه اندازه‌ای برای قهوه‌تان در نظر دارید؟",
                    listOf("Medium size with oat milk, please.", "Large iced, please.", "What sizes do you offer?"),
                    listOf(KeyWord("Size", "/saɪz/", "اندازه / سایز", "What size would you like?"))
                )
                2 -> Tuple4(
                    "Excellent choice! Would you like any fresh pastry or muffin with that?",
                    "انتخاب فوق‌العاده‌ای بود! آیا شیرینی تازه یا مافین هم میل دارید؟",
                    listOf("A chocolate muffin, please.", "No thanks, just the coffee.", "What fresh pastries do you have?"),
                    listOf(KeyWord("Pastry", "/ˈpeɪ.stri/", "شیرینی / کیک", "Fresh pastries are available."))
                )
                3 -> Tuple4(
                    "Perfect! That comes to $6.50 in total. Will you be paying with cash or card?",
                    "عالی! مجموعاً می‌شود ۶.۵۰ دلار. با نقد پرداخت می‌کنید یا کارت؟",
                    listOf("I'll pay with credit card.", "Here is $10 cash.", "Do you accept Apple Pay?"),
                    listOf(KeyWord("Pay", "/peɪ/", "پرداخت کردن", "I will pay with card."))
                )
                4 -> Tuple4(
                    "Thank you! Here is your receipt. Your order will be ready at the pick-up counter in 2 minutes. Enjoy!",
                    "متشکرم! این هم رسید شما. سفارشتان تا ۲ دقیقه دیگر در باجه تحویل آماده است. لذت ببرید!",
                    listOf("Thank you so much!", "Where is the seating area?", "Have a wonderful day!"),
                    listOf(KeyWord("Receipt", "/rɪˈsiːt/", "رسید / فاکتور", "Here is your purchase receipt."))
                )
                else -> Tuple4(
                    "You're very welcome! Is there anything else I can help you with today at Espresso Cafe?",
                    "خواهش می‌کنم! آیا چیز دیگری هست که بتوانم امروز در کافه اسپرسو به شما کمک کنم؟",
                    listOf("Can I have the Wi-Fi password?", "Where are the restrooms?", "Everything is great, thanks!"),
                    listOf(KeyWord("Welcome", "/ˈwel.kəm/", "خوش آمدید / خواهش می‌کنم", "You are always welcome!"))
                )
            }
            isInterview -> when (userTurnCount % 4) {
                1 -> Tuple4(
                    "Thank you for sharing. Could you tell me about a project you are most proud of?",
                    "ممنون از اشتراک‌گذاری. می‌توانید درباره پروژه‌ای که بیشترین افتخار را به آن می‌کنید بگوئید؟",
                    listOf("I built a real-time web application recently.", "I led a cross-functional team of 5 people.", "I optimized our customer onboarding process."),
                    listOf(KeyWord("Proud", "/praʊd/", "افتخار کردن / سربلند", "I am proud of this achievement."))
                )
                2 -> Tuple4(
                    "Impressive! How do you handle tight deadlines and high-pressure situations at work?",
                    "تاثیرگذار است! چطور با ددلاین‌های فشرده و شرایط پرفشار کاری برخورد می‌کنید؟",
                    listOf("I prioritize tasks and communicate clearly.", "I stay calm and break tasks into steps.", "I delegate when necessary."),
                    listOf(KeyWord("Deadline", "/ˈded.laɪn/", "مهلت زمانی / ددلاین", "We met the project deadline."))
                )
                3 -> Tuple4(
                    "That shows great adaptability. Where do you see yourself professionally in three years?",
                    "این نشان‌دهنده انعطاف‌پذیری عالی است. سه سال دیگر خود را از نظر حرفه‌ای کجا می‌بینید؟",
                    listOf("Leading senior technical projects.", "Growing as a full-stack engineer.", "Mentoring junior team members."),
                    listOf(KeyWord("Professionally", "/prəˈfeʃ.ən.əl.i/", "از نظر حرفه‌ای / شغلی", "Growing professionally every day."))
                )
                else -> Tuple4(
                    "Thank you for this insightful conversation! Do you have any questions for me about our company?",
                    "ممنون بابت این گفتگو هوشمندانه! آیا سوالی از من درباره شرکتمان دارید؟",
                    listOf("What is the team culture like?", "What are the main goals for this role?", "When can I expect to hear back?"),
                    listOf(KeyWord("Insightful", "/ˈɪn.saɪt.fəl/", "پر از بصیرت / آموزنده", "An insightful interview session."))
                )
            }
            else -> when (userTurnCount % 4) {
                1 -> Tuple4(
                    "That's a great point! How long have you been practicing this topic?",
                    "نکته بسیار خوبی بود! چه مدت است که روی این موضوع تمرین می‌کنید؟",
                    listOf("I have been practicing for a few months.", "I just started learning recently.", "I practice every single day."),
                    listOf(KeyWord("Practice", "/ˈpræk.tɪs/", "تمرین کردن", "Practice makes perfect."))
                )
                2 -> Tuple4(
                    "Wonderful! What is the most challenging part about it for you?",
                    "فوق‌العاده است! سخت‌ترین بخش آن برای شما چیست؟",
                    listOf("Pronunciation and accent.", "Remembering new vocabulary.", "Understanding native speakers."),
                    listOf(KeyWord("Challenging", "/ˈtʃæl.ɪn.dʒɪŋ/", "چالش‌برانگیز", "A challenging exercise."))
                )
                3 -> Tuple4(
                    "I completely understand. Daily continuous practice is the best secret to mastering it!",
                    "کاملاً متوجه می‌شوم. تمرین روزانه و مداوم بهترین راز برای تسلط بر آن است!",
                    listOf("I will keep practicing daily!", "Can you suggest some useful idioms?", "Thanks for the encouragement!"),
                    listOf(KeyWord("Continuous", "/kənˈtɪn.ju.əs/", "مداوم / پیوسته", "Continuous learning brings success."))
                )
                else -> Tuple4(
                    "You are making great progress! What topic shall we practice next?",
                    "شما پیشرفت بسیار خوبی داشته‌اید! چه موضوعی را در ادامه تمرین کنیم؟",
                    listOf("Let's talk about travel and vacations.", "I want to practice daily conversations.", "Can we review grammar rules?"),
                    listOf(KeyWord("Progress", "/ˈprəʊ.ɡres/", "پیشرفت / رشد", "Making steady progress every day."))
                )
            }
        }

        val grammar = when {
            userText.length < 4 -> "نکته آموزشی: سعی کنید جملات کامل‌تری بنویسید تا مهارت ساختار جملات شما تقویت شود."
            lower.contains("i is") -> "اصلاح گرامری: به جای 'I is' از 'I am' استفاده کنید."
            lower.contains("he go") -> "اصلاح گرامری: برای سوم شخص مفرد از 'he goes' استفاده کنید."
            else -> ""
        }

        return GeminiAiResponse(
            replyText = reply,
            translationPersian = translation,
            grammarCorrection = grammar,
            suggestedReplies = suggestions,
            keyVocabulary = keyVocab
        )
    }

    private data class Tuple4<A, B, C, D>(
        val first: A,
        val second: B,
        val third: C,
        val fourth: D
    )
}
