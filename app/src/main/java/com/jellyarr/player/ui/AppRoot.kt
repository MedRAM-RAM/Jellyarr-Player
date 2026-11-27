package com.jellyarr.player.ui

import android.net.Uri
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.jellyarr.player.JellyarrApp
import com.jellyarr.player.data.streams.PlaybackRequest
import com.jellyarr.player.data.streams.StreamRequest
import com.jellyarr.player.player.PlayerScreen
import com.jellyarr.player.ui.components.CenterLoading
import com.jellyarr.player.ui.screens.AddonsScreen
import com.jellyarr.player.ui.screens.CatalogScreen
import com.jellyarr.player.ui.screens.JellyfinDetailsScreen
import com.jellyarr.player.ui.screens.JellyfinLibraryScreen
import com.jellyarr.player.ui.screens.JellyfinLoginScreen
import com.jellyarr.player.ui.screens.MainScreen
import com.jellyarr.player.ui.screens.MetaDetailsScreen
import com.jellyarr.player.ui.screens.NuvioScreen
import com.jellyarr.player.ui.screens.OnboardingScreen
import com.jellyarr.player.ui.screens.PlaybackSettingsScreen
import com.jellyarr.player.ui.screens.StreamsScreen
import com.jellyarr.player.ui.theme.JColors

/** Navigation helper passed down the tree. */
class Navigator(val nav: NavHostController) {
    private val c get() = JellyarrApp.container

    fun open(target: Target) = when (target) {
        is Target.Jellyfin -> nav.navigate("jf/${Uri.encode(target.id)}")
        is Target.Meta -> nav.navigate(
            "meta/${Uri.encode(target.type)}/${Uri.encode(target.id)}" +
                (target.addonUrl?.let { "?addon=${Uri.encode(it)}" } ?: "")
        )
    }

    fun streams(req: StreamRequest) {
        c.pendingStreamRequest = req
        nav.navigate("streams")
    }

    fun play(req: PlaybackRequest) {
        c.pendingPlayback = req
        nav.navigate("player")
    }

    fun library(id: String, name: String) = nav.navigate("library/${Uri.encode(id)}/${Uri.encode(name)}")
    fun catalog(addonUrl: String, type: String, id: String, name: String) =
        nav.navigate("catalog/${Uri.encode(addonUrl)}/${Uri.encode(type)}/${Uri.encode(id)}/${Uri.encode(name)}")

    fun to(route: String) = nav.navigate(route)
    fun back() { if (!nav.popBackStack()) Unit }
}

val LocalNavigator = staticCompositionLocalOf<Navigator> { error("no navigator") }

@Composable
fun AppRoot(pendingInstall: MutableState<String?>) {
    val settings by JellyarrApp.container.settings.state.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val navigator = remember(navController) { Navigator(navController) }

    CompositionLocalProvider(
        LocalNavigator provides navigator,
        LocalLayoutDirection provides LayoutDirection.Rtl,
    ) {
        Box(Modifier.fillMaxSize().background(JColors.Background)) {
            if (!settings.loaded) {
                CenterLoading()
            } else {
            val start = remember { if (settings.onboardingDone) "main" else "onboarding" }
            NavHost(
                navController = navController,
                startDestination = start,
                enterTransition = { fadeIn() },
                exitTransition = { fadeOut() },
            ) {
                composable("onboarding") { OnboardingScreen() }
                composable("main") { MainScreen() }
                composable("jflogin") { JellyfinLoginScreen() }
                composable("addons?url={url}", arguments = listOf(navArgument("url") { type = NavType.StringType; nullable = true; defaultValue = null })) {
                    AddonsScreen(initialUrl = it.arguments?.getString("url"))
                }
                composable("nuvio") { NuvioScreen() }
                composable("playback") { PlaybackSettingsScreen() }
                composable("jf/{id}") { JellyfinDetailsScreen(it.arguments?.getString("id").orEmpty()) }
                composable(
                    "meta/{type}/{id}?addon={addon}",
                    arguments = listOf(navArgument("addon") { type = NavType.StringType; nullable = true; defaultValue = null }),
                ) {
                    MetaDetailsScreen(
                        type = it.arguments?.getString("type").orEmpty(),
                        id = it.arguments?.getString("id").orEmpty(),
                        addonUrl = it.arguments?.getString("addon"),
                    )
                }
                composable("library/{id}/{name}") {
                    JellyfinLibraryScreen(it.arguments?.getString("id").orEmpty(), it.arguments?.getString("name").orEmpty())
                }
                composable("catalog/{addon}/{type}/{id}/{name}") {
                    CatalogScreen(
                        addonUrl = it.arguments?.getString("addon").orEmpty(),
                        type = it.arguments?.getString("type").orEmpty(),
                        catalogId = it.arguments?.getString("id").orEmpty(),
                        name = it.arguments?.getString("name").orEmpty(),
                    )
                }
                composable("streams") { StreamsScreen() }
                composable("player") {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) { PlayerScreen() }
                }
            }

            LaunchedEffect(pendingInstall.value) {
                val url = pendingInstall.value ?: return@LaunchedEffect
                pendingInstall.value = null
                navController.navigate("addons?url=${Uri.encode(url)}")
            }
            }
        }
    }
}
