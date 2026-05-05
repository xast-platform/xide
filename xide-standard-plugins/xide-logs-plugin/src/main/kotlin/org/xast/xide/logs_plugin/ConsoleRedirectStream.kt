package org.xast.xide.logs_plugin

import java.io.OutputStream
import java.nio.charset.StandardCharsets

class ConsoleRedirectStream(val view: LogsView, val error: Boolean) : OutputStream() {
    private val buffer = StringBuilder()

    override fun write(b: Int) {
        write(byteArrayOf(b.toByte()), 0, 1)
    }

    override fun write(b: ByteArray, off: Int, len: Int) {
        var text = String(b, off, len, StandardCharsets.UTF_8)

        for (c in text) {
            when (c) {
                '\n' -> {
                    view.addLine(buffer.toString(), error)
                    buffer.setLength(0)
                }
                '\r' -> { 
                    // ignore
                }
                else -> buffer.append(c)
            }
        }
    }
}