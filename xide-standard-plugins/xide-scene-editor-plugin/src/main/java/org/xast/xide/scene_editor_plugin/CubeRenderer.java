package org.xast.xide.scene_editor_plugin;

import com.jogamp.opengl.*;

public class CubeRenderer implements GLEventListener {

    private float angle = 0;

    @Override
    public void init(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();

        gl.glEnable(GL2.GL_DEPTH_TEST);
        gl.glClearColor(0.1f, 0.1f, 0.15f, 1.0f);
    }

    @Override
    public void display(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();

        gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);

        gl.glLoadIdentity();
        gl.glTranslatef(0, 0, -5);
        gl.glRotatef(angle, 1, 1, 0);

        drawCube(gl);

        angle += 1.0f;
    }

    private void drawCube(GL2 gl) {
        gl.glBegin(GL2.GL_QUADS);

        // Front (red)
        gl.glColor3f(1, 0, 0);
        gl.glVertex3f(-1, -1,  1);
        gl.glVertex3f( 1, -1,  1);
        gl.glVertex3f( 1,  1,  1);
        gl.glVertex3f(-1,  1,  1);

        // Back (green)
        gl.glColor3f(0, 1, 0);
        gl.glVertex3f(-1, -1, -1);
        gl.glVertex3f(-1,  1, -1);
        gl.glVertex3f( 1,  1, -1);
        gl.glVertex3f( 1, -1, -1);

        // Left (blue)
        gl.glColor3f(0, 0, 1);
        gl.glVertex3f(-1, -1, -1);
        gl.glVertex3f(-1, -1,  1);
        gl.glVertex3f(-1,  1,  1);
        gl.glVertex3f(-1,  1, -1);

        // Right (yellow)
        gl.glColor3f(1, 1, 0);
        gl.glVertex3f(1, -1, -1);
        gl.glVertex3f(1,  1, -1);
        gl.glVertex3f(1,  1,  1);
        gl.glVertex3f(1, -1,  1);

        // Top (cyan)
        gl.glColor3f(0, 1, 1);
        gl.glVertex3f(-1, 1, -1);
        gl.glVertex3f(-1, 1,  1);
        gl.glVertex3f( 1, 1,  1);
        gl.glVertex3f( 1, 1, -1);

        // Bottom (magenta)
        gl.glColor3f(1, 0, 1);
        gl.glVertex3f(-1, -1, -1);
        gl.glVertex3f( 1, -1, -1);
        gl.glVertex3f( 1, -1,  1);
        gl.glVertex3f(-1, -1,  1);

        gl.glEnd();
    }

    @Override 
    public void reshape(GLAutoDrawable d, int x, int y, int w, int h) {
        GL2 gl = d.getGL().getGL2();
        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glLoadIdentity();
        float aspect = (float) w / h;
        gl.glFrustum(-aspect, aspect, -1, 1, 2, 10);
        gl.glMatrixMode(GL2.GL_MODELVIEW);
    }

    @Override public void dispose(GLAutoDrawable drawable) {}
}
