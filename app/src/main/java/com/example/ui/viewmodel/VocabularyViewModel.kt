package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.MasteryLevel
import com.example.data.model.VocabularyItem
import com.example.data.repository.VocabularyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class VocabularyViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repo = VocabularyRepository(db.vocabularyDao())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedFilter = MutableStateFlow("همه")
    val selectedFilter: StateFlow<String> = _selectedFilter.asStateFlow()

    init {
        viewModelScope.launch {
            repo.prepopulateDefaultIeltsWordsIfEmpty()
        }
    }

    val vocabularyList: StateFlow<List<VocabularyItem>> = combine(
        repo.allVocabulary,
        _searchQuery,
        _selectedFilter
    ) { list, query, filter ->
        list.filter { item ->
            val matchesQuery = query.isBlank() ||
                    item.word.contains(query, ignoreCase = true) ||
                    item.definition.contains(query, ignoreCase = true) ||
                    item.persianMeaning.contains(query) ||
                    item.category.contains(query, ignoreCase = true)

            val matchesFilter = when (filter) {
                "نوآموز" -> item.masteryLevel == 1
                "آشنا" -> item.masteryLevel == 2
                "پیشرفته" -> item.masteryLevel == 3
                "مسلط" -> item.masteryLevel >= 4 || item.isMastered
                "در حال یادگیری" -> item.masteryLevel < 4 && !item.isMastered
                else -> true
            }
            matchesQuery && matchesFilter
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val totalCount: StateFlow<Int> = repo.totalCount.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    val masteredCount: StateFlow<Int> = repo.masteredCount.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    fun onSearchQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun onFilterSelect(filter: String) {
        _selectedFilter.value = filter
    }

    fun updateMasteryLevel(item: VocabularyItem, newLevel: Int) {
        viewModelScope.launch {
            repo.updateMasteryLevel(item, newLevel)
        }
    }

    fun toggleMastered(item: VocabularyItem) {
        viewModelScope.launch {
            repo.toggleMastered(item)
        }
    }

    fun deleteWord(item: VocabularyItem) {
        viewModelScope.launch {
            repo.deleteWord(item)
        }
    }

    fun addNewWord(
        word: String,
        definition: String,
        meaning: String,
        example: String,
        category: String,
        masteryLevel: Int = 1
    ) {
        if (word.isBlank()) return
        viewModelScope.launch {
            val item = VocabularyItem(
                word = word.trim(),
                definition = definition.trim(),
                persianMeaning = meaning.trim(),
                exampleSentence = example.trim(),
                category = if (category.isBlank()) "IELTS Academic" else category.trim(),
                masteryLevel = masteryLevel.coerceIn(1, 4),
                isMastered = masteryLevel >= 4
            )
            repo.saveWord(item)
        }
    }
}
