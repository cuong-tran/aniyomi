package eu.kanade.tachiyomi.ui.browse.migration.anime

import androidx.compose.runtime.Immutable
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.tachiyomi.animesource.AnimeSource
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.anime.interactor.GetFavorites
import tachiyomi.domain.anime.model.Anime
import tachiyomi.domain.source.service.SourceManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class MigrateAnimeScreenModel(
    private val sourceId: Long,
    private val sourceManager: SourceManager = Injekt.get(),
    private val getFavorites: GetFavorites = Injekt.get(),
) : StateScreenModel<MigrateAnimeScreenModel.State>(State()) {

    private val _events: Channel<MigrationAnimeEvent> = Channel()
    val events: Flow<MigrationAnimeEvent> = _events.receiveAsFlow()

    init {
        screenModelScope.launch {
            mutableState.update { state ->
                state.copy(animeSource = sourceManager.getOrStub(sourceId))
            }

            getFavorites.subscribe(sourceId)
                .catch {
                    logcat(LogPriority.ERROR, it)
                    _events.send(MigrationAnimeEvent.FailedFetchingFavorites)
                    mutableState.update { state ->
                        state.copy(titleList = persistentListOf())
                    }
                }
                .map { anime ->
                    anime
                        .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.title })
                        .toImmutableList()
                }
                .collectLatest { list ->
                    mutableState.update { it.copy(titleList = list) }
                }
        }
    }

    @Immutable
    data class State(
        val animeSource: AnimeSource? = null,
        private val titleList: ImmutableList<Anime>? = null,
    ) {

        val titles: ImmutableList<Anime>
            get() = titleList ?: persistentListOf()

        val isLoading: Boolean
            get() = animeSource == null || titleList == null

        val isEmpty: Boolean
            get() = titles.isEmpty()
    }
}

sealed interface MigrationAnimeEvent {
    data object FailedFetchingFavorites : MigrationAnimeEvent
}
