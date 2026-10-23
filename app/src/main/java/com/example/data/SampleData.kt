package com.example.data

import com.example.model.*

object SampleData {

    val classLabels = mapOf(
        1 to "প্রথম শ্রেণি",
        2 to "দ্বিতীয় শ্রেণি",
        3 to "তৃতীয় শ্রেণি",
        4 to "চতুর্থ শ্রেণি",
        5 to "পঞ্চম শ্রেণি",
        6 to "ষষ্ঠ শ্রেণি",
        7 to "সপ্তম শ্রেণি",
        8 to "অষ্টম শ্রেণি",
        9 to "নবম শ্রেণি",
        10 to "দশম শ্রেণি",
        11 to "একাদশ শ্রেণি",
        12 to "দ্বাদশ শ্রেণি",
        13 to "বিশ্ববিদ্যালয় ভর্তি (এডমিশন)",
        0 to "সাধারণ জ্ঞান ও অন্যান্য"
    )

    val subjectsList = listOf(
        "বাংলা", "ইংরেজি", "গণিত", "সাধারণ বিজ্ঞান", "বাংলাদেশ ও বিশ্বপরিচয়",
        "তথ্য ও যোগাযোগ প্রযুক্তি", "পদার্থবিজ্ঞান", "রসায়ন", "জীববিজ্ঞান",
        "উচ্চতর গণিত", "সাধারণ জ্ঞান"
    )

