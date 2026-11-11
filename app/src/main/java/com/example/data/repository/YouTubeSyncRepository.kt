package com.example.data.repository

import com.example.data.api.YouTubeFeedEntry
import com.example.data.api.YouTubeFeedParser
import com.example.data.model.PracticeScenario
import com.example.data.model.ScenarioData
import com.example.data.model.YouTubeLesson
import com.example.data.model.YouTubeLessonData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

object YouTubeSyncRepository {

    private const val CHANNEL_FEED_URL = "https://www.youtube.com/feeds/videos.xml?channel_id=UC7nsJq5hVH6xvl7ZkdrOKkw"
    const val CHANNEL_WEB_URL = "https://youtube.com/@adibilearnskills?si=e9kQ2rrem3921gXT"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _lastSyncTime = MutableStateFlow<Long?>(null)
    val lastSyncTime: StateFlow<Long?> = _lastSyncTime.asStateFlow()

    private val _syncMessage = MutableStateFlow("آماده به‌روزرسانی")
    val syncMessage: StateFlow<String> = _syncMessage.asStateFlow()

    private val _lessons = MutableStateFlow<List<YouTubeLesson>>(YouTubeLessonData.lessonsList)
    val lessons: StateFlow<List<YouTubeLesson>> = _lessons.asStateFlow()

    private val _scenarios = MutableStateFlow<List<PracticeScenario>>(ScenarioData.scenariosList)
    val scenarios: StateFlow<List<PracticeScenario>> = _scenarios.asStateFlow()

