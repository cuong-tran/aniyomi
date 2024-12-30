package eu.kanade.domain.extension.interactor

import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.animesource.AnimeSource
import eu.kanade.tachiyomi.extension.model.Extension
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GetAnimeExtensionSources(
    private val preferences: SourcePreferences,
) {

    fun subscribe(extension: Extension.Installed): Flow<List<AnimeExtensionSourceItem>> {
        val isMultiSource = extension.animeSources.size > 1
        val isMultiLangSingleSource =
            isMultiSource && extension.animeSources.map { it.name }.distinct().size == 1

        return preferences.disabledAnimeSources().changes().map { disabledSources ->
            fun AnimeSource.isEnabled() = id.toString() !in disabledSources

            extension.animeSources
                .map { source ->
                    AnimeExtensionSourceItem(
                        animeSource = source,
                        enabled = source.isEnabled(),
                        labelAsName = isMultiSource && !isMultiLangSingleSource,
                    )
                }
        }
    }
}

data class AnimeExtensionSourceItem(
    val animeSource: AnimeSource,
    val enabled: Boolean,
    val labelAsName: Boolean,
)
