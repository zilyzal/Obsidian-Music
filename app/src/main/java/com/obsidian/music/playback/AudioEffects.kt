package com.obsidian.music.playback

import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.roundToInt

/**
 * Equalizer and soundstage widener attached to the player's audio session.
 * Kept tolerant on purpose: devices differ, and an unsupported effect must never stop playback.
 */
class AudioEffects {

    private var equalizer: Equalizer? = null
    private var virtualizer: Virtualizer? = null
    private var session = 0
    private var gains: FloatArray? = null
    private var spatial = false

    fun attach(sessionId: Int) {
        if (sessionId <= 0 || sessionId == session) return
        release()
        session = sessionId
        if (gains != null) applyEq()
        if (spatial) applySpatial()
    }

    fun setGains(values: FloatArray) {
        gains = values
        applyEq()
    }

    fun setSpatial(on: Boolean) {
        spatial = on
        applySpatial()
    }

    fun release() {
        runCatching { equalizer?.release() }
        runCatching { virtualizer?.release() }
        equalizer = null
        virtualizer = null
        session = 0
    }

    private fun applyEq() {
        val g = gains ?: return
        if (session <= 0) return
        runCatching {
            val eq = equalizer ?: Equalizer(0, session).also { equalizer = it }
            eq.setEnabled(true)
            val range = eq.bandLevelRange
            val low = range[0].toInt()
            val high = range[1].toInt()
            for (band in 0 until eq.numberOfBands.toInt()) {
                val centerHz = eq.getCenterFreq(band.toShort()) / 1000
                // Match each device band to the nearest of the five bands on screen.
                val nearest = UI_BANDS_HZ.indices.minByOrNull {
                    abs(ln(centerHz.coerceAtLeast(1).toFloat()) - ln(UI_BANDS_HZ[it].toFloat()))
                } ?: 0
                val level = (g.getOrElse(nearest) { 0f } * MAX_MILLIBELS).roundToInt().coerceIn(low, high)
                eq.setBandLevel(band.toShort(), level.toShort())
            }
        }
    }

    private fun applySpatial() {
        if (session <= 0) return
        runCatching {
            if (!spatial) {
                virtualizer?.setEnabled(false)
                return
            }
            val v = virtualizer ?: Virtualizer(0, session).also { virtualizer = it }
            if (v.strengthSupported) v.setStrength(700.toShort())
            v.setEnabled(true)
        }
    }

    private companion object {
        val UI_BANDS_HZ = intArrayOf(60, 230, 910, 3600, 14000)
        const val MAX_MILLIBELS = 1200 // +/-12 dB, as drawn on the faders
    }
}
