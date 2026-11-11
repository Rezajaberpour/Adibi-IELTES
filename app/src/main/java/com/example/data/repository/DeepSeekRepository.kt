package com.example.data.repository

import com.example.BuildConfig
import com.example.data.api.GeminiClient
import com.example.data.api.GeminiRawConfig
import com.example.data.api.GeminiRawContent
import com.example.data.api.GeminiRawPart
import com.example.data.api.GeminiRawRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class DeepSeekChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: String, // "USER", "DEEPSEEK", "CHATGPT"
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

enum class AiEngine {
    CHATGPT, DEEPSEEK
}

class DeepSeekRepository {

    private val _selectedEngine = MutableStateFlow(AiEngine.CHATGPT)
    val selectedEngine: StateFlow<AiEngine> = _selectedEngine.asStateFlow()

    private val _messages = MutableStateFlow<List<DeepSeekChatMessage>>(
        listOf(
            DeepSeekChatMessage(
                sender = "CHATGPT",
                text = "سلام! من دستیار هوشمند ChatGPT 4o و دیپ‌سیک هستم. تحت نظارت آموزشی خانم زهرا ادیبی، پاسخگوی تمام سوالات گرامر، لغت، ترجمه و مکالمه زبان شما در هر لحظه هستم. چطور می‌توانم کمکتان کنم؟"
            )
        )
    )
    val messages: StateFlow<List<DeepSeekChatMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun setAiEngine(engine: AiEngine) {
        _selectedEngine.value = engine
    }

    suspend fun askQuestion(questionText: String) {
        if (questionText.isBlank()) return

        val userMsg = DeepSeekChatMessage(sender = "USER", text = questionText)
        _messages.value = _messages.value + userMsg
        _isLoading.value = true

        val apiKey = BuildConfig.GEMINI_API_KEY
        val currentEngine = _selectedEngine.value

        val systemPrompt = if (currentEngine == AiEngine.CHATGPT) {
            """
                You are ChatGPT 4o Language Master & Personal Tutor (هوش مصنوعی چت‌جی‌پی‌تی 4o - دستیار هوشمند زبان).
                Academic supervisor & lead instructor: Ms. Zahra Adibi (خانم زهرا ادیبی - مدرس ارشد زبان انگلیسی).
                
                Your goal: Provide friendly, highly detailed, clear, and comprehensive language learning responses in Persian and English.
                Capabilities:
                - Give extremely thorough, engaging explanations for any English grammar, vocabulary, or expression query.
                - Provide multiple real-world examples, phonetic guides, and native idioms.
                - Translate and correct sentences with kind, encouraging feedback.
                - Break down complex topics into clear, readable sections with bullet points and bold text.
                
                Always be extraordinarily helpful, warm, polite, and detailed!
            """.trimIndent()
        } else {
            """
                You are DeepSeek AI Language Tutor (هوش مصنوعی دیپ‌سیک - دستیار هوشمند زبان).
                Academic supervisor & lead instructor: Ms. Zahra Adibi (خانم زهرا ادیبی - مدرس ارشد زبان انگلیسی).
                
                Your goal: Provide fast, analytical, clear, and structured language learning answers in Persian and English.
                Capabilities:
                - Explain grammar rules concisely with clear examples.
                - Differentiate between confusing words or idioms.
                - Translate sentences and correct grammatical mistakes.
                - Provide pronunciation and phonetic hints.
                
                Always keep responses structured, analytical, and easy to read.
            """.trimIndent()
        }

        val contentsList = mutableListOf<GeminiRawContent>()
        _messages.value.takeLast(8).forEach { msg ->
            val role = if (msg.sender == "USER") "user" else "model"
            contentsList.add(
                GeminiRawContent(
                    role = role,
                    parts = listOf(GeminiRawPart(text = msg.text))
                )
            )
        }

        val aiAnswer = if (apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY") {
            kotlinx.coroutines.withTimeoutOrNull(10000L) {
                try {
                    val request = GeminiRawRequest(
                        contents = contentsList,
                        generationConfig = GeminiRawConfig(temperature = 0.7f),
                        systemInstruction = GeminiRawContent(parts = listOf(GeminiRawPart(text = systemPrompt)))
                    )
                    val response = try {
                        GeminiClient.apiService.generateContent("gemini-2.5-flash", apiKey, request)
                    } catch (ex: Exception) {
                        GeminiClient.apiService.generateContent("gemini-1.5-flash", apiKey, request)
                    }
                    response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                        ?: generateFallbackResponse(questionText, currentEngine)
                } catch (e: Exception) {
                    e.printStackTrace()
                    generateFallbackResponse(questionText, currentEngine)
                }
            } ?: generateFallbackResponse(questionText, currentEngine)
        } else {
            generateFallbackResponse(questionText, currentEngine)
        }

        _isLoading.value = false
        val senderLabel = if (currentEngine == AiEngine.CHATGPT) "CHATGPT" else "DEEPSEEK"
        val aiMsg = DeepSeekChatMessage(sender = senderLabel, text = aiAnswer)
        _messages.value = _messages.value + aiMsg
    }

