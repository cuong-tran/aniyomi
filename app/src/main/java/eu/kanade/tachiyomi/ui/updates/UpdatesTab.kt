package eu.kanade.tachiyomi.ui.updates

import android.content.Context
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.FlipToBack
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.TabOptions
import eu.kanade.domain.ui.model.NavStyle
import eu.kanade.presentation.anime.EpisodeOptionsDialogScreen
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.NavigatorAdaptiveSheet
import eu.kanade.presentation.components.TabContent
import eu.kanade.presentation.components.TabbedScreen
import eu.kanade.presentation.updates.AnimeUpdateScreen
import eu.kanade.presentation.updates.UpdatesDeleteConfirmationDialog
import eu.kanade.presentation.util.Tab
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.anime.AnimeScreen
import eu.kanade.tachiyomi.ui.download.DownloadQueueScreen
import eu.kanade.tachiyomi.ui.home.HomeScreen
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.ui.player.settings.PlayerPreferences
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import mihon.feature.upcoming.UpcomingScreen
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import uy.kohesive.injekt.injectLazy

data object UpdatesTab : Tab {

    override val options: TabOptions
        @Composable
        get() {
            val isSelected = LocalTabNavigator.current.current.key == key
            val image = AnimatedImageVector.animatedVectorResource(R.drawable.anim_updates_enter)
            val index: UShort = when (currentNavigationStyle()) {
                NavStyle.MOVE_UPDATES_TO_MORE -> 4u // 5
                NavStyle.MOVE_HISTORY_TO_MORE -> 2u // 2
                NavStyle.MOVE_BROWSE_TO_MORE -> 1u // 2
                // NavStyle.MOVE_MANGA_TO_MORE -> 1u
            }
            return TabOptions(
                index = index,
                title = stringResource(MR.strings.label_recent_updates),
                icon = rememberAnimatedVectorPainter(image, isSelected),
            )
        }
    override suspend fun onReselect(navigator: Navigator) {
        navigator.push(DownloadQueueScreen)
    }

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val fromMore = currentNavigationStyle() == NavStyle.MOVE_UPDATES_TO_MORE

        TabbedScreen(
            titleRes = MR.strings.label_recent_updates,
            tabs = persistentListOf(
                animeUpdatesTab(context, fromMore),
            ),
        )

        LaunchedEffect(Unit) {
            (context as? MainActivity)?.ready = true
        }
    }
}

