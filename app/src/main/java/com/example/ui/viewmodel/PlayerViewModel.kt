package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.AppContainer
import com.example.data.model.SavedServer
import com.example.data.model.UserSettings
import com.example.data.remote.PlaybackSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PlayerUiState(
    val isLoading: Boolean = true,
    val session: PlaybackSession? = null,
    val error: String? = null
)

class PlayerViewModel(
    private val container: AppContainer,
    private val server: SavedServer,
    private val initialItemId: String,
    private val startFromBeginning: Boolean,
    private val settings: UserSettings
) : ViewModel() {

    private val repo = container.jellyfinRepository

    private val _state = MutableStateFlow(PlayerUiState())
    val state: StateFlow<PlayerUiState> = _state.asStateFlow()

    init {
        loadPlaybackSession(initialItemId, startFromBeginning)
    }

    fun loadPlaybackSession(itemId: String, fromStart: Boolean = false) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            repo.buildPlaybackSession(
                userId = server.userId,
                server = server,
                itemId = itemId,
                settings = settings,
                startFromBeginning = fromStart
            ).onSuccess { session ->
                _state.update { it.copy(isLoading = false, session = session) }
                repo.reportPlaybackStart(session, session.resumePositionMs)
            }.onFailure { err ->
                _state.update { it.copy(isLoading = false, error = err.localizedMessage ?: "Failed to load stream") }
            }
        }
    }

    fun reportProgress(positionMs: Long, isPaused: Boolean) {
        val session = _state.value.session ?: return
        viewModelScope.launch {
            repo.reportPlaybackProgress(session, positionMs, isPaused)
        }
    }

    fun reportStopped(positionMs: Long) {
        val session = _state.value.session ?: return
        viewModelScope.launch {
            repo.reportPlaybackStopped(session, positionMs)
        }
    }

    class Factory(
        private val container: AppContainer,
        private val server: SavedServer,
        private val itemId: String,
        private val startFromBeginning: Boolean,
        private val settings: UserSettings
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PlayerViewModel(container, server, itemId, startFromBeginning, settings) as T
        }
    }
}
