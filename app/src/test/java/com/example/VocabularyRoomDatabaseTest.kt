package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.VocabularyDao
import com.example.data.model.MasteryLevel
import com.example.data.model.VocabularyItem
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class VocabularyRoomDatabaseTest {

    private lateinit var db: AppDatabase
    private lateinit var vocabularyDao: VocabularyDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        vocabularyDao = db.vocabularyDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun insertAndQueryIeltsVocabularyWithMasteryLevel() = runBlocking {
        val vocab = VocabularyItem(
            word = "Eloquent",
            definition = "Fluent or persuasive in speaking or writing.",
            exampleSentence = "She delivered an eloquent argument in the debate.",
            masteryLevel = 3,
            persianMeaning = "فصیح و رسا",
            category = "IELTS Speaking"
        )

        val id = vocabularyDao.insertVocabulary(vocab)
        assertTrue(id > 0)

        val retrieved = vocabularyDao.getWord("Eloquent")
        assertNotNull(retrieved)
        assertEquals("Eloquent", retrieved?.word)
        assertEquals("Fluent or persuasive in speaking or writing.", retrieved?.definition)
        assertEquals("She delivered an eloquent argument in the debate.", retrieved?.exampleSentence)
        assertEquals(3, retrieved?.masteryLevel)
        assertEquals(MasteryLevel.PROFICIENT, retrieved?.masteryEnum)
    }

    @Test
    fun updateMasteryLevelToMastered() = runBlocking {
        val vocab = VocabularyItem(
            word = "Mitigate",
            definition = "To make something less severe.",
            exampleSentence = "Strategies to mitigate climate impact.",
            masteryLevel = 1
        )
        val id = vocabularyDao.insertVocabulary(vocab)

        // Update mastery level to 4 (Mastered)
        vocabularyDao.updateMasteryLevel(id, 4)

        val updated = vocabularyDao.getWord("Mitigate")
        assertNotNull(updated)
        assertEquals(4, updated?.masteryLevel)
        assertTrue(updated?.isMastered == true)
        assertEquals(MasteryLevel.MASTERED, updated?.masteryEnum)
    }

    @Test
    fun filterByMasteryLevelFlow() = runBlocking {
        vocabularyDao.insertAll(
            listOf(
                VocabularyItem(word = "Word1", definition = "Def1", exampleSentence = "Ex1", masteryLevel = 1),
                VocabularyItem(word = "Word2", definition = "Def2", exampleSentence = "Ex2", masteryLevel = 2),
                VocabularyItem(word = "Word3", definition = "Def3", exampleSentence = "Ex3", masteryLevel = 2)
            )
        )

        val level2Words = vocabularyDao.getVocabularyByMasteryLevel(2).first()
        assertEquals(2, level2Words.size)
        assertTrue(level2Words.all { it.masteryLevel == 2 })
    }
}
