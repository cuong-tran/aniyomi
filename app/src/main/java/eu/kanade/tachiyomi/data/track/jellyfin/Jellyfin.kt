package eu.kanade.tachiyomi.data.track.jellyfin

import android.graphics.Color
import dev.icerock.moko.resources.StringResource
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.animesource.AnimeSource
import eu.kanade.tachiyomi.data.database.models.AnimeTrack
import eu.kanade.tachiyomi.data.track.AnimeTracker
import eu.kanade.tachiyomi.data.track.BaseTracker
import eu.kanade.tachiyomi.data.track.EnhancedTracker
import eu.kanade.tachiyomi.data.track.model.TrackSearch
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import okhttp3.Dns
import tachiyomi.domain.anime.model.Anime
import tachiyomi.i18n.MR
import tachiyomi.domain.track.model.Track as DomainTrack

class Jellyfin(id: Long) : BaseTracker(id, "Jellyfin"), EnhancedTracker, AnimeTracker {

    companion object {
        const val UNSEEN = 1L
        const val WATCHING = 2L
        const val COMPLETED = 3L
    }

    override val client by lazy {
        networkService.client.newBuilder()
            .addInterceptor(JellyfinInterceptor())
            .dns(Dns.SYSTEM) // don't use DNS over HTTPS as it breaks IP addressing
            .build()
    }

    val api by lazy { JellyfinApi(id, client) }

    override fun getLogo() = R.drawable.ic_tracker_jellyfin

    override fun getLogoColor() = Color.rgb(0, 11, 37)

    override fun getStatusListAnime(): List<Long> = listOf(UNSEEN, WATCHING, COMPLETED)

    override fun getStatusForAnime(status: Long): StringResource? = when (status) {
        UNSEEN -> MR.strings.unseen
        WATCHING -> MR.strings.watching
        COMPLETED -> MR.strings.completed
        else -> null
    }

    override fun getWatchingStatus(): Long = WATCHING

    override fun getRewatchingStatus(): Long = -1

    override fun getCompletionStatus(): Long = COMPLETED

    override fun getScoreList(): ImmutableList<String> = persistentListOf()

    override fun displayScore(track: DomainTrack): String = ""

    override suspend fun update(track: AnimeTrack, didWatchEpisode: Boolean): AnimeTrack {
        return api.updateProgress(track)
    }

    override suspend fun bind(track: AnimeTrack, hasSeenEpisodes: Boolean): AnimeTrack {
        return track
    }

    override suspend fun searchAnime(query: String): List<TrackSearch> =
        throw Exception("Not used")

    override suspend fun refresh(track: AnimeTrack): AnimeTrack {
        val remoteTrack = api.getTrackSearch(track.tracking_url)
        track.copyPersonalFrom(remoteTrack)
        track.total_episodes = remoteTrack.total_episodes
        return track
    }

    override suspend fun login(username: String, password: String) {
        saveCredentials("user", "pass")
    }

    override fun loginNoop() {
        saveCredentials("user", "pass")
    }

    override fun getAcceptedSources() = listOf("eu.kanade.tachiyomi.animeextension.all.jellyfin.Jellyfin")

    override suspend fun match(anime: Anime): TrackSearch? =
        try {
            api.getTrackSearch(anime.url)
        } catch (e: Exception) {
            null
        }

    override fun isTrackFrom(track: DomainTrack, anime: Anime, animeSource: AnimeSource?): Boolean =
        track.remoteUrl == anime.url && animeSource?.let { accept(it) } == true

    override fun migrateTrack(track: DomainTrack, anime: Anime, newAnimeSource: AnimeSource): DomainTrack? {
        return if (accept(newAnimeSource)) {
            track.copy(remoteUrl = anime.url)
        } else {
            null
        }
    }
}
