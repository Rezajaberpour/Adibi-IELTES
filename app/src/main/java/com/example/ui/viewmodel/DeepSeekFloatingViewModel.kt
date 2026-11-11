package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.DeepSeekChatMessage
import com.example.data.repository.DeepSeekRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class DeepSeekFloatingViewModel(
    private val repository: DeepSeekRepository = DeepSeekRepository()
) : ViewModel() {

    val messages: StateFlow<List<DeepSeekChatMessage>> = repository.messages
    val isLoading: StateFlow<Boolean> = repository.isLoading
    val selectedEngine: StateFlow<com.example.data.repository.AiEngine> = repository.selectedEngine

    fun setEngine(engine: com.example.data.repository.AiEngine) {
        repository.setAiEngine(engine)
    }

    fun askQuestion(text: String) {
        viewModelScope.launch {
            repository.askQuestion(text)
        }
    }

    fun clearChat() {
        repository.clearChat()
    }
}
