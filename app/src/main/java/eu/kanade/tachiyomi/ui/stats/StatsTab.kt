package eu.kanade.tachiyomi.ui.stats

import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.TabOptions
import eu.kanade.presentation.components.TabContent
import eu.kanade.presentation.components.TabbedScreen
import eu.kanade.presentation.more.stats.AnimeStatsScreenContent
import eu.kanade.presentation.more.stats.StatsScreenState
import eu.kanade.presentation.util.Tab
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.main.MainActivity
import kotlinx.collections.immutable.persistentListOf
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.LoadingScreen

data object StatsTab : Tab {

    override val options: TabOptions
        @Composable
        get() {
            val isSelected = LocalTabNavigator.current.current.key == key
            val image = AnimatedImageVector.animatedVectorResource(R.drawable.anim_updates_enter)
            return TabOptions(
                index = 8u,
                title = stringResource(MR.strings.label_stats),
                icon = rememberAnimatedVectorPainter(image, isSelected),
            )
        }

    @Composable
    override fun Content() {
        val context = LocalContext.current

        val tabs = persistentListOf(
            animeStatsTab(),
        )
        val state = rememberPagerState { tabs.size }

        TabbedScreen(
            titleRes = MR.strings.label_stats,
            tabs = tabs,
            state = state,
        )

        LaunchedEffect(Unit) {
            (context as? MainActivity)?.ready = true
        }
    }
}

@Composable
fun Screen.animeStatsTab(): TabContent {
    val navigator = LocalNavigator.currentOrThrow

    val screenModel = rememberScreenModel { StatsScreenModel() }
    val state by screenModel.state.collectAsState()

    if (state is StatsScreenState.Loading) {
        LoadingScreen()
    }

    return TabContent(
        titleRes = MR.strings.label_anime,
        content = { contentPadding, _ ->

            if (state is StatsScreenState.Loading) {
                LoadingScreen()
            } else {
                AnimeStatsScreenContent(
                    state = state as StatsScreenState.SuccessAnime,
                    paddingValues = contentPadding,
                )
            }
        },
        navigateUp = navigator::pop,
    )
}
