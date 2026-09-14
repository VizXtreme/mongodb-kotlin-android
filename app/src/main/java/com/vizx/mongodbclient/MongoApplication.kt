package com.vizx.mongodbclient

import android.app.Application
import android.content.Intent
import android.os.Process
import android.util.Log

class MongoApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                Log.e("MongoAppCrash", "FATAL CRASH: ${throwable.message}", throwable)
                val intent = Intent(this, CrashActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    putExtra(CrashActivity.EXTRA_ERROR_CLASS, throwable.javaClass.name)
                    putExtra(CrashActivity.EXTRA_ERROR_MESSAGE, throwable.localizedMessage ?: throwable.toString())
                    putExtra(CrashActivity.EXTRA_STACK_TRACE, throwable.stackTraceToString())
                }
                startActivity(intent)
            } catch (e: Throwable) {
                Log.e("MongoAppCrash", "Failed to launch CrashActivity", e)
                defaultHandler?.uncaughtException(thread, throwable)
            } finally {
                Process.killProcess(Process.myPid())
                System.exit(10)
            }
        }
    }
}
