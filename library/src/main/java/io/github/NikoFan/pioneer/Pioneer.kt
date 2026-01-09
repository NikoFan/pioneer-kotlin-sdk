package io.github.NikoFan.pioneer

import io.github.NikoFan.pioneer.internal.MavlinkConnectionNdk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class Pioneer(
    ip: String = "192.168.4.1",
    mavlinkPort: Int = 8001,
    private val logger: Boolean = true
) {
    private val connection = MavlinkConnectionNdk(ip, mavlinkPort)

    suspend fun arm(): Boolean = withContext(Dispatchers.IO) { connection.arm() }
    suspend fun disarm(): Boolean = withContext(Dispatchers.IO) { connection.disarm() }

    fun close() = Unit
    fun isConnected() = true
}