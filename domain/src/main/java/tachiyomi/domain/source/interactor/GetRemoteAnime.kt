package tachiyomi.domain.source.interactor

import eu.kanade.tachiyomi.animesource.model.AnimeFilterList
import tachiyomi.domain.source.repository.SourcePagingSourceType
import tachiyomi.domain.source.repository.SourceRepository

class GetRemoteAnime(
    private val repository: SourceRepository,
) {

    fun subscribe(sourceId: Long, query: String, animeFilterList: AnimeFilterList): SourcePagingSourceType {
        return when (query) {
            QUERY_POPULAR -> repository.getPopular(sourceId)
            QUERY_LATEST -> repository.getLatest(sourceId)
            else -> repository.search(sourceId, query, animeFilterList)
        }
    }

    companion object {
        const val QUERY_POPULAR = "eu.kanade.domain.source.anime.interactor.POPULAR"
        const val QUERY_LATEST = "eu.kanade.domain.source.anime.interactor.LATEST"
    }
}
