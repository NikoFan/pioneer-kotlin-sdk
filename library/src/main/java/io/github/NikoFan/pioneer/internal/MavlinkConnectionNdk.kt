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
        initNative(ip, port) // ← ДОБАВЬ ЭТО!
    }

//    init {
//        Log.d(tag, "Инициализация MavlinkConnectionNdk: ip=$ip, port=$port")
//        // Запускаем фоновый приём сообщений от дрона
//        CoroutineScope(Dispatchers.IO).launch {
//            val buffer = ByteArray(300)
//            while (true) {
//                try {
//                    val packet = DatagramPacket(buffer, buffer.size)
//                    receiveSocket.receive(packet)
//                    val hex = bytesToHex(buffer.copyOf(packet.length))
//                    Log.d(tag, "Получен пакет (${packet.length} байт): $hex")
//
//                    if (buffer[0].toInt() and 0xFF == 0xFE && buffer[5].toInt() and 0xFF == 77) {
//                        Log.d(tag, "Обнаружен COMMAND_ACK")
//                    }
//
//                    // Проверяем: это MAVLink v1?
//                    if (buffer[0].toInt() and 0xFF != 0xFE) continue
//
//                    val msgId = buffer[5].toInt() and 0xFF
//                    if (msgId == 77) { // COMMAND_ACK
//                        val command = ByteBuffer.wrap(buffer.sliceArray(10..11))
//                            .order(ByteOrder.LITTLE_ENDIAN)
//                            .short.toInt()
//                        val result = buffer[12].toInt() and 0xFF
//                        ackQueue.trySend(command to result)
//                    }
//                } catch (e: Exception) {
//                    // Игнорируем ошибки (например, при закрытии сокета)
//                    Log.e(tag, "Ошибка приёма пакета", e)
//                }
//            }
//        }
//    }

//    private suspend fun sendCommandWithAck(
//        command: Int,
//        param1: Float = 0f,
//        param2: Float = 0f,
//        param3: Float = 0f,
//        param4: Float = 0f,
//        param5: Float = 0f,
//        param6: Float = 0f,
//        param7: Float = 0f,
//        logTag: String
//    ): Boolean {
//        Log.d(tag, "[$logTag] Начало отправки команды $command")
//        Log.d(tag, "[$logTag] Сигнал отправлен")
//
//        val seq = getNextSequence()
//        Log.d(tag, "[$logTag] Sequence: $seq")
//
//        val payload = ByteBuffer.allocate(33).order(ByteOrder.LITTLE_ENDIAN).apply {
//            putFloat(param1)
//            putFloat(param2)
//            putFloat(param3)
//            putFloat(param4)
//            putFloat(param5)
//            putFloat(param6)
//            putFloat(param7)
//            putShort(command.toShort())
//            put(0) // target_system = 1
//            put(0) // target_component = 0
//            put(0) // confirmation
//        }.array()
//
//        val header = ByteBuffer.allocate(6).order(ByteOrder.LITTLE_ENDIAN).apply {
//            put(0xFE.toByte())  // start
//            put(33)             // payload len
//            put(seq)            // sequence
//            put(255.toByte())   // system_id (GCS)
//            put(0)              // component_id (GCS)
//            put(76)             // msgid = COMMAND_LONG
//        }.array()
//
//        val crcInput = ByteBuffer.allocate(4 + 33).order(ByteOrder.LITTLE_ENDIAN).apply {
//            put(seq)
//            put(255.toByte())
//            put(0)
//            put(76)
//            put(payload)
//        }.array()
//        val crc = generateCrc(crcInput)
//
//        val packet = ByteBuffer.allocate(6 + 33 + 2).order(ByteOrder.LITTLE_ENDIAN).apply {
//            put(header)
//            put(payload)
//            putShort(crc)
//        }.array()
//
//        val hex = bytesToHex(packet)
//        Log.d(tag, "[$logTag] HEX пакета: $hex")
//
//        if (!sendPacket(packet)) {
//            Log.e(tag, "[$logTag] Ошибка отправки")
//            return false
//        }
//
//        // Ждём подтверждение до 5 секунд
//        val startTime = System.currentTimeMillis()
//        var resultReceived: Boolean? = null
//
//        while (System.currentTimeMillis() - startTime < 5000 && resultReceived == null) {
//            try {
//                withTimeout(100L) {
//                    val (cmd, result) = ackQueue.receive()
//                    if (cmd == command) {
//                        resultReceived = result in listOf(0, 2)
//                        if (resultReceived == true) {
//                            Log.d(tag, "[$logTag] Сигнал дошёл")
//                        } else {
//                            Log.w(tag, "[$logTag] Команда отклонена (result=$result)")
//                        }
//                    }
//                }
//            } catch (e: TimeoutCancellationException) {
//                // Продолжаем
//                Log.d(tag, "ОШИБКА TimeoutCancellationException", e)
//            }
//        }
//
//        return resultReceived ?: run {
//            Log.w(tag, "[$logTag] Сигнал не дошёл (таймаут)")
//            false
//        }
//    }

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
//    = sendCommandWithAck(
//        command = 400,
//        param1 = 1.0f,
//        logTag = "ARM"
//    )

    external fun disarm(): Boolean
//    = sendCommandWithAck(
//        command = 400,
//        param1 = 0.0f,
//        logTag = "DISARM"
//    )

    external fun takeoff(): Boolean
//    = sendCommandWithAck(
//        command = 22,
//        logTag = "TAKEOFF"
//    )

    external fun land(): Boolean
//    = sendCommandWithAck(
//        command = 21,
//        logTag = "LAND"
//    )

    external fun getFlightLogs(): Array<String>
    external fun clearLogs()

    fun close() {
        Log.d(tag, "Закрытие сокета")
        sendSocket.close()
        receiveSocket.close()
    }
}