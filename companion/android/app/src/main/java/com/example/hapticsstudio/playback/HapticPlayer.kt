/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.example.hapticsstudio.playback

import android.content.Context
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import com.example.hapticsstudio.model.AndroidWaveform
import java.io.File
import java.io.FileInputStream

private const val TAG = "HapticPlayer"

/**
 * Plays a pre-rendered [AndroidWaveform] with [VibrationEffect.createWaveform], synced with the
 * clip's `.wav` audio via [MediaPlayer].
 */
class HapticPlayer(context: Context) {

  private val appContext = context.applicationContext
  private val vibrator: Vibrator =
      (appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager)
          .defaultVibrator
  private val handler = Handler(Looper.getMainLooper())

  private var mediaPlayer: MediaPlayer? = null

  /**
   * Plays [waveform] together with [audioFile] when present. [onFinished] is invoked when the audio
   * finishes (or, when there is no audio, after the waveform's total duration elapses).
   */
  fun play(waveform: AndroidWaveform, audioFile: File?, onFinished: () -> Unit) {
    stop()

    if (waveform.timings.isEmpty()) {
      onFinished()
      return
    }

    val effect = VibrationEffect.createWaveform(waveform.timings, waveform.amplitudes, -1)

    if (audioFile != null && audioFile.exists()) {
      try {
        val player = getMediaPlayer()
        FileInputStream(audioFile).use { stream ->
          player.setDataSource(stream.fd)
        }
        player.setOnCompletionListener { onFinished() }
        player.setOnErrorListener { _, _, _ ->
          onFinished()
          true
        }
        player.prepare()
        player.start()
        handler.postDelayed({ vibrator.vibrate(effect) }, AUDIO_HAPTIC_LATENCY)
        return
      } catch (e: Exception) {
        Log.e(TAG, "Failed to play audio for clip: ${e.message}")
        resetMediaPlayer()
      }
    }

    // No audio (or audio failed): vibrate immediately and clear state after the waveform ends.
    vibrator.vibrate(effect)
    handler.postDelayed(onFinished, waveform.timings.sum())
  }

  fun stop() {
    handler.removeCallbacksAndMessages(null)
    vibrator.cancel()
    mediaPlayer?.reset()
  }

  fun release() {
    stop()
    resetMediaPlayer()
  }

  private fun getMediaPlayer(): MediaPlayer {
    val existing = mediaPlayer
    if (existing == null) {
      return MediaPlayer().also { mediaPlayer = it }
    }
    existing.reset()
    return existing
  }

  private fun resetMediaPlayer() {
    mediaPlayer?.release()
    mediaPlayer = null
  }

  companion object {
    // Audio-to-haptic latency offset, in milliseconds.
    const val AUDIO_HAPTIC_LATENCY = 170L
  }
}
