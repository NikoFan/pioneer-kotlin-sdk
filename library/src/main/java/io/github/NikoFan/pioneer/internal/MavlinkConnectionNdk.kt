package io.github.NikoFan.pioneer.internal

import android.util.Log


/**
 * JNI-обёртка для нативного MAVLink-контроллера
 *
 * Отвечает за:
 * - Загрузку native-библиотеки (libdrone_controller.so)
 * - Предоставление external-методов для вызова C++ функций
 * - Управление жизненным циклом соединения (init/close)
 *
 * ⚠️ Все public-методы должны вызываться из корутин (Dispatchers.IO),
 *    так как native-операции могут блокировать поток.
 */
class MavlinkConnectionNdk(ip: String, port: Int) {

    private val tag = "PioneerSDK"

    init {
        // Загружаем native-библиотеку (имя без префикса "lib" и расширения ".so")
        System.loadLibrary("drone_controller")

        // Инициализируем соединение с дроном
        initNative(ip, port)

        Log.d(tag, "Native connection initialized: $ip:$port")
    }
    // =========================================================================
    // БАЗОВЫЕ КОМАНДЫ УПРАВЛЕНИЯ ДРОНОМ
    // =========================================================================

    /** Запуск двигателей */
    external fun arm(): Boolean

    /** Выключение двигателей */
    external fun disarm(): Boolean

    /** Взлёт */
    external fun takeoff(): Boolean

    /** Посадка */
    external fun land(): Boolean

    // =========================================================================
    // УПРАВЛЕНИЕ СВЕТОДИОДАМИ
    // =========================================================================

    /**
     * Управление светодиодами (простой режим, fire-and-forget)
     *
     * @param ledId ID светодиода: 0..3 = конкретный, 255 = все
     * @param r,g,b интенсивность цвета (0..255)
     * @return true если команда отправлена успешно
     */
    external fun ledControl(ledId: Int, r: Int, g: Int, b: Int): Boolean

    // =========================================================================
    // УПРАВЛЕНИЕ СОЕДИНЕНИЕМ
    // =========================================================================

    /**
     * Инициализация UDP-соединения (native)
     * @param ip   IP-адрес дрона
     * @param port UDP-порт MAVLink
     */
    external fun initNative(ip: String, port: Int)

    /**
     * Закрытие соединения и освобождение ресурсов (native)
     */
    external fun closeNative()

    /**
     * Публичный метод для безопасного закрытия соединения
     */
    fun close() {
        Log.d(tag, "Closing native connection")
        closeNative()
    }
}