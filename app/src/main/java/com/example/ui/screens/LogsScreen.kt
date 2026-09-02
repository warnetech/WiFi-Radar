package com.example.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.data.preferences.RadarSettingsState
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
fun LogsScreen(
    devices: List<NokiaDeviceEntity>,
    nokiaThreats: List<NokiaDeviceEntity>,
    settings: RadarSettingsState,
    onManualSync: () -> Unit,
    onClearLogs: () -> Unit,
    onSelectDevice: (NokiaDeviceEntity) -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showOnlyNokia by remember { mutableStateOf(true) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }

    val displayedList = if (showOnlyNokia) nokiaThreats else devices
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = { Text("Clear Security Logs?") },
            text = { Text("This will remove all tracked devices and encounter histories from local storage.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearConfirmDialog = false
                        onClearLogs()
                    }
                ) {
                    Text("Clear All", color = RadarThreatRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("Cancel")
                }
            },
            containerColor = RadarDarkSurface,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("logs_screen_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Folder Auto-Upload Status Header
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = RadarDarkSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, RadarDarkBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("folder_sync_header_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(RadarCyan.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (settings.selectedFolderUri != null) Icons.Default.CloudDone else Icons.Default.CloudOff,
                                    contentDescription = null,
                                    tint = if (settings.selectedFolderUri != null) RadarGreen else TextMuted,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "AUTOMATIC LOG UPLOAD",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = TextPrimary
                                )
                                Text(
                                    text = if (!settings.selectedFolderName.isNullOrBlank())
                                        "Target: ${settings.selectedFolderName}"
                                    else
                                        "No folder selected (Uploads disabled)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (settings.selectedFolderUri != null) RadarCyan else RadarThreatRed
                                )
                            }
                        }

                        if (settings.selectedFolderUri == null) {
                            OutlinedButton(
                                onClick = onNavigateToSettings,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = RadarCyan),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Select Folder", fontSize = 12.sp)
                            }
                        } else {
                            Button(
                                onClick = onManualSync,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = RadarCyan,
                                    contentColor = RadarDarkSurface
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("sync_now_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudUpload,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Upload Now", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Status: ${settings.lastSyncStatus}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
            }
        }

        // Filter Chips & Log Actions Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = showOnlyNokia,
                        onClick = { showOnlyNokia = true },
                        label = { Text("NOKIA Threats (${nokiaThreats.size})") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = RadarThreatRed.copy(alpha = 0.25f),
                            selectedLabelColor = RadarThreatRed,
                            containerColor = RadarDarkSurface,
                            labelColor = TextSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = if (showOnlyNokia) RadarThreatRed else RadarDarkBorder,
                            enabled = true,
                            selected = showOnlyNokia
                        ),
                        modifier = Modifier.testTag("filter_nokia_chip")
                    )

                    FilterChip(
                        selected = !showOnlyNokia,
                        onClick = { showOnlyNokia = false },
                        label = { Text("All Devices (${devices.size})") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = RadarCyan.copy(alpha = 0.2f),
                            selectedLabelColor = RadarCyan,
                            containerColor = RadarDarkSurface,
                            labelColor = TextSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = if (!showOnlyNokia) RadarCyan else RadarDarkBorder,
                            enabled = true,
                            selected = !showOnlyNokia
                        ),
                        modifier = Modifier.testTag("filter_all_chip")
                    )
                }

                IconButton(
                    onClick = { showClearConfirmDialog = true },
                    modifier = Modifier.testTag("clear_logs_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = "Clear Logs",
                        tint = TextSecondary
                    )
                }
            }
        }

        // Log Entries
        if (displayedList.isEmpty()) {
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
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = if (showOnlyNokia) RadarThreatRed.copy(alpha = 0.5f) else TextMuted,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (showOnlyNokia) "No NOKIA- threats detected yet" else "No logged Wi-Fi devices",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (showOnlyNokia)
                                "The radar will immediately log and alert on devices starting with 'NOKIA-'."
                            else
                                "Run a scan from the Radar tab to discover and log devices.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(displayedList, key = { it.bssid }) { dev ->
                val firstSeenDateStr = dateFormat.format(Date(dev.firstSeenEpoch))
                val lastSeenDateStr = dateFormat.format(Date(dev.lastSeenEpoch))
                val durationStr = LogExporter.formatDuration(dev.firstSeenEpoch, dev.lastSeenEpoch)

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (dev.isNokiaTarget) RadarDarkSurfaceVariant else RadarDarkSurface
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        if (dev.isNokiaTarget) 1.5.dp else 1.dp,
                        if (dev.isNokiaTarget) RadarThreatRed else RadarDarkBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectDevice(dev) }
                        .testTag("log_card_${dev.bssid}")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (dev.isNokiaTarget) Icons.Default.Warning else Icons.Default.Wifi,
                                    contentDescription = null,
                                    tint = if (dev.isNokiaTarget) RadarThreatRed else RadarCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = dev.ssid,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (dev.isNokiaTarget) RadarThreatRed else TextPrimary
                                )
                            }

                            // Encounters Pill
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (dev.isNokiaTarget) RadarThreatRed.copy(alpha = 0.2f)
                                        else RadarCyan.copy(alpha = 0.15f)
                                    )
                                    .border(
                                        1.dp,
                                        if (dev.isNokiaTarget) RadarThreatRed else RadarCyan.copy(alpha = 0.5f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "${dev.encounterCount} Encounters",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (dev.isNokiaTarget) RadarThreatRed else RadarCyan
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "BSSID: ${dev.bssid}",
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            color = TextSecondary
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Time Range Box
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(RadarDarkSurface.copy(alpha = 0.7f))
                                .border(1.dp, RadarDarkBorder, RoundedCornerShape(10.dp))
                                .padding(10.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "First Seen:",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary
                                    )
                                    Text(
                                        text = firstSeenDateStr,
                                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                        color = TextPrimary
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Last Seen:",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary
                                    )
                                    Text(
                                        text = lastSeenDateStr,
                                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                        color = TextPrimary
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Time Range Duration:",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = if (dev.isNokiaTarget) RadarThreatRed else RadarCyan
                                    )
                                    Text(
                                        text = durationStr,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (dev.isNokiaTarget) RadarThreatRed else RadarCyan
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // RSSI & Signal Details
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Signal: ${dev.lastRssi} dBm",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (dev.lastRssi > -60) RadarGreen else RadarCyan
                            )
                            Text(
                                text = "${dev.frequency} MHz",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}
