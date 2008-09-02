/*
 * (c) ralfoide, http://gamez.googlecode.com/, 2008
 * Project: gamez
 * License TBD
 */


package com.alfray.mandelbrot.util;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import javax.microedition.khronos.opengles.GL10;

/**
 * Generic abstract implementation of a simple 3d sprite.
 * 
 * Derived classes need to call setupBuffers once in their constructor.
 */
public abstract class Shape {

    private boolean mUseLighting = false;
    
    // These fields are protected so they can be used in unit tests
    protected ShortBuffer xIndexBuffer;
    protected IntBuffer   xVertexBuffer;
    protected IntBuffer   xDiffuseBuffer;
    protected IntBuffer   xTexCoordBuffer;

    /**
     * Constructs a new Simple3dSprite.
     */
    public Shape() {
    }

    /** 
     * Draws the 3d sprite into the given GL context. It's up to the caller
     * to perform rotations and translations as needed.
     */
    public void drawTo(GL10 gl, int textureIndexUnit) {
    	gl.glFrontFace(GL10.GL_CW);

    	gl.glColor4x(xDiffuseBuffer.get(), xDiffuseBuffer.get(), xDiffuseBuffer.get(), xDiffuseBuffer.get());
    	xDiffuseBuffer.position(0);

        gl.glActiveTexture(textureIndexUnit); // TMU0
        gl.glTexCoordPointer(2 /* 2 coords per vertex */, GL10.GL_FIXED, 0, xTexCoordBuffer);
        
    	gl.glVertexPointer(3 /* 3 coords per vertex */, GL10.GL_FIXED, 0, xVertexBuffer);
        
        gl.glDrawElements(GL10.GL_TRIANGLES, xIndexBuffer.limit(),
                          GL10.GL_UNSIGNED_SHORT, xIndexBuffer);
    }

    /**
     * Setup the NIO buffers holding the geometry and color information
     * from regular float arrays. Derived classes must call this once in their
     * constructor.
     * 
     * @param vertices Vertices float array, (x,y,z) per vertex
     * @param normals Normals float array, one (x,y,z) per vertex
     * @param diffuse Diffuse color for all vertices. (r,g,b,a) tuplet.
     * @param indices List of triplets of vertices used to make CW triangles.
     */
    protected void setupBuffers(float[] vertices, float[] diffuse, short[] indices) {
    	assert vertices != null;
    	assert indices != null;
    	if (vertices != null) xVertexBuffer = floatArrayToIntBuffer(vertices);
    	if (diffuse != null) xDiffuseBuffer = floatArrayToIntBuffer(diffuse);

        xIndexBuffer = shortArrayToShortBuffer(indices);
    }

    protected void setupTextBuffers(float[] textCoord) {
		xTexCoordBuffer = floatArrayToIntBuffer(textCoord);
	}

    /**
     * Utility method that creates an NIO IntBuffer out of a float[]
     * array. This is used by derived classes to prepare vertex and normal
     * buffers and is just made available here as a convenience method.
     */
    protected IntBuffer floatArrayToIntBuffer(final float[] floatArray) {
        // An int is 4 bytes with 16/16 bits
        ByteBuffer vbb = ByteBuffer.allocateDirect(floatArray.length*4);
        vbb.order(ByteOrder.nativeOrder());
        IntBuffer buffer = vbb.asIntBuffer();
        for (float f : floatArray) {
            buffer.put((int)(f * 65536.0f));
        }
        buffer.position(0);
        return buffer;
    }

    protected ShortBuffer shortArrayToShortBuffer(final short[] shortArray) {
      // An int is 4 bytes with 16/16 bits
      ByteBuffer vbb = ByteBuffer.allocateDirect(shortArray.length*2);
      vbb.order(ByteOrder.nativeOrder());
      ShortBuffer buffer = vbb.asShortBuffer();
      buffer.put(shortArray);
      buffer.position(0);
      return buffer;
  }

    /**
     * Utility method to duplicate an int buffer.
     * Allocates a new buffer of the same size & native and copies all data
     * from one to the other.
     */
    protected IntBuffer duplicateIntBuffer(IntBuffer source) {
        ByteBuffer vbb = ByteBuffer.allocateDirect(source.limit() * 4);
        vbb.order(ByteOrder.nativeOrder());
        IntBuffer dest = vbb.asIntBuffer();
        source.position(0);
        dest.put(source);
        source.position(0);
        dest.position(0);
        return dest;
    }
}

