package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.export.LogExporter
import com.example.data.local.NokiaDeviceEntity
import com.example.scanner.RadarScanState
import com.example.ui.components.RadarScopeView
import com.example.ui.theme.RadarCyan
import com.example.ui.theme.RadarDarkBorder
import com.example.ui.theme.RadarDarkSurface
import com.example.ui.theme.RadarDarkSurfaceVariant
import com.example.ui.theme.RadarGreen
import com.example.ui.theme.RadarThreatRed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RadarScreen(
    devices: List<NokiaDeviceEntity>,
    nokiaThreats: List<NokiaDeviceEntity>,
    scanState: RadarScanState,
    onTriggerScan: () -> Unit,
    onToggleScanLoop: (Boolean) -> Unit,
    onInjectSample: (String) -> Unit,
    onSelectDevice: (NokiaDeviceEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.US)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("radar_screen_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Threat Alert Banner
        item {
            if (nokiaThreats.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(RadarThreatRed.copy(alpha = 0.18f))
                        .border(1.5.dp, RadarThreatRed, RoundedCornerShape(14.dp))
                        .padding(14.dp)
                        .testTag("threat_alert_banner")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(RadarThreatRed),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = TextPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "SECURITY RISK TARGETS DETECTED",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = RadarThreatRed
                                )
                                Text(
                                    text = "${nokiaThreats.size} NOKIA Device(s) Tracked • ${scanState.totalEncountersCount} Encounters",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextPrimary
                                )
                            }
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(RadarDarkSurface)
                        .border(1.dp, RadarDarkBorder, RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (scanState.isScanning) RadarGreen else TextMuted)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = if (scanState.isScanning) "RADAR SWEEP ACTIVE • TARGET: NOKIA-*" else "RADAR PAUSED",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = if (scanState.isScanning) RadarCyan else TextSecondary
                            )
                        }
                        Text(
                            text = scanState.scanMessage,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        // Animated Radar Scope
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = RadarDarkSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, RadarDarkBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp)
            ) {
                RadarScopeView(
                    devices = devices,
                    isScanning = scanState.isScanning,
                    onDeviceClick = onSelectDevice,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Radar Controls & Quick Test Injector
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onTriggerScan,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("scan_now_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RadarCyan,
                            contentColor = RadarDarkSurface
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("SCAN NOW", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { onToggleScanLoop(!scanState.isScanning) },
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("toggle_radar_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (scanState.isScanning) RadarDarkSurfaceVariant else RadarDarkSurfaceVariant,
                            contentColor = if (scanState.isScanning) RadarThreatRed else RadarGreen
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = if (scanState.isScanning) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (scanState.isScanning) "PAUSE" else "RESUME", fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Quick Target Test Sighting Injection Chips
                Text(
                    text = "TEST SIMULATOR (Inject Target Sightings):",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        AssistChip(
                            onClick = { onInjectSample("NOKIA-L6D8") },
                            label = { Text("+ NOKIA-L6D8") },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = RadarThreatRed.copy(alpha = 0.15f),
                                labelColor = RadarThreatRed
                            ),
                            border = AssistChipDefaults.assistChipBorder(borderColor = RadarThreatRed.copy(alpha = 0.5f), enabled = true),
                            modifier = Modifier.testTag("inject_nokia_l6d8")
                        )
                    }
                    item {
                        AssistChip(
                            onClick = { onInjectSample("NOKIA-P9E7") },
                            label = { Text("+ NOKIA-P9E7") },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = RadarThreatRed.copy(alpha = 0.15f),
                                labelColor = RadarThreatRed
                            ),
                            border = AssistChipDefaults.assistChipBorder(borderColor = RadarThreatRed.copy(alpha = 0.5f), enabled = true),
                            modifier = Modifier.testTag("inject_nokia_p9e7")
                        )
                    }
                    item {
                        AssistChip(
                            onClick = { onInjectSample("NOKIA-K2M9") },
                            label = { Text("+ NOKIA-K2M9") },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = RadarThreatRed.copy(alpha = 0.15f),
                                labelColor = RadarThreatRed
                            ),
                            border = AssistChipDefaults.assistChipBorder(borderColor = RadarThreatRed.copy(alpha = 0.5f), enabled = true)
                        )
                    }
                }
            }
        }

        // Identified Targets List
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TRACKED DEVICES (${devices.size})",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
                Text(
                    text = "Tap for Security Breakdown",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
        }

        if (devices.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = RadarDarkSurface),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, RadarDarkBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Wifi,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "No Wi-Fi signals in radar buffer",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        Text(
                            text = "Tap 'SCAN NOW' or test with '+ NOKIA-L6D8' above",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }
                }
            }
        } else {
            items(devices, key = { it.bssid }) { dev ->
                DeviceCard(
                    device = dev,
                    onClick = { onSelectDevice(dev) }
                )
            }
        }
    }
}

@Composable
fun DeviceCard(
    device: NokiaDeviceEntity,
    onClick: () -> Unit
) {
    val durationStr = LogExporter.formatDuration(device.firstSeenEpoch, device.lastSeenEpoch)
    val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.US)
    val lastSeenTime = dateFormat.format(Date(device.lastSeenEpoch))

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (device.isNokiaTarget) RadarDarkSurfaceVariant else RadarDarkSurface
        ),
        border = androidx.compose.foundation.BorderStroke(
            if (device.isNokiaTarget) 1.5.dp else 1.dp,
            if (device.isNokiaTarget) RadarThreatRed else RadarDarkBorder
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("device_card_${device.bssid}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(
                                if (device.isNokiaTarget) RadarThreatRed.copy(alpha = 0.2f)
                                else RadarCyan.copy(alpha = 0.15f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (device.isNokiaTarget) Icons.Default.Warning else Icons.Default.Wifi,
                            contentDescription = null,
                            tint = if (device.isNokiaTarget) RadarThreatRed else RadarCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = device.ssid,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            ),
                            color = if (device.isNokiaTarget) RadarThreatRed else TextPrimary
                        )
                        Text(
                            text = device.bssid,
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            color = TextSecondary
                        )
                    }
                }

                // Encounters Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (device.isNokiaTarget) RadarThreatRed.copy(alpha = 0.2f)
                            else RadarDarkSurfaceVariant
                        )
                        .border(
                            1.dp,
                            if (device.isNokiaTarget) RadarThreatRed.copy(alpha = 0.6f) else RadarDarkBorder,
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${device.encounterCount} Sighting${if (device.encounterCount > 1) "s" else ""}",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (device.isNokiaTarget) RadarThreatRed else RadarCyan
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Time range and RSSI
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Active Range: $durationStr (Last: $lastSeenTime)",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Text(
                    text = "${device.lastRssi} dBm",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = if (device.lastRssi > -60) RadarGreen else RadarCyan
                )
            }
        }
    }
}
