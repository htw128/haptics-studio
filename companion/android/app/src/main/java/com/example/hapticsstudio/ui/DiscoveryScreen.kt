/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.example.hapticsstudio.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.hapticsstudio.discovery.DiscoveryViewModel
import com.example.hapticsstudio.model.SocketEndpoint

private const val DEFAULT_PORT = 9999

private val IPV4_REGEX =
    Regex("^((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)\\.){3}(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)$")

private fun isValidIpv4(text: String): Boolean = IPV4_REGEX.matches(text)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoveryScreen(
    onConnect: (SocketEndpoint) -> Unit,
    viewModel: DiscoveryViewModel = viewModel(),
) {
  val endpoints by viewModel.endpoints.collectAsState()
  var showManual by remember { mutableStateOf(false) }

  Scaffold(
      containerColor = MaterialTheme.colorScheme.background,
      topBar = {
        TopAppBar(
            title = { Text("Haptics Studio") },
            colors =
                TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                ),
        )
      },
  ) { padding ->
    Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
      if (showManual) {
        ManualIpEntry(
            onCancel = { showManual = false },
            onConnect = { ip ->
              onConnect(SocketEndpoint(hostname = ip, ip = ip, port = DEFAULT_PORT))
            },
        )
      } else {
        Text(
            text = "Hosts",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.height(8.dp))
        val hosts = endpoints.values.sortedBy { it.hostname }
        if (hosts.isEmpty()) {
          Text(
              text = "No hosts available",
              color = MaterialTheme.colorScheme.onBackground,
              modifier = Modifier.padding(vertical = 12.dp),
          )
        } else {
          LazyColumn(
              modifier = Modifier.weight(1f),
              verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            items(hosts, key = { it.hostname }) { endpoint ->
              HostRow(endpoint = endpoint, onClick = { onConnect(endpoint) })
            }
          }
        }
        TextButton(onClick = { showManual = true }) {
          Text("Can't find your device?")
        }
      }
    }
  }
}

@Composable
private fun HostRow(endpoint: SocketEndpoint, onClick: () -> Unit) {
  Card(
      modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
  ) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(
          imageVector = Icons.Filled.Computer,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onSurface,
      )
      Spacer(modifier = Modifier.width(12.dp))
      Text(text = endpoint.hostname, color = MaterialTheme.colorScheme.onSurface)
    }
  }
}

@Composable
private fun ManualIpEntry(onCancel: () -> Unit, onConnect: (String) -> Unit) {
  var ip by remember { mutableStateOf("192.168.0.1") }
  val isValid = remember(ip) { isValidIpv4(ip) }

  Column(modifier = Modifier.fillMaxWidth()) {
    Text(
        text = "Enter the IP address",
        color = MaterialTheme.colorScheme.onBackground,
    )
    Spacer(modifier = Modifier.height(12.dp))
    OutlinedTextField(
        value = ip,
        onValueChange = { ip = it },
        singleLine = true,
        label = { Text("IP Address") },
        keyboardOptions =
            androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(modifier = Modifier.height(16.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
      OutlinedButton(onClick = onCancel) { Text("Cancel") }
      Button(onClick = { onConnect(ip) }, enabled = isValid) { Text("Connect") }
    }
  }
}
