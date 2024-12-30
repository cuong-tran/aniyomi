package tachiyomi.data.source

import kotlinx.coroutines.flow.Flow
import tachiyomi.data.DatabaseHandler
import tachiyomi.domain.source.model.StubAnimeSource
import tachiyomi.domain.source.repository.StubSourceRepository

class StubSourceRepositoryImpl(
    private val handler: DatabaseHandler,
) : StubSourceRepository {

    override fun subscribeAll(): Flow<List<StubAnimeSource>> {
        return handler.subscribeToList { sourcesQueries.findAll(::mapStubSource) }
    }

    override suspend fun getStubSource(id: Long): StubAnimeSource? {
        return handler.awaitOneOrNull { sourcesQueries.findOne(id, ::mapStubSource) }
    }

    override suspend fun upsertStubSource(id: Long, lang: String, name: String) {
        handler.await { sourcesQueries.upsert(id, lang, name) }
    }

    private fun mapStubSource(
        id: Long,
        lang: String,
        name: String,
    ): StubAnimeSource = StubAnimeSource(id = id, lang = lang, name = name)
}
