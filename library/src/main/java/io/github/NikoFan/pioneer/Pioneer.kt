package io.github.NikoFan.pioneer

import io.github.NikoFan.pioneer.internal.MavlinkConnectionNdk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext


/**
 * Представление цвета для светодиодов дрона
 *
 * @param r Red-компонента (0..255)
 * @param g Green-компонента (0..255)
 * @param b Blue-компонента (0..255)
 */
data class LedColor(val r: Int, val g: Int, val b: Int) {
    companion object {
        val RED    = LedColor(255, 0,   0)
        val GREEN  = LedColor(0,   255, 0)
        val BLUE   = LedColor(0,   0,   255)
        val WHITE  = LedColor(255, 255, 255)
        val YELLOW = LedColor(255, 255, 0)
        val OFF    = LedColor(0,   0,   0)
    }
}

/**
 * Высокоуровневый API для управления дроном GEOSCAN Pioneer
 *
 * Особенности:
 * - Все публичные методы являются suspend-функциями и выполняют операции в Dispatchers.IO
 * - Автоматическая инициализация native-соединения при создании
 * - Удобные обёртки для частых сценариев (мигание, установка всех светодиодов)
 *
 * @param ip          IP-адрес дрона (по умолчанию "192.168.4.1")
 * @param mavlinkPort UDP-порт MAVLink (по умолчанию 8001)
 * @param logger      Включить логирование (пока не используется, зарезервировано)
 */
class Pioneer(
    ip: String = "192.168.4.1",
    mavlinkPort: Int = 8001,
    @Suppress("UNUSED_PARAMETER") logger: Boolean = true
) {
    // Native-соединение: инициализируется в конструкторе
    private val connection = MavlinkConnectionNdk(ip, mavlinkPort)

    // =========================================================================
    // УПРАВЛЕНИЕ СВЕТОДИОДАМИ
    // =========================================================================

    /**
     * Управление светодиодами (простой режим)
     *
     *
     * @param ledId ID светодиода (0..3 или 255 для всех)
     * @param color цвет в формате RGB
     * @return true если команда успешно отправлена в сеть
     */
    suspend fun ledControl(ledId: Int, color: LedColor): Boolean =
        withContext(Dispatchers.IO) {
            connection.ledControl(ledId, color.r, color.g, color.b)
        }


    /**
     * Установить цвет всех светодиодов
     */
    suspend fun setAllLeds(color: LedColor): Boolean =
        ledControl(255, color)

    /**
     * Мигание светодиодов заданным цветом
     *
     * @param color      цвет для мигания
     * @param times      количество повторений (по умолчанию 3)
     * @param intervalMs интервал между переключениями в мс (по умолчанию 200)
     */
    suspend fun blinkLeds(
        color: LedColor,
        times: Int = 3,
        intervalMs: Long = 200
    ) {
        repeat(times) {
            ledControl(255, color)
            delay(intervalMs)
            ledControl(255, LedColor.OFF)
            delay(intervalMs)
        }
    }

    // =========================================================================
    // БАЗОВЫЕ КОМАНДЫ УПРАВЛЕНИЯ ПОЛЁТОМ
    // =========================================================================

    /** Запуск двигателей */
    suspend fun arm(): Boolean =
        withContext(Dispatchers.IO) { connection.arm() }

    /** Выключение двигателей */
    suspend fun disarm(): Boolean =
        withContext(Dispatchers.IO) { connection.disarm() }

    /** Взлёт */
    suspend fun takeoff(): Boolean =
        withContext(Dispatchers.IO) { connection.takeoff() }

    /** Посадка */
    suspend fun land(): Boolean =
        withContext(Dispatchers.IO) { connection.land() }

    // =========================================================================
    // УПРАВЛЕНИЕ ЖИЗНЕННЫМ ЦИКЛОМ
    // =========================================================================

    /**
     * Закрытие соединения с дроном
     *
     * Обязательно вызывать при завершении работы с объектом Pioneer,
     * чтобы освободить native-ресурсы (сокет, потоки).
     */
    fun close() {
        connection.close()
    }

    /**
     * Проверка состояния соединения
     */
    fun isConnected(): Boolean = true
}