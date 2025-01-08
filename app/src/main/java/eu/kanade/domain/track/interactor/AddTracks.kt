package eu.kanade.domain.track.interactor

import eu.kanade.domain.track.model.toDbTrack
import eu.kanade.domain.track.model.toDomainTrack
import eu.kanade.tachiyomi.animesource.AnimeSource
import eu.kanade.tachiyomi.data.database.models.AnimeTrack
import eu.kanade.tachiyomi.data.track.AnimeTracker
import eu.kanade.tachiyomi.data.track.EnhancedTracker
import eu.kanade.tachiyomi.data.track.Tracker
import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.util.lang.convertEpochMillisZone
import logcat.LogPriority
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.core.common.util.lang.withNonCancellableContext
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.anime.model.Anime
import tachiyomi.domain.episode.interactor.GetEpisodesByAnimeId
import tachiyomi.domain.history.interactor.GetHistory
import tachiyomi.domain.track.interactor.InsertTrack
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.time.ZoneOffset

class AddTracks(
    private val insertTrack: InsertTrack,
    private val syncChapterProgressWithTrack: SyncEpisodeProgressWithTrack,
    private val getEpisodesByAnimeId: GetEpisodesByAnimeId,
    private val trackerManager: TrackerManager,
) {

    // TODO: update all trackers based on common data
    suspend fun bind(tracker: AnimeTracker, item: AnimeTrack, animeId: Long) = withNonCancellableContext {
        withIOContext {
            val allChapters = getEpisodesByAnimeId.await(animeId)
            val hasSeenEpisodes = allChapters.any { it.seen }
            tracker.bind(item, hasSeenEpisodes)

            var track = item.toDomainTrack(idRequired = false) ?: return@withIOContext

            insertTrack.await(track)

            // TODO: merge into [SyncChapterProgressWithTrack]?
            // Update chapter progress if newer chapters marked read locally
            if (hasSeenEpisodes) {
                val latestLocalReadChapterNumber = allChapters
                    .sortedBy { it.episodeNumber }
                    .takeWhile { it.seen }
                    .lastOrNull()
                    ?.episodeNumber ?: -1.0

                if (latestLocalReadChapterNumber > track.lastEpisodeSeen) {
                    track = track.copy(
                        lastEpisodeSeen = latestLocalReadChapterNumber,
                    )
                    tracker.setRemoteLastEpisodeSeen(track.toDbTrack(), latestLocalReadChapterNumber.toInt())
                }

                if (track.startDate <= 0) {
                    val firstReadChapterDate = Injekt.get<GetHistory>().await(animeId)
                        .sortedBy { it.seenAt }
                        .firstOrNull()
                        ?.seenAt

                    firstReadChapterDate?.let {
                        val startDate = firstReadChapterDate.time.convertEpochMillisZone(
                            ZoneOffset.systemDefault(),
                            ZoneOffset.UTC,
                        )
                        track = track.copy(
                            startDate = startDate,
                        )
                        tracker.setRemoteStartDate(track.toDbTrack(), startDate)
                    }
                }
            }

            syncChapterProgressWithTrack.await(animeId, track, tracker)
        }
    }

    suspend fun bindEnhancedTrackers(anime: Anime, animeSource: AnimeSource) = withNonCancellableContext {
        withIOContext {
            trackerManager.loggedInTrackers()
                .filterIsInstance<EnhancedTracker>()
                .filter { it.accept(animeSource) }
                .forEach { service ->
                    try {
                        service.match(anime)?.let { track ->
                            track.anime_id = anime.id
                            (service as Tracker).animeService.bind(track)
                            insertTrack.await(track.toDomainTrack(idRequired = false)!!)

                            syncChapterProgressWithTrack.await(
                                anime.id,
                                track.toDomainTrack(idRequired = false)!!,
                                service.animeService,
                            )
                        }
                    } catch (e: Exception) {
                        logcat(
                            LogPriority.WARN,
                            e,
                        ) { "Could not match anime: ${anime.title} with service $service" }
                    }
                }
        }
    }
}
