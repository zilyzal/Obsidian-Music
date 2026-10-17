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
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextAlign
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
    var expanded by remember { mutableStateOf(false) }
    var position by remember { mutableLongStateOf(0L) }

    var asked by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        // Re-check the real state: the result map only holds what was requested, so a
        // notifications-only request would otherwise look like "audio denied".
        asked = true
        hasAudio = ContextCompat.checkSelfPermission(context, audioPermission) == PackageManager.PERMISSION_GRANTED
        if (hasAudio) vm.loadLibrary()
    }

    // Ask once on first launch; the notification permission is what lets the media notification show on Android 13+.
    LaunchedEffect(Unit) {
        val wanted = buildList {
            if (!hasAudio) add(audioPermission)
            if (Build.VERSION.SDK_INT >= 33 &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (wanted.isNotEmpty()) permissionLauncher.launch(wanted.toTypedArray())
    }

    // Pick up newly added songs whenever the app comes back to the foreground.
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

    BackHandler(enabled = expanded) { expanded = false }

    val accent = rememberAccent(state.current)
    val current = state.current

    Box(Modifier.fillMaxSize()) {
        AmbientBackground(accent)

        when {
            !hasAudio -> MessageCard(
                title = "Let's find your music",
                body = "Obsidian plays the songs already on your phone. Allow access to your audio files to get started.",
                action = "Allow access",
                accent = accent,
            ) {
                // After the system stops showing the dialog, the only way forward is app settings.
                val activity = context.findActivity()
                val blocked = asked && activity != null &&
                    !ActivityCompat.shouldShowRequestPermissionRationale(activity, audioPermission)
                if (blocked) {
                    context.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                    )
                } else {
                    permissionLauncher.launch(arrayOf(audioPermission))
                }
            }

            state.loading && state.library.isEmpty() ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = accent, strokeWidth = 3.dp, modifier = Modifier.size(32.dp))
                }

            // Judge the whole library, so an empty Favorites list still shows the home screen.
            state.library.isEmpty() -> MessageCard(
                title = "No music yet",
                body = "Add some songs to your phone and they'll show up here, newest first.",
                action = "Scan again",
                accent = accent,
            ) { vm.loadLibrary() }

            else -> HomeScreen(
                tracks = state.tracks,
                view = state.view,
                current = current,
                isPlaying = state.isPlaying,
                shuffle = state.shuffle,
                accent = accent,
                bottomPadding = if (current != null) 96.dp else 0.dp,
                onViewChange = vm::setView,
                onPlay = vm::play,
                onShufflePlay = vm::shufflePlay,
            )
        }

        // Soft fade under the mini player so rows dissolve into the background instead of
        // running right up to its edge.
        AnimatedVisibility(
            visible = current != null && !expanded && hasAudio && state.library.isNotEmpty(),
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = fadeIn(tween(300)),
            exit = fadeOut(tween(200)),
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Transparent,
                            0.65f to Obsidian.Base.copy(alpha = 0.85f),
                            1f to Obsidian.Base,
                        )
                    )
            )
        }

        AnimatedVisibility(
            visible = current != null && !expanded,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(bottom = 10.dp),
            enter = slideInVertically(spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)) { it } + fadeIn(tween(200)),
            exit = slideOutVertically(tween(220)) { it } + fadeOut(tween(160)),
        ) {
            current?.let { t ->
                MiniPlayer(
                    track = t,
                    isPlaying = state.isPlaying,
                    accent = accent,
                    positionProvider = { position },
                    durationMs = state.durationMs,
                    onOpen = { expanded = true },
                    onToggle = vm::togglePlayPause,
                    onNext = vm::next,
                )
            }
        }

        AnimatedVisibility(
            visible = expanded && current != null,
            enter = slideInVertically(spring(dampingRatio = 0.88f, stiffness = 420f)) { it } + fadeIn(tween(180)),
            exit = slideOutVertically(tween(280)) { it } + fadeOut(tween(220)),
        ) {
            current?.let { t ->
                NowPlaying(
                    track = t,
                    isPlaying = state.isPlaying,
                    shuffle = state.shuffle,
                    favorite = t.id in state.favorites,
                    accent = accent,
                    durationMs = state.durationMs,
                    positionProvider = { position },
                    onSeek = { ms -> vm.seekTo(ms); position = ms },
                    onToggle = vm::togglePlayPause,
                    onNext = vm::next,
                    onPrevious = vm::previous,
                    onShuffle = vm::toggleShuffle,
                    onFavorite = { vm.toggleFavorite(t) },
                    onClose = { expanded = false },
                )
            }
        }
    }
}

@Composable
private fun MessageCard(
    title: String,
    body: String,
    action: String,
    accent: androidx.compose.ui.graphics.Color,
    onAction: () -> Unit,
) {
    Box(Modifier.fillMaxSize().padding(28.dp), contentAlignment = Alignment.Center) {
        Column(
            Modifier
                .glass(RoundedCornerShape(32.dp), fillAlpha = 0.09f)
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(title, style = MaterialTheme.typography.headlineSmall, color = Obsidian.TextPrimary, textAlign = TextAlign.Center)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = Obsidian.TextSecondary, textAlign = TextAlign.Center)
            Box(
                Modifier
                    .padding(top = 12.dp)
                    .bouncyClick(0.94f, onAction)
                    .glass(CircleShape, fillAlpha = 0.14f, borderAlpha = 0.5f)
                    .background(accent.copy(alpha = 0.35f))
                    .padding(horizontal = 28.dp, vertical = 14.dp),
            ) {
                Text(action, style = MaterialTheme.typography.titleMedium, color = Obsidian.TextPrimary)
            }
        }
    }
}
