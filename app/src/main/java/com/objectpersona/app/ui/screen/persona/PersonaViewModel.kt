package com.objectpersona.app.ui.screen.persona

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.objectpersona.app.data.model.Persona
import com.objectpersona.app.data.repository.ObjectRepository
import com.objectpersona.app.service.PersonaGenerator
import com.objectpersona.app.util.EmojiMapper
import com.objectpersona.app.util.HashUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PersonaUiState(
    val isGenerating: Boolean = true,
    val persona: Persona? = null,
    val emoji: String = "🔮",
    val objectDescription: String = "",
    val isExistingObject: Boolean = false,
    val isSaving: Boolean = false,
    val savedObjectId: String? = null,
    val error: String? = null,
    // Editable fields
    val editedName: String = "",
    val editedGender: String = "male",
    val editedPersonality: List<String> = emptyList(),
    val editedSpeechStyle: String = "",
    val editedFreeDescription: String = ""
)

@HiltViewModel
class PersonaViewModel @Inject constructor(
    private val personaGenerator: PersonaGenerator,
    private val objectRepository: ObjectRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PersonaUiState())
    val uiState: StateFlow<PersonaUiState> = _uiState.asStateFlow()

    fun generatePersona(objectDescription: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isGenerating = true,
                objectDescription = objectDescription
            )

            // 檢查是否為已存在的物體
            val objectId = HashUtils.generateObjectId(objectDescription)
            val existing = objectRepository.getObject(objectId)

            if (existing != null) {
                val persona = objectRepository.entityToPersona(existing)
                _uiState.value = _uiState.value.copy(
                    isGenerating = false,
                    persona = persona,
                    emoji = existing.emoji,
                    isExistingObject = true,
                    editedName = persona.name,
                    editedGender = persona.gender,
                    editedPersonality = persona.personality,
                    editedSpeechStyle = persona.speechStyle
                )
                return@launch
            }

            try {
                val persona = personaGenerator.generatePersona(objectDescription)
                val emoji = EmojiMapper.getEmoji(objectDescription)
                _uiState.value = _uiState.value.copy(
                    isGenerating = false,
                    persona = persona,
                    emoji = emoji,
                    editedName = persona.name,
                    editedGender = persona.gender,
                    editedPersonality = persona.personality,
                    editedSpeechStyle = persona.speechStyle
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isGenerating = false,
                    error = "角色生成失敗：${e.message}"
                )
            }
        }
    }

    fun updateName(name: String) {
        _uiState.value = _uiState.value.copy(editedName = name)
    }

    fun updateGender(gender: String) {
        _uiState.value = _uiState.value.copy(editedGender = gender)
    }

    fun addPersonalityTag(tag: String) {
        val current = _uiState.value.editedPersonality
        if (current.size < 5 && tag.isNotBlank() && !current.contains(tag)) {
            _uiState.value = _uiState.value.copy(
                editedPersonality = current + tag
            )
        }
    }

    fun removePersonalityTag(tag: String) {
        _uiState.value = _uiState.value.copy(
            editedPersonality = _uiState.value.editedPersonality - tag
        )
    }

    fun updateSpeechStyle(style: String) {
        _uiState.value = _uiState.value.copy(editedSpeechStyle = style)
    }

    fun updateFreeDescription(desc: String) {
        _uiState.value = _uiState.value.copy(editedFreeDescription = desc)
    }

    fun saveAndStartChat() {
        viewModelScope.launch {
            val state = _uiState.value
            val persona = state.persona ?: return@launch

            _uiState.value = state.copy(isSaving = true)

            val finalPersona = persona.copy(
                name = state.editedName,
                gender = state.editedGender,
                personality = state.editedPersonality,
                speechStyle = state.editedSpeechStyle
            )

            val objectId = objectRepository.saveObject(
                description = state.objectDescription,
                persona = finalPersona,
                emoji = state.emoji
            )

            _uiState.value = state.copy(
                isSaving = false,
                savedObjectId = objectId
            )
        }
    }
}