    /**
     * Called whenever the app is opened or when the user pulls to refresh.
     * Fetches real-time video feed from @adibilearnskills YouTube channel,
     * builds and appends dynamic lessons and practice scenarios.
     */
    fun syncLatestContent(scope: CoroutineScope, onComplete: ((Boolean) -> Unit)? = null) {
        if (_isSyncing.value) return

        scope.launch {
            _isSyncing.value = true
            _syncMessage.value = "در حال دریافت آخرین آموزش‌ها و سناریوها از کانال یوتیوب..."

            val success = withContext(Dispatchers.IO) {
                try {
                    val request = Request.Builder()
                        .url(CHANNEL_FEED_URL)
                        .header("User-Agent", "Mozilla/5.0 (Android; Mobile; rv:120.0) Gecko/120.0 Firefox/120.0")
                        .header("Accept", "application/atom+xml,application/xml,text/xml")
                        .build()

                    val response = client.newCall(request).execute()
                    if (response.isSuccessful) {
                        val bodyString = response.body?.string() ?: ""
                        if (bodyString.isNotBlank()) {
                            val entries = YouTubeFeedParser.parse(bodyString)
                            if (entries.isNotEmpty()) {
                                processFeedEntries(entries)
                                true
                            } else {
                                false
                            }
                        } else {
                            false
                        }
                    } else {
                        false
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    false
                }
            }

            _isSyncing.value = false
            if (success) {
                _lastSyncTime.value = System.currentTimeMillis()
                _syncMessage.value = "آموزش‌ها و سناریوهای جدید از کانال @adibilearnskills اضافه شد"
            } else {
                _syncMessage.value = "عدم دسترسی به اینترنت؛ مطالب آفلاین بارگذاری شد"
            }
            onComplete?.invoke(success)
        }
    }

    private fun processFeedEntries(entries: List<YouTubeFeedEntry>) {
        val newLessons = mutableListOf<YouTubeLesson>()
        val newScenarios = mutableListOf<PracticeScenario>()

        for (entry in entries) {
            val scenarioId = "scenario_yt_${entry.videoId}"
            val lessonId = "lesson_yt_${entry.videoId}"

            val parsedInfo = parseVideoTitleAndDetails(entry.title, entry.description)

            val lesson = YouTubeLesson(
                id = lessonId,
                titlePersian = parsedInfo.titlePersian,
                titleEnglish = parsedInfo.titleEnglish,
                channelName = "کانال رسمی خانم زهرا ادیبی (@adibilearnskills)",
                category = parsedInfo.category,
                seriesName = parsedInfo.seriesName,
                summaryPersian = parsedInfo.summaryPersian,
                keyPoints = parsedInfo.keyPoints,
                targetScenarioId = scenarioId,
                videoUrl = "https://www.youtube.com/watch?v=${entry.videoId}",
                bandScore = parsedInfo.bandScore
            )
            newLessons.add(lesson)

            val scenario = PracticeScenario(
                id = scenarioId,
                titlePersian = parsedInfo.scenarioTitlePersian,
                titleEnglish = parsedInfo.scenarioTitleEnglish,
                category = parsedInfo.scenarioCategory,
                targetLanguage = "انگلیسی",
                targetLanguageCode = "en",
                difficultyLevel = parsedInfo.difficultyLevel,
                descriptionPersian = parsedInfo.scenarioDescPersian,
                initialAiGreeting = parsedInfo.initialAiGreeting,
                initialAiTranslation = parsedInfo.initialAiTranslation,
                systemPromptRole = parsedInfo.systemPromptRole,
                keyPhrases = parsedInfo.keyPhrases
            )
            newScenarios.add(scenario)
        }

        // Merge dynamically fetched lessons with seeded baseline, deduplicating by title/videoId
        val mergedLessons = mutableListOf<YouTubeLesson>()
        mergedLessons.addAll(newLessons)
        for (seed in YouTubeLessonData.lessonsList) {
            if (mergedLessons.none { it.id == seed.id || it.titleEnglish.equals(seed.titleEnglish, ignoreCase = true) }) {
                mergedLessons.add(seed)
            }
        }

        // Merge dynamically generated scenarios with seeded baseline, deduplicating by id
        val mergedScenarios = mutableListOf<PracticeScenario>()
        mergedScenarios.addAll(newScenarios)
        for (seed in ScenarioData.scenariosList) {
            if (mergedScenarios.none { it.id == seed.id }) {
                mergedScenarios.add(seed)
            }
        }

        _lessons.value = mergedLessons
        _scenarios.value = mergedScenarios
    }

    private data class ParsedVideoInfo(
        val titlePersian: String,
        val titleEnglish: String,
        val category: String,
        val seriesName: String,
        val summaryPersian: String,
        val keyPoints: List<String>,
        val bandScore: String,
        val scenarioTitlePersian: String,
        val scenarioTitleEnglish: String,
        val scenarioCategory: String,
        val difficultyLevel: String,
        val scenarioDescPersian: String,
        val initialAiGreeting: String,
        val initialAiTranslation: String,
        val systemPromptRole: String,
        val keyPhrases: List<Pair<String, String>>
    )

    private fun parseVideoTitleAndDetails(rawTitle: String, rawDesc: String): ParsedVideoInfo {
        val cleanTitle = rawTitle.replace("/ Zahra Adibi", "")
            .replace("/Zahra Adibi", "")
            .trim()

        val lower = (rawTitle + " " + rawDesc).lowercase()

        return when {
            lower.contains("children and the family") || lower.contains("lesson forty_ four") || lower.contains("lesson 44") -> {
                ParsedVideoInfo(
                    titlePersian = "درس ۴۴: واژگان کودکان و روابط خانوادگی (Children and Family)",
                    titleEnglish = cleanTitle,
                    category = "واژگان تخصصی آیلتس",
                    seriesName = "Check Your English Vocabulary for IELTS",
                    summaryPersian = "تحلیل واژگان کلیدی تربیت فرزند، الگوهای فرزندپروری، روابط اعضای خانواده و ارزش‌های خانوادگی برای رایتینگ و اسپیکینگ.",
                    keyPoints = listOf(
                        "واژگان سبک‌های فرزندپروری: Authoritative, Permissive, Upbringing",
                        "اصطلاحات پیوندهای خانوادگی: Nuclear family, Extended family, Tight-knit",
                        "پاسخ به سوالات پارت ۲ و ۳ اسپیکینگ پیرامون نقش والدین در شکل‌گیری شخصیت"
                    ),
                    bandScore = "Band 7.5+",
                    scenarioTitlePersian = "بحث خانواده و فرزندپروری (درس ۴۴)",
                    scenarioTitleEnglish = "Family Values & Parenting (Lesson 44)",
                    scenarioCategory = "زندگی روزمره",
                    difficultyLevel = "B2 (پیشرفته)",
                    scenarioDescPersian = "تمرین مباحثه درباره نقش خانواده، سبک‌های تربیت فرزند و ارزش‌های خانوادگی مبتنی بر درس ۴۴ یوتیوب",
                    initialAiGreeting = "Hello! In today's speaking practice from Lesson 44, let's discuss family values. How do you describe a tight-knit family?",
                    initialAiTranslation = "سلام! در تمرین اسپیکینگ امروز از درس ۴۴، بیایید درباره ارزش‌های خانوادگی صحبت کنیم. یک خانواده صمیمی و متحد را چطور توصیف می‌کنید؟",
                    systemPromptRole = "You are an IELTS Speaking Examiner evaluating the candidate on family relationships, parenting styles, and upbringing using Lesson 44 vocabulary.",
                    keyPhrases = listOf(
                        "A tight-knit family provides strong emotional support." to "یک خانواده متحد حمایت عاطفی قدرتمندی فراهم می‌کند.",
                        "Parenting styles significantly shape a child's character." to "سبک‌های فرزندپروری به شکل چشمگیری شخصیت کودک را شکل می‌دهند.",
                        "Upbringing plays a pivotal role in early development." to "تربیت نقشی محوری در رشد اولیه ایفا می‌کند.",
                        "Family traditions foster a sense of belonging." to "سنت‌های خانوادگی حس تعلق را تقویت می‌کنند."
                    )
                )
            }

            lower.contains("business and industry") || lower.contains("lesson forty_ three") || lower.contains("lesson 43") -> {
                ParsedVideoInfo(
                    titlePersian = "درس ۴۳: تجارت، صنایع و کارآفرینی (Business and Industry)",
                    titleEnglish = cleanTitle,
                    category = "واژگان تخصصی آیلتس",
                    seriesName = "Check Your English Vocabulary for IELTS",
                    summaryPersian = "آموزش اصطلاحات پیشرفته اقتصادی، صنایع نوظهور، کارآفرینی و چالش‌های دنیای کسب‌وکار.",
                    keyPoints = listOf(
                        "اصطلاحات دنیای تجارت: Entrepreneurship, Multinational corporation, Market share",
                        "چالش‌های محیط کار و بحران‌های مالی: Economic downturn, Profit margin, Innovation",
                        "استفاده از اصطلاحات رسمی برای تسک ۱ و ۲ رایتینگ آیلتس"
                    ),
                    bandScore = "Band 8.0+",
                    scenarioTitlePersian = "کسب‌وکار و کارآفرینی (درس ۴۳)",
                    scenarioTitleEnglish = "Business, Economy & Industry (Lesson 43)",
                    scenarioCategory = "کسب‌وکار و شغل",
                    difficultyLevel = "B2 (پیشرفته)",
                    scenarioDescPersian = "تمرین صحبت پیرامون استارتاپ‌ها، فضای کسب‌وکار و اقتصاد جهانی بر پایه درس ۴۳ یوتیوب",
                    initialAiGreeting = "Welcome! Let's explore Business and Industry from Lesson 43. What major challenges do new entrepreneurs face in competitive markets?",
                    initialAiTranslation = "خوش آمدید! بیایید مبحث تجارت و صنعت از درس ۴۳ را بررسی کنیم. کارآفرینان جدید در بازارهای رقابتی با چه چالش‌های بزرگی روبه‌رو هستند؟",
                    systemPromptRole = "You are an IELTS Speaking partner discussing business, entrepreneurship, and global market dynamics based on Lesson 43.",
                    keyPhrases = listOf(
                        "Securing funding is a crucial hurdle for startups." to "تامین سرمایه مانعی حیاتی برای استارتاپ‌هاست.",
                        "Technological innovation drives market competition." to "نوآوری فناوری پیشران رقابت بازار است.",
                        "Sustainable business models ensure long-term profitability." to "مدل‌های تجاری پایدار سودآوری بلندمدت را تضمین می‌کنند.",
                        "Workplace adaptability is essential for modern employees." to "سازگاری در محیط کار برای کارکنان امروزی ضروری است."
                    )
                )
            }

            lower.contains("the arts") || lower.contains("lesson forty_ two") || lower.contains("lesson 42") -> {
                ParsedVideoInfo(
                    titlePersian = "درس ۴۲: هنر، نقاشی و خلاقیت فرهنگی (The Arts)",
                    titleEnglish = cleanTitle,
                    category = "واژگان تخصصی آیلتس",
                    seriesName = "Check Your English Vocabulary for IELTS",
                    summaryPersian = "واژگان تخصصی توصیف آثار هنری، موسیقی، تئاتر، نمایشگاه‌ها و اهمیت حمایت از هنر.",
                    keyPoints = listOf(
                        "کلمات توصیف هنر: Aesthetic, Masterpiece, Provocative, Timeless",
                        "نقش هنر در سلامت روان و غنای جامعه",
                        "پاسخ به سوالات پارت ۳ درباره بودجه‌ریزی هنر توسط دولت‌ها"
                    ),
                    bandScore = "Band 7.5+",
                    scenarioTitlePersian = "نقد و بررسی هنر و فرهنگ (درس ۴۲)",
                    scenarioTitleEnglish = "The Arts & Cultural Heritage (Lesson 42)",
                    scenarioCategory = "زندگی روزمره",
                    difficultyLevel = "B2 (پیشرفته)",
                    scenarioDescPersian = "تمرین اسپیکینگ پیرامون آثار هنری، گالری‌ها و ارزش هنر در جامعه بر اساس درس ۴۲ یوتیوب",
                    initialAiGreeting = "Hello! Today we practice vocabulary from Lesson 42: The Arts. Do you believe public funding for the arts is essential?",
                    initialAiTranslation = "سلام! امروز واژگان درس ۴۲ (هنرها) را تمرین می‌کنیم. آیا فکر می‌کنید بودجه دولتی برای هنرها ضروری است؟",
                    systemPromptRole = "You are an IELTS examiner discussing art exhibitions, creative expression, and public funding for the arts.",
                    keyPhrases = listOf(
                        "Artistic expression enriches society's cultural identity." to "بیان هنری هویت فرهنگی جامعه را غنی می‌سازد.",
                        "Visual arts offer profound aesthetic enjoyment." to "هنرهای تجسمی لذت زیبایی‌شناختی عمیقی ارائه می‌دهند.",
                        "Preserving historical heritage requires dedicated funding." to "حفظ میراث تاریخی نیازمند بودجه اختصاصی است."
                    )
                )
            }

            lower.contains("architecture") || lower.contains("lesson forty_ one") || lower.contains("lesson 41") -> {
                ParsedVideoInfo(
                    titlePersian = "درس ۴۱: معماری و شهرسازی (Architecture)",
                    titleEnglish = cleanTitle,
                    category = "واژگان تخصصی آیلتس",
                    seriesName = "Check Your English Vocabulary for IELTS",
                    summaryPersian = "واژگان و اصطلاحات سبک‌های معماری سنتی و مدرن، سازه‌های پایدار و طراحی شهری.",
                    keyPoints = listOf(
                        "اصطلاحات معماری: Skyscraper, Historic preservation, Sustainable design",
                        "مقایسه سازه‌های کهن با ساختمان‌های شیشه‌ای و مدرن",
                        "کاربرد در توصیف شهرها و ساختمان‌های دیدنی در پارت ۲ اسپیکینگ"
                    ),
                    bandScore = "Band 7.5+",
                    scenarioTitlePersian = "معماری و طراحی شهری (درس ۴۱)",
                    scenarioTitleEnglish = "Architecture & Urban Design (Lesson 41)",
                    scenarioCategory = "سفر و گردشگری",
                    difficultyLevel = "B2 (پیشرفته)",
                    scenarioDescPersian = "تمرین توصیف بناهای دیدنی، مقایسه معماری مدرن و سنتی و فضاهای شهری بر اساس درس ۴۱",
                    initialAiGreeting = "Greetings! In this session from Lesson 41, let's talk about architecture. Do you prefer historical or ultra-modern buildings?",
                    initialAiTranslation = "درود! در این جلسه از درس ۴۱ بیایید درباره معماری صحبت کنیم. شما ساختمان‌های تاریخی را ترجیح می‌دهید یا بناهای بسیار مدرن؟",
                    systemPromptRole = "You are an English conversation partner discussing architectural landmarks and city planning.",
                    keyPhrases = listOf(
                        "The building combines traditional facade with contemporary design." to "ساختمان نمای سنتی را با طراحی معاصر ترکیب می‌کند.",
                        "Eco-friendly architecture minimizes energy consumption." to "معماری دوستدار محیط زیست مصرف انرژی را به حداقل می‌رساند.",
                        "Historical landmarks evoke a sense of heritage." to "بناهای تاریخی حس میراث کهن را تداعی می‌کنند."
                    )
                )
            }

            lower.contains("how something works") || lower.contains("lesson forty") || lower.contains("lesson 40") -> {
                ParsedVideoInfo(
                    titlePersian = "درس ۴۰: نحوه کارکرد سیستم‌ها و فرآیندها (How Something Works)",
                    titleEnglish = cleanTitle,
                    category = "واژگان تخصصی آیلتس",
                    seriesName = "Check Your English Vocabulary for IELTS",
                    summaryPersian = "آموزش گنجینه لغات توصیف مکانیسم کارکرد ابزارها، چرخه فرآیندها و سیستم‌های مکانیکی و دیجیتال برای تسک ۱ رایتینگ و اسپیکینگ.",
                    keyPoints = listOf(
                        "افعال توصیف مکانیسم: Function, Operate, Convert, Regulate",
                        "ترتیب زمانی و کلمات ربط فرآیند: Initially, Subsequently, Thereby",
                        "مهارت تسک ۱ دیاگرام و نمودارهای فرآیند"
                    ),
                    bandScore = "Band 7.5+",
                    scenarioTitlePersian = "توضیح کارکرد فرآیندها (درس ۴۰)",
                    scenarioTitleEnglish = "Describing How Things Work (Lesson 40)",
                    scenarioCategory = "کسب‌وکار و شغل",
                    difficultyLevel = "B2 (پیشرفته)",
                    scenarioDescPersian = "تمرین تشریح علمی گام به گام کارکرد وسایل، فناوری‌ها و سیستم‌های فنی بر پایه درس ۴۰",
                    initialAiGreeting = "Hello! In Lesson 40, we learn how to explain mechanisms clearly. Can you choose a device or system and describe how it functions?",
                    initialAiTranslation = "سلام! در درس ۴۰ یاد می‌گیریم چطور مکانیزم‌ها را واضح توضیح دهیم. می‌توانید یک دستگاه یا سیستم انتخاب کرده و نحوه کارکردش را شرح دهید؟",
                    systemPromptRole = "You are an IELTS technical and academic tutor evaluating process descriptions and sequence linkers.",
                    keyPhrases = listOf(
                        "The mechanism converts mechanical energy into electricity." to "این مکانیزم انرژی مکانیکی را به برق تبدیل می‌کند.",
                        "The primary sensor detects any subtle deviation." to "حسگر اولیه هرگونه انحراف ظریف را شناسایی می‌کند.",
                        "Subsequently, the system automatically recalibrates." to "متعاقباً، سیستم به صورت خودکار دوباره تنظیم می‌شود."
                    )
                )
            }

            lower.contains("addition, equation, and conclusion") || lower.contains("lesson thirty_nine") || lower.contains("lesson 39") -> {
                ParsedVideoInfo(
                    titlePersian = "درس ۳۹: واژگان اضافه کردن، برابری و نتیجه‌گیری (Cohesive Linkers)",
                    titleEnglish = cleanTitle,
                    category = "واژگان تخصصی آیلتس",
                    seriesName = "Check Your English Vocabulary for IELTS",
                    summaryPersian = "تسلط بر کلمات ربط پیشرفته رایتینگ و اسپیکینگ برای بیان افزوده‌ها، برابری و جمع‌بندی استدلال‌ها.",
                    keyPoints = listOf(
                        "عبارات افزودن ایده: Furthermore, In addition to, Not only... but also",
                        "عبارات برابری و هم‌ارزی: Equally, In like manner, Correspondingly",
                        "عبارات جمع‌بندی نهایی: To encapsulate, Taking everything into account"
                    ),
                    bandScore = "Band 8.0+",
                    scenarioTitlePersian = "جمع‌بندی و پیوند ایده‌ها (درس ۳۹)",
                    scenarioTitleEnglish = "Linking Ideas & Conclusions (Lesson 39)",
                    scenarioCategory = "کسب‌وکار و شغل",
                    difficultyLevel = "B2 (پیشرفته)",
                    scenarioDescPersian = "تمرین استفاده از حروف ربط آکادمیک برای اتصال جملات و نتیجه‌گیری موثر در مکالمات منطقی",
                    initialAiGreeting = "Welcome! Let's practice advanced cohesive connectors from Lesson 39. Can you state two complementary arguments and draw a conclusion?",
                    initialAiTranslation = "خوش آمدید! بیایید کلمات ربط پیشرفته انسجام را از درس ۳۹ تمرین کنیم. می‌توانید دو استدلال مکمل بیان کرده و نتیجه‌گیری کنید؟",
                    systemPromptRole = "You are an IELTS Writing and Speaking specialist focusing on coherence and cohesion.",
                    keyPhrases = listOf(
                        "Furthermore, renewable energy reduces carbon footprints." to "علاوه بر این، انرژی تجدیدپذیر ردپای کربن را کاهش می‌دهد.",
                        "Equally important is the public awareness campaign." to "به همان اندازه، کمپین آگاهی‌بخشی عمومی نیز اهمیت دارد.",
                        "In conclusion, proactive policies yield sustainable outcomes." to "در نتیجه، سیاست‌های پیشگیرانه نتایج پایداری به همراه دارند."
                    )
                )
            }

            lower.contains("around the world") || lower.contains("lesson thirty_eight") || lower.contains("lesson 38") -> {
                ParsedVideoInfo(
                    titlePersian = "درس ۳۸: سفر، گردشگری و کاوش جهان (Around the World)",
                    titleEnglish = cleanTitle,
                    category = "واژگان تخصصی آیلتس",
                    seriesName = "Check Your English Vocabulary for IELTS",
                    summaryPersian = "واژگان جامع سفر، اصطلاحات فرودگاه، هتل و جنبه‌های مثبت و منفی صنعت توریسم بین‌المللی.",
                    keyPoints = listOf(
                        "اصطلاحات سفر هوایی: Boarding pass, Excess baggage, Jet lag",
                        "واژگان توریسم فرهنگی و بوم‌گردی: Eco-tourism, Cultural heritage",
                        "پاسخ به سوالات متداول مسافرت در پارت ۱ و ۲ اسپیکینگ"
                    ),
                    bandScore = "Band 7.0+",
                    scenarioTitlePersian = "سفر بین‌المللی و بوم‌گردی (درس ۳۸)",
                    scenarioTitleEnglish = "World Travel & Tourism (Lesson 38)",
                    scenarioCategory = "سفر و گردشگری",
                    difficultyLevel = "B1 (متوسط)",
                    scenarioDescPersian = "تمرین مکالمه درباره مقاصد جذاب، رزرو امکانات سفر و اثرات گردشگری بر جوامع بومی بر مبنای درس ۳۸",
                    initialAiGreeting = "Hi there! Welcome to Lesson 38: Around the World. Which country would you most love to visit and why?",
                    initialAiTranslation = "سلام! به درس ۳۸ (پیرامون جهان) خوش آمدید. کدام کشور را بیش از همه دوست دارید ببینید و چرا؟",
                    systemPromptRole = "You are a friendly global traveler sharing cultural experiences and travel tips.",
                    keyPhrases = listOf(
                        "Traveling broadens one's perspective on diverse cultures." to "سفر دیدگاه انسان را نسبت به فرهنگ‌های گوناگون گسترش می‌دهد.",
                        "Eco-friendly tourism preserves fragile ecosystems." to "گردشگری دوستدار محیط زیست اکوسیستم‌های آسیب‌پذیر را حفظ می‌کند.",
                        "Local hospitality made my trip truly unforgettable." to "مهمان‌نوازی محلی سفر مرا حقیقتاً فراموش‌نشدنی ساخت."
                    )
                )
            }

            lower.contains("reciprocal pronouns") || lower.contains("lesson 10") -> {
                ParsedVideoInfo(
                    titlePersian = "درس ۱۰ گرامر: ضمایر متقابل (Each Other & One Another)",
                    titleEnglish = cleanTitle,
                    category = "گرامر ساختاری آیلتس",
                    seriesName = "English Grammar Series",
                    summaryPersian = "کاربرد دقیق ضمایر متقابل برای بیان روابط دوطرفه بین افراد و گروه‌ها با مثال‌های روان.",
                    keyPoints = listOf(
                        "تفاوت ظریف Each other (دو نفر) و One another (چند نفر یا کلی)",
                        "پرهیز از اشتباه رایج به کار بردن selves به جای pronouns reciprocal",
                        "طبیعی‌تر کردن مکالمات انگلیسی در توصیف دوستی‌ها و همکاری‌ها"
                    ),
                    bandScore = "Band 7.0+",
                    scenarioTitlePersian = "همکاری و روابط متقابل (درس ۱۰ گرامر)",
                    scenarioTitleEnglish = "Mutual Collaboration & Reciprocal Actions (Grammar Lesson 10)",
                    scenarioCategory = "زندگی روزمره",
                    difficultyLevel = "B1 (متوسط)",
                    scenarioDescPersian = "تمرین به کار بردن Each other و One another در توصیف کار تیمی و روابط بر مبنای درس ۱۰ گرامر یوتیوب",
                    initialAiGreeting = "Hello! Today we practice reciprocal actions from Grammar Lesson 10. How do members of your team support each other?",
                    initialAiTranslation = "سلام! امروز اقدامات متقابل را از درس ۱۰ گرامر تمرین می‌کنیم. اعضای تیم شما چگونه از یکدیگر حمایت می‌کنند؟",
                    systemPromptRole = "You are a friendly grammar coach encouraging the use of 'each other' and 'one another' in natural spoken English.",
                    keyPhrases = listOf(
                        "They have known each other since university." to "آن‌ها از دوران دانشگاه یکدیگر را می‌شناسند.",
                        "Team members must communicate openly with one another." to "اعضای تیم باید به صورت شفاف با یکدیگر ارتباط برقرار کنند.",
                        "We always help each other during busy deadlines." to "ما در موعدهای شلوغ کاری همیشه به یکدیگر کمک می‌کنیم."
                    )
                )
            }

            lower.contains("reflexive pronouns") || lower.contains("lesson 9") -> {
                ParsedVideoInfo(
                    titlePersian = "درس ۹ گرامر: ضمایر انعکاسی و عبارت By Myself",
                    titleEnglish = cleanTitle,
                    category = "گرامر ساختاری آیلتس",
                    seriesName = "English Grammar Series",
                    summaryPersian = "کاربرد ضمایر انعکاسی (Myself, Yourself, etc.) برای تاکید و بیان کارهای انفرادی بدون کمک دیگران.",
                    keyPoints = listOf(
                        "تفاوت I did it by myself با I did it on my own",
                        "کاربرد انعکاسی در افعال مانند Enjoy yourself, Introduce myself",
                        "خطاهای رایج آزمون اسپیکینگ و رایتینگ در کاربرد اشتباه myself به عنوان فاعل"
                    ),
                    bandScore = "Band 7.0+",
                    scenarioTitlePersian = "استقلال و کارهای فردی (درس ۹ گرامر)",
                    scenarioTitleEnglish = "Independence & Doing Things by Myself (Grammar Lesson 9)",
                    scenarioCategory = "زندگی روزمره",
                    difficultyLevel = "B1 (متوسط)",
                    scenarioDescPersian = "تمرین بیان کارهای شخصی، استقلال و تجارب انفرادی با ضمایر انعکاسی بر اساس درس ۹ یوتیوب",
                    initialAiGreeting = "Welcome! In Lesson 9, we focus on reflexive pronouns like 'by myself'. Have you ever traveled or completed a major project all by yourself?",
                    initialAiTranslation = "خوش آمدید! در درس ۹ روی ضمایر انعکاسی مثل 'by myself' تمرکز داریم. آیا تاکنون به تنهایی سفر کرده یا پروژه‌ای بزرگ را شخصاً تمام کرده‌اید؟",
                    systemPromptRole = "You are an English speaking partner engaging the student in stories about independent accomplishments using reflexive pronouns.",
                    keyPhrases = listOf(
                        "I managed to assemble the entire desk by myself." to "موفق شدم کل میز را به تنهایی سرهم کنم.",
                        "She taught herself how to play acoustic guitar." to "او خودش به صورت خودآموز نواختن گیتار آکوستیک را یاد گرفت.",
                        "We really enjoyed ourselves at the festival." to "ما در جشنواره واقعاً به خودمان خوش گذراندیم."
                    )
                )
            }

            lower.contains("stopping something") || lower.contains("cancel, call off") || lower.contains("lesson thirty_three") || lower.contains("lesson 33") -> {
                ParsedVideoInfo(
                    titlePersian = "درس ۳۳: اصطلاحات متوقف کردن و لغو (Cancel, Call off, Revoke, Repeal)",
                    titleEnglish = cleanTitle,
                    category = "واژگان تخصصی آیلتس",
                    seriesName = "Check Your English Vocabulary for IELTS",
                    summaryPersian = "تفاوت‌های دقیق کلمات Cancel, Call off, Revoke, Repeal و Abolish برای لغو رویدادها، قوانین و قراردادها.",
                    keyPoints = listOf(
                        "لغو رویدادها: Cancel vs Call off",
                        "لغو مجوز و مدرک قانونی: Revoke a license / passport",
                        "لغو قانون یا رسم کهن: Repeal an act / Abolish an outdated tradition"
                    ),
                    bandScore = "Band 7.5+",
                    scenarioTitlePersian = "لغو و توقف امور رسمی (درس ۳۳)",
                    scenarioTitleEnglish = "Cancelling & Revoking Formal Matters (Lesson 33)",
                    scenarioCategory = "کسب‌وکار و شغل",
                    difficultyLevel = "B2 (پیشرفته)",
                    scenarioDescPersian = "تمرین لغو جلسات، لغو قراردادها و استفاده از افعال رسمی لغو در انگلیسی کسب‌وکار بر اساس درس ۳۳",
                    initialAiGreeting = "Hi there! Welcome to Lesson 33: Stopping Something. Suppose you need to call off an important meeting. How would you announce it politely?",
                    initialAiTranslation = "سلام! به درس ۳۳ (متوقف کردن کاری) خوش آمدید. فرض کنید باید یک جلسه مهم را لغو کنید. چطور آن را محترمانه اعلام می‌کنید؟",
                    systemPromptRole = "You are a corporate business communication tutor practicing cancellation nuances (call off, postpone, revoke).",
                    keyPhrases = listOf(
                        "Unfortunately, we had to call off today's briefing." to "متاسفانه مجبور شدیم جلسه توجیهی امروز را لغو کنیم.",
                        "The government voted to repeal the controversial regulation." to "دولت به لغو مقررات بحث‌برانگیز رای داد.",
                        "The authority revoked his commercial driving license." to "مرجع ذیصلاح گواهینامه رانندگی تجاری او را باطل کرد."
                    )
                )
            }

            lower.contains("success and failure") || lower.contains("lesson thirty_four") || lower.contains("lesson 34") -> {
                ParsedVideoInfo(
                    titlePersian = "درس ۳۴: واژگان موفقیت و شکست (Success & Failure)",
                    titleEnglish = cleanTitle,
                    category = "واژگان تخصصی آیلتس",
                    seriesName = "Check Your English Vocabulary for IELTS",
                    summaryPersian = "اصطلاحات و واژگان سطح بالا برای صحبت درباره دستاوردها، غلبه بر موانع و درس گرفتن از شکست‌ها.",
                    keyPoints = listOf(
                        "واژگان موفقیت: Triumph, Accomplishment, Breakthrough",
                        "واژگان شکست و یادگیری: Setback, Pitfall, Resilient",
                        "بیان تجربیات شخصی در پارت ۲ اسپیکینگ آیلتس"
                    ),
                    bandScore = "Band 7.5+",
                    scenarioTitlePersian = "داستان‌های موفقیت و غلبه بر چالش‌ها (درس ۳۴)",
                    scenarioTitleEnglish = "Success, Failure & Resilience (Lesson 34)",
                    scenarioCategory = "زندگی روزمره",
                    difficultyLevel = "B2 (پیشرفته)",
                    scenarioDescPersian = "تمرین روایت دستاوردهای فردی، موانع کاری و استقامت در رسیدن به اهداف بر پایه درس ۳۴ یوتیوب",
                    initialAiGreeting = "Hello! In Lesson 34, we explore Success and Failure. Tell me about a challenging goal you worked hard to achieve.",
                    initialAiTranslation = "سلام! در درس ۳۴ موفقیت و شکست را بررسی می‌کنیم. درباره هدف چالش‌برانگیزی که برای رسیدن به آن سخت تلاش کردید به من بگویید.",
                    systemPromptRole = "You are an IELTS Speaking examiner assessing Part 2 long-turn narratives about personal achievements.",
                    keyPhrases = listOf(
                        "Overcoming initial setbacks made the achievement sweeter." to "غلبه بر موانع اولیه، این دستاورد را شیرین‌تر ساخت.",
                        "Persistence was the key to this breakthrough." to "پشتکار کلید دستیابی به این پیشرفت چشمگیر بود.",
                        "Failure provided valuable lessons for future endeavors." to "شکست درس‌های ارزشمندی برای تلاش‌های آینده به همراه داشت."
                    )
                )
            }

            lower.contains("task commands") || lower.contains("lesson thirty_five") || lower.contains("lesson 35") -> {
                ParsedVideoInfo(
                    titlePersian = "درس ۳۵: افعال دستوری سوالات تحلیلی آیلتس (Task Commands)",
                    titleEnglish = cleanTitle,
                    category = "واژگان تخصصی آیلتس",
                    seriesName = "Check Your English Vocabulary for IELTS",
                    summaryPersian = "رمزگشایی افعال دستوری سوالات آیلتس مانند Analyze, Evaluate, Assess, Elucidate, Justify.",
                    keyPoints = listOf(
                        "تفاوت Analyze و Evaluate در سنجش مزایا و معایب",
                        "نحوه پاسخ مستقیم به افعال دستوری بدون انحراف از موضوع",
                        "افزایش نمره Task Achievement در رایتینگ و اسپیکینگ"
                    ),
                    bandScore = "Band 8.0+",
                    scenarioTitlePersian = "پاسخ به سوالات تحلیلی آیلتس (درس ۳۵)",
                    scenarioTitleEnglish = "Answering IELTS Task Commands (Lesson 35)",
                    scenarioCategory = "کسب‌وکار و شغل",
                    difficultyLevel = "B2 (پیشرفته)",
                    scenarioDescPersian = "تمرین درک دقیق افعال دستوری سوالات آزمون و ارائه پاسخ‌های تحلیلی و مستدل بر پایه درس ۳۵",
                    initialAiGreeting = "Greetings! In Lesson 35, we master academic task commands. Can you evaluate the pros and cons of remote work?",
                    initialAiTranslation = "درود! در درس ۳۵ بر افعال دستوری سوالات آکادمیک مسلط می‌شویم. می‌توانید مزایا و معایب کار از راه دور را ارزیابی (Evaluate) کنید؟",
                    systemPromptRole = "You are an IELTS Academic examiner assessing analytical answers adhering strictly to task prompts.",
                    keyPhrases = listOf(
                        "To evaluate this effectively, we must weigh both benefits and drawbacks." to "برای ارزیابی موثر این موضوع، باید هر دو جنبه مزایا و معایب را بسنجیم.",
                        "The primary factor justifying this approach is cost efficiency." to "عامل اصلی که این رویکرد را توجیه می‌کند، کارایی هزینه است.",
                        "Data elucidates a clear upward trend in productivity." to "داده‌ها روند صعودی واضحی را در بهره‌وری روشن می‌سازند."
                    )
                )
            }

            lower.contains("time") || lower.contains("lesson thirty_six") || lower.contains("lesson 36") -> {
                ParsedVideoInfo(
                    titlePersian = "درس ۳۶: واژگان و اصطلاحات مدیریت زمان (Time)",
                    titleEnglish = cleanTitle,
                    category = "واژگان تخصصی آیلتس",
                    seriesName = "Check Your English Vocabulary for IELTS",
                    summaryPersian = "مجموعه واژگان و اصطلاحات پیرامون زمان، ضرب‌الاجل‌ها، وقت‌شناسی و مدیریت زمان در زندگی و کار.",
                    keyPoints = listOf(
                        "اصطلاحات زمان: Punctual, Time-consuming, In the nick of time",
                        "کالوکیشن‌های مدیریت زمان: Meet a tight deadline, Prioritize tasks",
                        "پاسخ به سوالات پارت ۱ و ۳ اسپیکینگ درباره سبک زندگی مدرن"
                    ),
                    bandScore = "Band 7.0+",
                    scenarioTitlePersian = "مدیریت زمان و ضرب‌الاجل‌ها (درس ۳۶)",
                    scenarioTitleEnglish = "Time Management & Deadlines (Lesson 36)",
                    scenarioCategory = "زندگی روزمره",
                    difficultyLevel = "B1 (متوسط)",
                    scenarioDescPersian = "تمرین مکالمه درباره برنامه‌ریزی، مقابله با تعلل و هماهنگی زمان‌بندی جلسات بر مبنای درس ۳۶ یوتیوب",
                    initialAiGreeting = "Hi! Welcome to Lesson 36: Time. How do you usually handle tight deadlines without feeling overwhelmed?",
                    initialAiTranslation = "سلام! به درس ۳۶ (زمان) خوش آمدید. معمولاً چطور با ضرب‌الاجل‌های فشرده بدون احساس کلافگی کنار می‌آیید؟",
                    systemPromptRole = "You are a professional life coach and speaking partner discussing effective time management.",
                    keyPhrases = listOf(
                        "Punctuality reflects professional dedication." to "وقت‌شناسی نشان‌دهنده تعهد حرفه‌ای است.",
                        "Prioritizing high-impact tasks prevents procrastination." to "اولویت‌بندی کارهای پربازده از به تعویق انداختن امور پیشگیری می‌کند.",
                        "We managed to finalize the presentation in the nick of time." to "موفق شدیم ارائه را در آخرین لحظات نهایی کنیم."
                    )
                )
            }

            else -> {
                // Generic structured mapping for any other current or future video from the channel
                val displayPersian = cleanTitle
                val displayEnglish = cleanTitle
                ParsedVideoInfo(
                    titlePersian = "آموزش ویدیویی: $displayPersian",
                    titleEnglish = displayEnglish,
                    category = "واژگان تخصصی آیلتس",
                    seriesName = "Adibi Learn Skills Official Series",
                    summaryPersian = if (rawDesc.isNotBlank()) rawDesc.take(180) + "..." else "آموزش تخصصی از کانال رسمی خانم زهرا ادیبی.",
                    keyPoints = listOf(
                        "نکات آموزشی و ساختارهای کلیدی تدریس‌شده در ویدیو",
                        "افزایش دایره واژگان مرتبط با سرفصل جهت ارتقای نمره آیلتس",
                        "تمرین تعاملی در بخش مکالمه هوشمند اپلیکیشن"
                    ),
                    bandScore = "Band 7.0+",
                    scenarioTitlePersian = "سناریوی تمرینی: $displayPersian",
                    scenarioTitleEnglish = "Speaking Practice: $displayEnglish",
                    scenarioCategory = "زندگی روزمره",
                    difficultyLevel = "B1 (متوسط)",
                    scenarioDescPersian = "تمرین مکالمه بر اساس جدیدترین محتوای منتشرشده در کانال یوتیوب خانم زهرا ادیبی",
                    initialAiGreeting = "Hello! Let's practice speaking based on this lesson from Adibi Learn Skills: '$displayEnglish'. What are your thoughts on this topic?",
                    initialAiTranslation = "سلام! بیایید مکالمه را بر اساس این درس از آکادمی ادیبی تمرین کنیم: '$displayEnglish'. نظر شما درباره این موضوع چیست؟",
                    systemPromptRole = "You are an encouraging IELTS tutor conversing about the topic of this YouTube lesson.",
                    keyPhrases = listOf(
                        "This topic plays a significant role in fluent English speaking." to "این موضوع نقشی مهم در روان صحبت کردن انگلیسی ایفا می‌کند.",
                        "Using precise terminology elevates your band score." to "استفاده از اصطلاحات دقیق نمره آزمون شما را ارتقا می‌دهد."
                    )
                )
            }
        }
    }
}
