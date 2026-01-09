package io.github.NikoFan.pioneer.internal

class MavlinkConnectionNdk(ip: String, port: Int) {
    init {
        System.loadLibrary("drone_controller")
        initNative(ip, port)
    }

    external fun arm(): Boolean
    external fun disarm(): Boolean

    private external fun initNative(ip: String, port: Int)

    companion object {
        init {
            System.loadLibrary("drone_controller")
        }
    }
}