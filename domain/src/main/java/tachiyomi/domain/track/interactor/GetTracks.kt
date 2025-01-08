package tachiyomi.domain.track.interactor

import kotlinx.coroutines.flow.Flow
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.track.model.Track
import tachiyomi.domain.track.repository.TrackRepository

class GetTracks(
    private val animetrackRepository: TrackRepository,
) {

    suspend fun awaitOne(id: Long): Track? {
        return try {
            animetrackRepository.getTrackById(id)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            null
        }
    }

    suspend fun await(animeId: Long): List<Track> {
        return try {
            animetrackRepository.getTracksByAnimeId(animeId)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            emptyList()
        }
    }

    fun subscribe(animeId: Long): Flow<List<Track>> {
        return animetrackRepository.getTracksByAnimeIdAsFlow(animeId)
    }
}
