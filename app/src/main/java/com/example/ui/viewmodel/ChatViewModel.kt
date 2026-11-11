package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.ChatMessage
import com.example.data.model.KeyWord
import com.example.data.model.PracticeScenario
import com.example.data.model.ScenarioData
import com.example.data.model.VocabularyItem
import com.example.data.repository.ChatRepository
import com.example.data.repository.VocabularyRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val chatRepo = ChatRepository(db.chatDao())
    private val vocabRepo = VocabularyRepository(db.vocabularyDao())

    private val _currentScenario = MutableStateFlow(ScenarioData.scenariosList.first())
    val currentScenario: StateFlow<PracticeScenario> = _currentScenario.asStateFlow()

    val messages: StateFlow<List<ChatMessage>> = _currentScenario.flatMapLatest { scenario ->
        chatRepo.getMessagesForScenario(scenario.id)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val userMessageCount: StateFlow<Int> = chatRepo.getUserMessageCount().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _latestGrammarCorrection = MutableStateFlow("")
    val latestGrammarCorrection: StateFlow<String> = _latestGrammarCorrection.asStateFlow()

    private val _suggestedReplies = MutableStateFlow<List<String>>(emptyList())
    val suggestedReplies: StateFlow<List<String>> = _suggestedReplies.asStateFlow()

    private val _latestKeyWords = MutableStateFlow<List<KeyWord>>(emptyList())
    val latestKeyWords: StateFlow<List<KeyWord>> = _latestKeyWords.asStateFlow()

    private val _totalScore = MutableStateFlow(25) // Initial practice score
    val totalScore: StateFlow<Int> = _totalScore.asStateFlow()

    private val _fluencyScore = MutableStateFlow(88) // Initial fluency %
    val fluencyScore: StateFlow<Int> = _fluencyScore.asStateFlow()

    private val _accuracyGrade = MutableStateFlow("A") // Initial grade
    val accuracyGrade: StateFlow<String> = _accuracyGrade.asStateFlow()

    private val _selectedYouTubeLesson = MutableStateFlow<com.example.data.model.YouTubeLesson?>(
        com.example.data.model.YouTubeLessonData.lessonsList.first()
    )
    val selectedYouTubeLesson: StateFlow<com.example.data.model.YouTubeLesson?> = _selectedYouTubeLesson.asStateFlow()

    init {
        val initialScenario = _currentScenario.value
        _suggestedReplies.value = initialScenario.keyPhrases.take(3).map { it.first }
        checkAndInitScenarioGreeting(initialScenario)
    }

    fun selectYouTubeLesson(lesson: com.example.data.model.YouTubeLesson) {
        _selectedYouTubeLesson.value = lesson
        val dynamicScenarios = com.example.data.repository.YouTubeSyncRepository.scenarios.value
        val targetScenario = dynamicScenarios.find { it.id == lesson.targetScenarioId }
            ?: ScenarioData.scenariosList.find { it.id == lesson.targetScenarioId }
            ?: dynamicScenarios.firstOrNull()
            ?: ScenarioData.scenariosList.first()
        selectScenario(targetScenario)
    }

    fun selectScenario(scenario: PracticeScenario) {
        _currentScenario.value = scenario
        _latestGrammarCorrection.value = ""
        _suggestedReplies.value = scenario.keyPhrases.take(3).map { it.first }
        _latestKeyWords.value = emptyList()
        checkAndInitScenarioGreeting(scenario)
    }

    private fun checkAndInitScenarioGreeting(scenario: PracticeScenario) {
        viewModelScope.launch {
            if (chatRepo.getMessageCountForScenario(scenario.id) == 0) {
                chatRepo.saveMessage(
                    ChatMessage(
                        scenarioId = scenario.id,
                        sender = "AI",
                        text = scenario.initialAiGreeting,
                        translationPersian = scenario.initialAiTranslation,
                        timestamp = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    fun sendMessage(userText: String) {
        val scenario = _currentScenario.value
        val trimmed = userText.trim()
        if (trimmed.isEmpty() || _isLoading.value) return

        viewModelScope.launch {
            _isLoading.value = true

            // Award points for participation
            val addedPoints = if (trimmed.length > 25) 15 else 10
            _totalScore.value += addedPoints
            val calcFluency = (85..98).random()
            _fluencyScore.value = calcFluency
            _accuracyGrade.value = if (calcFluency >= 92) "A+" else "A"

            val userMsg = ChatMessage(
                scenarioId = scenario.id,
                sender = "USER",
                text = trimmed,
                timestamp = System.currentTimeMillis()
            )
            chatRepo.saveMessage(userMsg)

            val currentHistory = messages.value
            val aiResponse = chatRepo.sendUserMessageAndGetAiResponse(
                scenario = scenario,
                userMessageText = trimmed,
                history = currentHistory
            )

            val aiMsg = ChatMessage(
                scenarioId = scenario.id,
                sender = "AI",
                text = aiResponse.replyText,
                translationPersian = aiResponse.translationPersian,
                grammarCorrection = aiResponse.grammarCorrection,
                timestamp = System.currentTimeMillis()
            )
            chatRepo.saveMessage(aiMsg)

            _latestGrammarCorrection.value = aiResponse.grammarCorrection
            _suggestedReplies.value = aiResponse.suggestedReplies
            _latestKeyWords.value = aiResponse.keyVocabulary
            _isLoading.value = false
        }
    }

    fun saveWordToVocabulary(keyWord: KeyWord) {
        viewModelScope.launch {
            val vocab = VocabularyItem(
                word = keyWord.word,
                targetLanguage = _currentScenario.value.targetLanguage,
                phonetic = keyWord.phonetic,
                persianMeaning = keyWord.persianMeaning,
                exampleSentence = keyWord.example,
                category = _currentScenario.value.titlePersian
            )
            vocabRepo.saveWord(vocab)
        }
    }

    fun clearCurrentChat() {
        viewModelScope.launch {
            val scenario = _currentScenario.value
            chatRepo.clearHistory(scenario.id)
            checkAndInitScenarioGreeting(scenario)
            _latestGrammarCorrection.value = ""
            _suggestedReplies.value = emptyList()
            _latestKeyWords.value = emptyList()
        }
    }
}
