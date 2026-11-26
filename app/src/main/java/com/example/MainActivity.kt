package com.example

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LiveTv
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import coil.compose.LocalImageLoader
import com.example.data.model.AppLanguage
import com.example.data.model.BaseItemDto
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LibraryBrowseScreen
import com.example.ui.screens.MediaDetailScreen
import com.example.ui.screens.PlayerScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.ServerConnectScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.CineFinTheme
import com.example.ui.theme.LocalStrings
import com.example.ui.viewmodel.DetailViewModel
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.PlayerViewModel

object Routes {
    const val CONNECT = "connect"
    const val HOME = "home"
    const val MOVIES = "movies"
    const val SERIES = "series"
    const val SEARCH = "search"
    const val SETTINGS = "settings"
    const val PRE_AUTH_SETTINGS = "pre_auth_settings"
    const val DETAIL = "detail/{itemId}"
    const val PLAYER = "player/{itemId}?startFromBeginning={startFromBeginning}"

    fun detail(itemId: String) = "detail/$itemId"
    fun player(itemId: String, startFromBeginning: Boolean = false) =
        "player/$itemId?startFromBeginning=$startFromBeginning"
}

private data class NavTab(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

class MainActivity : ComponentActivity() {
    private lateinit var appContainer: AppContainer

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appContainer = AppContainer(applicationContext)
        enableEdgeToEdge()

        setContent {
            val mainViewModel: MainViewModel = viewModel(factory = MainViewModel.Factory(appContainer))
            val settings by mainViewModel.settings.collectAsStateWithLifecycle()
            val windowSizeClass = calculateWindowSizeClass(this)

            CompositionLocalProvider(LocalImageLoader provides appContainer.imageLoader) {
                CineFinTheme(
                    themeMode = settings.themeMode,
                    language = settings.language
                ) {
                    CineFinAppRoot(
                        appContainer = appContainer,
                        mainViewModel = mainViewModel,
                        useNavigationRail = windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact
                    )
                }
            }
        }
    }
}

