package com.example.data.repository

import android.content.Context
import com.example.data.api.YouTubeFeedEntry
import com.example.data.api.YouTubeFeedParser
import com.example.data.model.PracticeScenario
import com.example.data.model.ScenarioData
import com.example.data.model.YouTubeLesson
import com.example.data.model.YouTubeLessonData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.ConnectionSpec
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLException

object YouTubeSyncRepository {

    private const val CHANNEL_ID = "UC7nsJq5hVH6xvl7ZkdrOKkw"
    private val RSS_ENDPOINTS = listOf(
        "https://www.youtube.com/feeds/videos.xml?channel_id=$CHANNEL_ID",
        "https://youtube.com/feeds/videos.xml?channel_id=$CHANNEL_ID"
    )
    private val HTML_ENDPOINTS = listOf(
        "https://www.youtube.com/@adibilearnskills/videos",
        "https://www.youtube.com/channel/$CHANNEL_ID/videos"
    )
    const val CHANNEL_WEB_URL = "https://youtube.com/@adibilearnskills?si=e9kQ2rrem3921gXT"

    private const val PREFS_NAME = "yt_channel_sync_cache_v2"
    private const val KEY_CACHED_ENTRIES = "cached_entries_json"
    private const val KEY_LAST_SYNC_TIME = "last_sync_time"

    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .callTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .retryOnConnectionFailure(true)
        .connectionSpecs(listOf(ConnectionSpec.MODERN_TLS, ConnectionSpec.COMPATIBLE_TLS))
        .build()

    @Volatile
    private var appContext: Context? = null

    @Volatile
    private var cacheLoaded = false

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _lastSyncTime = MutableStateFlow<Long?>(null)
    val lastSyncTime: StateFlow<Long?> = _lastSyncTime.asStateFlow()

    private val _lastSyncSucceeded = MutableStateFlow(false)
    val lastSyncSucceeded: StateFlow<Boolean> = _lastSyncSucceeded.asStateFlow()

    private val _syncedVideoCount = MutableStateFlow(0)
    val syncedVideoCount: StateFlow<Int> = _syncedVideoCount.asStateFlow()

    private val _syncMessage = MutableStateFlow("آماده همگام‌سازی با یوتیوب")
    val syncMessage: StateFlow<String> = _syncMessage.asStateFlow()

    private val _lessons = MutableStateFlow<List<YouTubeLesson>>(YouTubeLessonData.lessonsList)
    val lessons: StateFlow<List<YouTubeLesson>> = _lessons.asStateFlow()

    private val _scenarios = MutableStateFlow<List<PracticeScenario>>(ScenarioData.scenariosList)
    val scenarios: StateFlow<List<PracticeScenario>> = _scenarios.asStateFlow()

    fun init(context: Context) {
        if (appContext == null) {
            appContext = context.applicationContext
        }
        if (!cacheLoaded) {
            cacheLoaded = true
            loadCachedEntries()
        }
    }

    /**
     * Triggers sync automatically when the app opens or when visiting YouTube/Scenarios tabs
     * if content hasn't been successfully synced yet in this session.
     */
    fun syncIfNeeded(context: Context? = null) {
        if (context != null) init(context)
        if (_isSyncing.value) return
        if (!_lastSyncSucceeded.value) {
            syncLatestContent(context = context)
        }
    }

    /**
     * Backward-compatible overload accepting a CoroutineScope (ignored internally in favor of
     * a lifecycle-independent SupervisorJob scope so navigation never cancels or deadlocks sync).
     */
    fun syncLatestContent(scope: CoroutineScope, context: Context? = null, onComplete: ((Boolean) -> Unit)? = null) {
        syncLatestContent(context = context, onComplete = onComplete)
    }

