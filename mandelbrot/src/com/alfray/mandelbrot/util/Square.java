/*
 * (c) ralfoide, http://gamez.googlecode.com/, 2008
 * Project: gamez
 * License TBD
 */


package com.alfray.mandelbrot.util;

import javax.microedition.khronos.opengles.GL10;


//-----------------------------------------------

/**
 * The 3d square plate.
 */
public class Square extends Shape {

	/**
	 * Creates a square shape with 2 triangles.
	 * Flat on Z=0 plane, with X=[0 => 1], Y=[0, 1].
	 */
    public Square(float size) {
        
        /* vertices in CW order:
         *   view from top:
         *   1 --- 2  0-1-2       
         *   |   / |
         *   |  /  |
         *   | /   |
         *   0 --- 3  0-2-3
         */
        final float vertices[] = {
                   0,     0,     0,    // 0 (CW)
                   0,  size,     0,    // 1
                size,  size,     0,    // 2
                size,     0,     0,    // 3
         };

        final short indices[] = {
        		0, 1, 2,
                //0, 2, 3,
        };
        
        final float textCoord[] = {
                 0,  0,     // 0
                 0,  1,     // 1
                 1,  1,     // 2
                 1,  0,     // 3
        };

        final float diffuse[] = { 1, 1, 1, 1 }; // white

        setupBuffers(vertices, diffuse, indices);
		setupTextBuffers(textCoord);
    }
}

