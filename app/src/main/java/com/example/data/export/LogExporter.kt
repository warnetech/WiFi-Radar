package com.example.data.export

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.example.data.local.NokiaDeviceEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object LogExporter {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

    data class ExportResult(
        val success: Boolean,
        val message: String,
        val filesWritten: List<String> = emptyList(),
        val recordsCount: Int = 0
    )

    fun formatDuration(startEpoch: Long, endEpoch: Long): String {
        val diffMs = (endEpoch - startEpoch).coerceAtLeast(0L)
        val diffSec = diffMs / 1000
        val hours = diffSec / 3600
        val minutes = (diffSec % 3600) / 60
        val seconds = diffSec % 60

        return when {
            hours > 0 -> String.format(Locale.US, "%dh %02dm %02ds", hours, minutes, seconds)
            minutes > 0 -> String.format(Locale.US, "%dm %02ds", minutes, seconds)
            else -> String.format(Locale.US, "%ds", seconds)
        }
    }

    suspend fun exportLogsToFolder(
        context: Context,
        treeUri: Uri,
        devices: List<NokiaDeviceEntity>
    ): ExportResult = withContext(Dispatchers.IO) {
        try {
            val rootFolder = DocumentFile.fromTreeUri(context, treeUri)
                ?: return@withContext ExportResult(
                    success = false,
                    message = "Cannot access selected directory (permission denied or invalid path)"
                )

            if (!rootFolder.canWrite()) {
                return@withContext ExportResult(
                    success = false,
                    message = "Write permission denied for selected folder"
                )
            }

            // 1. Export CSV
            val csvFileName = "nokia_security_radar_logs.csv"
            var csvDoc = rootFolder.findFile(csvFileName)
            if (csvDoc == null) {
                csvDoc = rootFolder.createFile("text/csv", csvFileName)
            }

            if (csvDoc == null) {
                return@withContext ExportResult(
                    success = false,
                    message = "Failed to create CSV file in chosen folder"
                )
            }

            context.contentResolver.openOutputStream(csvDoc.uri, "wt")?.use { outputStream ->
                OutputStreamWriter(outputStream, Charsets.UTF_8).use { writer ->
                    // Header
                    writer.write("SSID,BSSID,First Seen Timestamp,First Seen Date,Last Seen Timestamp,Last Seen Date,Time Range Duration,Encounters,Last RSSI (dBm),Frequency (MHz),Capabilities,Threat Level\n")

                    // Rows
                    devices.forEach { dev ->
                        val firstSeenDateStr = dateFormat.format(Date(dev.firstSeenEpoch))
                        val lastSeenDateStr = dateFormat.format(Date(dev.lastSeenEpoch))
                        val duration = formatDuration(dev.firstSeenEpoch, dev.lastSeenEpoch)
                        val safeSsid = escapeCsv(dev.ssid)
                        val safeCaps = escapeCsv(dev.capabilities)

                        writer.write(
                            "$safeSsid,${dev.bssid},${dev.firstSeenEpoch},$firstSeenDateStr," +
                                "${dev.lastSeenEpoch},$lastSeenDateStr,$duration,${dev.encounterCount}," +
                                "${dev.lastRssi},${dev.frequency},$safeCaps,${dev.threatLevel}\n"
                        )
                    }
                    writer.flush()
                }
            } ?: return@withContext ExportResult(
                success = false,
                message = "Could not open stream for CSV file"
            )

            // 2. Export Human-Readable Threat Briefing TXT
            val txtFileName = "nokia_threat_briefing.txt"
            var txtDoc = rootFolder.findFile(txtFileName)
            if (txtDoc == null) {
                txtDoc = rootFolder.createFile("text/plain", txtFileName)
            }

            val nokiaThreats = devices.filter { it.isNokiaTarget }
            txtDoc?.let { doc ->
                context.contentResolver.openOutputStream(doc.uri, "wt")?.use { outputStream ->
                    OutputStreamWriter(outputStream, Charsets.UTF_8).use { writer ->
                        val nowStr = dateFormat.format(Date())
                        writer.write("====================================================\n")
                        writer.write("       WIFI RADAR SECURITY LOG & THREAT REPORT      \n")
                        writer.write("====================================================\n")
                        writer.write("Report Generated: $nowStr\n")
                        writer.write("Target Filter: Starts with 'NOKIA-'\n")
                        writer.write("Total Logged Devices: ${devices.size}\n")
                        writer.write("NOKIA High-Risk Threats Identified: ${nokiaThreats.size}\n\n")

                        if (nokiaThreats.isEmpty()) {
                            writer.write("Status: NO NOKIA- devices sighted in current logs.\n")
                        } else {
                            writer.write("--- IDENTIFIED NOKIA TARGETS ---\n")
                            nokiaThreats.forEachIndexed { index, target ->
                                val firstDate = dateFormat.format(Date(target.firstSeenEpoch))
                                val lastDate = dateFormat.format(Date(target.lastSeenEpoch))
                                val duration = formatDuration(target.firstSeenEpoch, target.lastSeenEpoch)

                                writer.write("\n[#${index + 1}] SSID: ${target.ssid}\n")
                                writer.write("    BSSID (MAC): ${target.bssid}\n")
                                writer.write("    First Seen: $firstDate\n")
                                writer.write("    Last Seen:  $lastDate\n")
                                writer.write("    Active Time Range: $duration\n")
                                writer.write("    Total Encounters: ${target.encounterCount}\n")
                                writer.write("    Signal Strength: ${target.lastRssi} dBm\n")
                                writer.write("    Frequency: ${target.frequency} MHz\n")
                                writer.write("    Security: ${target.capabilities}\n")
                                writer.write("    Risk Classification: ${target.threatLevel}\n")
                            }
                        }
                        writer.write("\n====================================================\n")
                        writer.flush()
                    }
                }
            }

            ExportResult(
                success = true,
                message = "Synced ${devices.size} devices (${nokiaThreats.size} NOKIA targets) to folder",
                filesWritten = listOf(csvFileName, txtFileName),
                recordsCount = devices.size
            )
        } catch (e: Exception) {
            ExportResult(
                success = false,
                message = "Export failed: ${e.localizedMessage ?: e.message}"
            )
        }
    }

    private fun escapeCsv(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
    }
}
