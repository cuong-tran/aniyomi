package eu.kanade.presentation.browse

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import eu.kanade.presentation.browse.components.GlobalAnimeSearchToolbar
import eu.kanade.tachiyomi.animesource.AnimeCatalogueSource
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.SearchScreenModel
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.SourceFilter
import tachiyomi.domain.anime.model.Anime
import tachiyomi.presentation.core.components.material.Scaffold

@Composable
fun MigrateAnimeSearchScreen(
    state: SearchScreenModel.State,
    fromSourceId: Long?,
    navigateUp: () -> Unit,
    onChangeSearchQuery: (String?) -> Unit,
    onSearch: (String) -> Unit,
    onChangeSearchFilter: (SourceFilter) -> Unit,
    onToggleResults: () -> Unit,
    getAnime: @Composable (Anime) -> State<Anime>,
    onClickSource: (AnimeCatalogueSource) -> Unit,
    onClickItem: (Anime) -> Unit,
    onLongClickItem: (Anime) -> Unit,
) {
    Scaffold(
        topBar = { scrollBehavior ->
            GlobalAnimeSearchToolbar(
                searchQuery = state.searchQuery,
                progress = state.progress,
                total = state.total,
                navigateUp = navigateUp,
                onChangeSearchQuery = onChangeSearchQuery,
                onSearch = onSearch,
                sourceFilter = state.sourceFilter,
                onChangeSearchFilter = onChangeSearchFilter,
                onlyShowHasResults = state.onlyShowHasResults,
                onToggleResults = onToggleResults,
                scrollBehavior = scrollBehavior,
            )
        },
    ) { paddingValues ->
        GlobalSearchContent(
            fromSourceId = fromSourceId,
            items = state.filteredItems,
            contentPadding = paddingValues,
            getAnime = getAnime,
            onClickSource = onClickSource,
            onClickItem = onClickItem,
            onLongClickItem = onLongClickItem,
        )
    }
}
