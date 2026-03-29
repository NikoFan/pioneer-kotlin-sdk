package io.github.NikoFan.pioneer


/**
 * Результат выполнения MAVLink-команды с подтверждением (ACK)
 *
 */
@Suppress("UNUSED")
sealed class LedCommandResult {
    object Sent : LedCommandResult()
    data class Accepted(val message: String = "OK") : LedCommandResult()
    data class Rejected(
        val code: Int,
        val message: String = when(code) {
            1 -> "TEMPORARILY_REJECTED"
            2 -> "DENIED"
            3 -> "UNSUPPORTED"
            4 -> "FAILED"
            5 -> "IN_PROGRESS"
            6 -> "CANCELLED"
            else -> "UNKNOWN_ERROR"
        }
    ) : LedCommandResult()
    object Timeout : LedCommandResult()
    data class Error(val exception: Exception) : LedCommandResult()
}

@Suppress("UNUSED")
internal fun Int.toLedResult(): LedCommandResult = when(this) {
    0 -> LedCommandResult.Accepted()
    1 -> LedCommandResult.Rejected(1)
    2 -> LedCommandResult.Rejected(2)
    3 -> LedCommandResult.Rejected(3)
    4 -> LedCommandResult.Rejected(4)
    5 -> LedCommandResult.Rejected(5)
    6 -> LedCommandResult.Rejected(6)
    -1 -> LedCommandResult.Timeout
    else -> LedCommandResult.Rejected(this)
}