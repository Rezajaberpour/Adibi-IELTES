package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.VocabularyItem
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) for IELTS Vocabulary Room database.
 */
@Dao
interface VocabularyDao {

    @Query("SELECT * FROM vocabulary ORDER BY createdAt DESC")
    fun getAllVocabulary(): Flow<List<VocabularyItem>>

    @Query("SELECT * FROM vocabulary WHERE masteryLevel = :level ORDER BY createdAt DESC")
    fun getVocabularyByMasteryLevel(level: Int): Flow<List<VocabularyItem>>

    @Query("SELECT * FROM vocabulary WHERE word LIKE '%' || :query || '%' OR definition LIKE '%' || :query || '%' OR persianMeaning LIKE '%' || :query || '%' ORDER BY createdAt DESC")
    fun searchVocabulary(query: String): Flow<List<VocabularyItem>>

    @Query("SELECT * FROM vocabulary WHERE LOWER(word) = LOWER(:word) LIMIT 1")
    suspend fun getWord(word: String): VocabularyItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVocabulary(item: VocabularyItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<VocabularyItem>)

    @Update
    suspend fun updateVocabulary(item: VocabularyItem)

    @Delete
    suspend fun deleteVocabulary(item: VocabularyItem)

    @Query("SELECT COUNT(*) FROM vocabulary")
    fun getVocabularyCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM vocabulary")
    suspend fun getVocabularyCountDirect(): Int

    @Query("SELECT COUNT(*) FROM vocabulary WHERE isMastered = 1 OR masteryLevel >= 4")
    fun getMasteredCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM vocabulary WHERE masteryLevel = :level")
    fun getCountByMasteryLevel(level: Int): Flow<Int>

    @Query("UPDATE vocabulary SET masteryLevel = :masteryLevel, isMastered = (:masteryLevel >= 4) WHERE id = :id")
    suspend fun updateMasteryLevel(id: Long, masteryLevel: Int)
}
