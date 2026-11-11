package com.example.data.repository

import com.example.data.local.VocabularyDao
import com.example.data.model.MasteryLevel
import com.example.data.model.VocabularyItem
import kotlinx.coroutines.flow.Flow

class VocabularyRepository(private val vocabularyDao: VocabularyDao) {

    val allVocabulary: Flow<List<VocabularyItem>> = vocabularyDao.getAllVocabulary()
    val totalCount: Flow<Int> = vocabularyDao.getVocabularyCount()
    val masteredCount: Flow<Int> = vocabularyDao.getMasteredCount()

    fun getVocabularyByMasteryLevel(level: Int): Flow<List<VocabularyItem>> =
        vocabularyDao.getVocabularyByMasteryLevel(level)

    fun searchVocabulary(query: String): Flow<List<VocabularyItem>> =
        vocabularyDao.searchVocabulary(query)

    suspend fun saveWord(item: VocabularyItem): Long {
        val existing = vocabularyDao.getWord(item.word)
        return if (existing != null) {
            val updated = item.copy(id = existing.id)
            vocabularyDao.updateVocabulary(updated)
            existing.id
        } else {
            vocabularyDao.insertVocabulary(item)
        }
    }

    suspend fun updateMasteryLevel(item: VocabularyItem, newLevel: Int) {
        val clampedLevel = newLevel.coerceIn(1, 4)
        val isMastered = clampedLevel >= 4
        vocabularyDao.updateVocabulary(
            item.copy(
                masteryLevel = clampedLevel,
                isMastered = isMastered
            )
        )
    }

    suspend fun toggleMastered(item: VocabularyItem) {
        val nextMastered = !item.isMastered
        val nextLevel = if (nextMastered) 4 else 1
        vocabularyDao.updateVocabulary(
            item.copy(
                isMastered = nextMastered,
                masteryLevel = nextLevel
            )
        )
    }

    suspend fun deleteWord(item: VocabularyItem) {
        vocabularyDao.deleteVocabulary(item)
    }

    suspend fun prepopulateDefaultIeltsWordsIfEmpty() {
        if (vocabularyDao.getVocabularyCountDirect() == 0) {
            val defaultWords = listOf(
                VocabularyItem(
                    word = "Ubiquitous",
                    definition = "Present, appearing, or found everywhere; pervasive.",
                    exampleSentence = "Mobile devices have become ubiquitous in modern academic research and everyday life.",
                    masteryLevel = 2,
                    phonetic = "/juːˈbɪk.wɪ.təs/",
                    persianMeaning = "همه‌جا حاضر، فراگیر",
                    examplePersian = "دستگاه‌های هوشمند در تحقیقات دانشگاهی و زندگی روزمره فراگیر شده‌اند.",
                    category = "IELTS Academic",
                    targetBandScore = "8.0+"
                ),
                VocabularyItem(
                    word = "Mitigate",
                    definition = "To make something bad less severe, serious, or painful; alleviate.",
                    exampleSentence = "Governments should invest in renewable energy to mitigate the harsh impacts of climate change.",
                    masteryLevel = 3,
                    phonetic = "/ˈmɪt.ɪ.ɡeɪt/",
                    persianMeaning = "کاهش دادن، تسکین بخشیدن",
                    examplePersian = "دولت‌ها باید در انرژی تجدیدپذیر سرمایه‌گذاری کنند تا اثرات تغییرات اقلیمی را کاهش دهند.",
                    category = "IELTS Writing Task 2",
                    targetBandScore = "7.5+"
                ),
                VocabularyItem(
                    word = "Pragmatic",
                    definition = "Dealing with things sensibly and realistically in a way that is based on practical rather than theoretical considerations.",
                    exampleSentence = "The committee adopted a pragmatic approach to solve the urban transportation crisis.",
                    masteryLevel = 1,
                    phonetic = "/præɡˈmæt.ɪk/",
                    persianMeaning = "عمل‌گرایانه، واقع‌بینانه",
                    examplePersian = "کمیته رویکردی عمل‌گرایانه را برای حل بحران حمل‌ونقل شهری اتخاذ نمود.",
                    category = "IELTS Speaking Part 3",
                    targetBandScore = "7.0+"
                ),
                VocabularyItem(
                    word = "Substantiate",
                    definition = "To provide evidence to support or prove the truth of an argument or claim.",
                    exampleSentence = "Candidates must substantiate their claims with concrete statistical data in IELTS essays.",
                    masteryLevel = 2,
                    phonetic = "/səbˈstæn.ʃi.eɪt/",
                    persianMeaning = "اثبات کردن، با مدرک مستند ساختن",
                    examplePersian = "داوطلبان باید ادعاهای خود را با داده‌های آماری مستند اثبات نمایند.",
                    category = "IELTS Writing Task 2",
                    targetBandScore = "8.0+"
                ),
                VocabularyItem(
                    word = "Eloquent",
                    definition = "Fluent or persuasive in speaking or writing; expressing feelings clearly and forcefully.",
                    exampleSentence = "She gave an eloquent presentation arguing for environmental conservation.",
                    masteryLevel = 4,
                    phonetic = "/ˈel.ə.kwənt/",
                    persianMeaning = "فصیح، بلیغ و رسا",
                    examplePersian = "او ارائه بسیار فصیحی در دفاع از حفاظت محیط زیست ارائه داد.",
                    category = "IELTS Speaking",
                    isMastered = true,
                    targetBandScore = "7.5+"
                ),
                VocabularyItem(
                    word = "Plausible",
                    definition = "Seeming reasonable, probable, or believable.",
                    exampleSentence = "The researcher proposed a plausible explanation for the sudden population shift.",
                    masteryLevel = 1,
                    phonetic = "/ˈplɔː.zə.bəl/",
                    persianMeaning = "باورپذیر، محتمل و منطقی",
                    examplePersian = "محقق توضیح قابل قبولی برای جابجایی ناگهانی جمعیت مطرح کرد.",
                    category = "IELTS Reading",
                    targetBandScore = "7.0+"
                )
            )
            vocabularyDao.insertAll(defaultWords)
        }
    }
}
