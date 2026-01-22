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
    private val udpIp = ip
    private val udpPort = port

    // Сокет для отправки команд
    private val sendSocket = DatagramSocket()

    // Сокет для приёма ответов (COMMAND_ACK)
    private val receiveSocket = DatagramSocket()

    // Очередь подтверждений: (command_id → result)
    private val ackQueue = Channel<Pair<Int, Int>>(Channel.UNLIMITED)

    // Мьютекс для потокобезопасности sequence
    private val mutex = Mutex()

    // Глобальный sequence counter (MAVLink)
    private var sequence: Byte = 0

    private fun bytesToHex(bytes: ByteArray): String {
        return bytes.joinToString(" ") { "%02X".format(it) }
    }
    init {
        System.loadLibrary("drone_controller")
        closeNative()
        initNative(ip, port)
    }

    private suspend fun getNextSequence(): Byte = mutex.withLock {
        val current = sequence
        sequence = if (sequence == 127.toByte()) 0 else (sequence + 1).toByte()
        current
    }

    private fun sendPacket(packet: ByteArray): Boolean {
        val hex = bytesToHex(packet)
        Log.d(tag, "Отправка пакета (${packet.size} байт): $hex")
        return try {
            val datagram = DatagramPacket(packet, packet.size, InetAddress.getByName(udpIp), udpPort)
            sendSocket.send(datagram)
            Log.d(tag, "Пакет успешно отправлен")
            true
        } catch (e: Exception) {
            Log.e(tag, "UDP send error: ${e.message}")
            false
        }
    }

    private fun generateCrc(data: ByteArray): Short {
        var crc = 0xFFFF
        for (byte in data) {
            crc = crc xor (byte.toInt() and 0xFF)
            for (j in 0 until 8) {
                if ((crc and 1) == 1) {
                    crc = (crc shr 1) xor 0xA001
                } else {
                    crc = crc shr 1
                }
            }
        }
        return crc.toShort()
    }

    // === ПУБЛИЧНЫЕ МЕТОДЫ ===

    external fun initNative(ip: String, port: Int)

    external fun arm(): Boolean


    external fun disarm(): Boolean


    external fun takeoff(): Boolean


    external fun land(): Boolean


    external fun getFlightLogs(): Array<String>
    external fun clearLogs()
    external fun closeNative() // ← ДОБАВЬ ЭТОТ МЕТОД!

    fun close() {
        Log.d(tag, "Закрытие сокета")
        closeNative() // ← ВЫЗЫВАЕМ NATIVE ЗАКРЫТИЕ
        sendSocket.close()
        receiveSocket.close()
    }
}