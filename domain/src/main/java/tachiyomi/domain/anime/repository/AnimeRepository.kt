package tachiyomi.domain.anime.repository

import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.anime.model.Anime
import tachiyomi.domain.anime.model.AnimeUpdate
import tachiyomi.domain.library.model.LibraryAnime

interface AnimeRepository {

    suspend fun getAnimeById(id: Long): Anime

    suspend fun getAnimeByIdAsFlow(id: Long): Flow<Anime>

    suspend fun getAnimeByUrlAndSourceId(url: String, sourceId: Long): Anime?

    fun getAnimeByUrlAndSourceIdAsFlow(url: String, sourceId: Long): Flow<Anime?>

    suspend fun getFavorites(): List<Anime>

    suspend fun getWatchedAnimeNotInLibrary(): List<Anime>

    suspend fun getLibraryAnime(): List<LibraryAnime>

    fun getLibraryAnimeAsFlow(): Flow<List<LibraryAnime>>

    fun getFavoritesBySourceId(sourceId: Long): Flow<List<Anime>>

    suspend fun getDuplicateLibraryAnime(id: Long, title: String): List<Anime>

    suspend fun getUpcomingAnime(statuses: Set<Long>): Flow<List<Anime>>

    suspend fun resetViewerFlags(): Boolean

    suspend fun setAnimeCategories(animeId: Long, categoryIds: List<Long>)

    suspend fun insert(anime: Anime): Long?

    suspend fun update(update: AnimeUpdate): Boolean

    suspend fun updateAll(animeUpdates: List<AnimeUpdate>): Boolean
}
