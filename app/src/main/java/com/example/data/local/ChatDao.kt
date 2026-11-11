package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.ChatMessage
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Query("SELECT * FROM chat_messages WHERE scenarioId = :scenarioId ORDER BY timestamp ASC")
    fun getMessagesForScenario(scenarioId: String): Flow<List<ChatMessage>>

    @Query("SELECT * FROM chat_messages ORDER BY timestamp DESC")
    fun getAllMessages(): Flow<List<ChatMessage>>

    @Query("SELECT COUNT(*) FROM chat_messages WHERE sender = 'USER'")
    fun getUserMessageCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM chat_messages WHERE scenarioId = :scenarioId")
    suspend fun getMessageCountForScenario(scenarioId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessage): Long

    @Query("DELETE FROM chat_messages WHERE scenarioId = :scenarioId")
    suspend fun clearMessagesForScenario(scenarioId: String)
}
