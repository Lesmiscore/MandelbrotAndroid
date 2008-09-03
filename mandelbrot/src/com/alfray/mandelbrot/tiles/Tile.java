/*
 * (c) ralfoide gmail com, 2008
 * Project: Mandelbrot
 * License TBD
 */


package com.alfray.mandelbrot.tiles;

import com.alfray.mandelbrot.NativeMandel;

import android.graphics.Bitmap;


public class Tile {
    
    public final static int SIZE = 256;
    public final static int FP8_1 = 256;
    public final static int FP8_E = 8;
    
    private static int[] sTempBlock = new int[SIZE * SIZE];
    private static short[] sColorMap = null;
    
    private final int mZoomFp8;
    private final int mI;
    private final int mJ;
    private short[] m565;
    private int mNativePtr;
    private final int mMaxIter;


    public Tile(int zoomFp8, int i, int j, int maxIter) {
        mZoomFp8 = zoomFp8;
        mI = i;
        mJ = j;
        mMaxIter = maxIter;
        mNativePtr = 0;
        m565 = null;
        
        // TODO -- HACK something better
        createColorMap(maxIter);
    }
    
    public void dispose() {
        
    }
    
    public boolean isReady() {
        return m565 != null;
    }
    
    public short[] getRgb565() {
        return m565;
    }
    
    public int getX() {
        return mI * SIZE;
    }
    
    public int getY() {
        return mJ * SIZE;
    }
    
    public void compute() {
        if (m565 == null) {
            float zoom = 256.0f / mZoomFp8;
            float x = (float)mI * zoom;
            float y = (float)mJ * zoom;
            float step = 1.0f / mZoomFp8;

            final int n = sTempBlock.length; 

            NativeMandel.mandelbrot(
                        x, step,
                        y, step,
                        SIZE, SIZE,
                        mMaxIter,
                        n, sTempBlock);
            
            short[] rgb565 = new short[SIZE * SIZE];
            for (int k = 0; k < n; ++k) {
                rgb565[k] = sColorMap[sTempBlock[k]];
            }
            
            m565 = rgb565;
        }
    }

    //-------

    private void createColorMap(int max_iter) {
        if (sColorMap == null || sColorMap.length != max_iter) {
            sColorMap = new short[max_iter + 1];
            for(int i = 0; i <= max_iter; i++) {
                sColorMap[i] = colorIndex565(i, max_iter);
            }
        }
    }

    // We'll hack a quick fixed palette with a gradient.
    // TODO Make palette customizable
    // color is ARGB with A=FF
    // we'll do B=0x80 => 0xFF
    // and RG=0x00 => 0xFF
    /*
    private int colorIndex(int iter, int max_iter) {
        float col_factor1 = 255.0f / max_iter;
        float col_factor2 = 223.0f * 2 / max_iter; // 0xFF-0x20=xDF=255-32=223
        int rg, b;
        if (iter >= max_iter) {
            rg = b = 0;
        } else  {
            rg = (int)(iter * col_factor1);
            b  = (int)(0x20 + iter * col_factor2);
        }
        return 0xFF000000 | (rg << 16) | (rg << 8) | (b);
    }
    */

    // We'll hack a quick fixed palette with a gradient.
    // TODO Make palette customizable
    // color is R5/G6/B6 with:
    // we'll do B=0x80 => 0x1F  (e.g. FF>>3)
    // and R=0x00 => 0x1F       (e.g. FF>>3)
    // and G=0x00 => 0x3F       (e.g. FF>>2)
    private short colorIndex565(int iter, int max_iter) {
        float col_factor1 = 255.0f / max_iter;
        float col_factor2 = 223.0f * 2 / max_iter; // 0xFF-0x20=xDF=255-32=223
        int rg, b;
        if (iter >= max_iter) {
            rg = b = 0;
        } else  {
            rg = (int)(iter * col_factor1);
            b  = (int)(0x20 + iter * col_factor2);
        }
        int rgb = (b >> 3) | ((rg & 0x0FC) << (5-2)) | ((rg & 0x0F8) << (5+6-3));
        short s = (short) (rgb & 0x07FFF);
        s -= (rgb & 0x08000); // fit the unsigned number in a signed short
        
        return s;
    }
}
