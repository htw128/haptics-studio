/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.example.hapticsstudio.playback

import android.content.Context
import java.io.File

/** Caches clip audio downloaded from the desktop app under `cacheDir/audio/<clipId>.wav`. */
class AudioCache(context: Context) {

  private val audioDir = File(context.cacheDir, "audio")

  fun audioFile(clipId: String): File = File(audioDir, "$clipId.wav")

  fun exists(clipId: String): Boolean = audioFile(clipId).exists()

  fun store(clipId: String, bytes: ByteArray) {
    if (!audioDir.exists()) {
      audioDir.mkdirs()
    }
    audioFile(clipId).writeBytes(bytes)
  }
}