@Composable
fun Screen.animeUpdatesTab(
    context: Context,
    fromMore: Boolean,
): TabContent {
    val navigator = LocalNavigator.currentOrThrow
    val screenModel = rememberScreenModel { AnimeUpdatesScreenModel() }
    val scope = rememberCoroutineScope()
    val state by screenModel.state.collectAsState()

    val navigateUp: (() -> Unit)? = if (fromMore) {
        {
            if (navigator.lastItem == HomeScreen) {
                scope.launch { HomeScreen.openTab(HomeScreen.Tab.Library()) }
            } else {
                navigator.pop()
            }
        }
    } else {
        null
    }

    suspend fun openEpisode(updateItem: AnimeUpdatesItem, altPlayer: Boolean = false) {
        val playerPreferences: PlayerPreferences by injectLazy()
        val update = updateItem.update
        val extPlayer = playerPreferences.alwaysUseExternalPlayer().get() != altPlayer
        MainActivity.startPlayerActivity(context, update.animeId, update.episodeId, extPlayer)
    }

    return TabContent(
        titleRes = MR.strings.label_anime_updates,
        searchEnabled = false,
        content = { contentPadding, _ ->
            AnimeUpdateScreen(
                state = state,
                snackbarHostState = screenModel.snackbarHostState,
                lastUpdated = screenModel.lastUpdated,
                onClickCover = { item -> navigator.push(AnimeScreen(item.update.animeId)) },
                onSelectAll = screenModel::toggleAllSelection,
                onInvertSelection = screenModel::invertSelection,
                onUpdateLibrary = screenModel::updateLibrary,
                onDownloadEpisode = screenModel::downloadEpisodes,
                onMultiBookmarkClicked = screenModel::bookmarkUpdates,
                onMultiMarkAsSeenClicked = screenModel::markUpdatesSeen,
                onMultiDeleteClicked = screenModel::showConfirmDeleteEpisodes,
                onUpdateSelected = screenModel::toggleSelection,
                onOpenEpisode = { updateItem: AnimeUpdatesItem, altPlayer: Boolean ->
                    scope.launchIO {
                        openEpisode(updateItem, altPlayer)
                    }
                    Unit
                },
            )

            val onDismissDialog = { screenModel.setDialog(null) }
            when (val dialog = state.dialog) {
                is AnimeUpdatesScreenModel.Dialog.DeleteConfirmation -> {
                    UpdatesDeleteConfirmationDialog(
                        onDismissRequest = onDismissDialog,
                        onConfirm = { screenModel.deleteEpisodes(dialog.toDelete) },
                        isManga = false,
                    )
                }
                is AnimeUpdatesScreenModel.Dialog.ShowQualities -> {
                    EpisodeOptionsDialogScreen.onDismissDialog = onDismissDialog
                    NavigatorAdaptiveSheet(
                        screen = EpisodeOptionsDialogScreen(
                            useExternalDownloader = screenModel.useExternalDownloader,
                            episodeTitle = dialog.episodeTitle,
                            episodeId = dialog.episodeId,
                            animeId = dialog.animeId,
                            sourceId = dialog.sourceId,
                        ),
                        onDismissRequest = onDismissDialog,
                    )
                }
                null -> {}
            }

            LaunchedEffect(Unit) {
                screenModel.events.collectLatest { event ->
                    when (event) {
                        AnimeUpdatesScreenModel.Event.InternalError -> screenModel.snackbarHostState.showSnackbar(
                            context.stringResource(
                                MR.strings.internal_error,
                            ),
                        )
                        is AnimeUpdatesScreenModel.Event.LibraryUpdateTriggered -> {
                            val msg = if (event.started) {
                                MR.strings.updating_library
                            } else {
                                MR.strings.update_already_running
                            }
                            screenModel.snackbarHostState.showSnackbar(context.stringResource(msg))
                        }
                    }
                }
            }

            LaunchedEffect(state.selectionMode) {
                HomeScreen.showBottomNav(!state.selectionMode)
            }

            LaunchedEffect(state.isLoading) {
                if (!state.isLoading) {
                    (context as? MainActivity)?.ready = true
                }
            }
            DisposableEffect(Unit) {
                screenModel.resetNewUpdatesCount()

                onDispose {
                    screenModel.resetNewUpdatesCount()
                }
            }
        },
        actions =
        if (screenModel.state.collectAsState().value.selected.isNotEmpty()) {
            persistentListOf(
                AppBar.Action(
                    title = stringResource(MR.strings.action_select_all),
                    icon = Icons.Outlined.SelectAll,
                    onClick = { screenModel.toggleAllSelection(true) },
                ),
                AppBar.Action(
                    title = stringResource(MR.strings.action_select_inverse),
                    icon = Icons.Outlined.FlipToBack,
                    onClick = { screenModel.invertSelection() },
                ),
            )
        } else {
            persistentListOf(
                AppBar.Action(
                    title = stringResource(MR.strings.action_view_upcoming),
                    icon = Icons.Outlined.CalendarMonth,
                    onClick = { navigator.push(UpcomingScreen()) },
                ),
                AppBar.Action(
                    title = stringResource(MR.strings.action_update_library),
                    icon = Icons.Outlined.Refresh,
                    onClick = { screenModel.updateLibrary() },
                ),
            )
        },
        navigateUp = navigateUp,
    )
}

private const val TAB_ANIME = 0
private const val TAB_MANGA = 1
