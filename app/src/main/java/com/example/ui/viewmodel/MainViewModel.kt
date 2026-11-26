package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.AppContainer
import com.example.data.model.AppLanguage
import com.example.data.model.BaseItemDto
import com.example.data.model.GenreDto
import com.example.data.model.GridDensity
import com.example.data.model.PlaybackMode
import com.example.data.model.PreferredCodec
import com.example.data.model.PublicSystemInfo
import com.example.data.model.ResumeBehavior
import com.example.data.model.SavedServer
import com.example.data.model.SortOption
import com.example.data.model.StreamQuality
import com.example.data.model.SubtitleSize
import com.example.data.model.ThemeMode
import com.example.data.model.UserSettings
import com.example.data.remote.HomeDashboardData
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

data class ConnectUiState(
    val serverUrl: String = "",
    val username: String = "",
    val password: String = "",
    val isTesting: Boolean = false,
    val isSigningIn: Boolean = false,
    val verifiedInfo: PublicSystemInfo? = null,
    val errorMessage: String? = null
)

data class LibraryFilterState(
    val sortOption: SortOption = SortOption.LATEST,
    val unwatchedOnly: Boolean = false,
    val favoritesOnly: Boolean = false,
    val selectedGenre: String? = null,
    val genres: List<GenreDto> = emptyList(),
    val items: List<BaseItemDto> = emptyList(),
    val totalCount: Int = 0,
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null
)

data class SearchUiState(
    val query: String = "",
    val selectedFilter: String = "Movie,Series,Episode",
    val results: List<BaseItemDto> = emptyList(),
    val isLoading: Boolean = false,
    val hasSearched: Boolean = false,
    val error: String? = null
)

@OptIn(FlowPreview::class)
class MainViewModel(private val container: AppContainer) : ViewModel() {

    private val repo = container.jellyfinRepository
    private val settingsRepo = container.settingsRepository

