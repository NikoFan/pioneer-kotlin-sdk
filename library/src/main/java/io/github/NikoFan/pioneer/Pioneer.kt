package io.github.NikoFan.pioneer


import io.github.NikoFan.pioneer.internal.MavlinkConnection
import kotlinx.coroutines.*

/**
 * Основной класс для управления дроном GEOSCAN.
 * Аналог класса Pioneer из pioneer-sdk (Python).
 *
 * Поддерживает:
 * - arm(), disarm()
 * - takeoff(), land()
 * - go_to_local_point(x, y, z, yaw)
 *
 * Работает через UDP-соединение с дроном по умолчанию на 192.168.4.1:8001.
 */
class Pioneer(
    ip: String = "192.168.4.1",
    mavlinkPort: Int = 8001,
    private val logger: Boolean = true
) {
    private val connection = MavlinkConnection(ip, mavlinkPort, logger)

    /**
     * Запуск двигателей (ARM).
     * @return true, если команда принята (MAV_RESULT_ACCEPTED или DENIED)
     */
    suspend fun arm(): Boolean = connection.arm()

    /**
     * Остановка двигателей (DISARM).
     */
    suspend fun disarm(): Boolean = connection.disarm()

    /**
     * Взлёт на высоту, заданную в настройках автопилота.
     */
//    suspend fun takeoff(): Boolean = connection.takeoff()
//
    /**
     * Посадка.
     */
    suspend fun land(): Boolean = connection.land()

//    /**
//     * Полёт в точку в локальной системе координат (NED).
//     * @param x, y, z — координаты в метрах. Z — вниз (NED), поэтому передаётся как -z.
//     * @param yaw — угол рысканья в радианах.
//     */
//    suspend fun goToLocalPoint(x: Float, y: Float, z: Float, yaw: Float = 0f): Boolean =
//        connection.goToLocalPoint(x, y, z, yaw)

    /**
     * Закрывает соединение с дроном.
     */
    fun close() {
        connection.close()
    }

    /**
     * Проверяет, есть ли связь с дроном (получены ли сообщения за последнюю секунду).
     */
    fun isConnected(): Boolean = connection.isConnected()

//    companion object {
//        const val SYSTEM_ID = 1
//        const val COMPONENT_ID = 1
//    }
}