    /**
     * Fetches real-time video feed from @adibilearnskills YouTube channel using:
     * 1) Official Atom RSS Feed (rich descriptions + timestamps)
     * 2) Official Channel /videos HTML ytInitialData fallback/extension (up to 30 videos)
     * Persists fetched entries locally so newly added videos survive offline restarts.
     */
    fun syncLatestContent(
        context: Context? = null,
        onComplete: ((Boolean) -> Unit)? = null
    ) {
        if (context != null) init(context)
        if (_isSyncing.value) return

        syncScope.launch {
            _isSyncing.value = true
            _syncMessage.value = "در حال دریافت آخرین ویدیوها و سناریوها از کانال @adibilearnskills..."

            var lastError: Exception? = null
            var lastHttpCode: Int? = null
            var syncedEntries: List<YouTubeFeedEntry> = emptyList()

            try {
                // Step 1: Fetch official YouTube Atom RSS feed (newest 15 videos with full descriptions)
                var rssEntries: List<YouTubeFeedEntry> = emptyList()
                for (url in RSS_ENDPOINTS) {
                    try {
                        val request = Request.Builder()
                            .url(url)
                            .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36")
                            .header("Accept", "application/atom+xml,application/xml,text/xml,*/*")
                            .header("Accept-Language", "en-US,en;q=0.9")
                            .build()

                        client.newCall(request).execute().use { response ->
                            if (response.isSuccessful) {
                                val body = response.body?.string().orEmpty()
                                val parsed = YouTubeFeedParser.parse(body)
                                if (parsed.isNotEmpty()) {
                                    rssEntries = parsed
                                }
                            } else {
                                lastHttpCode = response.code
                            }
                        }
                        if (rssEntries.isNotEmpty()) break
                    } catch (e: Exception) {
                        lastError = e
                    }
                }

                // Step 2: Fetch Channel /videos page HTML (fallback if RSS failed, or to extend beyond 15 videos)
                var htmlEntries: List<YouTubeFeedEntry> = emptyList()
                for (url in HTML_ENDPOINTS) {
                    try {
                        val request = Request.Builder()
                            .url(url)
                            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36")
                            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                            .header("Accept-Language", "en-US,en;q=0.9")
                            .header("Cookie", "CONSENT=YES+cb.20210328-17-p0.en+FX+417; SOCS=CAESHAgBEhJnd3NfMjAyMzA4MTAtMF9SQzIaAmVuIAEaBgiA_LyaBg")
                            .build()

                        client.newCall(request).execute().use { response ->
                            if (response.isSuccessful) {
                                val html = response.body?.string().orEmpty()
                                val parsed = YouTubeFeedParser.parseChannelVideosHtml(html)
                                if (parsed.isNotEmpty()) {
                                    htmlEntries = parsed
                                }
                            } else if (lastHttpCode == null) {
                                lastHttpCode = response.code
                            }
                        }
                        if (htmlEntries.isNotEmpty()) break
                    } catch (e: Exception) {
                        if (lastError == null) lastError = e
                    }
                }

                // Step 3: Merge RSS + HTML entries cleanly (RSS entries have richer descriptions & ISO dates)
                syncedEntries = mergeRssAndHtmlEntries(rssEntries, htmlEntries)

                if (syncedEntries.isNotEmpty()) {
                    // Merge with any previously cached entries so older synced items aren't lost
                    val previousCached = readCachedEntriesFromPrefs()
                    val combinedEntries = mergeWithPreviousCache(syncedEntries, previousCached)
                    saveCachedEntriesToPrefs(combinedEntries)
                    processFeedEntries(combinedEntries, isLive = true)

                    val now = System.currentTimeMillis()
                    _lastSyncTime.value = now
                    _lastSyncSucceeded.value = true
                    _syncedVideoCount.value = combinedEntries.size
                    saveLastSyncTime(now)

                    _syncMessage.value = "${combinedEntries.size} ویدیو و سناریو از کانال @adibilearnskills همگام‌سازی شد"
                    onComplete?.invoke(true)
                } else {
                    val reason = buildFriendlyErrorMessage(lastError, lastHttpCode)
                    val cachedCount = _syncedVideoCount.value
                    _syncMessage.value = if (cachedCount > 0) {
                        "$reason ($cachedCount ویدیوی ذخیره‌شده فعال است)"
                    } else {
                        "$reason؛ مطالب پایه بارگذاری شد"
                    }
                    onComplete?.invoke(false)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                val reason = buildFriendlyErrorMessage(e, lastHttpCode)
                _syncMessage.value = "$reason؛ مطالب ذخیره‌شده بارگذاری شد"
                onComplete?.invoke(false)
            } finally {
                _isSyncing.value = false
            }
        }
    }

    private fun buildFriendlyErrorMessage(error: Exception?, httpCode: Int?): String {
        return when (error) {
            is UnknownHostException, is ConnectException ->
                "عدم دسترسی به یوتیوب (اینترنت یا فیلترشکن را بررسی کنید)"
            is SocketTimeoutException ->
                "اتصال به یوتیوب زمان‌بر شد (فیلترشکن یا اینترنت را بررسی کنید)"
            is SSLException ->
                "خطای امنیت اتصال SSL به یوتیوب"
            is IOException ->
                "خطا در برقراری ارتباط با یوتیوب"
            else -> if (httpCode != null) {
                "خطای سرور یوتیوب (کد $httpCode)"
            } else {
                "امکان دریافت ویدیوهای جدید از یوتیوب برقرار نشد"
            }
        }
    }

    private fun mergeRssAndHtmlEntries(
        rssEntries: List<YouTubeFeedEntry>,
        htmlEntries: List<YouTubeFeedEntry>
    ): List<YouTubeFeedEntry> {
        if (rssEntries.isEmpty()) return htmlEntries
        if (htmlEntries.isEmpty()) return rssEntries

        val rssById = rssEntries.associateBy { it.videoId }
        val result = mutableListOf<YouTubeFeedEntry>()
        val seenIds = HashSet<String>()

        // Preserve newest order from HTML (which lists up to 30 videos newest-first), enriching with RSS details
        for (htmlItem in htmlEntries) {
            if (seenIds.add(htmlItem.videoId)) {
                val rssMatch = rssById[htmlItem.videoId]
                if (rssMatch != null) {
                    result.add(
                        rssMatch.copy(
                            thumbnailUrl = rssMatch.thumbnailUrl.ifBlank { htmlItem.thumbnailUrl }
                        )
                    )
                } else {
                    result.add(htmlItem)
                }
            }
        }

        // Ensure any RSS item not in HTML is included at the top
        val missingFromHtml = rssEntries.filter { seenIds.add(it.videoId) }
        return missingFromHtml + result
    }

    private fun mergeWithPreviousCache(
        fresh: List<YouTubeFeedEntry>,
        previous: List<YouTubeFeedEntry>
    ): List<YouTubeFeedEntry> {
        val merged = mutableListOf<YouTubeFeedEntry>()
        val seen = HashSet<String>()
        for (entry in fresh) {
            if (seen.add(entry.videoId)) {
                merged.add(entry)
            }
        }
        for (old in previous) {
            if (seen.add(old.videoId)) {
                merged.add(old)
            }
        }
        return merged.take(60)
    }

    private fun loadCachedEntries() {
        val cached = readCachedEntriesFromPrefs()
        if (cached.isNotEmpty()) {
            _syncedVideoCount.value = cached.size
            val lastSync = appContext
                ?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                ?.getLong(KEY_LAST_SYNC_TIME, 0L) ?: 0L
            if (lastSync > 0L) {
                _lastSyncTime.value = lastSync
            }
            processFeedEntries(cached, isLive = true)
            _syncMessage.value = "${cached.size} ویدیو از حافظه همگام‌سازی‌شده بارگذاری شد"
        }
    }

    private fun readCachedEntriesFromPrefs(): List<YouTubeFeedEntry> {
        val ctx = appContext ?: return emptyList()
        return try {
            val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val raw = prefs.getString(KEY_CACHED_ENTRIES, null) ?: return emptyList()
            val arr = JSONArray(raw)
            val list = mutableListOf<YouTubeFeedEntry>()
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val vid = obj.optString("videoId", "")
                val title = obj.optString("title", "")
                if (vid.isNotBlank() && title.isNotBlank()) {
                    list.add(
                        YouTubeFeedEntry(
                            videoId = vid,
                            title = title,
                            link = obj.optString("link", "https://www.youtube.com/watch?v=$vid"),
                            published = obj.optString("published", ""),
                            description = obj.optString("description", ""),
                            thumbnailUrl = obj.optString("thumbnailUrl", "https://i.ytimg.com/vi/$vid/hqdefault.jpg")
                        )
                    )
                }
            }
            list
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun saveCachedEntriesToPrefs(entries: List<YouTubeFeedEntry>) {
        val ctx = appContext ?: return
        try {
            val arr = JSONArray()
            for (entry in entries) {
                val obj = JSONObject().apply {
                    put("videoId", entry.videoId)
                    put("title", entry.title)
                    put("link", entry.link)
                    put("published", entry.published)
                    put("description", entry.description)
                    put("thumbnailUrl", entry.thumbnailUrl)
                }
                arr.put(obj)
            }
            ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_CACHED_ENTRIES, arr.toString())
                .apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun saveLastSyncTime(timeMs: Long) {
        val ctx = appContext ?: return
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putLong(KEY_LAST_SYNC_TIME, timeMs)
            .apply()
    }

    /**
     * Exposed internally for unit testing and feed transformation verification.
     */
    internal fun processFeedEntries(entries: List<YouTubeFeedEntry>, isLive: Boolean = true) {
        val newLessons = mutableListOf<YouTubeLesson>()
        val newScenarios = mutableListOf<PracticeScenario>()

        for (entry in entries.distinctBy { it.videoId }) {
            val scenarioId = "scenario_yt_${entry.videoId}"
            val lessonId = "lesson_yt_${entry.videoId}"

            val parsedInfo = parseVideoTitleAndDetails(entry.title, entry.description)
            val displayPublished = formatPublishedDate(entry.published)

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
                bandScore = parsedInfo.bandScore,
                thumbnailUrl = entry.thumbnailUrl.ifBlank { "https://i.ytimg.com/vi/${entry.videoId}/hqdefault.jpg" },
                publishedAt = displayPublished,
                isLiveSynced = isLive
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

        // Merge dynamically fetched lessons with seeded baseline, deduplicating matching topics/lessons
        val mergedLessons = mutableListOf<YouTubeLesson>()
        mergedLessons.addAll(newLessons)
        val liveTitlesLower = newLessons.map { it.titleEnglish.lowercase() }
        for (seed in YouTubeLessonData.lessonsList) {
            val isAlreadyCovered = mergedLessons.any { it.id == seed.id || it.titleEnglish.equals(seed.titleEnglish, ignoreCase = true) } ||
                isSeedCoveredByLiveEntries(seed.id, liveTitlesLower)
            if (!isAlreadyCovered) {
                mergedLessons.add(seed)
            }
        }

        // Merge dynamically generated scenarios with core baseline scenarios
        val mergedScenarios = mutableListOf<PracticeScenario>()
        mergedScenarios.addAll(newScenarios)
        for (seed in ScenarioData.scenariosList) {
            if (mergedScenarios.none { it.id == seed.id }) {
                mergedScenarios.add(seed)
            }
        }

        _lessons.value = mergedLessons.distinctBy { it.id }
        _scenarios.value = mergedScenarios.distinctBy { it.id }
    }

    private fun isSeedCoveredByLiveEntries(seedId: String, liveTitlesLower: List<String>): Boolean {
        return when (seedId) {
            "yt_ielts_vocab_interview" -> liveTitlesLower.any { it.contains("thirty_seven") || it.contains("lesson 37") }
            "yt_ielts_vocab_travel" -> liveTitlesLower.any { it.contains("thirty_eight") || it.contains("lesson 38") }
            "yt_ielts_vocab_arts" -> liveTitlesLower.any { it.contains("forty_ two") || it.contains("forty_two") || it.contains("lesson 42") }
            "yt_grammar_relative_clauses" -> liveTitlesLower.any { it.contains("adjective clauses") || it.contains("lesson 8") }
            else -> false
        }
    }

    private fun formatPublishedDate(raw: String): String {
        if (raw.isBlank()) return ""
        // ISO format: 2026-11-17T06:15:29+00:00 -> 2026-11-17
        val tIndex = raw.indexOf('T')
        return if (tIndex > 0) raw.substring(0, tIndex) else raw
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
            .replace("Zahra Adibi", "")
            .trim()
            .trimEnd('/', '|', '-', ' ')

        val lower = (rawTitle + " " + rawDesc).lowercase()
        val lessonNum = extractLessonNumber(rawTitle.lowercase())

        return when {
            lessonNum == 46 || (lower.contains("education") && lessonNum == null) -> {
                ParsedVideoInfo(
                    titlePersian = "درس ۴۶: واژگان آموزش، دانشگاه و سیستم آموزشی (Education)",
                    titleEnglish = cleanTitle,
                    category = "واژگان تخصصی آیلتس",
                    seriesName = "Check Your English Vocabulary for IELTS",
                    summaryPersian = "آموزش جامع واژگان و مکالمات موضوعی درباره تحصیل، مدارس، دانشگاه‌ها، اهداف آکادمیک و روش‌های نوین تدریس برای اسپیکینگ و رایتینگ.",
                    keyPoints = listOf(
                        "اصطلاحات دانشگاهی و تحصیلی: Curriculum, Academic achievement, Lifelong learning",
                        "توصیف روش‌های تدریس و محیط‌های آموزشی: Interactive learning, Blended education",
                        "پاسخ‌های حرفه‌ای پارت ۱ تا ۳ اسپیکینگ پیرامون نقش آموزش در آینده شغلی"
                    ),
                    bandScore = "Band 8.0+",
                    scenarioTitlePersian = "آموزش و نظام دانشگاهی (درس ۴۶)",
                    scenarioTitleEnglish = "Education & Academic Goals (Lesson 46)",
                    scenarioCategory = "کسب‌وکار و شغل",
                    difficultyLevel = "B2 (پیشرفته)",
                    scenarioDescPersian = "تمرین مکالمه درباره سیستم‌های آموزشی، دانشگاه‌ها، مهارت‌آموزی و تجربه یادگیری بر پایه درس ۴۶ یوتیوب",
                    initialAiGreeting = "Hello! Welcome to speaking practice for Lesson 46: Education. What qualities make a teaching method truly effective for learners?",
                    initialAiTranslation = "سلام! به تمرین مکالمه درس ۴۶ (آموزش) خوش آمدید. چه ویژگی‌هایی یک روش تدریس را برای یادگیرندگان واقعاً مؤثر می‌سازد؟",
                    systemPromptRole = "You are an IELTS Speaking Examiner discussing education systems, university life, and modern teaching methodologies based on Lesson 46.",
                    keyPhrases = listOf(
                        "Practical curricula bridge the gap between theory and career." to "برنامه‌های درسی کاربردی شکاف بین تئوری و شغل را پر می‌کنند.",
                        "Lifelong learning is essential in a rapidly evolving job market." to "یادگیری مادام‌العمر در بازار کار پرشتاب امروزی ضروری است.",
                        "Critical thinking should take precedence over rote memorization." to "تفکر نقادانه باید بر حفظ‌کردن طوطی‌وار اولویت داشته باشد.",
                        "Access to quality education empowers entire communities." to "دسترسی به آموزش باکیفیت کل جوامع را توانمند می‌سازد."
                    )
                )
            }

            lessonNum == 45 || lower.contains("crime and the law") -> {
                ParsedVideoInfo(
                    titlePersian = "درس ۴۵: واژگان جرم، قانون و عدالت اجتماعی (Crime and the Law)",
                    titleEnglish = cleanTitle,
                    category = "واژگان تخصصی آیلتس",
                    seriesName = "Check Your English Vocabulary for IELTS",
                    summaryPersian = "یادگیری لغات تخصصی و کالوکیشن‌های کلیدی پیرامون قوانین، پیشگیری از جرم، مجازات، دادگاه و عدالت اجتماعی در تسک ۲ رایتینگ و اسپیکینگ.",
                    keyPoints = listOf(
                        "کالوکیشن‌های حقوقی و قضایی: Enforce the law, Deter crime, Rehabilitation",
                        "انواع تخلفات و اقدامات قانونی: Petty offense, Juvenile delinquency, Legislation",
                        "استدلال‌سازی آکادمیک درباره مجازات و پیشگیری از جرم در پارت ۳ اسپیکینگ"
                    ),
                    bandScore = "Band 8.0+",
                    scenarioTitlePersian = "قانون، امنیت و پیشگیری از جرم (درس ۴۵)",
                    scenarioTitleEnglish = "Crime, Justice & Law (Lesson 45)",
                    scenarioCategory = "کسب‌وکار و شغل",
                    difficultyLevel = "B2 (پیشرفته)",
                    scenarioDescPersian = "تمرین استدلال انگلیسی درباره نقش قوانین، عدالت اجتماعی و راهکارهای پیشگیری از جرم بر مبنای درس ۴۵ یوتیوب",
                    initialAiGreeting = "Welcome! In today's session from Lesson 45 (Crime and the Law), do you think rehabilitation programs are more effective than strict penalties?",
                    initialAiTranslation = "خوش آمدید! در جلسه امروز از درس ۴۵ (جرم و قانون)، آیا فکر می‌کنید برنامه‌های بازپروری از مجازات‌های سخت‌گیرانه مؤثرترند؟",
                    systemPromptRole = "You are an IELTS Speaking examiner evaluating arguments about legislation, crime prevention, and justice using Lesson 45 vocabulary.",
                    keyPhrases = listOf(
                        "Strict enforcement of laws acts as an effective deterrent." to "اجرای دقیق قوانین به عنوان یک عامل بازدارنده مؤثر عمل می‌کند.",
                        "Rehabilitation programs reduce the likelihood of reoffending." to "برنامه‌های بازپروری احتمال تکرار جرم را کاهش می‌دهند.",
                        "Addressing root socioeconomic causes prevents juvenile crime." to "رسیدگی به ریشه‌های اقتصادی-اجتماعی از بزهکاری نوجوانان پیشگیری می‌کند.",
                        "Fair legislation safeguards citizens' fundamental rights." to "قانون‌گذاری عادلانه از حقوق بنیادین شهروندان محافظت می‌کند."
                    )
                )
            }

            lessonNum == 44 || lower.contains("children and the family") -> {
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

            lessonNum == 43 || lower.contains("business and industry") -> {
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

            lessonNum == 42 || lower.contains("the arts") -> {
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

            lessonNum == 41 || lower.contains("architecture") -> {
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

            lessonNum == 40 || lower.contains("how something works") -> {
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

            lessonNum == 39 || lower.contains("addition, equation, and conclusion") -> {
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

            lessonNum == 38 || lower.contains("around the world") -> {
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

            lessonNum == 37 || lower.contains("interview expressions") -> {
                ParsedVideoInfo(
                    titlePersian = "درس ۳۷: واژگان و اصطلاحات طلایی مصاحبه (Interview Expressions)",
                    titleEnglish = cleanTitle,
                    category = "واژگان تخصصی آیلتس",
                    seriesName = "Check Your English Vocabulary for IELTS",
                    summaryPersian = "آموزش گنجینه لغات و ساختارهای کلیدی پاسخگویی به سوالات مصاحبه آیلتس و شغلی با تکنیک‌های بیان دستاوردها.",
                    keyPoints = listOf(
                        "کاربرد اصطلاحات تخصصی بیان تجارب کاری و تحصیلی در پاسخ‌های منسجم",
                        "پاسخ به سوالات رفتاری با ساختار روش STAR (موقعیت، وظیفه، اقدام، نتیجه)",
                        "کالوکیشن‌های رسمی برای بالا بردن نمره Lexical Resource به بالای ۷.۵"
                    ),
                    bandScore = "Band 7.5+",
                    scenarioTitlePersian = "مصاحبه شغلی و آیلتس (درس ۳۷)",
                    scenarioTitleEnglish = "Useful Interview Expressions (Lesson 37)",
                    scenarioCategory = "کسب‌وکار و شغل",
                    difficultyLevel = "B2 (پیشرفته)",
                    scenarioDescPersian = "تمرین پاسخ به پرسش‌های مصاحبه با عبارت‌های طبیعی و حرفه‌ای بر مبنای درس ۳۷ یوتیوب",
                    initialAiGreeting = "Welcome! Based on Lesson 37, let's practice interview expressions. How would you describe your greatest professional achievement?",
                    initialAiTranslation = "خوش آمدید! بر اساس درس ۳۷، بیایید اصطلاحات مصاحبه را تمرین کنیم. بزرگ‌ترین دستاورد حرفه‌ای خود را چطور توصیف می‌کنید؟",
                    systemPromptRole = "You are an executive interviewer and IELTS coach practicing persuasive interview expressions.",
                    keyPhrases = listOf(
                        "I take pride in delivering measurable results under pressure." to "به ارائه نتایج قابل سنجش زیر فشار کاری افتخار می‌کنم.",
                        "Collaborating across departments enhanced our overall efficiency." to "همکاری بین دپارتمان‌ها بهره‌وری کلی ما را افزایش داد.",
                        "I thrive in dynamic roles that demand continuous learning." to "من در نقش‌های پویا که نیازمند یادگیری مداوم هستند رشد می‌کنم."
                    )
                )
            }

            (lessonNum == 8 && (lower.contains("grammar") || lower.contains("adjective"))) || lower.contains("adjective clauses") || lower.contains("relative pronouns") -> {
                ParsedVideoInfo(
                    titlePersian = "درس ۸ گرامر: جملات پیرو و ضمایر موصولی (Adjective Clauses)",
                    titleEnglish = cleanTitle,
                    category = "گرامر ساختاری آیلتس",
                    seriesName = "English Grammar Series",
                    summaryPersian = "نحوه ساخت جملات مرکب و پیچیده با استفاده درست از ضمایر موصولی Who, Which, That, Whose جهت جهش نمره Grammatical Range.",
                    keyPoints = listOf(
                        "تفاوت Defining و Non-defining relative clauses و کاربرد ویرگول در آزمون رایتینگ",
                        "ترکیب دو جمله ساده به یک ساختار ترکیبی منسجم در بخش اسپیکینگ",
                        "پیشگیری از خطای تکرار فاعل یا مفعول در جملات موصولی"
                    ),
                    bandScore = "Band 7.5+",
                    scenarioTitlePersian = "توصیف با جملات موصولی (درس ۸ گرامر)",
                    scenarioTitleEnglish = "Adjective Clauses & Relative Pronouns (Lesson 8)",
                    scenarioCategory = "زندگی روزمره",
                    difficultyLevel = "B1 (متوسط)",
                    scenarioDescPersian = "تمرین توصیف افراد، اشیاء و مکان‌ها با ضمایر موصولی Who, Which و That بر پایه درس ۸ گرامر یوتیوب",
                    initialAiGreeting = "Hello! In Grammar Lesson 8, we practice Adjective Clauses. Can you describe a person who has deeply inspired your life?",
                    initialAiTranslation = "سلام! در درس ۸ گرامر، جملات موصولی را تمرین می‌کنیم. می‌توانید فردی را توصیف کنید که عمیقاً الهام‌بخش زندگی شما بوده است؟",
                    systemPromptRole = "You are an IELTS grammar tutor encouraging natural use of relative pronouns (who, which, that, whose, where).",
                    keyPhrases = listOf(
                        "She is the mentor who guided my academic journey." to "او استادی است که مسیر علمی مرا هدایت کرد.",
                        "That is a book which profoundly changed my perspective." to "این کتابی است که دیدگاه مرا عمیقاً تغییر داد.",
                        "The city where I grew up blends tradition with modernity." to "شهری که در آن بزرگ شدم سنت را با مدرنیته در هم می‌‌آمیزد."
                    )
                )
            }

            (lessonNum == 10 && lower.contains("grammar")) || lower.contains("reciprocal pronouns") -> {
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

            (lessonNum == 9 && lower.contains("grammar")) || lower.contains("reflexive pronouns") -> {
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

            lessonNum == 33 || lower.contains("stopping something") || lower.contains("cancel, call off") -> {
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

            lessonNum == 34 || lower.contains("success and failure") -> {
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

            lessonNum == 35 || lower.contains("task commands") -> {
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

            lessonNum == 36 -> {
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

            else -> buildDynamicFallbackInfo(cleanTitle, rawDesc, lower)
        }
    }

    private fun buildDynamicFallbackInfo(cleanTitle: String, rawDesc: String, lower: String): ParsedVideoInfo {
        val detectedCategory = when {
            lower.contains("upgrade") -> "ارتقای گفتار (Upgrade)"
            lower.contains("grammar") || lower.contains("pronoun") || lower.contains("clause") || lower.contains("tense") -> "گرامر ساختاری آیلتس"
            lower.contains("picture") || lower.contains("speaking") || lower.contains("demo") || lower.contains("reading") -> "اسپیکینگ و توصیف تصویر"
            else -> "واژگان تخصصی آیلتس"
        }

        val detectedSeries = when (detectedCategory) {
            "ارتقای گفتار (Upgrade)" -> "Upgrade Your English Series"
            "گرامر ساختاری آیلتس" -> "English Grammar for IELTS"
            "اسپیکینگ و توصیف تصویر" -> "Speaking & Demo Teaching"
            else -> "Check Your English Vocabulary for IELTS"
        }

        val scenarioCat = when {
            lower.contains("business") || lower.contains("work") || lower.contains("job") || lower.contains("academic") -> "کسب‌وکار و شغل"
            lower.contains("travel") || lower.contains("world") || lower.contains("airport") || lower.contains("hotel") -> "سفر و گردشگری"
            else -> "زندگی روزمره"
        }

        val lessonNumber = extractLessonNumber(lower)
        val persianPrefix = when {
            detectedCategory == "ارتقای گفتار (Upgrade)" && lessonNumber != null -> "ارتقای مکالمه (قسمت ${toPersianDigits(lessonNumber)}): "
            detectedCategory == "گرامر ساختاری آیلتس" && lessonNumber != null -> "درس ${toPersianDigits(lessonNumber)} گرامر: "
            lessonNumber != null -> "درس ${toPersianDigits(lessonNumber)}: "
            else -> "آموزش جدید کانال: "
        }

        val shortTopic = cleanTitle
            .replace("Check Your English Vocabulary for IELTS", "", ignoreCase = true)
            .replace("Upgrade Your English", "", ignoreCase = true)
            .replace("English Grammar Lesson", "", ignoreCase = true)
            .replace("English Grammar", "", ignoreCase = true)
            .trim(' ', '/', '|', '-', ':')
            .ifBlank { cleanTitle }

        val cleanDesc = rawDesc.replace(Regex("#\\S+"), "").trim()
        val summaryText = if (cleanDesc.isNotBlank()) {
            "سرفصل اختصاصی جدید از کانال خانم زهرا ادیبی — ${cleanDesc.take(180)}"
        } else {
            "آموزش تخصصی جدید منتشرشده در کانال یوتیوب خانم زهرا ادیبی (@adibilearnskills): $cleanTitle"
        }

        return ParsedVideoInfo(
            titlePersian = "$persianPrefix$shortTopic",
            titleEnglish = cleanTitle,
            category = detectedCategory,
            seriesName = detectedSeries,
            summaryPersian = summaryText,
            keyPoints = listOf(
                "آموزش کاربردی و ساختارهای کلیدی تدریس‌شده در ویدیوی $shortTopic",
                "تقویت دایره لغات و اصطلاحات نیتیو جهت ارتقای نمره اسپیکینگ و رایتینگ آیلتس",
                "تمرین مستقیم مکالمه این موضوع در بخش چت هوشمند اپلیکیشن"
            ),
            bandScore = "Band 7.5+",
            scenarioTitlePersian = "تمرین مکالمه: $shortTopic",
            scenarioTitleEnglish = "Practice: $shortTopic",
            scenarioCategory = scenarioCat,
            difficultyLevel = "B2 (پیشرفته)",
            scenarioDescPersian = "تمرین تعاملی مکالمه انگلیسی متناسب با آموزش جدید «$cleanTitle» در کانال یوتیوب",
            initialAiGreeting = "Hello! Let's practice English speaking based on the latest lesson from Adibi Learn Skills: \"$cleanTitle\". What key idea or vocabulary caught your attention in this topic?",
            initialAiTranslation = "سلام! بیایید مکالمه انگلیسی را بر اساس جدیدترین درس کانال آکادمی ادیبی تمرین کنیم: «$cleanTitle». کدام نکته یا واژه در این موضوع بیشتر توجه شما را جلب کرد؟",
            systemPromptRole = "You are an encouraging IELTS Speaking tutor guiding a realistic practice conversation specifically around the lesson '$cleanTitle'.",
            keyPhrases = listOf(
                "Using precise vocabulary makes my argument much clearer." to "استفاده از واژگان دقیق، استدلال من را بسیار شفاف‌تر می‌کند.",
                "Let me illustrate this point with a practical real-life example." to "اجازه دهید این نکته را با یک مثال کاربردی از زندگی واقعی توضیح دهم.",
                "Overall, mastering these expressions boosts speaking fluency." to "در مجموع، تسلط بر این اصطلاحات روانی مکالمه را افزایش می‌دهد."
            )
        )
    }

    private fun extractLessonNumber(rawLower: String): Int? {
        val normalized = rawLower.replace('_', ' ').replace(Regex("\\s+"), " ")
        Regex("""(?:lesson|part)\s+(\d+)""").find(normalized)?.groupValues?.getOrNull(1)?.toIntOrNull()?.let {
            return it
        }
        val wordToNum = listOf(
            "fifty" to 50,
            "forty nine" to 49, "forty eight" to 48, "forty seven" to 47,
            "forty six" to 46, "forty five" to 45, "forty four" to 44,
            "forty three" to 43, "forty two" to 42, "forty one" to 41,
            "forty" to 40,
            "thirty nine" to 39, "thirty eight" to 38, "thirty seven" to 37,
            "thirty six" to 36, "thirty five" to 35, "thirty four" to 34,
            "thirty three" to 33, "thirty two" to 32, "thirty one" to 31,
            "thirty" to 30,
            "twenty nine" to 29, "twenty eight" to 28, "twenty seven" to 27,
            "twenty six" to 26, "twenty five" to 25
        )
        for ((word, num) in wordToNum) {
            if (normalized.contains(word)) return num
        }
        return null
    }

    private fun toPersianDigits(num: Int): String {
        val persianDigits = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
        return num.toString().map { ch ->
            if (ch in '0'..'9') persianDigits[ch - '0'] else ch
        }.joinToString("")
    }
}
