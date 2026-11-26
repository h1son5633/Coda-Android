package com.h1son5633.coda.data

import android.content.Context
import android.os.Build
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Last-resort crash capture: records the most recent uncaught exception to
 * a file so the next launch can show it (installed first thing in
 * [com.h1son5633.coda.CodaApp.onCreate]).
 */
object CrashReporter {

    private const val FILE_NAME = "last_crash.txt"

    fun install(context: Context) {
        val appContext = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val trace = StringWriter().also { sw ->
                    throwable.printStackTrace(PrintWriter(sw))
                }.toString()
                val header = buildString {
                    append("Coda crash report\n")
                    append("time: ")
                    append(SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date()))
                    append('\n')
                    append("device: ").append(Build.MANUFACTURER).append(' ')
                    append(Build.MODEL).append(" / Android ").append(Build.VERSION.RELEASE)
                    append(" (API ").append(Build.VERSION.SDK_INT).append(")\n")
                    append("thread: ").append(thread.name).append("\n\n")
                }
                File(appContext.filesDir, FILE_NAME).writeText(header + trace)
            } catch (_: Exception) {
                // Never let reporting mask the original crash.
            }
            previous?.uncaughtException(thread, throwable)
        }
    }

    /** Record a caught throwable the same way an uncaught crash is stored. */
    fun record(context: Context, throwable: Throwable) {
        try {
            val trace = StringWriter().also { sw ->
                throwable.printStackTrace(PrintWriter(sw))
            }.toString()
            val header = "Coda crash report (caught)\ndevice: " +
                Build.MANUFACTURER + " " + Build.MODEL +
                " / Android " + Build.VERSION.RELEASE + "\n\n"
            File(context.applicationContext.filesDir, FILE_NAME)
                .writeText(header + trace)
        } catch (_: Exception) {
        }
    }

    fun readLastCrash(context: Context): String? {
        val file = File(context.applicationContext.filesDir, FILE_NAME)
        if (!file.exists()) return null
        return try {
            file.readText().takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            null
        }
    }

    fun clear(context: Context) {
        try {
            File(context.applicationContext.filesDir, FILE_NAME).delete()
        } catch (_: Exception) {
        }
    }
}
