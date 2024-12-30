package eu.kanade.tachiyomi.ui.browse.source.browse

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.components.AdaptiveSheet
import eu.kanade.tachiyomi.animesource.model.AnimeFilter
import eu.kanade.tachiyomi.animesource.model.AnimeFilterList
import tachiyomi.core.common.preference.TriState
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.CheckboxItem
import tachiyomi.presentation.core.components.CollapsibleBox
import tachiyomi.presentation.core.components.HeadingItem
import tachiyomi.presentation.core.components.SelectItem
import tachiyomi.presentation.core.components.SortItem
import tachiyomi.presentation.core.components.TextItem
import tachiyomi.presentation.core.components.TriStateItem
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun SourceFilterAnimeDialog(
    onDismissRequest: () -> Unit,
    filters: AnimeFilterList,
    onReset: () -> Unit,
    onFilter: () -> Unit,
    onUpdate: (AnimeFilterList) -> Unit,
) {
    val updateFilters = { onUpdate(filters) }

    AdaptiveSheet(onDismissRequest = onDismissRequest) {
        LazyColumn {
            stickyHeader {
                Row(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.background)
                        .padding(8.dp),
                ) {
                    TextButton(onClick = onReset) {
                        Text(
                            text = stringResource(MR.strings.action_reset),
                            style = LocalTextStyle.current.copy(
                                color = MaterialTheme.colorScheme.primary,
                            ),
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    Button(onClick = {
                        onFilter()
                        onDismissRequest()
                    }) {
                        Text(stringResource(MR.strings.action_filter))
                    }
                }
                HorizontalDivider()
            }

            items(filters) {
                FilterItem(it, updateFilters)
            }
        }
    }
}

@Composable
private fun FilterItem(animeFilter: AnimeFilter<*>, onUpdate: () -> Unit) {
    when (animeFilter) {
        is AnimeFilter.Header -> {
            HeadingItem(animeFilter.name)
        }
        is AnimeFilter.Separator -> {
            HorizontalDivider()
        }
        is AnimeFilter.CheckBox -> {
            CheckboxItem(
                label = animeFilter.name,
                checked = animeFilter.state,
            ) {
                animeFilter.state = !animeFilter.state
                onUpdate()
            }
        }
        is AnimeFilter.TriState -> {
            TriStateItem(
                label = animeFilter.name,
                state = animeFilter.state.toTriStateFilter(),
            ) {
                animeFilter.state = animeFilter.state.toTriStateFilter().next().toTriStateInt()
                onUpdate()
            }
        }
        is AnimeFilter.Text -> {
            TextItem(
                label = animeFilter.name,
                value = animeFilter.state,
            ) {
                animeFilter.state = it
                onUpdate()
            }
        }
        is AnimeFilter.Select<*> -> {
            SelectItem(
                label = animeFilter.name,
                options = animeFilter.values,
                selectedIndex = animeFilter.state,
                onSelect = {
                    animeFilter.state = it
                    onUpdate()
                },
            )
        }
        is AnimeFilter.Sort -> {
            CollapsibleBox(
                heading = animeFilter.name,
            ) {
                Column {
                    animeFilter.values.mapIndexed { index, item ->
                        SortItem(
                            label = item,
                            sortDescending = animeFilter.state?.ascending?.not()
                                ?.takeIf { index == animeFilter.state?.index },
                        ) {
                            val ascending = if (index == animeFilter.state?.index) {
                                !animeFilter.state!!.ascending
                            } else {
                                animeFilter.state!!.ascending
                            }
                            animeFilter.state = AnimeFilter.Sort.Selection(
                                index = index,
                                ascending = ascending,
                            )
                            onUpdate()
                        }
                    }
                }
            }
        }
        is AnimeFilter.Group<*> -> {
            CollapsibleBox(
                heading = animeFilter.name,
            ) {
                Column {
                    animeFilter.state
                        .filterIsInstance<AnimeFilter<*>>()
                        .map { FilterItem(animeFilter = it, onUpdate = onUpdate) }
                }
            }
        }
    }
}

private fun Int.toTriStateFilter(): TriState {
    return when (this) {
        AnimeFilter.TriState.STATE_IGNORE -> TriState.DISABLED
        AnimeFilter.TriState.STATE_INCLUDE -> TriState.ENABLED_IS
        AnimeFilter.TriState.STATE_EXCLUDE -> TriState.ENABLED_NOT
        else -> throw IllegalStateException("Unknown TriState state: $this")
    }
}

private fun TriState.toTriStateInt(): Int {
    return when (this) {
        TriState.DISABLED -> AnimeFilter.TriState.STATE_IGNORE
        TriState.ENABLED_IS -> AnimeFilter.TriState.STATE_INCLUDE
        TriState.ENABLED_NOT -> AnimeFilter.TriState.STATE_EXCLUDE
    }
}
