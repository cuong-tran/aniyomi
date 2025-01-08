package eu.kanade.tachiyomi.data.track.shikimori.dto

import eu.kanade.tachiyomi.data.track.model.TrackSearch
import eu.kanade.tachiyomi.data.track.shikimori.ShikimoriApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SMEntry(
    val id: Long,
    val name: String,
    val chapters: Long?,
    val episodes: Long?,
    val image: SUEntryCover,
    val score: Double,
    val url: String,
    val status: String,
    val kind: String,
    @SerialName("aired_on")
    val airedOn: String?,
) {
    fun toAnimeTrack(trackId: Long): TrackSearch {
        return TrackSearch.create(trackId).apply {
            remote_id = this@SMEntry.id
            title = name
            total_episodes = episodes!!
            cover_url = ShikimoriApi.BASE_URL + image.preview
            summary = ""
            score = this@SMEntry.score
            tracking_url = ShikimoriApi.BASE_URL + url
            publishing_status = this@SMEntry.status
            publishing_type = kind
            start_date = airedOn ?: ""
        }
    }
}

@Serializable
data class SUEntryCover(
    val preview: String,
)
