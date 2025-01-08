package tachiyomi.domain.library.service

import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.core.common.preference.TriState
import tachiyomi.core.common.preference.getEnum
import tachiyomi.domain.anime.model.Anime
import tachiyomi.domain.library.model.LibraryDisplayMode
import tachiyomi.domain.library.model.LibrarySort

class LibraryPreferences(
    private val preferenceStore: PreferenceStore,
) {

    fun displayMode() = preferenceStore.getObject(
        "pref_display_mode_library",
        LibraryDisplayMode.default,
        LibraryDisplayMode.Serializer::serialize,
        LibraryDisplayMode.Serializer::deserialize,
    )

    fun sortingMode() = preferenceStore.getObject(
        "animelib_sorting_mode",
        LibrarySort.default,
        LibrarySort.Serializer::serialize,
        LibrarySort.Serializer::deserialize,
    )

    fun lastUpdatedTimestamp() = preferenceStore.getLong(Preference.appStateKey("library_update_last_timestamp"), 0L)
    fun autoUpdateInterval() = preferenceStore.getInt("pref_library_update_interval_key", 0)

    fun autoUpdateDeviceRestrictions() = preferenceStore.getStringSet(
        "library_update_restriction",
        setOf(
            DEVICE_ONLY_ON_WIFI,
        ),
    )

    fun autoUpdateItemRestrictions() = preferenceStore.getStringSet(
        "library_update_manga_restriction",
        setOf(
            ENTRY_HAS_UNVIEWED,
            ENTRY_NON_COMPLETED,
            ENTRY_NON_VIEWED,
            ENTRY_OUTSIDE_RELEASE_PERIOD,
        ),
    )

    fun autoUpdateMetadata() = preferenceStore.getBoolean("auto_update_metadata", false)

    fun showContinueViewingButton() =
        preferenceStore.getBoolean("display_continue_reading_button", false)

    // Common Category

    fun categoryTabs() = preferenceStore.getBoolean("display_category_tabs", true)

    fun categoryNumberOfItems() = preferenceStore.getBoolean("display_number_of_items", false)

    fun categorizedDisplaySettings() = preferenceStore.getBoolean("categorized_display", false)

    fun hideHiddenCategoriesSettings() = preferenceStore.getBoolean("hidden_categories", false)

    fun filterIntervalCustom() = preferenceStore.getEnum(
        "pref_filter_library_interval_custom",
        TriState.DISABLED,
    )

    // Common badges

    fun downloadBadge() = preferenceStore.getBoolean("display_download_badge", false)

    fun localBadge() = preferenceStore.getBoolean("display_local_badge", true)

    fun languageBadge() = preferenceStore.getBoolean("display_language_badge", false)

    fun newShowUpdatesCount() = preferenceStore.getBoolean("library_show_updates_count", true)

    // Common Cache

    fun autoClearItemCache() = preferenceStore.getBoolean("auto_clear_chapter_cache", false)

    // Random Sort Seed

    fun randomSortSeed() = preferenceStore.getInt("library_random_anime_sort_seed", 0)

    // Mixture Columns

    fun animePortraitColumns() = preferenceStore.getInt("pref_animelib_columns_portrait_key", 0)

    fun animeLandscapeColumns() = preferenceStore.getInt("pref_animelib_columns_landscape_key", 0)

    // Mixture Filter

    fun filterDownloadedAnime() =
        preferenceStore.getEnum("pref_filter_animelib_downloaded_v2", TriState.DISABLED)

    fun filterUnseen() =
        preferenceStore.getEnum("pref_filter_animelib_unread_v2", TriState.DISABLED)

    fun filterStartedAnime() =
        preferenceStore.getEnum("pref_filter_animelib_started_v2", TriState.DISABLED)

    fun filterBookmarkedAnime() =
        preferenceStore.getEnum("pref_filter_animelib_bookmarked_v2", TriState.DISABLED)

    fun filterCompletedAnime() =
        preferenceStore.getEnum("pref_filter_animelib_completed_v2", TriState.DISABLED)

    fun filterTrackedAnime(id: Int) =
        preferenceStore.getEnum("pref_filter_animelib_tracked_${id}_v2", TriState.DISABLED)

    // Mixture Update Count

    fun newAnimeUpdatesCount() = preferenceStore.getInt("library_unseen_updates_count", 0)

    // Mixture Category

    fun defaultCategory() = preferenceStore.getInt("default_anime_category", -1)

    fun lastUsedAnimeCategory() = preferenceStore.getInt(Preference.appStateKey("last_used_anime_category"), 0)

    fun updateCategories() =
        preferenceStore.getStringSet("animelib_update_categories", emptySet())

    fun updateCategoriesExclude() =
        preferenceStore.getStringSet("animelib_update_categories_exclude", emptySet())

    // Mixture Item

    fun filterEpisodeBySeen() =
        preferenceStore.getLong("default_episode_filter_by_seen", Anime.SHOW_ALL)

    fun filterEpisodeByDownloaded() =
        preferenceStore.getLong("default_episode_filter_by_downloaded", Anime.SHOW_ALL)

    fun filterEpisodeByBookmarked() =
        preferenceStore.getLong("default_episode_filter_by_bookmarked", Anime.SHOW_ALL)

    // and upload date
    fun sortEpisodeBySourceOrNumber() = preferenceStore.getLong(
        "default_episode_sort_by_source_or_number",
        Anime.EPISODE_SORTING_SOURCE,
    )

    fun displayEpisodeByNameOrNumber() = preferenceStore.getLong(
        "default_chapter_display_by_name_or_number",
        Anime.EPISODE_DISPLAY_NAME,
    )

    fun sortEpisodeByAscendingOrDescending() = preferenceStore.getLong(
        "default_chapter_sort_by_ascending_or_descending",
        Anime.EPISODE_SORT_DESC,
    )

    fun setEpisodeSettingsDefault(anime: Anime) {
        filterEpisodeBySeen().set(anime.unseenFilterRaw)
        filterEpisodeByDownloaded().set(anime.downloadedFilterRaw)
        filterEpisodeByBookmarked().set(anime.bookmarkedFilterRaw)
        sortEpisodeBySourceOrNumber().set(anime.sorting)
        displayEpisodeByNameOrNumber().set(anime.displayMode)
        sortEpisodeByAscendingOrDescending().set(
            if (anime.sortDescending()) Anime.EPISODE_SORT_DESC else Anime.EPISODE_SORT_ASC,
        )
    }

    // region Swipe Actions

    fun swipeEpisodeStartAction() =
        preferenceStore.getEnum("pref_episode_swipe_end_action", EpisodeSwipeAction.ToggleSeen)

    fun swipeEpisodeEndAction() = preferenceStore.getEnum(
        "pref_episode_swipe_start_action",
        EpisodeSwipeAction.ToggleBookmark,
    )

    fun swipeChapterStartAction() =
        preferenceStore.getEnum("pref_chapter_swipe_end_action", ChapterSwipeAction.ToggleRead)

    fun swipeChapterEndAction() = preferenceStore.getEnum(
        "pref_chapter_swipe_start_action",
        ChapterSwipeAction.ToggleBookmark,
    )

    // endregion

    enum class EpisodeSwipeAction {
        ToggleSeen,
        ToggleBookmark,
        Download,
        Disabled,
    }

    enum class ChapterSwipeAction {
        ToggleRead,
        ToggleBookmark,
        Download,
        Disabled,
    }

    companion object {
        const val DEVICE_ONLY_ON_WIFI = "wifi"
        const val DEVICE_NETWORK_NOT_METERED = "network_not_metered"
        const val DEVICE_CHARGING = "ac"

        const val ENTRY_NON_COMPLETED = "manga_ongoing"
        const val ENTRY_HAS_UNVIEWED = "manga_fully_read"
        const val ENTRY_NON_VIEWED = "manga_started"
        const val ENTRY_OUTSIDE_RELEASE_PERIOD = "manga_outside_release_period"
    }
}
