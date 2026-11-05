package com.boostfromdr.mobile

/**
 * Native one-euro filter bridge.
 * Input and output units are radians/second from Android's gyroscope.
 */
object NativeFilter {
    init {
        System.loadLibrary("boostfilter")
    }

    external fun process(x: Float, y: Float, z: Float, deltaSeconds: Float): FloatArray
    external fun reset()
}
