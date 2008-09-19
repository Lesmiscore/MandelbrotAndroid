/*
 * (c) ralfoide gmail com, 2008
 * Project: Mandelbrot
 * License: GPL version 3 or any later version
 */


package com.alfray.mandelbrot.tiles;

import com.alfray.mandelbrot.NativeMandel;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;


public class Tile {
    
    public final static int SIZE = 128;
    
    public final static Config BMP_CONFIG = Bitmap.Config.RGB_565;

    private final static int FP8_1 = 128;
    private final static int SERIAL_VERSION = 1;
    
    private static int[] sTempBlock = new int[SIZE * SIZE];
    private static int[] sTempColor = new int[SIZE * SIZE];
    private static int[] sTempLine = new int[SIZE];
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
    
    public Tile(int[] serialized) {
        assert serialized.length >= 7;
        assert serialized[0] == SERIAL_VERSION;
        assert serialized[1] == SIZE;
        
        mHashKey = serialized[2];
        mZoomLevel = serialized[3];
        mMaxIter = serialized[4];
        mI = serialized[5];
        mJ = serialized[6];
        
        if (serialized.length > 7) {
            mBitmap = Bitmap.createBitmap(serialized, 7, SIZE, SIZE, SIZE, BMP_CONFIG);
        }
    }

    /** Serialize to int array */
    public int[] serialize() {
        Bitmap bmp = mBitmap;
        int nn = SIZE * SIZE;
        int[] result = new int[7 + (bmp == null ? 0 : nn)];
        result[0] = SERIAL_VERSION;
        result[1] = SIZE;
        result[2] = mHashKey;
        result[3] = mZoomLevel;
        result[4] = mMaxIter;
        result[5] = mI;
        result[6] = mJ;
        if (bmp != null) bmp.getPixels(result, 7, SIZE, 0, 0, SIZE, SIZE);
        return result;
    }
    
    public int getZoomLevel() {
		return mZoomLevel;
	}

    /**
     * Computes hash key with this assumptions:
     * - i..j meaningful 15 bits + sign bit
     * - neither maxIter nor zoom level are considered in the hash.
     * 
     * TileContext keeps a different cache for each zoom level, and maxIter is
     * linked to the zoom level, so neither need to be hashed here.
     * 
     * The sign bit for i is in bit 15. The sign bit for j is in bit 31 (MSB).
     * If j is negative, we count it from "-0" to "-N" (instead of -1..-N).
     * This way, to get the "mirror key" in j we just need to xor bit 31.
     */
    public static int computeKey(int i, int j) {
    	int h = 0;
    	if (j < 0) { 
    		h |= 0x80000000;
    		j = -j-1; 
		}
    	if (i < 0) {
    		h |= 0x00008000;
    		i = -i;
    	}
    	h |= (i & 0x07FFF) | ((j & 0x7FFF) << 16);
    	return h;
    }

    /** Key for mirror in j */
    public int computeMirrorKey() {
    	return mHashKey ^ 0x80000000;
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

    /** Has the bitmap been computed yet? */
    public boolean isReady() {
        return mBitmap != null;
    }

    /** Returns bitmap. Null if isReady()==false */
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
            
            Bitmap bmp = Bitmap.createBitmap(SIZE, SIZE, BMP_CONFIG);
            bmp.setPixels(sTempColor, 0, SIZE, 0, 0, SIZE, SIZE);
            mBitmap = bmp;
        }
    }

    /** Runs from the TileThread */
	public void fromMirror(Tile tile) {
		if (mBitmap == null && tile.mBitmap != null) {
            Bitmap bmp = Bitmap.createBitmap(SIZE, SIZE, BMP_CONFIG);

            tile.mBitmap.getPixels(sTempColor, 0, SIZE, 0, 0, SIZE, SIZE);

            // reverse in Y
            for (int y1 = 0, y2 = SIZE * (SIZE-1); y1 < y2; y1 += SIZE, y2 -= SIZE) {
            	System.arraycopy(sTempColor, y1, sTempLine,   0, SIZE); // y1->temp
            	System.arraycopy(sTempColor, y2, sTempColor, y1, SIZE); // y2->y1
            	System.arraycopy(sTempLine,   0, sTempColor, y2, SIZE); // temp->y2
            }
            
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
