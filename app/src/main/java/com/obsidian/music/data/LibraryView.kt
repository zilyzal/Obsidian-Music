package com.obsidian.music.data

/** The ways the home list can be arranged or filtered. */
enum class LibraryView(val label: String) {
    LATEST("Latest"),
    FAVORITES("Favorites"),
    TITLE("A to Z"),
    ARTIST("Artist"),
    OLDEST("Oldest"),
    LONGEST("Longest"),
}

/** The library arrives newest-first from MediaStore, so LATEST needs no work. */
fun List<Track>.arranged(view: LibraryView, favorites: Set<Long>): List<Track> = when (view) {
    LibraryView.LATEST -> this
    LibraryView.FAVORITES -> filter { it.id in favorites }
    LibraryView.TITLE -> sortedBy { it.title.lowercase() }
    LibraryView.ARTIST -> sortedWith(compareBy<Track>({ it.artist.lowercase() }, { it.title.lowercase() }))
    LibraryView.OLDEST -> reversed()
    LibraryView.LONGEST -> sortedByDescending { it.durationMs }
}
