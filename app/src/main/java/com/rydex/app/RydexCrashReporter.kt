package com.rydex.app

import android.content.Context
import java.io.File

object RydexCrashReporter {
    private const val FILE_NAME = "rydex_last_crash.txt"

    fun install(context: Context) {
        val appContext = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching {
                val report = "RYDEX crash report\n" +
                    "Thread: " + thread.name + "\n" +
                    "Exception: " + throwable::class.java.name + "\n" +
                    "Message: " + (throwable.message.orEmpty()) + "\n\n" +
                    throwable.stackTraceToString()
                File(appContext.filesDir, FILE_NAME).writeText(report)
            }

            previous?.uncaughtException(thread, throwable)
        }
    }

    fun read(context: Context): String? =
        runCatching {
            File(context.applicationContext.filesDir, FILE_NAME)
                .takeIf { it.exists() }
                ?.readText()
                ?.takeIf { it.isNotBlank() }
        }.getOrNull()

    fun clear(context: Context) {
        runCatching {
            File(context.applicationContext.filesDir, FILE_NAME).delete()
        }
    }
}