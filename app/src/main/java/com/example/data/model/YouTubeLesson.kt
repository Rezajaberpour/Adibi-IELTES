package com.example.data.model

data class YouTubeLesson(
    val id: String,
    val titlePersian: String,
    val titleEnglish: String,
    val channelName: String = "کانال رسمی خانم زهرا ادیبی (@adibilearnskills)",
    val category: String,
    val seriesName: String,
    val summaryPersian: String,
    val keyPoints: List<String>,
    val targetScenarioId: String,
    val videoUrl: String = "https://youtube.com/@adibilearnskills?si=e9kQ2rrem3921gXT",
    val bandScore: String = "IELTS 7.0+"
)

object YouTubeLessonData {

    const val OFFICIAL_CHANNEL_URL = "https://youtube.com/@adibilearnskills?si=e9kQ2rrem3921gXT"
    const val INSTRUCTOR_NAME = "خانم زهرا ادیبی"
    const val CHANNEL_HANDLE = "@adibilearnskills"

    val seriesCategories = listOf(
        "همه سرفصل‌ها",
        "واژگان تخصصی آیلتس",
        "ارتقای گفتار (Upgrade)",
        "اسپیکینگ و توصیف تصویر",
        "گرامر ساختاری آیلتس"
    )

    val lessonsList = listOf(
        YouTubeLesson(
            id = "yt_ielts_vocab_interview",
            titlePersian = "درس ۳۷: واژگان و اصطلاحات طلایی مصاحبه آیلتس و شغلی",
            titleEnglish = "Check Your Vocabulary for IELTS / Lesson 37: Useful Interview Expressions",
            category = "واژگان تخصصی آیلتس",
            seriesName = "Check Your English Vocabulary for IELTS",
            summaryPersian = "آموزش گنجینه لغات و ساختارهای کلیدی پاسخگویی به سوالات مصاحبه آیلتس و شغلی با تکنیک‌های بیان دستاوردها.",
            keyPoints = listOf(
                "کاربرد اصطلاحات تخصصی بیان تجارب کاری و تحصیلی در پاسخ‌های منسجم",
                "پاسخ به سوالات رفتاری با ساختار روش STAR (موقعیت، وظیفه، اقدام، نتیجه)",
                "کالوکیشن‌های رسمی برای بالا بردن نمره Lexical Resource به بالای ۷.۵"
            ),
            targetScenarioId = "job_interview",
            bandScore = "Band 7.5+"
        ),
        YouTubeLesson(
            id = "yt_upgrade_stop_very",
            titlePersian = "ارتقای مکالمه ۱: به جای Very از صفات پیشرفته استفاده کنید!",
            titleEnglish = "Upgrade Your English | Part 1: Stop Saying 'Very'",
            category = "ارتقای گفتار (Upgrade)",
            seriesName = "Upgrade Your English Series",
            summaryPersian = "جایگزین کردن صفت‌های معمولی و ضعیف با کلمات حرفه‌ای آیلتس جهت ارتقای روانی و طبیعی صحبت کردن نیتیو.",
            keyPoints = listOf(
                "جایگزینی Very tired با Exhausted و Worn out",
                "جایگزینی Very important با Crucial, Essential و Paramount",
                "جایگزینی Very cold با Freezing و Very crowded با Packed"
            ),
            targetScenarioId = "casual_freetalk",
            bandScore = "Band 7.0+"
        ),
        YouTubeLesson(
            id = "yt_cafe_picture_desc",
            titlePersian = "توصیف تصویر و مکالمه کافه: سفارش طبیعی به زبان انگلیسی",
            titleEnglish = "Picture Description: An Iranian Café & Native Ordering Masterclass",
            category = "اسپیکینگ و توصیف تصویر",
            seriesName = "Speaking & Picture Description",
            summaryPersian = "توصیف دقیق جزئیات یک تصویر کافه به همراه اصطلاحات نیتیو برای ثبت سفارش، بیان ترجیحات و تعامل با باریستا.",
            keyPoints = listOf(
                "توصیف فضاسازی و حس‌وحال کافه با افعال و صفت‌های حسی (Cozy, Bustling, Inviting)",
                "عبارت I'd like to get a medium oat latte به جای فرمول‌های ابتدایی",
                "نحوه پاسخ به سوال پرکاربرد For here or to go و پرداخت بدون تماس"
            ),
            targetScenarioId = "cafe_order",
            bandScore = "Band 6.5+"
        ),
        YouTubeLesson(
            id = "yt_ielts_vocab_travel",
            titlePersian = "درس ۳۸: واژگان بین‌المللی سفر، فرودگاه و گردشگری",
            titleEnglish = "Check Your Vocabulary for IELTS / Lesson 38: Around the World",
            category = "واژگان تخصصی آیلتس",
            seriesName = "Check Your English Vocabulary for IELTS",
            summaryPersian = "مجموعه واژگان و اصطلاحات موضوعی درباره سفر، اقامت در هتل، بررسی پرواز و اثرات صنعت گردشگری در تسک رایتینگ و اسپیکینگ.",
            keyPoints = listOf(
                "اصطلاحات فرودگاهی (Boarding pass, Excess baggage, Security check, Gate announcement)",
                "مکالمه پذیرش هتل و درخواست‌های ویژه اقامت (Late check-out, Amenities)",
                "کالوکیشن‌های پیرامون مزایا و چالش‌های گردشگری (Mass tourism, Cultural heritage, Eco-friendly travel)"
            ),
            targetScenarioId = "airport_checkin",
            bandScore = "Band 7.0+"
        ),
        YouTubeLesson(
            id = "yt_grammar_relative_clauses",
            titlePersian = "گرامر طلایی آیلتس: جملات پیرو و موصولی (Adjective Clauses)",
            titleEnglish = "Adjective Clauses & Relative Pronouns for Complex Sentences",
            category = "گرامر ساختاری آیلتس",
            seriesName = "English Grammar for IELTS",
            summaryPersian = "نحوه ساخت جملات مرکب و پیچیده با استفاده درست از ضمایر موصولی Who, Which, That, Whose جهت جهش نمره Grammatical Range.",
            keyPoints = listOf(
                "تفاوت Defining و Non-defining relative clauses و کاربرد ویرگول در آزمون رایتینگ",
                "ترکیب دو جمله ساده به یک ساختار ترکیبی منسجم در بخش اسپیکینگ",
                "پیشگیری از خطای تکرار فاعل یا مفعول در جملات موصولی"
            ),
            targetScenarioId = "academic_seminar",
            bandScore = "Band 7.5+"
        ),
        YouTubeLesson(
            id = "yt_upgrade_part2_nuances",
            titlePersian = "ارتقای مکالمه ۲: اصطلاحات مفهومی و تفاوت You're Good",
            titleEnglish = "Upgrade Your English | Part 2: 'YOU'RE GOOD' ≠ 'YOU'RE GOOD'",
            category = "ارتقای گفتار (Upgrade)",
            seriesName = "Upgrade Your English Series",
            summaryPersian = "بررسی ظرافت‌های اصطلاحی زبان روزمره، تفاوت معانی بر اساس لحن و کانتکست و درک گفتار نیتیو.",
            keyPoints = listOf(
                "تفاوت معنایی You're good به عنوان «خواهش می‌کنم / مشکلی نیست» با حالت تعریف از مهارت",
                "نحوه واکنش طبیعی به تعارفات و عذرخواهی‌ها در مکالمات اسپیکینگ",
                "تمرین لهجه و بالا بردن آهنگ صوتی (Intonation) در جملات مثبت و منفی"
            ),
            targetScenarioId = "casual_freetalk",
            bandScore = "Band 7.0+"
        ),
        YouTubeLesson(
            id = "yt_ielts_vocab_arts",
            titlePersian = "درس ۴۲: واژگان هنر، فرهنگ و خلاقیت در آزمون آیلتس",
            titleEnglish = "Check Your Vocabulary for IELTS / Lesson 42: The Arts",
            category = "واژگان تخصصی آیلتس",
            seriesName = "Check Your English Vocabulary for IELTS",
            summaryPersian = "یادگیری اصطلاحات سطح بالا برای سوالات پرچالش موضوعات هنر، موسیقی، معماری، تئاتر و ادبیات در آزمون‌های اخیر آیلتس.",
            keyPoints = listOf(
                "واژگان توصیف آثار هنری (Aesthetic, Masterpiece, Provocative, Timeless)",
                "بحث درباره نقش بودجه‌های دولتی در توسعه فرهنگی برای رایتینگ تسک ۲",
                "پاسخ‌های آماده با ایده‌پردازی خلاقانه برای پارت ۳ اسپیکینگ"
            ),
            targetScenarioId = "academic_seminar",
            bandScore = "Band 8.0+"
        ),
        YouTubeLesson(
            id = "yt_grammar_tenses_mastery",
            titlePersian = "زمان‌های انگلیسی بدون حفظ کردن: کاربرد زمان حال ساده و استمراری",
            titleEnglish = "Present Simple & Continuous — Beyond Just Memorizing!",
            category = "گرامر ساختاری آیلتس",
            seriesName = "English Grammar for IELTS",
            summaryPersian = "تحلیل عمیق تفاوت‌های کاربردی زمان حال ساده برای عادات و حقایق، و حال استمراری برای رویدادهای موقت و برنامه‌های آینده.",
            keyPoints = listOf(
                "افعال حالتی (Stative Verbs) که در حالت استمراری به کار نمی‌روند",
                "بیان روندهای در حال تغییر جامعه با حال استمراری (e.g., Prices are rising rapidly)",
                "اجتناب از خطاهای اساسی در تطابق فاعل و فعل سوم شخص مفرد"
            ),
            targetScenarioId = "casual_freetalk",
            bandScore = "Band 6.5+"
        )
    )
}
