package eu.kanade.domain.anime.interactor

import eu.kanade.domain.anime.model.hasCustomCover
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.data.cache.CoverCache
import tachiyomi.domain.anime.interactor.FetchInterval
import tachiyomi.domain.anime.model.Anime
import tachiyomi.domain.anime.model.AnimeUpdate
import tachiyomi.domain.anime.repository.AnimeRepository
import tachiyomi.source.local.isLocal
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.time.Instant
import java.time.ZonedDateTime

class UpdateAnime(
    private val animeRepository: AnimeRepository,
    private val fetchInterval: FetchInterval,
) {

    suspend fun await(animeUpdate: AnimeUpdate): Boolean {
        return animeRepository.update(animeUpdate)
    }

    suspend fun awaitAll(animeUpdates: List<AnimeUpdate>): Boolean {
        return animeRepository.updateAll(animeUpdates)
    }

    suspend fun awaitUpdateFromSource(
        localAnime: Anime,
        remoteAnime: SAnime,
        manualFetch: Boolean,
        coverCache: CoverCache = Injekt.get(),
    ): Boolean {
        val remoteTitle = try {
            remoteAnime.title
        } catch (_: UninitializedPropertyAccessException) {
            ""
        }

        // if the anime isn't a favorite, set its title from source and update in db
        val title = if (remoteTitle.isEmpty() || localAnime.favorite) null else remoteTitle

        val coverLastModified =
            when {
                // Never refresh covers if the url is empty to avoid "losing" existing covers
                remoteAnime.thumbnail_url.isNullOrEmpty() -> null
                !manualFetch && localAnime.thumbnailUrl == remoteAnime.thumbnail_url -> null
                localAnime.isLocal() -> Instant.now().toEpochMilli()
                localAnime.hasCustomCover(coverCache) -> {
                    coverCache.deleteFromCache(localAnime, false)
                    null
                }
                else -> {
                    coverCache.deleteFromCache(localAnime, false)
                    Instant.now().toEpochMilli()
                }
            }

        val thumbnailUrl = remoteAnime.thumbnail_url?.takeIf { it.isNotEmpty() }

        return animeRepository.update(
            AnimeUpdate(
                id = localAnime.id,
                title = title,
                coverLastModified = coverLastModified,
                author = remoteAnime.author,
                artist = remoteAnime.artist,
                description = remoteAnime.description,
                genre = remoteAnime.getGenres(),
                thumbnailUrl = thumbnailUrl,
                status = remoteAnime.status.toLong(),
                animeUpdateStrategy = remoteAnime.update_strategy,
                initialized = true,
            ),
        )
    }

    suspend fun awaitUpdateFetchInterval(
        anime: Anime,
        dateTime: ZonedDateTime = ZonedDateTime.now(),
        window: Pair<Long, Long> = fetchInterval.getWindow(dateTime),
    ): Boolean {
        return animeRepository.update(
            fetchInterval.toAnimeUpdate(anime, dateTime, window),
        )
    }

    suspend fun awaitUpdateLastUpdate(animeId: Long): Boolean {
        return animeRepository.update(AnimeUpdate(id = animeId, lastUpdate = Instant.now().toEpochMilli()))
    }

    suspend fun awaitUpdateCoverLastModified(mangaId: Long): Boolean {
        return animeRepository.update(
            AnimeUpdate(id = mangaId, coverLastModified = Instant.now().toEpochMilli()),
        )
    }

    suspend fun awaitUpdateFavorite(animeId: Long, favorite: Boolean): Boolean {
        val dateAdded = when (favorite) {
            true -> Instant.now().toEpochMilli()
            false -> 0
        }
        return animeRepository.update(
            AnimeUpdate(id = animeId, favorite = favorite, dateAdded = dateAdded),
        )
    }
}