    val defaultExams: List<Exam> = listOf(
        Exam(
            id = 101L,
            title = "বাংলাদেশ ও মুক্তিযুদ্ধ বিশেষ মডেল টেস্ট",
            durationMinutes = 10,
            classLevel = 10,
            subject = "বাংলাদেশ ও বিশ্বপরিচয়",
            chapter = "বাংলাদেশের মুক্তিযুদ্ধ",
            negMark = 0.25f,
            questions = listOf(
                Question(
                    id = 1001L,
                    examId = 101L,
                    body = "বাংলাদেশের স্বাধীনতা দিবস কবে পালন করা হয়?",
                    options = listOf("২৬ মার্চ", "১৬ ডিসেম্বর", "২১ ফেব্রুয়ারি", "১৪ এপ্রিল"),
                    correctIndex = 0,
                    explanation = "১৯৭১ সালের ২৬ মার্চ বঙ্গবন্ধু শেখ মুজিবুর রহমান বাংলাদেশের স্বাধীনতা ঘোষণা করেন। তাই প্রতি বছর ২৬ মার্চ স্বাধীনতা দিবস হিসেবে পালিত হয়।"
                ),
                Question(
                    id = 1002L,
                    examId = 101L,
                    body = "১৯৭১ সালের মুক্তিযুদ্ধে বাংলাদেশকে কয়টি সেক্টরে বিভক্ত করা হয়েছিল?",
                    options = listOf("১১ টি", "১০ টি", "৮ টি", "১২ টি"),
                    correctIndex = 0,
                    explanation = "মুক্তিযুদ্ধ সুষ্ঠুভাবে পরিচালনার জন্য সমগ্র বাংলাদেশকে মোট ১১টি প্রশাসনিক ও সামরিক সেক্টরে বিভক্ত করা হয়।"
                ),
                Question(
                    id = 1003L,
                    examId = 101L,
                    body = "বাংলাদেশের জাতীয় স্মৃতিসৌধ কোন জেলায় অবস্থিত?",
                    options = listOf("ঢাকা (সাভার)", "গাজীপুর", "নারায়ণগঞ্জ", "মুন্সীগঞ্জ"),
                    correctIndex = 0,
                    explanation = "জাতীয় স্মৃতিসৌধ ঢাকার অদূরে সাভারে অবস্থিত। এর স্থপতি ছিলেন সৈয়দ মাইনুল হোসেন।"
                ),
                Question(
                    id = 1004L,
                    examId = 101L,
                    body = "ঐতিহাসিক ৭ই মার্চের ভাষণ ইউনেস্কো কোন ক্যাটাগরিতে অন্তর্ভুক্ত করেছে?",
                    options = listOf("মেমোরি অব দ্য ওয়ার্ল্ড রেজিস্টার", "বিশ্ব ঐতিহ্যবাহী স্থান", "ইনট্যানজিবল কালচারাল হেরিটেজ", "বায়োস্ফিয়ার রিজার্ভ"),
                    correctIndex = 0,
                    explanation = "২০১৭ সালের ৩০ অক্টোবর ইউনেস্কো বঙ্গবন্ধুর ৭ই মার্চের ভাষণকে 'বিশ্ব প্রামাণ্য ঐতিহ্য' হিসেবে স্বীকৃতি দেয়।"
                ),
                Question(
                    id = 1005L,
                    examId = 101L,
                    body = "বীরশ্রেষ্ঠ খেতাবপ্রাপ্ত শহীদ মুক্তিযোদ্ধার সংখ্যা কত?",
                    options = listOf("৭ জন", "৮ জন", "১০ জন", "১১ জন"),
                    correctIndex = 0,
                    explanation = "মহান মুক্তিযুদ্ধে সর্বোচ্চ আত্মত্যাগের স্বীকৃতি হিসেবে ৭ জন মুক্তিযোদ্ধাকে 'বীরশ্রেষ্ঠ' উপাধিতে ভূষিত করা হয়।"
                )
            )
        ),
        Exam(
            id = 102L,
            title = "সাধারণ বিজ্ঞান ও প্রকৃতি কুইজ",
            durationMinutes = 8,
            classLevel = 9,
            subject = "সাধারণ বিজ্ঞান",
            chapter = "পদার্থ ও পরিবেশ",
            negMark = 0.25f,
            questions = listOf(
                Question(
                    id = 2001L,
                    examId = 102L,
                    body = "বায়ুমণ্ডলে কোন গ্যাসের পরিমাণ সবচেয়ে বেশি?",
                    options = listOf("নাইট্রোজেন", "অক্সিজেন", "কার্বন ডাই অক্সাইড", "আর্গন"),
                    correctIndex = 0,
                    explanation = "বায়ুমণ্ডলে প্রায় ৭৮.০৮% অংশ জুড়ে থাকে নাইট্রোজেন গ্যাস।"
                ),
                Question(
                    id = 2002L,
                    examId = 102L,
                    body = "উদ্ভিদ সালোকসংশ্লেষণ প্রক্রিয়ায় কোন গ্যাস ত্যাগ করে?",
                    options = listOf("অক্সিজেন", "কার্বন ডাই অক্সাইড", "মিথেন", "নাইট্রোজেন"),
                    correctIndex = 0,
                    explanation = "সালোকসংশ্লেষণে সূর্যালোক ও পানির সাহায্যে উদ্ভিদ গ্লুকোজ তৈরির পাশাপাশি উপজাত হিসেবে অক্সিজেন নির্গমন করে।"
                ),
                Question(
                    id = 2003L,
                    examId = 102L,
                    body = "পানির রাসায়নিক সংকেত কোনটি?",
                    options = listOf("H₂O", "CO₂", "NaCl", "CH₄"),
                    correctIndex = 0,
                    explanation = "পানির একটি অণু দুটি হাইড্রোজেন পরমাণু এবং একটি অক্সিজেন পরমাণু দ্বারা গঠিত (H₂O)।"
                ),
                Question(
                    id = 2004L,
                    examId = 102L,
                    body = "মানুষের রক্তে অক্সিজেন পরিবহনে কোন প্রোটিন কাজ করে?",
                    options = listOf("হিমোগ্লোবিন", "ইনসুলিন", "কেরাটিন", "ফাইব্রিন"),
                    correctIndex = 0,
                    explanation = "লোহিত রক্তকণিকায় অবস্থিত হিমোগ্লোবিন ফুসফুস থেকে সারা দেহে অক্সিজেন পরিবহন করে।"
                ),
                Question(
                    id = 2005L,
                    examId = 102L,
                    body = "সৌরজগতের সবচেয়ে বড় গ্রহ কোনটি?",
                    options = listOf("বৃহস্পতি (Jupiter)", "শনি", "মঙ্গল", "পৃথিবী"),
                    correctIndex = 0,
                    explanation = "বৃহস্পতি সৌরজগতের সর্ববৃহৎ গ্রহ। এর ব্যাস ও ভর অন্য সব গ্রহের সমষ্টির চেয়েও বেশি।"
                )
            )
        ),
        Exam(
            id = 103L,
            title = "বাংলা ব্যাকরণ ও সাহিত্য প্রস্তুতি",
            durationMinutes = 10,
            classLevel = 10,
            subject = "বাংলা",
            chapter = "ধ্বনি ও সন্ধি",
            negMark = 0.25f,
            questions = listOf(
                Question(
                    id = 3001L,
                    examId = 103L,
                    body = "বাংলা ভাষার মূল উৎস কোনটি?",
                    options = listOf("প্রাকৃত ভাষা", "সংস্কৃত ভাষা", "ফারসি ভাষা", "পালি ভাষা"),
                    correctIndex = 0,
                    explanation = "ড. মুহম্মদ শহীদুল্লাহ্‌র মতে গৌড়ীয় প্রাকৃত এবং ড. সুনীতিকুমার চট্টোপাধ্যায়ের মতে মাগধী প্রাকৃত থেকে বাংলা ভাষার উৎপত্তি।"
                ),
                Question(
                    id = 3002L,
                    examId = 103L,
                    body = "কাজী নজরুল ইসলামকে কোন কবি বলা হয়?",
                    options = listOf("বিদ্রোহী কবি", "পল্লিকবি", "স্বভাব কবি", "রূপসী বাংলার কবি"),
                    correctIndex = 0,
                    explanation = "কাজী নজরুল ইসলাম অন্যায় ও শোষণের বিরুদ্ধে আপসহীন লেখার জন্য 'বিদ্রোহী কবি' হিসেবে খ্যাতি লাভ করেন।"
                ),
                Question(
                    id = 3003L,
                    examId = 103L,
                    body = "'সন্ধি' শব্দের অর্থ কী?",
                    options = listOf("মিলন", "বিচ্ছেদ", "পরিবর্তন", "সংক্ষেপ"),
                    correctIndex = 0,
                    explanation = "পাশাপাশি অবস্থিত দুটি ধ্বনির মিলনকে ব্যাকরণে সন্ধি বলা হয়।"
                ),
                Question(
                    id = 3004L,
                    examId = 103L,
                    body = "বাংলা বর্ণমালায় মোট কতটি বর্ণ রয়েছে?",
                    options = listOf("৫০ টি", "৩৯ টি", "১১ টি", "৫২ টি"),
                    correctIndex = 0,
                    explanation = "বাংলা বর্ণমালায় ১১টি স্বরবর্ণ এবং ৩৯টি ব্যঞ্জনবর্ণ মিলিয়ে মোট ৫০টি বর্ণ রয়েছে।"
                ),
                Question(
                    id = 3005L,
                    examId = 103L,
                    body = "রবীন্দ্রনাথ ঠাকুর কত সালে নোবেল পুরস্কার লাভ করেন?",
                    options = listOf("১৯১৩", "১৯১১", "১৯১৫", "১৯২১"),
                    correctIndex = 0,
                    explanation = "১৯১৩ সালে 'গীতাঞ্জলি' কাব্যগ্রন্থের ইংরেজি অনুবাদের জন্য তিনি সাহিত্যে নোবেল পুরস্কার পান।"
                )
            )
        ),
        Exam(
            id = 104L,
            title = "গণিত দ্রুত সমাধান মডেল টেস্ট",
            durationMinutes = 12,
            classLevel = 8,
            subject = "গণিত",
            chapter = "পাটিগণিত ও বীজগণিত",
            negMark = 0f,
            questions = listOf(
                Question(
                    id = 4001L,
                    examId = 104L,
                    body = "এক থেকে দশ পর্যন্ত মৌলিক সংখ্যা কয়টি?",
                    options = listOf("৪ টি (২, ৩, ৫, ৭)", "৫ টি", "৩ টি", "৬ টি"),
                    correctIndex = 0,
                    explanation = "১ থেকে ১০ পর্যন্ত মৌলিক সংখ্যাগুলো হলো ২, ৩, ৫ এবং ৭। মোট ৪টি।"
                ),
                Question(
                    id = 4002L,
                    examId = 104L,
                    body = "ত্রিভুজের তিন কোণের সমষ্টি কত ডিগ্রি?",
                    options = listOf("১৮০°", "৩৬০°", "৯০°", "২৭০°"),
                    correctIndex = 0,
                    explanation = "যেকোনো ইউক্লিডীয় ত্রিভুজের তিনটি অন্তঃকোণের যোগফল সর্বদা দুই সমকোণ বা ১৮০ ডিগ্রি।"
                ),
                Question(
                    id = 4003L,
                    examId = 104L,
                    body = "(a + b)² এর সঠিক সূত্র কোনটি?",
                    options = listOf("a² + 2ab + b²", "a² - 2ab + b²", "a² + b²", "(a - b)² + 2ab"),
                    correctIndex = 0,
                    explanation = "(a + b)² = a² + 2ab + b²।"
                ),
                Question(
                    id = 4004L,
                    examId = 104L,
                    body = "একটি আয়তক্ষেত্রের দৈর্ঘ্য ৮ মিটার ও প্রস্থ ৫ মিটার হলে ক্ষেত্রফল কত?",
                    options = listOf("৪০ বর্গমিটার", "২৬ বর্গমিটার", "১৩ বর্গমিটার", "৮০ বর্গমিটার"),
                    correctIndex = 0,
                    explanation = "ক্ষেত্রফল = দৈর্ঘ্য × প্রস্থ = ৮ × ৫ = ৪০ বর্গমিটার।"
                ),
                Question(
                    id = 4005L,
                    examId = 104L,
                    body = "বৃত্তের কেন্দ্র থেকে পরিধি পর্যন্ত দূরত্বকে কী বলা হয়?",
                    options = listOf("ব্যাসার্ধ (Radius)", "ব্যাস (Diameter)", "জ্যা (Chord)", "পরিধি (Circumference)"),
                    correctIndex = 0,
                    explanation = "বৃত্তের কেন্দ্র হতে পরিধিস্থ যেকোনো বিন্দুর দূরত্বকে বৃত্তটির ব্যাসার্ধ বলে।"
                )
            )
        )
    )

