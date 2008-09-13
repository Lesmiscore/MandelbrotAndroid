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
    public final static int FP8_1 = 256;
    public final static int FP8_E = 8;
    
    private static int[] sTempBlock = new int[SIZE * SIZE];
    private static int[] sColorMap = null;
    
    private final int mZoomFp8;
    private final int mI;
    private final int mJ;
    private final String mHashKey;

    private Bitmap mBitmap;
    private int mNativePtr;
    private final int mMaxIter;
    private int mViewX;
    private int mViewY;

    public Tile(String key, int zoomFp8, int i, int j, int maxIter) {
        mHashKey = key;
        mZoomFp8 = zoomFp8;
        mMaxIter = maxIter;
        mI = i;
        mJ = j;
        mNativePtr = 0;
        mBitmap = null;
        mViewX = 0;
        mViewY = 0;
        
        // TODO -- HACK something better
        createColorMap(maxIter);
    }

    public Tile(int zoomFp8, int i, int j, int maxIter) {
        this(computeKey(zoomFp8, i, j, maxIter), zoomFp8, i, j, maxIter);
    }

    // The key is a combination of zoom+i+j+maxiter
    public static String computeKey(int zoomFp8, int i, int j, int maxIter) {
        return String.format("(%d,%d)x%d-%d", i, j, zoomFp8, maxIter);
    }

    @Override
    public int hashCode() {
        return mHashKey.hashCode();
    }
    
    @Override
    public boolean equals(Object o) {
        return (o instanceof Tile) && ((Tile) o).mHashKey.equals(mHashKey);
    }
    
    @Override
    public String toString() {
        return String.format("Tile: %s {%dx%d}", mHashKey, mViewX, mViewY);
    }
    
    public void dispose() {
        
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
    
    public int getViewX() {
        return mViewX;
    }
    
    public int getViewY() {
        return mViewY;
    }
    
    public void setViewXY(int x, int y) {
        mViewX = x;
        mViewY = y;
    }
    
    public void compute() {
        if (mBitmap == null) {
            float zoom = (float)FP8_1 / mZoomFp8;
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
                
            for (int k = 0; k < n; ++k) {
                sTempBlock[k] = sColorMap[sTempBlock[k]];
            }
            
            Bitmap bmp = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.RGB_565);
            bmp.setPixels(sTempBlock, 0, SIZE, 0, 0, SIZE, SIZE);
            mBitmap = bmp;
        }
    }

    //-------

    private void createColorMap(int max_iter) {
        if (sColorMap == null || sColorMap.length != max_iter) {
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
