package com.obsidian.music.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.obsidian.music.ui.components.LiveEqualizerBars
import com.obsidian.music.ui.components.ProceduralObsidianArtwork
import com.obsidian.music.data.LibraryView
import com.obsidian.music.ui.components.formatDurationMs
import com.obsidian.music.ui.model.ObsidianAlbum
import com.obsidian.music.ui.model.ObsidianTrack
import com.obsidian.music.ui.theme.PillShape
import com.obsidian.music.ui.theme.liquidGelPress
import com.obsidian.music.ui.theme.obsidianInnerGlassCard
import com.obsidian.music.ui.theme.staggeredLiquidEntrance

// Bottom clearance so scrolled tracks glide cleanly underneath the detached floating mini-player + dock
private val BottomFloatingClearance = 196.dp

@Composable
fun LiquidHomeScreen(
    tracks: List<ObsidianTrack>,
    recentlyPlayed: List<ObsidianTrack>,
    currentTrackId: String?,
    isPlaying: Boolean,
    reduceTransparency: Boolean,
    onPlayTrack: (ObsidianTrack) -> Unit,
    onShuffleAll: () -> Unit,
    onToggleReduceTransparency: (Boolean) -> Unit,
    artworkSlot: @Composable (ObsidianTrack, Modifier) -> Unit
) {
    val featured = recentlyPlayed.ifEmpty { tracks.take(6) }

    LazyColumn(
        contentPadding = PaddingValues(top = 12.dp, bottom = BottomFloatingClearance),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        // Editorial Obsidian Header + Liquid Glass Toggle Chip
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "OBSIDIAN",
                        color = Color.White.copy(alpha = 0.54f),
                        fontSize = 11.sp,
                        letterSpacing = 2.6.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Listen Now",
                        color = Color.White,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Live Glass Mode Switch Chip
                Row(
                    modifier = Modifier
                        .liquidGelPress()
                        .clip(PillShape)
                        .background(Color.White.copy(alpha = 0.12f))
                        .border(0.9.dp, Color.White.copy(alpha = 0.28f), PillShape)
                        .clickable { onToggleReduceTransparency(!reduceTransparency) }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = "Liquid Glass mode",
                        tint = if (reduceTransparency) Color.White.copy(alpha = 0.5f) else Color(0xFF8BE9FD),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (reduceTransparency) "Frosted" else "Liquid",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Featured Horizontal Glass Carousel
        if (featured.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Recently Played",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        itemsIndexed(featured, key = { _, t -> "recent_${t.id}" }) { index, track ->
                            FeaturedGlassTile(
                                track = track,
                                isCurrent = track.id == currentTrackId,
                                isPlaying = isPlaying,
                                index = index,
                                onClick = { onPlayTrack(track) },
                                artworkSlot = artworkSlot
                            )
                        }
                    }
                }
            }
        }

        // Quick Shuffle Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "All Tracks",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier
                        .liquidGelPress()
                        .clip(PillShape)
                        .background(Color.White.copy(alpha = 0.14f))
                        .border(0.8.dp, Color.White.copy(alpha = 0.26f), PillShape)
                        .clickable(onClick = onShuffleAll)
                        .padding(horizontal = 14.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Rounded.Shuffle, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                    Text("Shuffle", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // Staggered Track Rows
        itemsIndexed(tracks, key = { _, t -> "home_${t.id}" }) { index, track ->
            GlassTrackRow(
                track = track,
                isCurrent = track.id == currentTrackId,
                isPlaying = isPlaying,
                index = index,
                onClick = { onPlayTrack(track) },
                artworkSlot = artworkSlot,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}

@Composable
fun LiquidTracksLibraryScreen(
    tracks: List<ObsidianTrack>,
    view: LibraryView,
    favoriteCount: Int,
    currentTrackId: String?,
    isPlaying: Boolean,
    onViewChange: (LibraryView) -> Unit,
    onPlayTrack: (ObsidianTrack) -> Unit,
    artworkSlot: @Composable (ObsidianTrack, Modifier) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(top = 14.dp, bottom = BottomFloatingClearance),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        item {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                Text(
                    text = if (view == LibraryView.FAVORITES) "$favoriteCount FAVORITES" else "${tracks.size} LOCAL TRACKS",
                    color = Color.White.copy(alpha = 0.54f),
                    fontSize = 11.sp,
                    letterSpacing = 2.0.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Library",
                    color = Color.White,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(LibraryView.entries.toList()) { option ->
                    val selected = option == view
                    Box(
                        modifier = Modifier
                            .liquidGelPress()
                            .clip(PillShape)
                            .background(if (selected) Color.White.copy(alpha = 0.26f) else Color.White.copy(alpha = 0.09f))
                            .border(
                                0.9.dp,
                                if (selected) Color.White.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.16f),
                                PillShape
                            )
                            .clickable { onViewChange(option) }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = option.label,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                        )
                    }
                }
            }
        }

        if (tracks.isEmpty()) {
            item {
                Text(
                    text = if (view == LibraryView.FAVORITES) "Star songs in the player to see them here." else "Nothing here yet.",
                    color = Color.White.copy(alpha = 0.55f),
                    fontSize = 14.sp,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 18.dp)
                )
            }
        }

        itemsIndexed(tracks, key = { _, t -> "lib_${t.id}" }) { index, track ->
            GlassTrackRow(
                track = track,
                isCurrent = track.id == currentTrackId,
                isPlaying = isPlaying,
                index = index,
                onClick = { onPlayTrack(track) },
                artworkSlot = artworkSlot,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}

@Composable
fun LiquidAlbumsScreen(
    albums: List<ObsidianAlbum>,
    onPlayAlbum: (ObsidianAlbum) -> Unit,
    albumArtworkSlot: @Composable (ObsidianAlbum, Modifier) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)) {
            Text(
                text = "${albums.size} ALBUMS",
                color = Color.White.copy(alpha = 0.54f),
                fontSize = 11.sp,
                letterSpacing = 2.0.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Albums",
                color = Color.White,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold
            )
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = BottomFloatingClearance),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            itemsIndexed(albums, key = { _, a -> a.id }) { index, album ->
                Column(
                    modifier = Modifier
                        .staggeredLiquidEntrance(index)
                        .liquidGelPress(pressedScale = 0.96f)
                        .obsidianInnerGlassCard(RoundedCornerShape(22.dp))
                        .clickable { onPlayAlbum(album) }
                        .padding(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .border(0.8.dp, Color.White.copy(alpha = 0.24f), RoundedCornerShape(16.dp))
                    ) {
                        albumArtworkSlot(album, Modifier.fillMaxSize())
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(8.dp)
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.52f))
                                .border(0.8.dp, Color.White.copy(alpha = 0.42f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = "Play album",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = album.title,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${album.artist} · ${album.trackCount} tracks",
                        color = Color.White.copy(alpha = 0.62f),
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun LiquidSearchScreen(
    query: String,
    tracks: List<ObsidianTrack>,
    currentTrackId: String?,
    isPlaying: Boolean,
    onQueryChange: (String) -> Unit,
    onPlayTrack: (ObsidianTrack) -> Unit,
    artworkSlot: @Composable (ObsidianTrack, Modifier) -> Unit
) {
    val filtered = remember(query, tracks) {
        if (query.isBlank()) tracks
        else tracks.filter {
            it.title.contains(query, ignoreCase = true) ||
                it.artist.contains(query, ignoreCase = true) ||
                it.album.contains(query, ignoreCase = true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        Text(
            text = "Search",
            color = Color.White,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
        )

        // Liquid Search Capsule
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .obsidianInnerGlassCard(PillShape)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.Search, contentDescription = null, tint = Color.White.copy(alpha = 0.72f))
            Spacer(Modifier.width(10.dp))
            Box(modifier = Modifier.weight(1f)) {
                if (query.isEmpty()) {
                    Text(
                        text = "Songs, artists, or albums...",
                        color = Color.White.copy(alpha = 0.45f),
                        fontSize = 15.sp
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = TextStyle(color = Color.White, fontSize = 15.sp),
                    cursorBrush = SolidColor(Color.White),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (query.isNotEmpty()) {
                IconButton(
                    onClick = { onQueryChange("") },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = "Clear", tint = Color.White.copy(alpha = 0.72f))
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        LazyColumn(
            contentPadding = PaddingValues(bottom = BottomFloatingClearance),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            itemsIndexed(filtered, key = { _, t -> "search_${t.id}" }) { index, track ->
                GlassTrackRow(
                    track = track,
                    isCurrent = track.id == currentTrackId,
                    isPlaying = isPlaying,
                    index = index,
                    onClick = { onPlayTrack(track) },
                    artworkSlot = artworkSlot,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    }
}

@Composable
private fun FeaturedGlassTile(
    track: ObsidianTrack,
    isCurrent: Boolean,
    isPlaying: Boolean,
    index: Int,
    onClick: () -> Unit,
    artworkSlot: @Composable (ObsidianTrack, Modifier) -> Unit
) {
    Column(
        modifier = Modifier
            .width(154.dp)
            .staggeredLiquidEntrance(index)
            .liquidGelPress(pressedScale = 0.95f)
            .obsidianInnerGlassCard(
                shape = RoundedCornerShape(22.dp),
                selected = isCurrent,
                accentColor = track.accentColor
            )
            .clickable(onClick = onClick)
            .padding(10.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(16.dp))
                .border(0.8.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
        ) {
            artworkSlot(track, Modifier.fillMaxSize())
            if (isCurrent) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.62f)),
                    contentAlignment = Alignment.Center
                ) {
                    LiveEqualizerBars(isPlaying = isPlaying, tint = track.accentColor, modifier = Modifier.size(13.dp))
                }
            }
        }
        Spacer(Modifier.height(9.dp))
        Text(
            text = track.title,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = track.artist,
            color = Color.White.copy(alpha = 0.64f),
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun GlassTrackRow(
    track: ObsidianTrack,
    isCurrent: Boolean,
    isPlaying: Boolean,
    index: Int,
    onClick: () -> Unit,
    artworkSlot: @Composable (ObsidianTrack, Modifier) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .staggeredLiquidEntrance(index)
            .liquidGelPress(pressedScale = 0.98f)
            .obsidianInnerGlassCard(
                shape = RoundedCornerShape(20.dp),
                selected = isCurrent,
                accentColor = track.accentColor
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(RoundedCornerShape(14.dp))
                .border(0.8.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(14.dp))
        ) {
            artworkSlot(track, Modifier.fillMaxSize())
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                color = if (isCurrent) Color.White else Color.White.copy(alpha = 0.94f),
                fontSize = 15.sp,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "${track.artist} · ${track.album}",
                color = Color.White.copy(alpha = 0.62f),
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(Modifier.width(8.dp))

        if (track.isFavorite) {
            Icon(
                imageVector = Icons.Rounded.Favorite,
                contentDescription = null,
                tint = Color(0xFFFF4D6D),
                modifier = Modifier
                    .padding(end = 8.dp)
                    .size(15.dp)
            )
        }

        if (isCurrent) {
            LiveEqualizerBars(
                isPlaying = isPlaying,
                tint = track.accentColor,
                modifier = Modifier.size(16.dp)
            )
        } else {
            Text(
                text = formatDurationMs(track.durationMs),
                color = Color.White.copy(alpha = 0.50f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/** Default album artwork slot if Coil/Glide isn't wired yet */
@Composable
fun DefaultAlbumArtwork(album: ObsidianAlbum, modifier: Modifier = Modifier) {
    ProceduralObsidianArtwork(
        primary = Color(album.accentHex),
        secondary = Color(album.secondaryHex),
        modifier = modifier
    )
}
