package org.fossify.messages.helpers

/** Small platform-independent Base64 codec used by the .homa-theme format. */
internal object ThemeBase64 {
    private const val ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"

    fun encode(bytes: ByteArray): String {
        if (bytes.isEmpty()) return ""
        val out = StringBuilder((bytes.size + 2) / 3 * 4)
        var i = 0
        while (i < bytes.size) {
            val b0 = bytes[i++].toInt() and 0xff
            val b1 = if (i < bytes.size) bytes[i++].toInt() and 0xff else -1
            val b2 = if (i < bytes.size) bytes[i++].toInt() and 0xff else -1
            out.append(ALPHABET[b0 ushr 2])
            out.append(ALPHABET[((b0 and 0x03) shl 4) or if (b1 >= 0) b1 ushr 4 else 0])
            out.append(if (b1 >= 0) ALPHABET[((b1 and 0x0f) shl 2) or if (b2 >= 0) b2 ushr 6 else 0] else '=')
            out.append(if (b2 >= 0) ALPHABET[b2 and 0x3f] else '=')
        }
        return out.toString()
    }

    fun decode(value: String): ByteArray {
        val input = value.filterNot(Char::isWhitespace)
        require(input.isNotEmpty() && input.length % 4 == 0) { "Invalid Base64" }
        require(input.none { it == '=' } || input.endsWith("=") || input.endsWith("==")) { "Invalid Base64" }
        val out = ByteArrayOutputStream(input.length / 4 * 3)
        var i = 0
        while (i < input.length) {
            val c0 = valueOf(input[i++])
            val c1 = valueOf(input[i++])
            val c2 = input[i++].let { if (it == '=') -1 else valueOf(it) }
            val c3 = input[i++].let { if (it == '=') -1 else valueOf(it) }
            require(c2 >= 0 || c3 < 0) { "Invalid Base64" }
            require(c3 >= 0 || c2 >= 0) { "Invalid Base64" }
            out.write((c0 shl 2) or (c1 ushr 4))
            if (c2 >= 0) out.write(((c1 and 0x0f) shl 4) or (c2 ushr 2))
            if (c3 >= 0) out.write(((c2 and 0x03) shl 6) or c3)
            if (c2 < 0) require(c3 < 0 && i == input.length) { "Invalid Base64" }
            if (c3 < 0) require(i == input.length) { "Invalid Base64" }
        }
        return out.toByteArray()
    }

    private fun valueOf(char: Char): Int = ALPHABET.indexOf(char).also { require(it >= 0) { "Invalid Base64" } }

    private class ByteArrayOutputStream(initialSize: Int) {
        private var buffer = ByteArray(initialSize.coerceAtLeast(1))
        private var size = 0
        fun write(value: Int) {
            if (size == buffer.size) buffer = buffer.copyOf(buffer.size * 2)
            buffer[size++] = value.toByte()
        }
        fun toByteArray(): ByteArray = buffer.copyOf(size)
    }
}
