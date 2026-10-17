package com.obsidian.music.data

/** The ways the home list can be arranged or filtered. */
enum class LibraryView(val label: String) {
    LATEST("Latest"),
    FAVORITES("Favorites"),
    OLDEST("Oldest"),
    LONGEST("Longest"),
}

/** The library arrives newest-first from MediaStore, so LATEST needs no work. */
fun List<Track>.arranged(view: LibraryView, favorites: Set<Long>): List<Track> = when (view) {
    LibraryView.LATEST -> this
    LibraryView.FAVORITES -> filter { it.id in favorites }
    LibraryView.OLDEST -> reversed()
    LibraryView.LONGEST -> sortedByDescending { it.durationMs }
}
