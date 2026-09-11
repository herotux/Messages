package android.util

/** JVM-test implementation of the Android Base64 API used by theme file tests. */
object Base64 {
    const val DEFAULT: Int = 0
    const val NO_WRAP: Int = 2

    @JvmStatic
    fun decode(input: String, flags: Int): ByteArray = java.util.Base64.getDecoder().decode(input)

    @JvmStatic
    fun encodeToString(input: ByteArray, flags: Int): String = java.util.Base64.getEncoder().encodeToString(input)
}
