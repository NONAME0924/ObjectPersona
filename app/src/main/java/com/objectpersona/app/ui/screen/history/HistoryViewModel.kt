package com.objectpersona.app.ui.screen.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.objectpersona.app.data.db.entity.ObjectEntity
import com.objectpersona.app.data.repository.MessageRepository
import com.objectpersona.app.data.repository.ObjectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HistoryItem(
    val objectId: String,
    val emoji: String,
    val personaName: String,
    val lastMessage: String?,
    val lastActiveAt: Long
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val objectRepository: ObjectRepository,
    private val messageRepository: MessageRepository
) : ViewModel() {

    private val _items = MutableStateFlow<List<HistoryItem>>(emptyList())
    val items: StateFlow<List<HistoryItem>> = _items.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadHistory()
    }

    private fun loadHistory() {
        viewModelScope.launch {
            objectRepository.getAllObjectsFlow().collect { objects ->
                val historyItems = objects.map { entity ->
                    val lastMsg = messageRepository.getLastMessage(entity.objectId)
                    HistoryItem(
                        objectId = entity.objectId,
                        emoji = entity.emoji,
                        personaName = entity.personaName,
                        lastMessage = lastMsg?.content,
                        lastActiveAt = entity.lastActiveAt
                    )
                }
                _items.value = historyItems
                _isLoading.value = false
            }
        }
    }

    fun deleteObject(objectId: String) {
        viewModelScope.launch {
            objectRepository.deleteObject(objectId)
        }
    }
}
