/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.example.hapticsstudio.net

import android.util.Log
import com.example.hapticsstudio.model.SocketEndpoint
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import org.json.JSONObject

private const val TAG = "SocketClient"

/**
 * Thin wrapper over the official `io.socket` [Socket] client. Connects to the desktop's Socket.IO
 * server (`http://<ip>:<port>`, path `/ws`) over the websocket transport, forwarding the device
 * identity as connection query parameters.
 */
class SocketClient {

  private var socket: Socket? = null

  fun connect(endpoint: SocketEndpoint, query: String, onReady: () -> Unit = {}) {
    disconnect()

    val url = "http://${endpoint.ip}:${endpoint.port}"
    val options =
        IO.Options().apply {
          path = "/ws"
          transports = arrayOf("websocket")
          reconnection = true
          reconnectionDelay = 1000
          this.query = query
        }

    val client =
        try {
          IO.socket(url, options)
        } catch (e: Exception) {
          Log.e(TAG, "Failed to create socket for $url", e)
          return
        }

    socket = client
    // Register application handlers on the live socket BEFORE connecting so no early events
    // (e.g. auth_required / auth_granted) are missed.
    onReady()
    client.connect()
  }

  fun on(event: String, listener: Emitter.Listener) {
    socket?.on(event, listener)
  }

  fun emit(event: String, payload: JSONObject) {
    socket?.emit(event, payload)
  }

  fun emit(event: String) {
    socket?.emit(event)
  }

  fun disconnect() {
    socket?.let {
      it.off()
      it.disconnect()
      it.close()
    }
    socket = null
  }
}
