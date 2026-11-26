package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.AppContainer
import com.example.data.model.BaseItemDto
import com.example.data.remote.MediaDetailBundle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DetailUiState(
    val isLoading: Boolean = true,
    val bundle: MediaDetailBundle? = null,
    val selectedSeasonIndex: Int = 0,
    val episodes: List<BaseItemDto> = emptyList(),
    val isLoadingEpisodes: Boolean = false,
    val error: String? = null
)

class DetailViewModel(
    private val container: AppContainer,
    private val userId: String,
    private val itemId: String
) : ViewModel() {

    private val repo = container.jellyfinRepository

    private val _state = MutableStateFlow(DetailUiState())
    val state: StateFlow<DetailUiState> = _state.asStateFlow()

    init {
        loadDetail()
    }

    fun loadDetail() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            repo.getMediaDetail(userId, itemId)
                .onSuccess { bundle ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            bundle = bundle,
                            selectedSeasonIndex = 0,
                            episodes = bundle.firstSeasonEpisodes
                        )
                    }
                }
                .onFailure { err ->
                    _state.update { it.copy(isLoading = false, error = err.localizedMessage) }
                }
        }
    }

    fun selectSeason(index: Int) {
        val bundle = _state.value.bundle ?: return
        val season = bundle.seasons.getOrNull(index) ?: return
        _state.update { it.copy(selectedSeasonIndex = index, isLoadingEpisodes = true) }
        viewModelScope.launch {
            repo.getSeasonEpisodes(userId, bundle.item.id, season.id)
                .onSuccess { eps ->
                    _state.update { it.copy(episodes = eps, isLoadingEpisodes = false) }
                }
                .onFailure {
                    _state.update { it.copy(isLoadingEpisodes = false) }
                }
        }
    }

    fun toggleFavorite() {
        val currentItem = _state.value.bundle?.item ?: return
        val wasFav = currentItem.userData?.isFavorite == true
        viewModelScope.launch {
            repo.toggleFavorite(userId, currentItem.id, wasFav).onSuccess { nowFav ->
                _state.update { st ->
                    val b = st.bundle ?: return@update st
                    val updatedUserData = (b.item.userData ?: com.example.data.model.UserItemDataDto())
                        .copy(isFavorite = nowFav)
                    st.copy(bundle = b.copy(item = b.item.copy(userData = updatedUserData)))
                }
            }
        }
    }

    fun togglePlayed() {
        val currentItem = _state.value.bundle?.item ?: return
        val wasPlayed = currentItem.userData?.played == true
        viewModelScope.launch {
            repo.togglePlayed(userId, currentItem.id, wasPlayed).onSuccess { nowPlayed ->
                _state.update { st ->
                    val b = st.bundle ?: return@update st
                    val updatedUserData = (b.item.userData ?: com.example.data.model.UserItemDataDto())
                        .copy(played = nowPlayed)
                    st.copy(bundle = b.copy(item = b.item.copy(userData = updatedUserData)))
                }
            }
        }
    }

    class Factory(
        private val container: AppContainer,
        private val userId: String,
        private val itemId: String
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DetailViewModel(container, userId, itemId) as T
        }
    }
}