    val spinPrizes: List<SpinPrize> = listOf(
        SpinPrize(1, "🪙", "+২০ কয়েন", "coin", 20),
        SpinPrize(2, "⚡", "+৫০ XP", "xp", 50),
        SpinPrize(3, "🪙", "+৫০ কয়েন", "coin", 50),
        SpinPrize(4, "⚡", "+১০০ XP", "xp", 100),
        SpinPrize(5, "🎁", "সুপার গিফট", "coin", 100),
        SpinPrize(6, "🍀", "আবার চেষ্টা", "none", 0)
    )

    val badgesList: List<BadgeItem> = listOf(
        BadgeItem("first_test", "🥇", "প্রথম পরীক্ষা", "প্রথম কোনো পরীক্ষা সফলভাবে সম্পন্ন করেছ", true),
        BadgeItem("perfect_score", "💯", "পারফেক্ট স্কোর", "কোনো পরীক্ষায় ১০০% নম্বর অর্জন করেছ", false),
        BadgeItem("streak_3", "🔥", "৩ দিনের স্ট্রিক", "টানা ৩ দিন অ্যাপে অধ্যয়ন সম্পন্ন", true),
        BadgeItem("streak_7", "⚡", "৭ দিনের স্ট্রিক", "টানা ৭ দিন ডেইলি চ্যালেঞ্জ ও স্ট্রিক সক্রিয়", false),
        BadgeItem("quiz_master", "🧠", "কুইজ মাস্টার", "১০টির বেশি পরীক্ষায় অংশগ্রহণ", true),
        BadgeItem("game_champ", "🎮", "গেম চ্যাম্পিয়ন", "গেম জোনে সর্বোচ্চ স্কোর অর্জন", false)
    )

