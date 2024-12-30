package eu.kanade.tachiyomi.data.track.myanimelist

import eu.kanade.tachiyomi.data.database.models.AnimeTrack

fun AnimeTrack.toMyAnimeListStatus() = when (status) {
    MyList.WATCHING -> "watching"
    MyList.READING -> "watching"
    MyList.COMPLETED -> "completed"
    MyList.ON_HOLD -> "on_hold"
    MyList.DROPPED -> "dropped"
    MyList.PLAN_TO_READ -> "plan_to_watch"
    MyList.REREADING -> "watching"
    MyList.PLAN_TO_WATCH -> "plan_to_watch"
    MyList.REWATCHING -> "watching"
    else -> null
}

fun getStatus(status: String?) = when (status) {
    "reading" -> MyList.READING
    "watching" -> MyList.WATCHING
    "completed" -> MyList.COMPLETED
    "on_hold" -> MyList.ON_HOLD
    "dropped" -> MyList.DROPPED
    "plan_to_read" -> MyList.PLAN_TO_READ
    "plan_to_watch" -> MyList.PLAN_TO_WATCH
    else -> MyList.READING
}
