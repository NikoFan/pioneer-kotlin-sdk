package io.github.NikoFan.pioneer.model

/**
 * Данные о позиции дрона в локальной системе координат (NED).
 * x, y — в метрах от точки взлёта.
 * z — высота (в метрах), но в NED система z направлен вниз → у нас инвертируется.
 */
data class Position(
    val x: Float,
    val y: Float,
    val z: Float, // здесь уже "высота над землёй", не NED-z
    val yaw: Float = 0f
)