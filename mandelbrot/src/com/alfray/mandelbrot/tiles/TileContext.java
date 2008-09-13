/*
 * (c) ralfoide gmail com, 2008
 * Project: Mandelbrot
 * License TBD
 */

package com.alfray.mandelbrot.tiles;

import java.util.HashMap;

import android.util.Log;

public class TileContext {
    
    private static final String TAG = "TileContext";
    
    private int mZoomFp8;
    private HashMap<String, Tile> mTileCache;
    private int mViewWidth;
    private int mViewHeight;
    private Tile[] mVisibleTiles;
    private TileView mTileView;
    private int mMaxIter;
    private boolean mViewNeedsInvalidate;
    private TileThread mTileThread;

    public TileContext() {
        if (mTileThread == null) {
            mTileThread = new TileThread();
            mTileThread.setCompletedCallback(new TileCompletedCallback());
            mTileThread.start();
        }
    }

    public void resetScreen() {
        mZoomFp8 = Tile.FP8_1;
        mMaxIter = 17;
    }
    
    /** Runs from the UI thread */
    public Tile[] getVisibleTiles() {
        return mVisibleTiles;
    }

    public void logd(String format, Object...args) {
        Log.d(TAG, String.format(format, args));
    }

    /** Runs from the UI thread */
    public void onSizeChanged(int viewWidth, int viewHeight) {
        logd("onSizeChanged: %dx%d", viewWidth, viewHeight);

        mViewWidth = viewWidth;
        mViewHeight = viewHeight;
        
        initVisible();
    }
    
    /** Runs from the UI (activity) thread */
    public void setView(TileView tileView) {
        mTileView = tileView;
        if (tileView != null && mViewNeedsInvalidate) {
            invalidateView();
        }
        if (mTileThread != null) {
            logd("Pause TileThread");
            mTileThread.pauseThread(tileView == null);
        }
    }

    /** Runs from the UI thread */
    private void initVisible() {
        final int w = mViewWidth;
        final int h = mViewHeight;
        final int SZ = Tile.SIZE;
        
        int nx = 1;//-- (w / SZ) + 1;
        int ny = 1;//-- (h / SZ) + 1;
        mVisibleTiles = new Tile[nx * ny];
        
        int px_left = (w - nx * SZ) / 2;
        int py_top  = (h - ny * SZ) / 2;
        int ti_left = 0 ;//-- - (w/2 - SZ/2 + SZ) / SZ;
        int tj_top  = 0 ;//-- - (h/2 - SZ/2 + SZ) / SZ;
        
        logd("Init tiles: %dx%d(%d), left/top=%dx%d (%dx%d)",
                nx, ny, mVisibleTiles.length,
                ti_left, tj_top, px_left, py_top);
        
        for (int k = 0, j = 0; j < ny; j++, tj_top++, py_top += SZ) {
            int ti = ti_left;
            int px = px_left;
            for (int i = 0; i < nx; k++, i++, ti++, px += SZ) {
                Tile t = requestTile(ti, tj_top);
                t.setViewXY(px, py_top);
                mVisibleTiles[k] = t;
            }
        }
        
        invalidateView();
    }

    /** Runs from the UI thread */
    private Tile requestTile(int i, int j) {
        if (mTileCache == null) mTileCache = new HashMap<String, Tile>();

        String key = Tile.computeKey(mZoomFp8, i, j, mMaxIter);
        Tile t = mTileCache.get(key);
        
        if (t == null) {
            t = new Tile(key, mZoomFp8, i, j, mMaxIter);
            mTileCache.put(key, t);

            mTileThread.schedule(t);
        }
        
        return t;
    }

    /** Runs from the UI thread */
    private void invalidateView() {
        if (mTileView != null) {
            mViewNeedsInvalidate = false;
            mTileView.postInvalidate();
        } else {
            mViewNeedsInvalidate = true;
        }
    }

    /** Runs from the UI thread or TileThread */
    public void invalidateTile(Tile tile) {
        if (tile == null) return;
        logd("Invalidate " + tile.toString());
        if (mTileView != null) {
            mViewNeedsInvalidate = false;
            final int SZ = Tile.SIZE;
            final int x = tile.getViewX();
            final int y = tile.getViewY();
            mTileView.postInvalidate(x, y, x + SZ, y + SZ);
        } else {
            mViewNeedsInvalidate = true;
        }
    }

    /** Runs from the TileThread */
    private class TileCompletedCallback implements ITileCompleted {
        public void onTileCompleted(Tile tile) {
            invalidateTile(tile);
        }
    }
}
