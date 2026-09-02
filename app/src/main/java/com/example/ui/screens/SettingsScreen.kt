package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.documentfile.provider.DocumentFile
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

@Composable
fun SettingsScreen(
    settings: RadarSettingsState,
    onFolderSelected: (Uri, String) -> Unit,
    onUpdateInterval: (Int) -> Unit,
    onToggleAutoUpload: (Boolean) -> Unit,
    onUpdatePrefix: (String) -> Unit,
    onToggleVibration: (Boolean) -> Unit,
    onManualSync: () -> Unit,
    onInjectTestNokia: (String) -> Unit,
    onClearAllLogs: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // SAF Folder Picker Launcher
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            val docFile = DocumentFile.fromTreeUri(context, uri)
            val folderName = docFile?.name ?: "Selected Directory"
            onFolderSelected(uri, folderName)
        }
    }

    var tempPrefix by remember(settings.targetPrefix) { mutableStateOf(settings.targetPrefix) }
    var intervalSliderVal by remember(settings.scanIntervalSeconds) {
        mutableFloatStateOf(settings.scanIntervalSeconds.toFloat())
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("settings_screen_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Section: Folder Selection & Auto-Upload
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = RadarDarkSurface),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, RadarCyan.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("folder_settings_card")
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(RadarCyan.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = null,
                                tint = RadarCyan,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "UPLOAD DESTINATION FOLDER",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary
                            )
                            Text(
                                text = "Logs will automatically save into this folder",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Selected Folder Display
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(RadarDarkSurfaceVariant)
                            .border(1.dp, RadarDarkBorder, RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (!settings.selectedFolderName.isNullOrBlank())
                                        settings.selectedFolderName
                                    else
                                        "No folder selected yet",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (settings.selectedFolderUri != null) TextPrimary else RadarThreatRed
                                )
                                Text(
                                    text = if (settings.selectedFolderUri != null)
                                        "Storage Access Granted"
                                    else
                                        "Tap 'Choose Folder' below to select directory",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (settings.selectedFolderUri != null) RadarGreen else TextMuted
                                )
                            }
                            if (settings.selectedFolderUri != null) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = RadarGreen,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { folderPickerLauncher.launch(null) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("choose_folder_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = RadarCyan,
                                contentColor = RadarDarkSurface
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                if (settings.selectedFolderUri != null) "Change Folder" else "Choose Folder",
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (settings.selectedFolderUri != null) {
                            OutlinedButton(
                                onClick = onManualSync,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = RadarCyan),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("test_write_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudUpload,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Sync Now")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Auto Upload Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Auto-Upload Logs",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = TextPrimary
                            )
                            Text(
                                text = "Automatically updates 'nokia_security_radar_logs.csv' and summary briefing on each scan.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        Switch(
                            checked = settings.autoUploadEnabled && settings.selectedFolderUri != null,
                            enabled = settings.selectedFolderUri != null,
                            onCheckedChange = { onToggleAutoUpload(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = RadarDarkSurface,
                                checkedTrackColor = RadarCyan
                            ),
                            modifier = Modifier.testTag("auto_upload_switch")
                        )
                    }

                    if (settings.lastSyncTimestamp > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Last Export: ${settings.lastSyncStatus}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }
            }
        }

        // Section: Scan Interval in Seconds
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = RadarDarkSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, RadarDarkBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("interval_settings_card")
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(RadarCyan.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = null,
                                tint = RadarCyan,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "SCAN INTERVAL",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary
                            )
                            Text(
                                text = "Radar repeat interval in seconds",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Current Interval:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        Text(
                            text = "${settings.scanIntervalSeconds} Seconds",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = RadarCyan
                        )
                    }

                    Slider(
                        value = intervalSliderVal,
                        onValueChange = { intervalSliderVal = it },
                        onValueChangeFinished = { onUpdateInterval(intervalSliderVal.toInt()) },
                        valueRange = 3f..60f,
                        steps = 57,
                        colors = SliderDefaults.colors(
                            thumbColor = RadarCyan,
                            activeTrackColor = RadarCyan,
                            inactiveTrackColor = RadarDarkBorder
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("interval_slider")
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Preset chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        listOf(3, 5, 10, 15, 30, 60).forEach { sec ->
                            FilterChip(
                                selected = settings.scanIntervalSeconds == sec,
                                onClick = {
                                    intervalSliderVal = sec.toFloat()
                                    onUpdateInterval(sec)
                                },
                                label = { Text("${sec}s", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = RadarCyan.copy(alpha = 0.25f),
                                    selectedLabelColor = RadarCyan,
                                    containerColor = RadarDarkSurfaceVariant,
                                    labelColor = TextSecondary
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    borderColor = if (settings.scanIntervalSeconds == sec) RadarCyan else RadarDarkBorder,
                                    enabled = true,
                                    selected = settings.scanIntervalSeconds == sec
                                )
                            )
                        }
                    }
                }
            }
        }

        // Section: Target Identification Rules & Alerts
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = RadarDarkSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, RadarDarkBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("target_rules_card")
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(RadarThreatRed.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = RadarThreatRed,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "SECURITY TARGET RULE",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary
                            )
                            Text(
                                text = "Strict matching prefix for security risk devices",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = tempPrefix,
                        onValueChange = {
                            tempPrefix = it
                            onUpdatePrefix(it)
                        },
                        label = { Text("Target Prefix (e.g. NOKIA-)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RadarThreatRed,
                            unfocusedBorderColor = RadarDarkBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedLabelColor = RadarThreatRed
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("target_prefix_field"),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Flags all devices whose name begins exactly with '$tempPrefix' (e.g. '${tempPrefix}L6D8', '${tempPrefix}P9E7').",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Vibration,
                                contentDescription = null,
                                tint = RadarThreatRed,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Vibrate on Threat Detection",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Tactile alert whenever a target is identified",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }
                        Switch(
                            checked = settings.alertVibration,
                            onCheckedChange = { onToggleVibration(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = RadarDarkSurface,
                                checkedTrackColor = RadarThreatRed
                            ),
                            modifier = Modifier.testTag("vibration_switch")
                        )
                    }
                }
            }
        }

        // Section: Test Simulator & Data Management
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = RadarDarkSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, RadarDarkBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("data_management_card")
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "SIMULATOR & LOG MAINTENANCE",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                    Text(
                        text = "Inject sample Nokia devices or reset database",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onInjectTestNokia("NOKIA-L6D8") },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = RadarThreatRed),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Simulate L6D8", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = { onInjectTestNokia("NOKIA-P9E7") },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = RadarThreatRed),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Simulate P9E7", fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = onClearAllLogs,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("clear_database_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RadarDarkSurfaceVariant,
                            contentColor = RadarThreatRed
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Clear All Stored Logs & Sightings")
                    }
                }
            }
        }
    }
}
