package eu.kanade.tachiyomi.util.episode

import eu.kanade.tachiyomi.data.download.DownloadCache
import tachiyomi.domain.anime.model.Anime
import tachiyomi.domain.episode.model.Episode
import tachiyomi.source.local.isLocal
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/**
 * Returns a copy of the list with not downloaded chapters removed.
 */
fun List<Episode>.filterDownloadedEpisodes(anime: Anime): List<Episode> {
    if (anime.isLocal()) return this

    val downloadCache: DownloadCache = Injekt.get()

    return filter {
        downloadCache.isEpisodeDownloaded(
            it.name,
            it.scanlator,
            anime.title,
            anime.source,
            false,
        )
    }
}
