package com.obsidian.music.playback

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.obsidian.music.MainActivity

/**
 * Owns the player so music keeps going with the screen off or the app closed.
 * The MediaSession gives us the system notification, lockscreen controls,
 * headset buttons and Bluetooth metadata with no extra code.
 *
 * It also hosts the equalizer and soundstage effects, which the app drives
 * through two small custom session commands.
 */
class PlaybackService : MediaSessionService() {

    private var session: MediaSession? = null
    private val effects = AudioEffects()

    override fun onCreate() {
        super.onCreate()

        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                /* handleAudioFocus = */ true,
            )
            .setHandleAudioBecomingNoisy(true) // pause when headphones unplug
            .setWakeMode(C.WAKE_MODE_LOCAL)    // CPU wake lock only while playing local files
            .build()
            .apply {
                repeatMode = Player.REPEAT_MODE_ALL // finish a song, move on, loop at the end
                playWhenReady = false
            }

        effects.attach(player.audioSessionId)
        player.addListener(object : Player.Listener {
            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                effects.attach(audioSessionId)
            }
        })

        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        session = MediaSession.Builder(this, player)
            .setSessionActivity(openApp)
            .setCallback(EffectsCallback())
            .build()
    }

    private inner class EffectsCallback : MediaSession.Callback {
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
        ): MediaSession.ConnectionResult {
            val commands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
                .add(SessionCommand(CMD_EQ, Bundle.EMPTY))
                .add(SessionCommand(CMD_SPATIAL, Bundle.EMPTY))
                .build()
            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(commands)
                .build()
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle,
        ): ListenableFuture<SessionResult> {
            when (customCommand.customAction) {
                CMD_EQ -> args.getFloatArray(KEY_GAINS)?.let(effects::setGains)
                CMD_SPATIAL -> effects.setSpatial(args.getBoolean(KEY_ENABLED))
            }
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onTaskRemoved(rootIntent: Intent?) {
        // Swiping the app away stops playback only if nothing is playing.
        val player = session?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        effects.release()
        session?.run {
            player.release()
            release()
        }
        session = null
        super.onDestroy()
    }

    companion object {
        const val CMD_EQ = "obsidian.eq"
        const val CMD_SPATIAL = "obsidian.spatial"
        const val KEY_GAINS = "gains"
        const val KEY_ENABLED = "enabled"
    }
}