    val studyNotes: List<ContentItem> = listOf(
        ContentItem(
            id = 1L,
            classLevel = 10,
            subject = "বিজ্ঞান",
            chapter = "সালোকসংশ্লেষণ",
            title = "সালোকসংশ্লেষণের পূর্ণাঙ্গ সারাংশ ও বিক্রিয়া",
            kind = "note",
            body = "সালোকসংশ্লেষণ হলো জৈব-রাসায়নিক প্রক্রিয়া যার মাধ্যমে সবুজ উদ্ভিদ সূর্যালোকের উপস্থিতিতে ক্লোরোফিলের সাহায্যে পানি এবং কার্বন ডাই-অক্সাইড ব্যবহার করে শর্করা জাতীয় খাদ্য তৈরি করে এবং অক্সিজেন নির্গমন করে।\n\nবিক্রিয়া:\n6CO₂ + 12H₂O + আলো/ক্লোরোফিল → C₆H₁₂O₆ + 6O₂ + 6H₂O"
        ),
        ContentItem(
            id = 2L,
            classLevel = 10,
            subject = "গণিত",
            chapter = "বীজগণিতীয় সূত্রাবলি",
            title = "বর্গ ও ঘনের গুরুত্বপূর্ণ সূত্রাবলি",
            kind = "note",
            body = "১. (a + b)² = a² + 2ab + b²\n২. (a - b)² = a² - 2ab + b²\n৩. a² - b² = (a + b)(a - b)\n৪. (a + b)³ = a³ + 3a²b + 3ab² + b³\n৫. (a - b)³ = a³ - 3a²b + 3ab² - b³\n৬. a³ + b³ = (a + b)(a² - ab + b²)"
        ),
        ContentItem(
            id = 3L,
            classLevel = 9,
            subject = "ইংরেজি",
            chapter = "Tense & Structure",
            title = "12 Tenses Easy Formula",
            kind = "note",
            body = "Present Indefinite: Subject + V1 + Object\nPresent Continuous: Subject + am/is/are + V-ing + Object\nPresent Perfect: Subject + have/has + V3 + Object\nPast Indefinite: Subject + V2 + Object\nFuture Indefinite: Subject + will + V1 + Object"
        )
    )

