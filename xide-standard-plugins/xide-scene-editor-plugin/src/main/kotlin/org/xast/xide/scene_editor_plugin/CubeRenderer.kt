package org.xast.xide.scene_editor_plugin

import com.jogamp.opengl.*

class CubeRenderer : GLEventListener {
    private var angle: Float = 0.0f

    override fun init(drawable: GLAutoDrawable) {
        val gl = drawable.gl.gL4

        gl.glEnable(GL4.GL_DEPTH_TEST)
        gl.glClearColor(0.1f, 0.1f, 0.15f, 1.0f)
    }

    override fun display(drawable: GLAutoDrawable) {
        val gl = drawable.gl.gL4

        gl.glClear(GL4.GL_COLOR_BUFFER_BIT or GL4.GL_DEPTH_BUFFER_BIT)

        // gl.glLoadIdentity()
        // gl.glTranslatef(0, 0, -5)
        // gl.glRotatef(angle, 1, 1, 0)

        drawCube(gl)

        angle += 1.0f
    }

    fun drawCube(gl: GL4) {
        // gl.glBegin(GL4.GL_QUADS)

        // // Front (red)
        // gl.glColor3f(1, 0, 0)
        // gl.glVertex3f(-1, -1,  1)
        // gl.glVertex3f( 1, -1,  1)
        // gl.glVertex3f( 1,  1,  1)
        // gl.glVertex3f(-1,  1,  1)

        // // Back (green)
        // gl.glColor3f(0, 1, 0)
        // gl.glVertex3f(-1, -1, -1)
        // gl.glVertex3f(-1,  1, -1)
        // gl.glVertex3f( 1,  1, -1)
        // gl.glVertex3f( 1, -1, -1)

        // // Left (blue)
        // gl.glColor3f(0, 0, 1)
        // gl.glVertex3f(-1, -1, -1)
        // gl.glVertex3f(-1, -1,  1)
        // gl.glVertex3f(-1,  1,  1)
        // gl.glVertex3f(-1,  1, -1)

        // // Right (yellow)
        // gl.glColor3f(1, 1, 0)
        // gl.glVertex3f(1, -1, -1)
        // gl.glVertex3f(1,  1, -1)
        // gl.glVertex3f(1,  1,  1)
        // gl.glVertex3f(1, -1,  1)

        // // Top (cyan)
        // gl.glColor3f(0, 1, 1)
        // gl.glVertex3f(-1, 1, -1)
        // gl.glVertex3f(-1, 1,  1)
        // gl.glVertex3f( 1, 1,  1)
        // gl.glVertex3f( 1, 1, -1)

        // // Bottom (magenta)
        // gl.glColor3f(1, 0, 1)
        // gl.glVertex3f(-1, -1, -1)
        // gl.glVertex3f( 1, -1, -1)
        // gl.glVertex3f( 1, -1,  1)
        // gl.glVertex3f(-1, -1,  1)

        // gl.glEnd()
    }

    override fun reshape(d: GLAutoDrawable, x: Int, y: Int, w: Int, h: Int) {
        val gl = d.gl.gL4
        // gl.glMatrixMode(GL4.GL_PROJECTION)
        // gl.glLoadIdentity()
        val aspect = w.toFloat() / h
        // gl.glFrustum(-aspect, aspect, -1, 1, 2, 10)
        // gl.glMatrixMode(GL4.GL_MODELVIEW)
    }

    override fun dispose(drawable: GLAutoDrawable) {}
}
