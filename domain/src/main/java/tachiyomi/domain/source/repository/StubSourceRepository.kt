
package tachiyomi.domain.source.repository

import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.source.model.StubAnimeSource

interface StubSourceRepository {
    fun subscribeAll(): Flow<List<StubAnimeSource>>

    suspend fun getStubSource(id: Long): StubAnimeSource?

    suspend fun upsertStubSource(id: Long, lang: String, name: String)
}
