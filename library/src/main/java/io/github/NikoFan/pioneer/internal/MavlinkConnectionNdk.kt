package io.github.NikoFan.pioneer.internal

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.ByteBuffer
import java.nio.ByteOrder

class MavlinkConnectionNdk(ip: String, port: Int) {
    private val tag = "LogPanel"
    init {
        System.loadLibrary("drone_controller")
        closeNative()
        initNative(ip, port)
    }
    // === ПУБЛИЧНЫЕ МЕТОДЫ ===
    external fun initNative(ip: String, port: Int)

    external fun arm(): Boolean


    external fun disarm(): Boolean


    external fun takeoff(): Boolean


    external fun land(): Boolean


    external fun getFlightLogs(): Array<String>
    external fun clearLogs()
    external fun closeNative()

    fun close() {
        Log.d(tag, "Закрытие сокета")
        closeNative() // NATIVE закрытие
    }
}