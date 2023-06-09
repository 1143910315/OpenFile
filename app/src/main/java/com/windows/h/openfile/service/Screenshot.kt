package com.windows.h.openfile.service

import android.app.Service
import android.content.Intent
import android.os.IBinder

class Screenshot : Service() {

    private var isRunning = false

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        isRunning = true
        Thread {
            while (isRunning) {
                // 执行处理代码
                doSomething()
                Thread.sleep(1000)
            }
        }.start()
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun doSomething() {
        // TODO: 执行处理代码
    }
}