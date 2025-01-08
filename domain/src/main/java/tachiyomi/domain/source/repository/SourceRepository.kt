package tachiyomi.domain.source.repository

import androidx.paging.PagingSource
import eu.kanade.tachiyomi.animesource.model.AnimeFilterList
import eu.kanade.tachiyomi.animesource.model.SAnime
import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.source.model.Source
import tachiyomi.domain.source.model.SourceWithCount

typealias SourcePagingSourceType = PagingSource<Long, SAnime>

interface SourceRepository {

    fun getSources(): Flow<List<Source>>

    fun getOnlineSources(): Flow<List<Source>>

    fun getSourcesWithFavoriteCount(): Flow<List<Pair<Source, Long>>>

    fun getSourcesWithNonLibraryAnime(): Flow<List<SourceWithCount>>

    fun search(sourceId: Long, query: String, animeFilterList: AnimeFilterList): SourcePagingSourceType

    fun getPopular(sourceId: Long): SourcePagingSourceType

    fun getLatest(sourceId: Long): SourcePagingSourceType
}
