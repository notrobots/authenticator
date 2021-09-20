package dev.notrobots.authenticator.time

import android.os.Handler
import android.os.Looper
import dev.notrobots.authenticator.util.now

class TimerTask : Runnable {
    private var startTime = 0L
    private var handler = Handler(Looper.getMainLooper())
    private var listener: Listener? = null

    override fun run() {
        listener?.onTick(0)

//        if (progress = 0) {
//            listener?.onFinish()
//        }

        handler.postDelayed(this, 100)
    }

    /*
        start = 1631633559
        current = 1631633743
        step = 30

        current - start = 184
        184 / step = 6.1

     */

    fun getCounter(): Long {
        return now() / 1000 / 30
    }

    fun getProgress(): Long {
        val counter = getCounter()
        val next = counter + 1
//        val nextStart =


        return 0
    }

    fun start() {
        startTime = now()
        handler.post(this)
    }

    fun setListener(listener: Listener) {
        this.listener = listener
    }

    interface Listener {
        fun onTick(timeLeft: Long)
        fun onFinish()
    }
}