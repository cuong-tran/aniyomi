package eu.kanade.tachiyomi.data.track.shikimori

import android.graphics.Color
import dev.icerock.moko.resources.StringResource
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.AnimeTrack
import eu.kanade.tachiyomi.data.track.AnimeTracker
import eu.kanade.tachiyomi.data.track.BaseTracker
import eu.kanade.tachiyomi.data.track.DeletableTracker
import eu.kanade.tachiyomi.data.track.model.TrackSearch
import eu.kanade.tachiyomi.data.track.shikimori.dto.SMOAuth
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import tachiyomi.domain.track.model.Track
import tachiyomi.i18n.MR
import uy.kohesive.injekt.injectLazy

class Shikimori(id: Long) :
    BaseTracker(
        id,
        "Shikimori",
    ),
    AnimeTracker,
    DeletableTracker {

    companion object {
        const val READING = 1L
        const val COMPLETED = 2L
        const val ON_HOLD = 3L
        const val DROPPED = 4L
        const val PLAN_TO_READ = 5L
        const val REREADING = 6L

        private val SCORE_LIST = IntRange(0, 10)
            .map(Int::toString)
            .toImmutableList()
    }

    private val json: Json by injectLazy()

    private val interceptor by lazy { ShikimoriInterceptor(this) }

    private val api by lazy { ShikimoriApi(id, client, interceptor) }

    override fun getScoreList(): ImmutableList<String> = SCORE_LIST

    override fun indexToScore(index: Int): Double {
        return index.toDouble()
    }

    override fun displayScore(track: Track): String {
        return track.score.toInt().toString()
    }

    private suspend fun add(track: AnimeTrack): AnimeTrack {
        return api.addLibAnime(track, getUsername())
    }

    override suspend fun update(track: AnimeTrack, didWatchEpisode: Boolean): AnimeTrack {
        if (track.status != COMPLETED) {
            if (didWatchEpisode) {
                if (track.last_episode_seen.toLong() == track.total_episodes && track.total_episodes > 0) {
                    track.status = COMPLETED
                } else if (track.status != REREADING) {
                    track.status = READING
                }
            }
        }

        return api.updateLibAnime(track, getUsername())
    }

    override suspend fun delete(track: Track) {
        api.deleteLibAnime(track)
    }

    override suspend fun bind(track: AnimeTrack, hasSeenChapters: Boolean): AnimeTrack {
        val remoteTrack = api.findLibAnime(track, getUsername())
        return if (remoteTrack != null) {
            track.copyPersonalFrom(remoteTrack)
            track.library_id = remoteTrack.library_id

            if (track.status != COMPLETED) {
                val isRereading = track.status == REREADING
                track.status = if (!isRereading && hasSeenChapters) READING else track.status
            }

            update(track)
        } else {
            // Set default fields if it's not found in the list
            track.status = if (hasSeenChapters) READING else PLAN_TO_READ
            track.score = 0.0
            add(track)
        }
    }

    override suspend fun searchAnime(query: String): List<TrackSearch> {
        return api.searchAnime(query)
    }

    override suspend fun refresh(track: AnimeTrack): AnimeTrack {
        api.findLibAnime(track, getUsername())?.let { remoteTrack ->
            track.library_id = remoteTrack.library_id
            track.copyPersonalFrom(remoteTrack)
            track.total_episodes = remoteTrack.total_episodes
        } ?: throw Exception("Could not find anime")
        return track
    }

    override fun getLogo() = R.drawable.ic_tracker_shikimori

    override fun getLogoColor() = Color.rgb(40, 40, 40)

    override fun getStatusListAnime(): List<Long> {
        return listOf(READING, COMPLETED, ON_HOLD, DROPPED, PLAN_TO_READ, REREADING)
    }

    override fun getStatusForAnime(status: Long): StringResource? = when (status) {
        READING -> MR.strings.watching
        PLAN_TO_READ -> MR.strings.plan_to_watch
        COMPLETED -> MR.strings.completed
        ON_HOLD -> MR.strings.on_hold
        DROPPED -> MR.strings.dropped
        REREADING -> MR.strings.repeating_anime
        else -> null
    }

    override fun getWatchingStatus(): Long = READING

    override fun getRewatchingStatus(): Long = REREADING

    override fun getCompletionStatus(): Long = COMPLETED

    override suspend fun login(username: String, password: String) = login(password)

    suspend fun login(code: String) {
        try {
            val oauth = api.accessToken(code)
            interceptor.newAuth(oauth)
            val user = api.getCurrentUser()
            saveCredentials(user.toString(), oauth.accessToken)
        } catch (e: Throwable) {
            logout()
        }
    }

    fun saveToken(oauth: SMOAuth?) {
        trackPreferences.trackToken(this).set(json.encodeToString(oauth))
    }

    fun restoreToken(): SMOAuth? {
        return try {
            json.decodeFromString<SMOAuth>(trackPreferences.trackToken(this).get())
        } catch (e: Exception) {
            null
        }
    }

    override fun logout() {
        super.logout()
        trackPreferences.trackToken(this).delete()
        interceptor.newAuth(null)
    }
}
