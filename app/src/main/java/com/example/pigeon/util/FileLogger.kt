package com.example.pigeon.util

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Captures the running process's logcat output to a file so the dev can pull it
 * with `adb pull` (or open it from a file browser) instead of scraping the
 * Android Studio logcat window.
 *
 * The file lives at `<app external files dir>/pigeon.log` — for this app that's
 *   /storage/emulated/0/Android/data/com.example.pigeon/files/pigeon.log
 *
 * Truncated on every [init] call (i.e. every cold start of the process). For a
 * long-running session the file grows monotonically until the next cold start;
 * we don't rotate mid-session because that would silently drop the lines that
 * are usually the most interesting.
 *
 * Captures only this process (`--pid=<myPid>`), so peer devices' logs don't
 * pollute it.
 */
object FileLogger {
    private const val TAG = "FileLogger"
    private const val LOG_FILE_NAME = "pigeon.log"

    @Volatile private var pumpThread: Thread? = null
    @Volatile private var logcatProcess: Process? = null
    @Volatile private var logFilePath: String? = null

    fun init(context: Context) {
        if (pumpThread != null) {
            Log.w(TAG, "Already initialised — ignoring duplicate init().")
            return
        }
        try {
            val logDir = context.getExternalFilesDir(null) ?: context.filesDir
            val logFile = File(logDir, LOG_FILE_NAME)
            logFilePath = logFile.absolutePath

            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())
            logFile.writeText("=== Pigeon log started at $timestamp (pid=${android.os.Process.myPid()}) ===\n")

            // Clear the logcat ring buffer so we don't replay lines from the previous run.
            try { Runtime.getRuntime().exec(arrayOf("logcat", "-c")).waitFor() } catch (_: Exception) {}

            // -v threadtime → readable timestamps + pid/tid context
            // --pid filter → only this process's lines, ignoring system noise
            val proc = Runtime.getRuntime().exec(arrayOf(
                "logcat",
                "-v", "threadtime",
                "--pid=${android.os.Process.myPid()}"
            ))
            logcatProcess = proc

            pumpThread = Thread({
                try {
                    proc.inputStream.bufferedReader().use { reader ->
                        FileOutputStream(logFile, true).bufferedWriter().use { writer ->
                            reader.forEachLine { line ->
                                writer.write(line)
                                writer.newLine()
                                writer.flush()
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Pump thread died", e)
                }
            }, "FileLogger-Pump").apply {
                isDaemon = true
                start()
            }

            Log.i(TAG, "Logging to ${logFile.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start FileLogger", e)
        }
    }

    /** Path to the log file on disk, or null if [init] hasn't run yet. */
    fun path(): String? = logFilePath
}
