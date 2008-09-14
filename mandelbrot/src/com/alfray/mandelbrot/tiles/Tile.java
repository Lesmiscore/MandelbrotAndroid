/*
 * (c) ralfoide gmail com, 2008
 * Project: Mandelbrot
 * License TBD
 */


package com.alfray.mandelbrot.tiles;

import com.alfray.mandelbrot.NativeMandel;

import android.graphics.Bitmap;


public class Tile {
    
    public final static int SIZE = 128;
    private final static int FP8_1 = 128;
    
    private static int[] sTempBlock = new int[SIZE * SIZE];
    private static int[] sTempColor = new int[SIZE * SIZE];
    private static int[] sColorMap = null;
    
    private final int mZoomLevel;
    private final int mI;
    private final int mJ;
    private final int mHashKey;

    private Bitmap mBitmap;
    @SuppressWarnings("unused") private int mNativePtr;
    private final int mMaxIter;

    public Tile(int key, int zoomLevel, int i, int j, int maxIter) {
        mHashKey = key;
        mZoomLevel = zoomLevel;
        mMaxIter = maxIter;
        mI = i;
        mJ = j;
        mNativePtr = 0;
        mBitmap = null;
    }

    public Tile(int zoomLevel, int i, int j, int maxIter) {
        this(computeKey(i, j), zoomLevel, i, j, maxIter);
    }

    // The key is a combination of zoom+i+j+maxiter
    /*
    public static String computeKey(int zoomFp8, int i, int j, int maxIter) {
        return String.format("(%d,%d)x%d-%d", i, j, zoomFp8, maxIter);
    }
    */
    /**
     * Computes hash key with this assumptions:
     * - i..j meaningful 8 bits
     * - maxIter meaningful 8 bits
     * - zoomFp8>>(FP8_E-1) meaningful 8 bits (that is initial zoom is 0.5)
     */
    public static int computeKey(int i, int j) {
    	int h = (i & 0x0FFFF) | ((j & 0x7FFF) << 16);
    	return h;
    }

    @Override
    public int hashCode() {
        return mHashKey;
    }
    
    @Override
    public boolean equals(Object o) {
        return (o instanceof Tile) && ((Tile) o).mHashKey == mHashKey;
    }
    
    @Override
    public String toString() {
        return String.format("%08x", mHashKey);
    }
    
    public void dispose() {
        // pass
    }
    
    public boolean isReady() {
        return mBitmap != null;
    }
    
    public Bitmap getBitmap() {
        return mBitmap;
    }
    
    public int getVirtualX() {
        return mI * SIZE;
    }
    
    public int getVirtualY() {
        return mJ * SIZE;
    }
    
    public int getI() {
        return mI;
    }
    
    public int getJ() {
        return mJ;
    }
    
    public static int getZoomFp8(int zoomLevel) {
    	if (zoomLevel == 0) {
    		return FP8_1 / 2;
    	} else {
    		return FP8_1 * zoomLevel;
    	}
    }

    /** Runs from the TileThread */
    public void compute() {
        if (mBitmap == null) {
        	int zoomFp8 = getZoomFp8(mZoomLevel);
            float inv_zoom = (float)FP8_1 / zoomFp8;
            float x = (float)mI * inv_zoom;
            float y = (float)mJ * inv_zoom;
            float step = inv_zoom / SIZE;


            // Note: currently runs only the single TileThread
            // so it's ok to use static arrays. But it's still
            // very ugly.

            createColorMap(mMaxIter); // ugly...

            final int n = sTempBlock.length; 

            NativeMandel.mandelbrot(
                        x, step,
                        y, step,
                        SIZE, SIZE,
                        mMaxIter,
                        n, sTempBlock);
                
            for (int k = 0; k < n; ++k) {
                sTempColor[k] = sColorMap[sTempBlock[k]];
            }
            
            Bitmap bmp = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.RGB_565);
            bmp.setPixels(sTempColor, 0, SIZE, 0, 0, SIZE, SIZE);
            mBitmap = bmp;
        }
    }

    //-------

    private void createColorMap(int max_iter) {
        if (sColorMap == null || sColorMap.length != max_iter + 1) {
            sColorMap = new int[max_iter + 1];
            for(int i = 0; i <= max_iter; i++) {
                sColorMap[i] = colorIndex(i, max_iter);
            }
        }
    }

    // We'll hack a quick fixed palette with a gradient.
    // TODO Make palette customizable
    // color is ARGB with A=FF
    // we'll do B=0x80 => 0xFF
    // and RG=0x00 => 0xFF
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
}