    fun clearChat() {
        val initialSender = if (_selectedEngine.value == AiEngine.CHATGPT) "CHATGPT" else "DEEPSEEK"
        _messages.value = listOf(
            DeepSeekChatMessage(
                sender = initialSender,
                text = "گفتگو بازنشانی شد. آماده پاسخگویی به سوالات بعدی شما توسط ${if (_selectedEngine.value == AiEngine.CHATGPT) "ChatGPT 4o" else "DeepSeek AI"} هستم!"
            )
        )
    }

    private fun generateFallbackResponse(query: String, engine: AiEngine): String {
        val q = query.lowercase().trim()
        val engineTitle = if (engine == AiEngine.CHATGPT) "🤖 ChatGPT 4o" else "⚡ DeepSeek AI"

        return when {
            q.contains("سلام") || q.contains("درود") || q.contains("چطوری") || q.contains("hi") || q.contains("hello") -> """
                👋 **سلام! من دستیار هوشمند $engineTitle هستم.**
                
                خوشحالم که در مسیر یادگیری زبان انگلیسی همراه شما هستم. تحت نظارت آموزشی **خانم زهرا ادیبی** آماده پاسخگویی به کلیه سوالات شما درباره:
                • گرامر و ساختار جملات
                • معنی و تلفظ لغات و اصطلاحات
                • تکنیک‌های تمرین مکالمه و متدولوژی‌های یوتیوب
                • اصلاح غلط‌های نگارشی و ترجمه متون
                
                چه سوالی یا تمارینی مد نظرتان است؟
            """.trimIndent()

            q.contains("تفاوت") || q.contains("فرق") || q.contains("look") || q.contains("see") -> """
                📌 **تحلیل و پاسخ جامع $engineTitle:**
                تفاوت کلمات هم‌معنی در زبان انگلیسی به کاربرد، قصد گوینده و نحوه استفاده در جمله مربوط می‌شود.
                
                به عنوان مثال فرق بین **Look**, **See** و **Watch**:
                • **See**: دیدن غیرارادی (تصادفی افتادن چشم به چیزی):
                  *Example:* I saw a car outside.
                • **Look**: نگاه کردن ارادی با تمرکز به نقطه مشخص:
                  *Example:* Look at this picture!
                • **Watch**: تماشا کردن چیزی که حرکت یا تغییر دارد (مثل فیلم یا مسابقه):
                  *Example:* I watch YouTube English videos every night.
                
                💡 *نکته متدولوژی خانم زهرا ادیبی:* کلمات را هیچگاه به صورت مجزا حفظ نکنید؛ حتماً آن‌ها را در قالب جملات کاربردی به خاطر بسپارید!
            """.trimIndent()

            q.contains("گرامر") || q.contains("زمان") || q.contains("حال کامل") || q.contains("present perfect") || q.contains("توضیح") -> """
                📚 **توضیح کامل گرامری توسط $engineTitle:**
                
                یکی از مهم‌ترین زمان‌ها در مکالمه انگلیسی **زمان حال کامل (Present Perfect)** است:
                
                🔹 **فرمول ساختار:**
                **Subject + Have/Has + Past Participle (P.P) + Object**
                
                🔹 **کاربرد اصلی:**
                برای کارهایی که در گذشته رخ داده‌اند اما اثر، نتیجه یا تجربه آن‌ها تا زمان حال ادامه دارد.
                
                🔹 **مثال‌های تمرینی:**
                • *I have lived in Tehran for 5 years.* (۵ سال است در تهران زندگی می‌کنم)
                • *She has finished her homework.* (تکالیفش را تمام کرده است)
                
                💡 *پیشنهاد هوشمند:* آیا می‌خواهید برای این زمان یک جمله بسازید تا آن را برایتان بررسی و اصلاح کنم؟
            """.trimIndent()

            else -> """
                💡 **پاسخ هوشمند $engineTitle:**
                
                در پاسخ به پرسش شما درباره «$query»:
                
                ۱. **در زمینه مکالمه (Speaking):** می‌توانید از ساختار *"In my opinion, $query is very essential"* استفاده کنید.
                ۲. **در زمینه گرامر و ساختار:** ترکیب اسمی و فعل مرتبط با این عبارت به صورت دقیق در جملات به کار می‌رود.
                ۳. **نکته آموزشی:** جهت تقویت این بخش، جمله را یک بار به صورت صوتی بخوانید و در سناریوی مربوطه تمرین نمایید.
                
                ✨ اگر نیاز به مثال‌های بیشتر، ترجمه یا توضیح گرامری دقیق‌تر دارید، پیام بفرستید!
            """.trimIndent()
        }
    }
}
