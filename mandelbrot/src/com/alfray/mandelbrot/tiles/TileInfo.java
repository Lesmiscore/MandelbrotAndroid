/*
 * (c) ralfoide gmail com, 2008
 * Project: Mandelbrot
 * License TBD
 */

package com.alfray.mandelbrot.tiles;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Paint.Style;

/**
 * @deprecated
 */
public class TileInfo {

    private final int mI;
    private final int mJ;
    private final float mMbSize;
    private final int mPxSize;
    private boolean mDirty;

    private Bitmap mBitmap;
    private Canvas mCanvas;
    private Paint mPaint;
    private Rect mRect;

    /** Number of passes for the multi-pass algorithm */
    private static final int NUM_PASSES = 6;

    private int mStateMaxIter = 20;
    private int mStateStartColumn = 0;
    private int mStateStartLine = 0;
    private int mStateMaxPass = NUM_PASSES;
    private int mStateStartPass = NUM_PASSES;
    private boolean mDone;

    public TileInfo(int i, int j, float mbSize, int pxSize) {
        mI = i;
        mJ = j;
        mMbSize = mbSize;
        mPxSize = pxSize;
        mDirty = true;
        mDone = false;
        
        mBitmap = Bitmap.createBitmap(pxSize, pxSize, Bitmap.Config.RGB_565);
    }
    
    public boolean loadFromDisk() {
        return false;
    }
    
    public boolean saveToDisk() {
        return false;
    }

    // ----------

    /**
     * Computes the Mandelbrot set using a multiple pass method.
     * For a step N > 1, computes:
     * 0,0 for first pass and fills a N*N square
     * 1,1 and fills a N/2 * N/2 square
     * 1,0 ''
     * 0,1 ''
     * N is 1 << pass_number so f.ex. if we start at pass 8, we fill
     * a 256x256 square and at pass 1 we fill a 2x2 square. Since we do
     * always four quadrants there's no pass 0.
     */
    public void run() {
        mCanvas = new Canvas(mBitmap);
        mPaint = new Paint();
        mPaint.setStyle(Style.FILL);

        int numLines = 0;
        boolean mContinue = true;
        
        int sx = mPxSize;
        int sy = mPxSize;
        int max_iter = mStateMaxIter;
        float x0 = mMbSize * mI;
        float sx1 = mMbSize / mPxSize;
        float sy1 = sx1;  // Assumes pixels are square
        float y0 = mMbSize * mJ;
        int i1 = mStateStartColumn;
        int j1 = mStateStartLine;
        int pn = mStateMaxPass;
        int pc = mStateStartPass;
        for ( ; pc > 0 && mContinue && numLines > 0; pc--) {
            int n = 1 << pc;
            int n2 = n >> 1;
            boolean is_firt_pass = (pn == pc);
            mRect = new Rect(0, 0, n2, n2);

            for (int j2 = j1 + n2; j1 < sy && mContinue; j1 += n, j2 += n) {
                float y1 = y0 + j1 * sy1;
                float y2 = y0 + j2 * sy1;
                for (int i2 = i1 + n2; i1 < sx && mContinue; i1 += n, i2 += n) {
                    float x1 = x0 + i1 * sx1;
                    float x2 = x0 + i2 * sx1;
                    if (is_firt_pass) square(x1, y1, i1, j1, max_iter, n2);
                    square(x2, y1, i2, j1, max_iter, n2);
                    square(x1, y2, i1, j2, max_iter, n2);
                    square(x2, y2, i2, j2, max_iter, n2);
                } // for i
                if (mContinue) {
                    i1 = 0;
                    numLines--;
                }
            } // for j
            if (mContinue && numLines > 0) j1 = 0;
        } // for pc
        mStateStartColumn = i1;
        mStateStartLine = j1;
        mStateStartPass = pc;
        mDone = (mContinue && numLines > 0);
    }

    private void square(float x, float y, int i, int j, int max_iter, int n) {
        int c = computePoint(x, y, max_iter);
        c = colorIndex(c, max_iter);
        
        if (n > 1) {
            mRect.offsetTo(i, j);
            mPaint.setColor(c);
            mCanvas.drawRect(mRect, mPaint);
        } else {
            mBitmap.setPixel(i, j, c);
        }
    }

    /**
     * Computes a given Mandelbrot point.
     */
    private int computePoint(float x0, float y0, int max_iter) {
        float x = x0;
        float y = y0;
        float x2 = x * x;
        float y2 = y * y;
        int iter = 0;
        while (x2 + y2 < 4 && iter < max_iter) {
            float xtemp = x2 - y2 + x0;
            y = 2 * x * y + y0;
            x = xtemp;
            x2 = x * x;
            y2 = y * y;
            ++iter;
        }
        
        return iter;
    }

    // We'll hack a quick fixed palette with a gradient.
    // TODO Make palette customizable
    // color is ARGB with A=FF
    // we'll do B=0x80 => 0xFF
    // and RG=0x00 => 0xFF
    private int colorIndex(int iter, int max_iter) {
        float col_factor1 = 255.0f / max_iter;
        float col_factor2 = 223.0f * 2/ max_iter; // 0xFF-0x20=xDF=255-32=223
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
