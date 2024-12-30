package tachiyomi.data.source

import eu.kanade.tachiyomi.animesource.AnimeCatalogueSource
import eu.kanade.tachiyomi.animesource.AnimeSource
import eu.kanade.tachiyomi.animesource.model.AnimeFilterList
import eu.kanade.tachiyomi.animesource.online.AnimeHttpSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import tachiyomi.data.DatabaseHandler
import tachiyomi.domain.source.model.SourceWithCount
import tachiyomi.domain.source.model.StubAnimeSource
import tachiyomi.domain.source.repository.SourcePagingSourceType
import tachiyomi.domain.source.repository.SourceRepository
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.domain.source.model.Source as DomainSource

class SourceRepositoryImpl(
    private val sourceManager: SourceManager,
    private val handler: DatabaseHandler,
) : SourceRepository {

    override fun getSources(): Flow<List<DomainSource>> {
        return sourceManager.catalogueSources.map { sources ->
            sources.map {
                mapSourceToDomainSource(it).copy(
                    supportsLatest = it.supportsLatest,
                )
            }
        }
    }

    override fun getOnlineSources(): Flow<List<DomainSource>> {
        return sourceManager.catalogueSources.map { sources ->
            sources
                .filterIsInstance<AnimeHttpSource>()
                .map(::mapSourceToDomainSource)
        }
    }

    override fun getSourcesWithFavoriteCount(): Flow<List<Pair<DomainSource, Long>>> {
        return combine(
            handler.subscribeToList { animesQueries.getSourceIdWithFavoriteCount() },
            sourceManager.catalogueSources,
        ) { sourceIdWithFavoriteCount, _ -> sourceIdWithFavoriteCount }
            .map {
                it.map { (sourceId, count) ->
                    val source = sourceManager.getOrStub(sourceId)
                    val domainSource = mapSourceToDomainSource(source).copy(
                        isStub = source is StubAnimeSource,
                    )
                    domainSource to count
                }
            }
    }

    override fun getSourcesWithNonLibraryAnime(): Flow<List<SourceWithCount>> {
        val sourceIdWithNonLibraryAnime =
            handler.subscribeToList { animesQueries.getSourceIdsWithNonLibraryAnime() }
        return sourceIdWithNonLibraryAnime.map { sourceId ->
            sourceId.map { (sourceId, count) ->
                val source = sourceManager.getOrStub(sourceId)
                val domainSource = mapSourceToDomainSource(source).copy(
                    isStub = source is StubAnimeSource,
                )
                SourceWithCount(domainSource, count)
            }
        }
    }

    override fun search(
        sourceId: Long,
        query: String,
        animeFilterList: AnimeFilterList,
    ): SourcePagingSourceType {
        val source = sourceManager.get(sourceId) as AnimeCatalogueSource
        return SourceSearchPagingSource(source, query, animeFilterList)
    }

    override fun getPopular(sourceId: Long): SourcePagingSourceType {
        val source = sourceManager.get(sourceId) as AnimeCatalogueSource
        return SourcePopularPagingSource(source)
    }

    override fun getLatest(sourceId: Long): SourcePagingSourceType {
        val source = sourceManager.get(sourceId) as AnimeCatalogueSource
        return SourceLatestPagingSource(source)
    }

    private fun mapSourceToDomainSource(animeSource: AnimeSource): DomainSource = DomainSource(
        id = animeSource.id,
        lang = animeSource.lang,
        name = animeSource.name,
        supportsLatest = false,
        isStub = false,
    )
}