    val flashcardSeeds = listOf(
        Flashcard(1L, "পদ্মা সেতুর দৈর্ঘ্য কত?", "৬.১৫ কিলোমিটার", 1),
        Flashcard(2L, "মানবদেহের স্বাভাবিক তাপমাত্রা কত?", "৯৮.৪° ফারেনহাইট (বা ৩৭° সে.)", 2),
        Flashcard(3L, "বায়ুমণ্ডলে অক্সিজেনের শতকরা পরিমাণ কত?", "প্রায় ২০.৯৫%", 1),
        Flashcard(4L, "আলোর গতি সেকেন্ডে কত?", "প্রায় ৩,০০,০০০ কিলোমিটার", 3),
        Flashcard(5L, "সবচেয়ে হালকা গ্যাস কোনটি?", "হাইড্রোজেন (Hydrogen)", 0)
    )

    val sampleFriends = listOf(
        FriendItem("f1", "তানভীর আহমেদ", "🦊", 4, 340, 5, 12, true),
        FriendItem("f2", "সাদিয়া ইসলাম", "🌸", 5, 480, 8, 16, true),
        FriendItem("f3", "রাকিবুল হাসান", "🚀", 3, 210, 2, 7, true),
        FriendItem("f4", "মেহজাবিন চৌধুরী", "⭐", 6, 620, 14, 21, true)
    )
}
