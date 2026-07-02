/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.example.hapticsstudio.discovery

import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log
import com.example.hapticsstudio.model.SocketEndpoint
import java.net.DatagramPacket
import java.net.DatagramSocket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject

private const val TAG = "UdpDiscovery"

/**
 * Listens for the UDP broadcasts the Haptics Studio desktop sends on [DISCOVERY_PORT] to advertise
 * itself on the LAN. Each datagram carries a JSON payload `{hostname, port}`; we pair it with the
 * sender's IP address to build a [SocketEndpoint].
 */
class UdpDiscovery(context: Context) {

  private val appContext = context.applicationContext
  private val scope = CoroutineScope(Dispatchers.IO)

  private var socket: DatagramSocket? = null
  private var multicastLock: WifiManager.MulticastLock? = null
  private var listenJob: Job? = null

  private val _endpoints = MutableStateFlow<Map<String, SocketEndpoint>>(emptyMap())
  val endpoints: StateFlow<Map<String, SocketEndpoint>> = _endpoints.asStateFlow()

  fun start() {
    if (listenJob != null) {
      return
    }

    val wifiManager =
        appContext.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    multicastLock =
        wifiManager.createMulticastLock("HapticsStudioDiscovery").apply {
          setReferenceCounted(true)
          acquire()
        }

    listenJob = scope.launch {
      try {
        val datagramSocket =
            DatagramSocket(null).apply {
              reuseAddress = true
              broadcast = true
              bind(java.net.InetSocketAddress(DISCOVERY_PORT))
            }
        socket = datagramSocket

        val buffer = ByteArray(2048)
        while (isActive) {
          val packet = DatagramPacket(buffer, buffer.size)
          datagramSocket.receive(packet)
          handlePacket(packet)
        }
      } catch (e: Exception) {
        if (isActive) {
          Log.w(TAG, "UDP discovery stopped: ${e.message}")
        }
      }
    }
  }

  private fun handlePacket(packet: DatagramPacket) {
    try {
      val message = String(packet.data, packet.offset, packet.length)
      val json = JSONObject(message)
      val hostname = json.optString("hostname")
      val port = json.optInt("port")
      if (hostname.isEmpty() || port == 0) {
        return
      }
      val ip = packet.address?.hostAddress ?: return
      val endpoint = SocketEndpoint(hostname = hostname, ip = ip, port = port)
      _endpoints.value = _endpoints.value + (hostname to endpoint)
    } catch (e: Exception) {
      Log.w(TAG, "Failed to parse discovery packet: ${e.message}")
    }
  }

  fun stop() {
    listenJob?.cancel()
    listenJob = null
    socket?.close()
    socket = null
    multicastLock?.let {
      if (it.isHeld) {
        it.release()
      }
    }
    multicastLock = null
  }

  companion object {
    const val DISCOVERY_PORT = 9998
  }
}
