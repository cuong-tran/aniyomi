package eu.kanade.tachiyomi.ui.deeplink

import androidx.compose.runtime.Immutable
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.domain.anime.model.toDomainAnime
import eu.kanade.domain.anime.model.toSAnime
import eu.kanade.domain.episode.interactor.SyncEpisodesWithSource
import eu.kanade.tachiyomi.animesource.AnimeSource
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.animesource.model.SEpisode
import eu.kanade.tachiyomi.animesource.online.ResolvableAnimeSource
import eu.kanade.tachiyomi.animesource.online.UriType
import kotlinx.coroutines.flow.update
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.domain.anime.interactor.GetAnimeByUrlAndSourceId
import tachiyomi.domain.anime.interactor.NetworkToLocalAnime
import tachiyomi.domain.anime.model.Anime
import tachiyomi.domain.episode.interactor.GetEpisodeByUrlAndAnimeId
import tachiyomi.domain.episode.model.Episode
import tachiyomi.domain.source.service.SourceManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class DeepLinkScreenModel(
    query: String = "",
    private val sourceManager: SourceManager = Injekt.get(),
    private val networkToLocalAnime: NetworkToLocalAnime = Injekt.get(),
    private val getEpisodeByUrlAndAnimeId: GetEpisodeByUrlAndAnimeId = Injekt.get(),
    private val getAnimeByUrlAndSourceId: GetAnimeByUrlAndSourceId = Injekt.get(),
    private val syncEpisodesWithSource: SyncEpisodesWithSource = Injekt.get(),
) : StateScreenModel<DeepLinkScreenModel.State>(State.Loading) {

    init {
        screenModelScope.launchIO {
            val source = sourceManager.getCatalogueSources()
                .filterIsInstance<ResolvableAnimeSource>()
                .firstOrNull { it.getUriType(query) != UriType.Unknown }

            val anime = source?.getAnime(query)?.let {
                getAnimeFromSAnime(it, source.id)
            }

            val episode = if (source?.getUriType(query) == UriType.Episode && anime != null) {
                source.getEpisode(query)?.let { getEpisodeFromSEpisode(it, anime, source) }
            } else {
                null
            }

            mutableState.update {
                if (anime == null) {
                    State.NoResults
                } else {
                    if (episode == null) {
                        State.Result(anime)
                    } else {
                        State.Result(anime, episode.id)
                    }
                }
            }
        }
    }

    private suspend fun getEpisodeFromSEpisode(sEpisode: SEpisode, anime: Anime, animeSource: AnimeSource): Episode? {
        val localEpisode = getEpisodeByUrlAndAnimeId.await(sEpisode.url, anime.id)

        return if (localEpisode == null) {
            val sourceEpisodes = animeSource.getEpisodeList(anime.toSAnime())
            val newEpisodes = syncEpisodesWithSource.await(sourceEpisodes, anime, animeSource, false)
            newEpisodes.find { it.url == sEpisode.url }
        } else {
            localEpisode
        }
    }

    private suspend fun getAnimeFromSAnime(sAnime: SAnime, sourceId: Long): Anime {
        return getAnimeByUrlAndSourceId.await(sAnime.url, sourceId)
            ?: networkToLocalAnime.await(sAnime.toDomainAnime(sourceId))
    }

    sealed interface State {
        @Immutable
        data object Loading : State

        @Immutable
        data object NoResults : State

        @Immutable
        data class Result(val anime: Anime, val episodeId: Long? = null) : State
    }
}
