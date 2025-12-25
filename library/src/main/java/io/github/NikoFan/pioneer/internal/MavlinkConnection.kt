package io.github.NikoFan.pioneer.internal

import io.dronefleet.mavlink.common.MavCmd
import io.github.NikoFan.pioneer.Pioneer
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Внутренний класс для отправки команд дрону GEOSCAN через MAVLink 1 по UDP.
 * Не требует внешних сериализаторов — всё встроено.
 */
internal class MavlinkConnection(
    private val ip: String,
    private val port: Int,
    private val logger: Boolean = true
) {
    private val socket = DatagramSocket()
    private var sequence: Byte = 0

    // ===========================
    // Публичные команды
    // ===========================

    suspend fun arm(): Boolean {
        sendCommandLong(
            command = MavCmd.MAV_CMD_COMPONENT_ARM_DISARM.ordinal,
            param1 = 1f
        )
        return true
    }

    suspend fun disarm(): Boolean {
        sendCommandLong(
            command = MavCmd.MAV_CMD_COMPONENT_ARM_DISARM.ordinal,
            param1 = 0f
        )
        return true
    }

    suspend fun takeoff(altitude: Float = 2f): Boolean {
        sendCommandLong(
            command = MavCmd.MAV_CMD_NAV_TAKEOFF.ordinal,
            param7 = altitude
        )
        return true
    }

    suspend fun land(): Boolean {
        sendCommandLong(command = MavCmd.MAV_CMD_NAV_LAND.ordinal)
        return true
    }

    suspend fun goToLocalPoint(x: Float, y: Float, z: Float, yaw: Float = 0f): Boolean {
        // Отправляем SET_POSITION_TARGET_LOCAL_NED
        val payload = setPositionTargetLocalNedToBytes(
            timeBootMs = System.currentTimeMillis().toUInt(),
            targetSystem = Pioneer.SYSTEM_ID,
            targetComponent = Pioneer.COMPONENT_ID,
            coordinateFrame = 8, // MAV_FRAME_LOCAL_NED
            typeMask = 0b0000110111111000, // Используем позицию и yaw
            x = x,
            y = y,
            z = -z, // NED: Z вниз
            yaw = yaw
        )
        val header = MavlinkHeader(
            sequence = getNextSequence(),
            systemId = 255, // GCS
            componentId = 0,
            messageId = 84 // SET_POSITION_TARGET_LOCAL_NED = 84
        )
        val bytes = serializeMavlink1(header, payload)
        sendUdp(bytes)
        return true
    }

    // ===========================
    // Вспомогательные методы
    // ===========================

    private fun sendCommandLong(
        command: Int,
        param1: Float = 0f,
        param2: Float = 0f,
        param3: Float = 0f,
        param4: Float = 0f,
        param5: Float = 0f,
        param6: Float = 0f,
        param7: Float = 0f
    ) {
        val payload = commandLongToBytes(
            targetSystem = Pioneer.SYSTEM_ID,
            targetComponent = Pioneer.COMPONENT_ID,
            command = command,
            confirmation = 0,
            param1 = param1,
            param2 = param2,
            param3 = param3,
            param4 = param4,
            param5 = param5,
            param6 = param6,
            param7 = param7
        )
        val header = MavlinkHeader(
            sequence = getNextSequence(),
            systemId = 255,
            componentId = 0,
            messageId = 76 // COMMAND_LONG = 76
        )
        val bytes = serializeMavlink1(header, payload)
        sendUdp(bytes)
    }

    private fun commandLongToBytes(
        targetSystem: Int,
        targetComponent: Int,
        command: Int,
        confirmation: Int,
        param1: Float, param2: Float, param3: Float, param4: Float,
        param5: Float, param6: Float, param7: Float
    ): ByteArray {
        val payload = ByteBuffer.allocate(33).order(ByteOrder.LITTLE_ENDIAN)
        payload.putFloat(param1)
        payload.putFloat(param2)
        payload.putFloat(param3)
        payload.putFloat(param4)
        payload.putFloat(param5)
        payload.putFloat(param6)
        payload.putFloat(param7)
        payload.putShort(command.toShort())
        payload.put(targetSystem.toByte())
        payload.put(targetComponent.toByte())
        payload.put(confirmation.toByte())
        return payload.array()
    }

    private fun setPositionTargetLocalNedToBytes(
        timeBootMs: UInt,
        targetSystem: Int,
        targetComponent: Int,
        coordinateFrame: Int,
        typeMask: Int,
        x: Float, y: Float, z: Float,
        yaw: Float
    ): ByteArray {
        val payload = ByteBuffer.allocate(53).order(ByteOrder.LITTLE_ENDIAN)
        payload.putInt(timeBootMs.toInt())
        payload.put(targetSystem.toByte())
        payload.put(targetComponent.toByte())
        payload.put(coordinateFrame.toByte())
        payload.putInt(typeMask)
        payload.putFloat(x)
        payload.putFloat(y)
        payload.putFloat(z)
        // vx, vy, vz
        payload.putFloat(0f).putFloat(0f).putFloat(0f)
        // afx, afy, afz
        payload.putFloat(0f).putFloat(0f).putFloat(0f)
        payload.putFloat(yaw)
        payload.putFloat(0f) // yaw_rate
        return payload.array()
    }

    private fun getNextSequence(): Byte {
        val seq = sequence
        sequence = if (sequence == 255.toByte()) 0 else (sequence + 1).toByte()
        return seq
    }

    private data class MavlinkHeader(
        val sequence: Byte,
        val systemId: Int,
        val componentId: Int,
        val messageId: Int
    )

    private fun serializeMavlink1(header: MavlinkHeader, payload: ByteArray): ByteArray {
        val fullLength = 6 + payload.size + 2
        val buffer = ByteBuffer.allocate(fullLength).order(ByteOrder.LITTLE_ENDIAN)

        buffer.put(0xFE.toByte())        // start byte
        buffer.put(payload.size.toByte()) // payload length
        buffer.put(header.sequence)
        buffer.put(header.systemId.toByte())
        buffer.put(header.componentId.toByte())
        buffer.put(header.messageId.toByte())
        buffer.put(payload)

        val crc = generateCrc(buffer, 1, 5 + payload.size)
        buffer.putShort(crc)

        return buffer.array()
    }

    private fun generateCrc(buffer: ByteBuffer, offset: Int, length: Int): Short {
        var crc: Int = 0xFFFF
        for (i in offset until offset + length) {
            crc = crc xor (buffer.get(i).toInt() and 0xff)
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

    private fun sendUdp(bytes: ByteArray) {
        try {
            val packet = DatagramPacket(bytes, bytes.size, InetAddress.getByName(ip), port)
            socket.send(packet)
            log("Sent ${bytes.size} bytes to $ip:$port")
        } catch (e: Exception) {
            log("UDP error: ${e.message}")
        }
    }

    private fun log(msg: String) {
        if (logger) println("[Pioneer-Kotlin] $msg")
    }

    fun close() {
        socket.close()
        log("Connection closed")
    }

    fun isConnected(): Boolean = !socket.isClosed
}