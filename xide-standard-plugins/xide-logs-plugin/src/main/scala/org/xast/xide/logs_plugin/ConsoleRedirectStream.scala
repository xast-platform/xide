package org.xast.xide.logs_plugin

import java.io.OutputStream
import java.nio.charset.StandardCharsets

class ConsoleRedirectStream(
    val view: LogsView, 
    val error: Boolean
) extends OutputStream():
    
    private val buffer = StringBuilder()

    override def write(b: Int) = write(Array(b.toByte), 0, 1)

    override def write(b: Array[Byte], off: Int, len: Int) =
        val text = new String(b, off, len, StandardCharsets.UTF_8)

        text.foreach:
            case '\n' =>
                view.addLine(buffer.toString, error)
                buffer.setLength(0)
                
            case '\r' => // ignore

            case c => buffer.append(c)