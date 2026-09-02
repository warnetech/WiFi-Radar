package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.export.LogExporter
import com.example.data.local.NokiaDeviceEntity
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
fun DeviceDetailSheet(
    device: NokiaDeviceEntity,
    onDismiss: () -> Unit
) {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
    val firstSeenStr = dateFormat.format(Date(device.firstSeenEpoch))
    val lastSeenStr = dateFormat.format(Date(device.lastSeenEpoch))
    val durationStr = LogExporter.formatDuration(device.firstSeenEpoch, device.lastSeenEpoch)

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = RadarDarkSurface),
            border = androidx.compose.foundation.BorderStroke(
                1.5.dp,
                if (device.isNokiaTarget) RadarThreatRed else RadarDarkBorder
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("device_detail_card")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    if (device.isNokiaTarget) RadarThreatRed.copy(alpha = 0.2f)
                                    else RadarCyan.copy(alpha = 0.2f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (device.isNokiaTarget) Icons.Default.Warning else Icons.Default.Wifi,
                                contentDescription = null,
                                tint = if (device.isNokiaTarget) RadarThreatRed else RadarCyan,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = device.ssid,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                ),
                                color = TextPrimary
                            )
                            Text(
                                text = device.bssid,
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                color = TextSecondary
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_detail_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Security Status Badge
                if (device.isNokiaTarget) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(RadarThreatRed.copy(alpha = 0.15f))
                            .border(1.dp, RadarThreatRed.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = RadarThreatRed,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "SECURITY RISK: IDENTIFIED NOKIA TARGET",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = RadarThreatRed
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Stats Grid
                DetailStatRow(
                    label = "Encounters",
                    value = "${device.encounterCount} Sighting${if (device.encounterCount > 1) "s" else ""}",
                    highlight = true
                )
                Spacer(modifier = Modifier.height(8.dp))

                DetailStatRow(
                    label = "First Seen",
                    value = firstSeenStr
                )
                Spacer(modifier = Modifier.height(8.dp))

                DetailStatRow(
                    label = "Last Seen",
                    value = lastSeenStr
                )
                Spacer(modifier = Modifier.height(8.dp))

                DetailStatRow(
                    label = "Active Time Range",
                    value = durationStr,
                    highlight = true
                )
                Spacer(modifier = Modifier.height(8.dp))

                DetailStatRow(
                    label = "Signal Strength",
                    value = "${device.lastRssi} dBm",
                    valueColor = if (device.lastRssi > -60) RadarGreen else RadarCyan
                )
                Spacer(modifier = Modifier.height(8.dp))

                DetailStatRow(
                    label = "Frequency / Band",
                    value = "${device.frequency} MHz (${if (device.frequency > 4000) "5 GHz" else "2.4 GHz"})"
                )
                Spacer(modifier = Modifier.height(8.dp))

                DetailStatRow(
                    label = "Capabilities",
                    value = device.capabilities.ifBlank { "Standard WPA2" }
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dismiss_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RadarDarkSurfaceVariant,
                        contentColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Close Device Details")
                }
            }
        }
    }
}

@Composable
fun DetailStatRow(
    label: String,
    value: String,
    highlight: Boolean = false,
    valueColor: Color = TextPrimary
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(RadarDarkSurfaceVariant.copy(alpha = 0.5f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = if (highlight) FontWeight.Bold else FontWeight.Normal,
                fontFamily = if (highlight) FontFamily.Default else FontFamily.Monospace
            ),
            color = valueColor
        )
    }
}
