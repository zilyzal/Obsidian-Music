@file:OptIn(ExperimentalSharedTransitionApi::class)

package com.obsidian.music.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.style.TextOverflow
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.obsidian.music.data.LibraryView
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.obsidian.music.PlayerViewModel

private val audioPermission =
    if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO
    else Manifest.permission.READ_EXTERNAL_STORAGE

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
fun ObsidianApp(vm: PlayerViewModel) {
    val context = LocalContext.current
    val state by vm.state.collectAsStateWithLifecycle()
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    var hasAudio by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, audioPermission) == PackageManager.PERMISSION_GRANTED)
    }
    // True once Android will no longer show the permission dialog: the only way forward is Settings.
    var blocked by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    var position by remember { mutableLongStateOf(0L) }
    val keyboard = LocalSoftwareKeyboardController.current

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        // Re-check the real state: the result map only holds what was requested.
        hasAudio = ContextCompat.checkSelfPermission(context, audioPermission) == PackageManager.PERMISSION_GRANTED
        val activity = context.findActivity()
        blocked = !hasAudio && activity != null &&
            !ActivityCompat.shouldShowRequestPermissionRationale(activity, audioPermission)
        if (hasAudio) vm.loadLibrary()
    }

    // Pick up newly added songs whenever the app comes back to the foreground
    // (this also catches the person returning from Settings after allowing access).
    LifecycleEventEffect(Lifecycle.Event.ON_START) {
        hasAudio = ContextCompat.checkSelfPermission(context, audioPermission) == PackageManager.PERMISSION_GRANTED
        if (hasAudio) vm.loadLibrary()
    }

    // Position ticks only while a song is playing and the screen is visible.
    LaunchedEffect(state.isPlaying, state.current?.id) {
        position = vm.currentPositionMs()
        if (state.isPlaying) {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.trackPosition { position = it }
            }
        }
    }

    var searchOpen by remember { mutableStateOf(false) }

    BackHandler(enabled = expanded) { expanded = false }
    BackHandler(enabled = !expanded && menuOpen) { menuOpen = false }
    BackHandler(enabled = !expanded && !menuOpen && searchOpen) {
        searchOpen = false
        vm.setQuery("")
    }

    val accent = rememberAccent(state.current)
    val current = state.current
    val onHome = hasAudio && state.library.isNotEmpty()
    val homeLayer = rememberLayerBackdrop()

    SharedTransitionLayout(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize()) {
            // Everything the floating glass refracts: background glows and the song list.
            Box(Modifier.fillMaxSize().layerBackdrop(homeLayer)) {
                AmbientBackground(accent, backdrop = if (hasAudio) AmbientScene.Home else AmbientScene.Welcome)

                when {
                    !hasAudio -> WelcomeScreen(accent = accent, blocked = blocked) {
                        if (blocked) {
                            context.startActivity(
                                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                            )
                        } else {
                            // Notifications are what lets the media notification show on Android 13+.
                            val wanted = buildList {
                                add(audioPermission)
                                if (Build.VERSION.SDK_INT >= 33 &&
                                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                                ) add(Manifest.permission.POST_NOTIFICATIONS)
                            }
                            permissionLauncher.launch(wanted.toTypedArray())
                        }
                    }

                    state.loading && state.library.isEmpty() ->
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = accent, strokeWidth = 3.dp, modifier = Modifier.size(32.dp))
                        }

                    // Judge the whole library, so an empty Favorites list still shows the home screen.
                    state.library.isEmpty() -> Box(
                        Modifier.fillMaxSize().padding(horizontal = 32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        StateCard(
                            icon = Icons.Rounded.MusicNote,
                            title = "No music yet",
                            body = "Add some songs to your phone and they'll show up here, newest first.",
                            accent = accent,
                            actionLabel = "Scan again",
                            onAction = vm::loadLibrary,
                        )
                    }

                    else -> HomeList(
                        library = state.library,
                        favorites = state.favorites,
                        view = state.view,
                        query = state.query,
                        current = current,
                        isPlaying = state.isPlaying,
                        accent = accent,
                        bottomPadding = if (current != null) 96.dp else 0.dp,
                        onPlay = { t ->
                            if (searchOpen) keyboard?.hide()
                            vm.play(t)
                        },
                        onPlayNext = vm::playNext,
                        onBrowseLatest = { vm.setView(LibraryView.LATEST) },
                    )
                }

                if (onHome) {
                    // Rows dissolve into the background before they reach the floating glass,
                    // so no list text competes with the controls' own text.
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 64.dp)
                            .background(
                                Brush.verticalGradient(
                                    0f to Obsidian.Base.copy(alpha = 0.92f),
                                    0.6f to Obsidian.Base.copy(alpha = 0.55f),
                                    1f to Color.Transparent,
                                )
                            )
                    )
                    AnimatedVisibility(
                        visible = current != null,
                        modifier = Modifier.align(Alignment.BottomCenter),
                        enter = fadeIn(tween(300)),
                        exit = fadeOut(tween(200)),
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(170.dp)
                                .background(
                                    Brush.verticalGradient(
                                        0f to Color.Transparent,
                                        0.55f to Obsidian.Base.copy(alpha = 0.85f),
                                        1f to Obsidian.Base,
                                    )
                                )
                        )
                    }
                }
            }

            CompositionLocalProvider(LocalGlassBackdrop provides homeLayer) {
                if (onHome) {
                    TopBar(
                        view = state.view,
                        query = state.query,
                        searchOpen = searchOpen,
                        canShuffle = state.tracks.isNotEmpty(),
                        shuffle = state.shuffle,
                        accent = accent,
                        menuOpen = menuOpen,
                        onOpenMenu = { menuOpen = !menuOpen },
                        onSearchOpenChange = { open ->
                            searchOpen = open
                            if (!open) vm.setQuery("")
                        },
                        onQueryChange = vm::setQuery,
                        onShuffle = vm::shufflePlay,
                    )
                }

                // Dims everything while the view menu is open; tap outside to close.
                AnimatedVisibility(visible = menuOpen && onHome, enter = fadeIn(tween(180)), exit = fadeOut(tween(160))) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(Obsidian.Scrim.copy(alpha = 0.45f))
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { menuOpen = false }
                    )
                }
                AnimatedVisibility(
                    visible = menuOpen && onHome,
                    modifier = Modifier.padding(
                        top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 12.dp + TopBarHeight + 10.dp,
                        start = 18.dp,
                    ),
                    enter = fadeIn(tween(140)) + scaleIn(
                        spring(dampingRatio = 0.68f, stiffness = 420f),
                        initialScale = 0.6f,
                        transformOrigin = TransformOrigin(0.15f, 0f),
                    ),
                    exit = fadeOut(tween(130)) + scaleOut(tween(160), targetScale = 0.8f, transformOrigin = TransformOrigin(0.15f, 0f)),
                ) {
                    ViewMenu(
                        selected = state.view,
                        accent = accent,
                        onSelect = { v ->
                            menuOpen = false
                            vm.setView(v)
                        },
                    )
                }

                // Mini player <-> full player. The cover flies between the two.
                val stage = when {
                    current == null || !hasAudio -> Stage.None
                    expanded -> Stage.Full
                    else -> Stage.Mini
                }
                AnimatedContent(
                    targetState = stage,
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.BottomCenter,
                    transitionSpec = {
                        when {
                            targetState == Stage.Full ->
                                (fadeIn(tween(220)) + slideInVertically(spring(dampingRatio = 0.86f, stiffness = 360f)) { it / 5 }) togetherWith
                                    fadeOut(tween(160))
                            initialState == Stage.Full ->
                                fadeIn(tween(220, delayMillis = 80)) togetherWith
                                    (fadeOut(tween(200)) + slideOutVertically(tween(260)) { it / 5 })
                            else ->
                                (slideInVertically(spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)) { it } + fadeIn(tween(200))) togetherWith
                                    (slideOutVertically(tween(220)) { it } + fadeOut(tween(160)))
                        }
                    },
                    label = "playerStage",
                ) { s ->
                    val t = current
                    if (t == null || s == Stage.None) {
                        Box(Modifier)
                    } else {
                        val art = Modifier.sharedElement(
                            sharedContentState = rememberSharedContentState(key = "cover"),
                            animatedVisibilityScope = this@AnimatedContent,
                            boundsTransform = { _, _ -> spring(dampingRatio = 0.82f, stiffness = 300f) },
                        )
                        if (s == Stage.Mini) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                                MiniPlayer(
                                    track = t,
                                    isPlaying = state.isPlaying,
                                    accent = accent,
                                    positionProvider = { position },
                                    durationMs = state.durationMs,
                                    onOpen = {
                                        if (searchOpen) keyboard?.hide()
                                        expanded = true
                                    },
                                    onToggle = vm::togglePlayPause,
                                    onNext = vm::next,
                                    onPrevious = vm::previous,
                                    modifier = Modifier
                                        .windowInsetsPadding(WindowInsets.navigationBars)
                                        .padding(bottom = 10.dp),
                                    artModifier = art,
                                )
                            }
                        } else {
                            NowPlaying(
                                track = t,
                                isPlaying = state.isPlaying,
                                shuffle = state.shuffle,
                                repeatOne = state.repeatOne,
                                favorite = t.id in state.favorites,
                                accent = accent,
                                playingFrom = state.playingFrom ?: "Your music",
                                durationMs = state.durationMs,
                                positionProvider = { position },
                                upNext = state.upNext,
                                sleepAtMs = state.sleepAtMs,
                                sleepAfterSong = state.sleepAfterSong,
                                onSeek = { ms -> vm.seekTo(ms); position = ms },
                                onToggle = vm::togglePlayPause,
                                onNext = vm::next,
                                onPrevious = vm::previous,
                                onShuffle = vm::toggleShuffle,
                                onRepeat = vm::toggleRepeatOne,
                                onFavorite = { vm.toggleFavorite(t) },
                                onJumpTo = vm::jumpTo,
                                onSleep = vm::setSleepTimer,
                                onSleepAfterSong = vm::setSleepAfterSong,
                                onClose = { expanded = false },
                                artModifier = art,
                            )
                        }
                    }
                }

                // Glass toast ("Playing next: ...") that drops in from the top.
                val toast = state.toast
                AnimatedVisibility(
                    visible = toast != null,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 12.dp + TopBarHeight + 12.dp),
                    enter = slideInVertically(spring(dampingRatio = 0.6f, stiffness = 380f)) { -it * 2 } + fadeIn(tween(160)),
                    exit = slideOutVertically(tween(220)) { -it * 2 } + fadeOut(tween(180)),
                ) {
                    var last by remember { mutableStateOf("") }
                    if (toast != null) last = toast.text
                    Text(
                        last,
                        style = ObsidianType.MiniSub,
                        color = Obsidian.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .padding(horizontal = 40.dp)
                            .liquidGlass(CircleShape, GlassStyle.Panel, tint = accent, tintAlpha = 0.12f)
                            .padding(horizontal = 18.dp, vertical = 11.dp),
                    )
                }
            }
        }
    }
}

private enum class Stage { None, Mini, Full }
