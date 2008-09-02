/*
 * (c) ralfoide gmail com, 2008
 * Project: Mandelbrot
 * License TBD
 */

package com.alfray.mandelbrot.tiles;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import javax.microedition.khronos.opengles.GL10;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLU;
import android.view.SurfaceHolder;

import com.alfray.mandelbrot.util.GLBaseThread;
import com.alfray.mandelbrot.util.Square;

public class RenderThread extends GLBaseThread {

    /** Current distance to the camera */
    private float mDistance = 30.0f;
    // GLFixed version of mDistance. use changeViewDistance to update this. */
    private int xDistance;
    
    private Square mSquare;
    private TileContext mTileContext;

    public RenderThread(SurfaceHolder surfaceHolder) {
        super(surfaceHolder);
        
        mSquare = new Square(Tile.SIZE);
    }

    public void setTileContext(TileContext tileContext) {
        mTileContext = tileContext;
        invalidate();
    }


    /* (non-Javadoc)
     * @see com.alfray.mandelbrot.util.BaseThread#clear()
     */
    @Override
    public void clear() {
    }
    
    @Override
    protected void setupScene(GL10 gl, int w, int h) {
        super.setupScene(gl, w, h);

        gl.glMatrixMode(GL10.GL_PROJECTION);
        gl.glLoadIdentity();
        
        float w2 = (float)w / 2.0f;
        float h2 = (float)h / 2.0f;
        GLU.gluOrtho2D(gl, -w2, w2, h2, -h2);

        changeViewDistance(0); // compute xDistance
        
        gl.glDisable(GL10.GL_DITHER);

        gl.glDisable(GL10.GL_CULL_FACE);
        gl.glDisable(GL10.GL_DEPTH_TEST);

        // lighting -- none
        gl.glDisable(GL10.GL_LIGHTING);

        // textures
        gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT, GL10.GL_FASTEST); // NICEST);

        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
        gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
        
        gl.glShadeModel(GL10.GL_FLAT);
    }

    /**
     * Associates a given texture index to a given TMU renderer.
     *
     * @param gl The GL context
     * @param texture_index The GL texture index (GL_TEXTURE1..n)
     */
    private void associateTexture(GL10 gl, int texture_index) {
        setupTexture(gl, 0 /* TMU0 */, texture_index, GL10.GL_REPLACE);
    }

    /**
     * Associates a given texture index to a given TMU renderer.
     *
     * @param gl The GL context
     * @param tmu_index TMU0 or TMU1
     * @param texture_index The GL texture index (GL_TEXTURE1..n)
     * @param mode GL_REPLACE, GL_BLEND, etc.
     */
    private void setupTexture(GL10 gl, int tmu_index, int texture_index, int mode) {
        gl.glActiveTexture(tmu_index); // TMU0 or TMU1
        gl.glBindTexture(GL10.GL_TEXTURE_2D, texture_index);
        gl.glEnable(GL10.GL_TEXTURE_2D);
        gl.glTexEnvx(GL10.GL_TEXTURE_ENV, GL10.GL_TEXTURE_ENV_MODE, mode);
    }

    /**
     * Loads a texture. This is specialized to load a 565 texture.
     *
     * @param gl The GL context
     * @param input An short array where each entry is an unsigned 16-bit 565 color
     *              stored by row (i.e. for y { for x }).
     * @param width Width of texture in pixels
     * @param height Height of texture in pixels
     * @param tex_num The GL texture index (GL_TEXTURE1..n)
     */
    private void loadTexture565(GL10 gl, short[] input, int width, int height, int tex_num) {
        ByteBuffer byte_buf;
        
        byte_buf = ByteBuffer.allocateDirect(width * height * 2);
        byte_buf.order(ByteOrder.nativeOrder());

        ShortBuffer short_buf = byte_buf.asShortBuffer();
        
        short_buf.put(input);
        
        byte_buf.position(0);
        
        gl.glBindTexture(GL10.GL_TEXTURE_2D, tex_num);

        gl.glTexParameterx(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
        gl.glTexParameterx(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);
        gl.glTexParameterx(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);//  NEAREST);
        gl.glTexParameterx(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR);//NEAREST);

        gl.glTexImage2D(
                GL10.GL_TEXTURE_2D, // target 
                0, // level
                GL10.GL_UNSIGNED_SHORT_5_6_5, // internalformat
                width, // width
                height, // height
                0, // border
                GL10.GL_UNSIGNED_SHORT_5_6_5, // format
                GL10.GL_UNSIGNED_BYTE, // type
                byte_buf // pixels
            );
    }
 
    /**
     * Adjusts the distance to the camera by the given delta.
     * Positive values make object look farther.
     *
     * @param delta The distance to add or substract. +1=farther, -1=closer
     */
    public void changeViewDistance(float delta) {
        mDistance += delta;
        xDistance = (int)(mDistance * 65536);
    }

    /* (non-Javadoc)
     * @see com.alfray.mandelbrot.util.GLBaseThread#drawScene(javax.microedition.khronos.opengles.GL10, int, int)
     */
    @Override
    protected void drawScene(GL10 gl, int w, int h) {
        // Clear screen and buffers. Depth buffer is not enabled here.
        gl.glClearColor(0,0,0,1); // clear to black
        gl.glClear(GL10.GL_COLOR_BUFFER_BIT /* | GL10.GL_DEPTH_BUFFER_BIT */);

        // set model view
        gl.glMatrixMode(GL10.GL_MODELVIEW);
        gl.glLoadIdentity();
        gl.glTranslatex(0, 1 << 16, 0 - xDistance);
        //--gl.glRotatex(xAngleH, 0, 1 << 16, 0);
        //--gl.glRotatex(xAngleV, 1 << 16, 0, 0);

        if (mTileContext != null) {
            Tile t = mTileContext.getVisibleTiles();
            if (t != null) {
                short[] texture = t.getRgb565();
                if (texture != null) {
                    associateTexture(gl, GL10.GL_TEXTURE0);
                    loadTexture565(gl, texture, t.SIZE, t.SIZE, GL10.GL_TEXTURE0);
                }
            }
        }
    }

}
