package io.github.NikoFan.pioneer.internal

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

internal class MavlinkConnection(
    private val ip: String,
    private val port: Int,
    private val logger: Boolean = true
) {
    private val socket = DatagramSocket()

    suspend fun arm(): Boolean {
        // Точный ARM-пакет из pioneer_sdk (Python)
        val armPacket = byteArrayOf(
            0xfe.toByte(),  // start
            0x21,           // payload len = 33
            0x00,           // seq
            -0x01,          // system_id = 255
            0x00,           // component_id = 0
            0x4c,           // COMMAND_LONG = 76

            0x00, 0x00, -0x80, 0x3f, // param1 = 1.0f
            0x00, 0x00, 0x00, 0x00, // param2 = 0.0
            0x00, 0x00, 0x00, 0x00, // param3
            0x00, 0x00, 0x00, 0x00, // param4
            0x00, 0x00, 0x00, 0x00, // param5
            0x00, 0x00, 0x00, 0x00, // param6
            0x00, 0x00, 0x00, 0x00, // param7

            0x90.toByte(), 0x01.toByte(),     // command = 400 (little-endian)
            0x00,           // target_system = 0
            0x00,           // target_component = 0
            0x00,           // confirmation = 0

            -0x77, -0x3f    // CRC = 0x89c1
        )
        return sendPacket(armPacket)
    }

    suspend fun land(): Boolean {
        val landPacket = byteArrayOf(
            0xfe.toByte(), 0x21, 0x02, -0x01, 0x00, 0x4c,
            0x00, 0x00, 0x00, 0x00, // param1 = 0
            0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00,
            0x15.toByte(), 0x00.toByte(), // command = 21 (MAV_CMD_NAV_LAND)
            0x00, // target_system = 0
            0x00,
            0x00,
            -0x1c, -0x3f // CRC для LAND
        )
        return sendPacket(landPacket)
    }

    suspend fun disarm(): Boolean {
        val disarmPacket = byteArrayOf(
            0xfe.toByte(),  // start
            0x21,           // payload len = 33
            0x01,           // sequence
            -0x01,          // system_id = 255 (GCS)
            0x00,           // component_id = 0
            0x4c,           // COMMAND_LONG

            0x00, 0x00, 0x00, 0x00, // param1 = 0.0
            0x00, 0x00, 0x00, 0x00, // param2 = 0
            0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00,

            0x90.toByte(), 0x01.toByte(), // command = 400
            0x00, // ← target_system = 0 (как в ARM!)
            0x00, // ← target_component = 0 (как в ARM!)
            0x00, // confirmation = 0

            -0x77, -0x3f // ← CRC = 89c1 (тот же, что и в ARM!)
        )
        return sendPacket(disarmPacket)
    }

    private fun sendPacket(packet: ByteArray): Boolean {
        if (logger) {
            val hex = packet.joinToString("") { "%02x".format(it.toInt() and 0xFF) }
            println("Kotlin HEX: $hex")
        }
        return try {
            val address = InetAddress.getByName(ip)
            val datagram = DatagramPacket(packet, packet.size, address, port)
            socket.send(datagram)
            true
        } catch (e: Exception) {
            if (logger) println("UDP send error: ${e.message}")
            false
        }
    }

    fun close() {
        socket.close()
    }

    fun isConnected(): Boolean = !socket.isClosed
}