    val settings: StateFlow<UserSettings> = settingsRepo.settingsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, UserSettings())

    private val _connectState = MutableStateFlow(ConnectUiState())
    val connectState: StateFlow<ConnectUiState> = _connectState.asStateFlow()

    private val _homeState = MutableStateFlow<UiState<HomeDashboardData>>(UiState.Loading)
    val homeState: StateFlow<UiState<HomeDashboardData>> = _homeState.asStateFlow()

    private val _moviesState = MutableStateFlow(LibraryFilterState())
    val moviesState: StateFlow<LibraryFilterState> = _moviesState.asStateFlow()

    private val _seriesState = MutableStateFlow(LibraryFilterState())
    val seriesState: StateFlow<LibraryFilterState> = _seriesState.asStateFlow()

    private val _searchState = MutableStateFlow(SearchUiState())
    val searchState: StateFlow<SearchUiState> = _searchState.asStateFlow()

    private val _cacheSizeBytes = MutableStateFlow(0L)
    val cacheSizeBytes: StateFlow<Long> = _cacheSizeBytes.asStateFlow()

    private val searchQueryFlow = MutableStateFlow("")
    private var lastLoadedServerId: String? = null

    init {
        viewModelScope.launch {
            val deviceId = settingsRepo.getOrCreateDeviceId()
            settings.collectLatest { currentSettings ->
                repo.syncSessionConfig(currentSettings, deviceId)
                val active = currentSettings.activeServer
                if (active != null && active.id != lastLoadedServerId) {
                    lastLoadedServerId = active.id
                    loadAllServerContent(active.userId)
                } else if (active == null) {
                    lastLoadedServerId = null
                }
            }
        }

        viewModelScope.launch {
            searchQueryFlow
                .debounce(350)
                .distinctUntilChanged()
                .collectLatest { query ->
                    executeSearch(query)
                }
        }

        refreshCacheSize()
    }

    fun refreshCacheSize() {
        _cacheSizeBytes.value = container.getImageCacheSizeBytes()
    }

    fun clearImageCache() {
        container.clearImageCache()
        refreshCacheSize()
    }

    // --- Connection & Auth ---

    fun updateServerUrl(url: String) {
        _connectState.update { it.copy(serverUrl = url, verifiedInfo = null, errorMessage = null) }
    }

    fun updateUsername(username: String) {
        _connectState.update { it.copy(username = username, errorMessage = null) }
    }

    fun updatePassword(password: String) {
        _connectState.update { it.copy(password = password, errorMessage = null) }
    }

    fun testServerConnection(emptyUrlMsg: String, failedMsg: String) {
        val url = _connectState.value.serverUrl
        if (url.isBlank()) {
            _connectState.update { it.copy(errorMessage = emptyUrlMsg) }
            return
        }
        viewModelScope.launch {
            _connectState.update { it.copy(isTesting = true, errorMessage = null, verifiedInfo = null) }
            repo.testServerConnection(url)
                .onSuccess { info ->
                    _connectState.update {
                        it.copy(
                            isTesting = false,
                            verifiedInfo = info,
                            serverUrl = repo.normalizeServerUrl(url)
                        )
                    }
                }
                .onFailure { err ->
                    _connectState.update {
                        it.copy(
                            isTesting = false,
                            errorMessage = "$failedMsg\n(${err.localizedMessage ?: ""})".trim()
                        )
                    }
                }
        }
    }

    fun signIn(emptyUrlMsg: String, emptyUserMsg: String, authFailedMsg: String) {
        val state = _connectState.value
        if (state.serverUrl.isBlank()) {
            _connectState.update { it.copy(errorMessage = emptyUrlMsg) }
            return
        }
        if (state.username.isBlank()) {
            _connectState.update { it.copy(errorMessage = emptyUserMsg) }
            return
        }
        viewModelScope.launch {
            _connectState.update { it.copy(isSigningIn = true, errorMessage = null) }
            repo.signIn(state.serverUrl, state.username, state.password)
                .onSuccess {
                    _connectState.update { ConnectUiState() }
                }
                .onFailure { err ->
                    _connectState.update {
                        it.copy(
                            isSigningIn = false,
                            errorMessage = "$authFailedMsg\n(${err.localizedMessage ?: ""})".trim()
                        )
                    }
                }
        }
    }

    fun switchServer(server: SavedServer) {
        viewModelScope.launch {
            repo.switchServer(server)
        }
    }

    fun removeSavedServer(serverId: String) {
        viewModelScope.launch {
            settingsRepo.removeSavedServer(serverId)
        }
    }

    fun signOut() {
        viewModelScope.launch {
            repo.signOut()
            _homeState.value = UiState.Loading
            _moviesState.value = LibraryFilterState()
            _seriesState.value = LibraryFilterState()
            _searchState.value = SearchUiState()
        }
    }

    // --- Content Loading ---

    private fun loadAllServerContent(userId: String) {
        refreshHome(userId)
        loadMovies(reset = true, userIdOverride = userId)
        loadSeries(reset = true, userIdOverride = userId)
        loadGenres(userId)
    }

    fun refreshHome(userIdOverride: String? = null) {
        val userId = userIdOverride ?: settings.value.activeServer?.userId ?: return
        viewModelScope.launch {
            _homeState.value = UiState.Loading
            repo.getHomeDashboard(userId)
                .onSuccess { data ->
                    _homeState.value = UiState.Success(data)
                    refreshCacheSize()
                }
                .onFailure { err ->
                    _homeState.value = UiState.Error(err.localizedMessage ?: "Connection error")
                }
        }
    }

    private fun loadGenres(userId: String) {
        viewModelScope.launch {
            repo.getGenres(userId, "Movie").onSuccess { genres ->
                _moviesState.update { it.copy(genres = genres) }
            }
            repo.getGenres(userId, "Series").onSuccess { genres ->
                _seriesState.update { it.copy(genres = genres) }
            }
        }
    }

    fun loadMovies(reset: Boolean = false, userIdOverride: String? = null) {
        val userId = userIdOverride ?: settings.value.activeServer?.userId ?: return
        val current = _moviesState.value
        val startIndex = if (reset) 0 else current.items.size
        if (!reset && (current.isLoading || current.isLoadingMore || (current.totalCount > 0 && current.items.size >= current.totalCount))) {
            return
        }

        viewModelScope.launch {
            _moviesState.update {
                if (reset) it.copy(isLoading = true, error = null) else it.copy(isLoadingMore = true, error = null)
            }
            repo.getLibraryItems(
                userId = userId,
                itemTypes = "Movie",
                sortOption = current.sortOption,
                unwatchedOnly = current.unwatchedOnly,
                favoritesOnly = current.favoritesOnly,
                genre = current.selectedGenre,
                startIndex = startIndex,
                limit = 60
            ).onSuccess { (newItems, total) ->
                _moviesState.update { st ->
                    st.copy(
                        items = if (reset) newItems else (st.items + newItems).distinctBy { it.id },
                        totalCount = total,
                        isLoading = false,
                        isLoadingMore = false
                    )
                }
            }.onFailure { err ->
                _moviesState.update {
                    it.copy(isLoading = false, isLoadingMore = false, error = err.localizedMessage)
                }
            }
        }
    }

    fun loadSeries(reset: Boolean = false, userIdOverride: String? = null) {
        val userId = userIdOverride ?: settings.value.activeServer?.userId ?: return
        val current = _seriesState.value
        val startIndex = if (reset) 0 else current.items.size
        if (!reset && (current.isLoading || current.isLoadingMore || (current.totalCount > 0 && current.items.size >= current.totalCount))) {
            return
        }

        viewModelScope.launch {
            _seriesState.update {
                if (reset) it.copy(isLoading = true, error = null) else it.copy(isLoadingMore = true, error = null)
            }
            repo.getLibraryItems(
                userId = userId,
                itemTypes = "Series",
                sortOption = current.sortOption,
                unwatchedOnly = current.unwatchedOnly,
                favoritesOnly = current.favoritesOnly,
                genre = current.selectedGenre,
                startIndex = startIndex,
                limit = 60
            ).onSuccess { (newItems, total) ->
                _seriesState.update { st ->
                    st.copy(
                        items = if (reset) newItems else (st.items + newItems).distinctBy { it.id },
                        totalCount = total,
                        isLoading = false,
                        isLoadingMore = false
                    )
                }
            }.onFailure { err ->
                _seriesState.update {
                    it.copy(isLoading = false, isLoadingMore = false, error = err.localizedMessage)
                }
            }
        }
    }

    fun updateMoviesFilter(
        sortOption: SortOption = _moviesState.value.sortOption,
        unwatchedOnly: Boolean = _moviesState.value.unwatchedOnly,
        favoritesOnly: Boolean = _moviesState.value.favoritesOnly,
        selectedGenre: String? = _moviesState.value.selectedGenre
    ) {
        _moviesState.update {
            it.copy(
                sortOption = sortOption,
                unwatchedOnly = unwatchedOnly,
                favoritesOnly = favoritesOnly,
                selectedGenre = selectedGenre
            )
        }
        loadMovies(reset = true)
    }

    fun updateSeriesFilter(
        sortOption: SortOption = _seriesState.value.sortOption,
        unwatchedOnly: Boolean = _seriesState.value.unwatchedOnly,
        favoritesOnly: Boolean = _seriesState.value.favoritesOnly,
        selectedGenre: String? = _seriesState.value.selectedGenre
    ) {
        _seriesState.update {
            it.copy(
                sortOption = sortOption,
                unwatchedOnly = unwatchedOnly,
                favoritesOnly = favoritesOnly,
                selectedGenre = selectedGenre
            )
        }
        loadSeries(reset = true)
    }

    // --- Search ---

    fun updateSearchQuery(query: String) {
        _searchState.update { it.copy(query = query) }
        searchQueryFlow.value = query
    }

    fun updateSearchTypeFilter(filterTypes: String) {
        _searchState.update { it.copy(selectedFilter = filterTypes) }
        viewModelScope.launch {
            executeSearch(_searchState.value.query)
        }
    }

    fun submitSearch(query: String) {
        _searchState.update { it.copy(query = query) }
        viewModelScope.launch {
            if (query.isNotBlank()) {
                settingsRepo.addRecentSearch(query)
            }
            executeSearch(query)
        }
    }

    fun clearRecentSearches() {
        viewModelScope.launch { settingsRepo.clearRecentSearches() }
    }

    private suspend fun executeSearch(query: String) {
        val userId = settings.value.activeServer?.userId ?: return
        if (query.isBlank()) {
            _searchState.update { it.copy(results = emptyList(), isLoading = false, hasSearched = false, error = null) }
            return
        }
        _searchState.update { it.copy(isLoading = true, error = null) }
        repo.searchItems(userId, query, _searchState.value.selectedFilter)
            .onSuccess { items ->
                _searchState.update { it.copy(results = items, isLoading = false, hasSearched = true) }
            }
            .onFailure { err ->
                _searchState.update { it.copy(isLoading = false, hasSearched = true, error = err.localizedMessage) }
            }
    }

    // --- Settings Updates ---

    fun setAutoLogin(v: Boolean) = viewModelScope.launch { settingsRepo.updateAutoLogin(v) }
    fun setPlaybackMode(m: PlaybackMode) = viewModelScope.launch { settingsRepo.updatePlaybackMode(m) }
    fun setWifiQuality(q: StreamQuality) = viewModelScope.launch { settingsRepo.updateWifiQuality(q) }
    fun setMobileQuality(q: StreamQuality) = viewModelScope.launch { settingsRepo.updateMobileQuality(q) }
    fun setPreferredCodec(c: PreferredCodec) = viewModelScope.launch { settingsRepo.updatePreferredCodec(c) }
    fun setDefaultSpeed(s: Float) = viewModelScope.launch { settingsRepo.updateDefaultSpeed(s) }
    fun setSeekSeconds(s: Int) = viewModelScope.launch { settingsRepo.updateSeekSeconds(s) }
    fun setAutoPlayNext(v: Boolean) = viewModelScope.launch { settingsRepo.updateAutoPlayNext(v) }
    fun setResumeBehavior(b: ResumeBehavior) = viewModelScope.launch { settingsRepo.updateResumeBehavior(b) }
    fun setPreferredAudioLang(l: String) = viewModelScope.launch { settingsRepo.updatePreferredAudioLang(l) }
    fun setPreferredSubLang(l: String) = viewModelScope.launch { settingsRepo.updatePreferredSubLang(l) }
    fun setSubtitleSize(s: SubtitleSize) = viewModelScope.launch { settingsRepo.updateSubtitleSize(s) }
    fun setKeepScreenOn(v: Boolean) = viewModelScope.launch { settingsRepo.updateKeepScreenOn(v) }
    fun setThemeMode(m: ThemeMode) = viewModelScope.launch { settingsRepo.updateThemeMode(m) }
    fun setLanguage(l: AppLanguage) = viewModelScope.launch { settingsRepo.updateLanguage(l) }
    fun setShowProgressBars(v: Boolean) = viewModelScope.launch { settingsRepo.updateShowProgressBars(v) }
    fun setGridDensity(d: GridDensity) = viewModelScope.launch { settingsRepo.updateGridDensity(d) }
    fun setTimeoutSeconds(s: Int) = viewModelScope.launch { settingsRepo.updateTimeoutSeconds(s) }

    class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainViewModel(container) as T
        }
    }
}
