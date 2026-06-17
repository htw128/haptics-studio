/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.example.hapticsstudio.discovery

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.hapticsstudio.model.SocketEndpoint
import kotlinx.coroutines.flow.StateFlow

/** Owns the [UdpDiscovery] lifecycle and exposes the discovered desktop hosts to the UI. */
class DiscoveryViewModel(application: Application) : AndroidViewModel(application) {

  private val discovery = UdpDiscovery(application)

  val endpoints: StateFlow<Map<String, SocketEndpoint>> = discovery.endpoints

  init {
    discovery.start()
  }

  override fun onCleared() {
    super.onCleared()
    discovery.stop()
  }
}