@Composable
private fun CineFinAppRoot(
    appContainer: AppContainer,
    mainViewModel: MainViewModel,
    useNavigationRail: Boolean
) {
    val strings = LocalStrings.current
    val settings by mainViewModel.settings.collectAsStateWithLifecycle()
    val connectState by mainViewModel.connectState.collectAsStateWithLifecycle()
    val homeState by mainViewModel.homeState.collectAsStateWithLifecycle()
    val moviesState by mainViewModel.moviesState.collectAsStateWithLifecycle()
    val seriesState by mainViewModel.seriesState.collectAsStateWithLifecycle()
    val searchState by mainViewModel.searchState.collectAsStateWithLifecycle()
    val cacheSizeBytes by mainViewModel.cacheSizeBytes.collectAsStateWithLifecycle()

    val activeServer = settings.activeServer
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    // If disconnected, show Server Connect flow (with optional Settings modal route)
    if (activeServer == null) {
        NavHost(navController = navController, startDestination = Routes.CONNECT) {
            composable(Routes.CONNECT) {
                ServerConnectScreen(
                    connectState = connectState,
                    settings = settings,
                    onUrlChange = mainViewModel::updateServerUrl,
                    onUsernameChange = mainViewModel::updateUsername,
                    onPasswordChange = mainViewModel::updatePassword,
                    onTestServer = {
                        mainViewModel.testServerConnection(
                            emptyUrlMsg = strings.errorEmptyUrl,
                            failedMsg = strings.errorConnectionFailed
                        )
                    },
                    onSignIn = {
                        mainViewModel.signIn(
                            emptyUrlMsg = strings.errorEmptyUrl,
                            emptyUserMsg = strings.errorEmptyUser,
                            authFailedMsg = strings.errorAuthFailed
                        )
                    },
                    onSelectSavedServer = mainViewModel::switchServer,
                    onRemoveSavedServer = mainViewModel::removeSavedServer,
                    onToggleLanguage = {
                        val nextLang = if (settings.language == AppLanguage.ARABIC) {
                            AppLanguage.ENGLISH
                        } else AppLanguage.ARABIC
                        mainViewModel.setLanguage(nextLang)
                    },
                    onOpenSettings = { navController.navigate(Routes.PRE_AUTH_SETTINGS) }
                )
            }
            composable(Routes.PRE_AUTH_SETTINGS) {
                SettingsScreen(
                    settings = settings,
                    cacheSizeBytes = cacheSizeBytes,
                    showBackButton = true,
                    onBack = { navController.popBackStack() },
                    onSwitchServer = { srv ->
                        mainViewModel.switchServer(srv)
                        navController.popBackStack()
                    },
                    onSignOut = mainViewModel::signOut,
                    onAutoLoginChange = mainViewModel::setAutoLogin,
                    onPlaybackModeChange = mainViewModel::setPlaybackMode,
                    onWifiQualityChange = mainViewModel::setWifiQuality,
                    onMobileQualityChange = mainViewModel::setMobileQuality,
                    onCodecChange = mainViewModel::setPreferredCodec,
                    onDefaultSpeedChange = mainViewModel::setDefaultSpeed,
                    onSeekSecondsChange = mainViewModel::setSeekSeconds,
                    onAutoPlayNextChange = mainViewModel::setAutoPlayNext,
                    onResumeBehaviorChange = mainViewModel::setResumeBehavior,
                    onAudioLangChange = mainViewModel::setPreferredAudioLang,
                    onSubLangChange = mainViewModel::setPreferredSubLang,
                    onSubtitleSizeChange = mainViewModel::setSubtitleSize,
                    onKeepScreenOnChange = mainViewModel::setKeepScreenOn,
                    onThemeModeChange = mainViewModel::setThemeMode,
                    onLanguageChange = mainViewModel::setLanguage,
                    onShowProgressBarsChange = mainViewModel::setShowProgressBars,
                    onGridDensityChange = mainViewModel::setGridDensity,
                    onTimeoutChange = mainViewModel::setTimeoutSeconds,
                    onClearCache = mainViewModel::clearImageCache
                )
            }
        }
        return
    }

    val tabs = listOf(
        NavTab(Routes.HOME, strings.navHome, Icons.Filled.Home, Icons.Outlined.Home),
        NavTab(Routes.MOVIES, strings.navMovies, Icons.Filled.Movie, Icons.Outlined.Movie),
        NavTab(Routes.SERIES, strings.navSeries, Icons.Filled.LiveTv, Icons.Outlined.LiveTv),
        NavTab(Routes.SEARCH, strings.navSearch, Icons.Filled.Search, Icons.Outlined.Search),
        NavTab(Routes.SETTINGS, strings.navSettings, Icons.Filled.Settings, Icons.Outlined.Settings)
    )
    val showTopLevelChrome = currentRoute in tabs.map { it.route }

    fun navigateToTab(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun openItemDetailOrPlay(item: BaseItemDto) {
        if (item.type == "Episode") {
            navController.navigate(Routes.player(item.id, startFromBeginning = false))
        } else {
            navController.navigate(Routes.detail(item.id))
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showTopLevelChrome && !useNavigationRail) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    tabs.forEach { tab ->
                        val selected = currentRoute == tab.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = { navigateToTab(tab.route) },
                            icon = {
                                Icon(
                                    imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = tab.label
                                )
                            },
                            label = { Text(tab.label, maxLines = 1) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (showTopLevelChrome && useNavigationRail) {
                NavigationRail(
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    tabs.forEach { tab ->
                        val selected = currentRoute == tab.route
                        NavigationRailItem(
                            selected = selected,
                            onClick = { navigateToTab(tab.route) },
                            icon = {
                                Icon(
                                    imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = tab.label
                                )
                            },
                            label = { Text(tab.label, maxLines = 1) }
                        )
                    }
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                NavHost(navController = navController, startDestination = Routes.HOME) {
                    composable(Routes.HOME) {
                        HomeScreen(
                            homeState = homeState,
                            settings = settings,
                            onRefresh = { mainViewModel.refreshHome() },
                            onOpenDetail = { item -> navController.navigate(Routes.detail(item.id)) },
                            onPlayDirect = { item ->
                                if (item.type == "Series") {
                                    navController.navigate(Routes.detail(item.id))
                                } else {
                                    navController.navigate(Routes.player(item.id, startFromBeginning = false))
                                }
                            },
                            onNavigateMovies = { navigateToTab(Routes.MOVIES) },
                            onNavigateSeries = { navigateToTab(Routes.SERIES) }
                        )
                    }

                    composable(Routes.MOVIES) {
                        LibraryBrowseScreen(
                            title = strings.navMovies,
                            state = moviesState,
                            settings = settings,
                            onRefresh = { mainViewModel.loadMovies(reset = true) },
                            onLoadMore = { mainViewModel.loadMovies(reset = false) },
                            onFilterChange = { sort, unwatched, fav, genre ->
                                mainViewModel.updateMoviesFilter(sort, unwatched, fav, genre)
                            },
                            onItemClick = { item -> navController.navigate(Routes.detail(item.id)) }
                        )
                    }

                    composable(Routes.SERIES) {
                        LibraryBrowseScreen(
                            title = strings.navSeries,
                            state = seriesState,
                            settings = settings,
                            onRefresh = { mainViewModel.loadSeries(reset = true) },
                            onLoadMore = { mainViewModel.loadSeries(reset = false) },
                            onFilterChange = { sort, unwatched, fav, genre ->
                                mainViewModel.updateSeriesFilter(sort, unwatched, fav, genre)
                            },
                            onItemClick = { item -> navController.navigate(Routes.detail(item.id)) }
                        )
                    }

                    composable(Routes.SEARCH) {
                        SearchScreen(
                            state = searchState,
                            settings = settings,
                            onQueryChange = mainViewModel::updateSearchQuery,
                            onSubmitSearch = mainViewModel::submitSearch,
                            onFilterChange = mainViewModel::updateSearchTypeFilter,
                            onClearHistory = mainViewModel::clearRecentSearches,
                            onItemClick = { item -> openItemDetailOrPlay(item) }
                        )
                    }

                    composable(Routes.SETTINGS) {
                        SettingsScreen(
                            settings = settings,
                            cacheSizeBytes = cacheSizeBytes,
                            showBackButton = false,
                            onSwitchServer = mainViewModel::switchServer,
                            onSignOut = mainViewModel::signOut,
                            onAutoLoginChange = mainViewModel::setAutoLogin,
                            onPlaybackModeChange = mainViewModel::setPlaybackMode,
                            onWifiQualityChange = mainViewModel::setWifiQuality,
                            onMobileQualityChange = mainViewModel::setMobileQuality,
                            onCodecChange = mainViewModel::setPreferredCodec,
                            onDefaultSpeedChange = mainViewModel::setDefaultSpeed,
                            onSeekSecondsChange = mainViewModel::setSeekSeconds,
                            onAutoPlayNextChange = mainViewModel::setAutoPlayNext,
                            onResumeBehaviorChange = mainViewModel::setResumeBehavior,
                            onAudioLangChange = mainViewModel::setPreferredAudioLang,
                            onSubLangChange = mainViewModel::setPreferredSubLang,
                            onSubtitleSizeChange = mainViewModel::setSubtitleSize,
                            onKeepScreenOnChange = mainViewModel::setKeepScreenOn,
                            onThemeModeChange = mainViewModel::setThemeMode,
                            onLanguageChange = mainViewModel::setLanguage,
                            onShowProgressBarsChange = mainViewModel::setShowProgressBars,
                            onGridDensityChange = mainViewModel::setGridDensity,
                            onTimeoutChange = mainViewModel::setTimeoutSeconds,
                            onClearCache = mainViewModel::clearImageCache
                        )
                    }

                    composable(
                        route = Routes.DETAIL,
                        arguments = listOf(navArgument("itemId") { type = NavType.StringType })
                    ) { entry ->
                        val itemId = entry.arguments?.getString("itemId") ?: return@composable
                        val detailViewModel: DetailViewModel = viewModel(
                            key = "detail_${activeServer.id}_$itemId",
                            factory = DetailViewModel.Factory(appContainer, activeServer.userId, itemId)
                        )
                        val detailState by detailViewModel.state.collectAsStateWithLifecycle()

                        MediaDetailScreen(
                            state = detailState,
                            settings = settings,
                            onBack = { navController.popBackStack() },
                            onRetry = { detailViewModel.loadDetail() },
                            onSelectSeason = detailViewModel::selectSeason,
                            onToggleFavorite = detailViewModel::toggleFavorite,
                            onTogglePlayed = detailViewModel::togglePlayed,
                            onPlayItem = { targetId, fromBeginning ->
                                navController.navigate(Routes.player(targetId, fromBeginning))
                            },
                            onOpenSimilar = { sim -> navController.navigate(Routes.detail(sim.id)) }
                        )
                    }

                    composable(
                        route = Routes.PLAYER,
                        arguments = listOf(
                            navArgument("itemId") { type = NavType.StringType },
                            navArgument("startFromBeginning") {
                                type = NavType.BoolType
                                defaultValue = false
                            }
                        )
                    ) { entry ->
                        val itemId = entry.arguments?.getString("itemId") ?: return@composable
                        val startFromBeginning = entry.arguments?.getBoolean("startFromBeginning") ?: false
                        val playerViewModel: PlayerViewModel = viewModel(
                            key = "player_${activeServer.id}_${itemId}_$startFromBeginning",
                            factory = PlayerViewModel.Factory(
                                container = appContainer,
                                server = activeServer,
                                itemId = itemId,
                                startFromBeginning = startFromBeginning,
                                settings = settings
                            )
                        )
                        val playerUiState by playerViewModel.state.collectAsStateWithLifecycle()

                        PlayerScreen(
                            uiState = playerUiState,
                            server = activeServer,
                            settings = settings,
                            onRetry = { playerViewModel.loadPlaybackSession(itemId, startFromBeginning) },
                            onLoadEpisode = { nextId -> playerViewModel.loadPlaybackSession(nextId, true) },
                            onReportProgress = playerViewModel::reportProgress,
                            onReportStopped = playerViewModel::reportStopped,
                            onExit = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}
