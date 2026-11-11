package com.example.data.model

object ScenarioData {

    val scenariosList = listOf(
        PracticeScenario(
            id = "cafe_order",
            titlePersian = "سفارش در کافه",
            titleEnglish = "Ordering at a Cafe",
            category = "زندگی روزمره",
            targetLanguage = "انگلیسی",
            targetLanguageCode = "en",
            difficultyLevel = "A2 (پایه)",
            descriptionPersian = "تمرین سفارش انواع قهوه، دسر، پرداخت صورت‌حساب و درخواست‌های ویژه از باریستا",
            initialAiGreeting = "Hi there! Welcome to Espresso Craft Cafe. What can I get started for you today?",
            initialAiTranslation = "سلام! به کافه اسپرسو کرافت خوش آمدید. امروز چه چیزی می‌توانم برایتان آماده کنم؟",
            systemPromptRole = "You are a friendly barista at a modern urban coffee shop.",
            keyPhrases = listOf(
                "I'd like a cappuccino with oat milk, please." to "یک کاپوچینو با شیر جو دوسر می‌خواهم، لطفا.",
                "Do you have any gluten-free muffins?" to "آیا مافین بدون گلوتن دارید؟",
                "Can I pay with credit card or contactless?" to "می‌توانم با کارت اعتباری یا بدون تماس پرداخت کنم؟",
                "To go or for here?" to "بیرون‌بر یا برای میل کردن همین‌جا؟",
                "Could I get an extra shot of espresso?" to "می‌شود یک شات اسپرسو اضافه بگذارید؟",
                "Where can I find sugar and napkins?" to "شکر و دستمال کاغذی را کجا پیدا کنم؟"
            )
        ),
        PracticeScenario(
            id = "job_interview",
            titlePersian = "مصاحبه شغلی",
            titleEnglish = "Job Interview Practice",
            category = "کسب‌وکار و شغل",
            targetLanguage = "انگلیسی",
            targetLanguageCode = "en",
            difficultyLevel = "B2 (پیشرفته)",
            descriptionPersian = "تمرین پاسخ به پرسش‌های تخصصی مصاحبه مانند معرفی سوابق، نقاط قوت و چالش‌های کاری",
            initialAiGreeting = "Welcome to our interview session. Thank you for coming. To begin, could you briefly introduce yourself?",
            initialAiTranslation = "به جلسه مصاحبه ما خوش آمدید. ممنون که آمدید. برای شروع، می‌توانید کوتاهاً خودتان را معرفی کنید؟",
            systemPromptRole = "You are a senior hiring manager at an international tech company.",
            keyPhrases = listOf(
                "I have 3 years of experience in mobile development." to "من ۳ سال تجربه کاری در توسعه موبایل دارم.",
                "My key strength is analytical problem solving." to "نقطه قوت اصلی من حل تحلیلی مسئله است.",
                "I thrive in fast-paced collaborative environments." to "من در محیط‌های پویا و همکاری رشد می‌کنم.",
                "What are the main goals for this role?" to "اهداف اصلی این نقش شغلی چیستند؟",
                "How do you measure success in this position?" to "شاخص موفقیت در این موقعیت چگونه سنجیده می‌شود؟"
            )
        ),
        PracticeScenario(
            id = "airport_checkin",
            titlePersian = "فرودگاه و پرواز",
            titleEnglish = "Airport Check-in & Travel",
            category = "سفر و گردشگری",
            targetLanguage = "انگلیسی",
            targetLanguageCode = "en",
            difficultyLevel = "A2 (پایه)",
            descriptionPersian = "تمرین تحویل چمدان، انتخاب صندلی کنار پنجره، دریافت کارت پرواز و پیدا کردن گیت",
            initialAiGreeting = "Good day! Passport and booking code, please. Are you checking in any bags today?",
            initialAiTranslation = "روز خوش! لطفا پاسپورت و کد رزرو را ارائه دهید. آیا امروز چمدانی برای تحویل دارید؟",
            systemPromptRole = "You are an airline customer check-in agent at an international airport.",
            keyPhrases = listOf(
                "Here is my passport and booking reference." to "بفرمایید، این پاسپورت و کد رزرو من است.",
                "I'd prefer a window seat near the front." to "صندلی کنار پنجره در بخش جلو را ترجیح می‌دهم.",
                "Which gate does flight 402 depart from?" to "پرواز ۴۰۲ از کدام گیت حرکت می‌کند؟",
                "Is there any baggage weight allowance limit?" to "آیا محدودیتی در وزن بار مجاز وجود دارد؟",
                "Where is the security check entrance?" to "ورودی بازرسی امنیتی کجاست؟"
            )
        ),
        PracticeScenario(
            id = "hotel_reception",
            titlePersian = "رزرو و پذیرش هتل",
            titleEnglish = "Hotel Reception & Stay",
            category = "سفر و گردشگری",
            targetLanguage = "انگلیسی",
            targetLanguageCode = "en",
            difficultyLevel = "B1 (متوسط)",
            descriptionPersian = "تحویل اتاق، دریافت رمز وای‌فای، پرسش در مورد ساعت صبحانه و درخواست خدمتکار",
            initialAiGreeting = "Welcome to Grand Horizon Hotel! How may I assist you with your reservation today?",
            initialAiTranslation = "به هتل گرند هورایزن خوش آمدید! امروز چطور می‌توانم در خصوص رزروتان به شما کمک کنم؟",
            systemPromptRole = "You are a helpful front-desk receptionist at a luxury hotel.",
            keyPhrases = listOf(
                "I have a reservation under the name Adibi." to "من رزروی به نام ادیبی دارم.",
                "What time is breakfast served in the morning?" to "صبحانه صبح‌ها چه ساعتی سرو می‌شود؟",
                "Could I request late check-out tomorrow?" to "آیا می‌توانم درخواست خروج دیرتر بدهم؟",
                "What is the Wi-Fi network and password?" to "شبکه و رمز وای‌فای چیست؟",
                "Can you arrange a taxi to the airport?" to "می‌توانید تاکسی به فرودگاه هماهنگ کنید؟"
            )
        ),
        PracticeScenario(
            id = "casual_freetalk",
            titlePersian = "چت آزاد و دوستانه",
            titleEnglish = "Casual Friendly Chat",
            category = "زندگی روزمره",
            targetLanguage = "انگلیسی",
            targetLanguageCode = "en",
            difficultyLevel = "B1 (متوسط)",
            descriptionPersian = "مکالمه روان در مورد سرگرمی‌ها، فیلم و کتاب، برنامه‌های آخر هفته و علایق شخصی",
            initialAiGreeting = "Hey! It's so good to talk with you. What exciting things have you been up to recently?",
            initialAiTranslation = "سلام! خیلی خوبه که باهات صحبت می‌کنم. این روزا به چه کارهای هیجان‌انگیزی مشغول بودی؟",
            systemPromptRole = "You are a warm, witty friend chatting over coffee.",
            keyPhrases = listOf(
                "In my free time, I enjoy reading and podcasts." to "در وقت آزادم، از کتاب و پادکست لذت می‌برم.",
                "Have you watched any interesting movies lately?" to "اخیرا فیلم جالبی دیده‌ای؟",
                "I'm planning a relaxing trip next weekend." to "در حال برنامه‌ریزی یک سفر آرامش‌بخش برای آخر هفته هستم.",
                "Tell me more about your hobbies!" to "درباره سرگرمی‌هایت بیشتر بگو!",
                "That sounds absolutely amazing!" to "این واقعا فوق‌العاده به نظر می‌رسه!"
            )
        ),
        PracticeScenario(
            id = "shopping_malls",
            titlePersian = "خرید و چانه‌زنی",
            titleEnglish = "Shopping & Boutique",
            category = "زندگی روزمره",
            targetLanguage = "انگلیسی",
            targetLanguageCode = "en",
            difficultyLevel = "A2 (پایه)",
            descriptionPersian = "پرسش درباره سایز و رنگ، اتاق پرو، تخفیف و فاکتور خرید",
            initialAiGreeting = "Hello! Looking for anything specific today? Feel free to browse or let me know if you need a different size!",
            initialAiTranslation = "سلام! امروز دنبال چیز خاصی می‌گردید؟ راحت باشید یا اگر سایز دیگری خواستید خبرم کنید!",
            systemPromptRole = "You are a stylish sales assistant in a fashion boutique.",
            keyPhrases = listOf(
                "Do you have this jacket in medium size?" to "آیا این کاپشن را در سایز مدیوم دارید؟",
                "Where can I find the fitting rooms?" to "اتاق‌های پرو را کجا می‌توانم پیدا کنم؟",
                "Is there any special discount today?" to "آیا امروز تخفیف ویژه‌ای وجود دارد؟",
                "Can I exchange this if it doesn't fit?" to "اگر اندازه‌ام نشد می‌توانم تعویض کنم؟",
                "I'll take this one, thanks!" to "همین را برمی‌دارم، ممنون!"
            )
        )
    )

    fun getScenarioById(id: String): PracticeScenario {
        return scenariosList.find { it.id == id } ?: scenariosList.first()
    }
}
