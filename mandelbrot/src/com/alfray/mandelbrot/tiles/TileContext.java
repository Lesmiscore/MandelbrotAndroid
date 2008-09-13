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
    private int mPanningX;
    private int mPanningY;
    private Tile[] mVisibleTiles;
    private TileView mTileView;
    private int mMaxIter;
    private boolean mViewNeedsInvalidate;
    private TileThread mTileThread;

    private int mMiddleX;

    private int mMiddleY;

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
        mPanningX = 0;
        mPanningY = 0;
    }
    
    /** Runs from the UI thread */
    public Tile[] getVisibleTiles() {
        return mVisibleTiles;
    }
    
    public int getOffsetX() {
        return mMiddleX + mPanningX;
    }
    
    public int getOffsetY() {
        return mMiddleY + mPanningY;
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
        
        int nx = (w / SZ) + 2;
        int ny = (h / SZ) + 2;
        mVisibleTiles = new Tile[nx * ny];

        int ofx = mMiddleX = w/2;
        int ofy = mMiddleY = h/2;

        int i = ij_for_xy(-ofx);
        int j = ij_for_xy(-ofy);

        logd("Init tiles: %dx%d(%d), left/top=%dx%d (%dx%d)",
                nx, ny, mVisibleTiles.length,
                i,j, -ofx,-ofy);

        int xs = xy_for_ij(i);
        int ys = xy_for_ij(j);
        
        for (int k = 0, y = ys; y < ofy; y += SZ, j++) {
            for (int i1 = i, x = xs; x < ofx; x += SZ, i1++, k++) {
                Tile t = requestTile(i1, j);
                mVisibleTiles[k] = t;
            }
        }
        
        invalidateView();
    }
    
    private int xy_for_ij(int ij) {
        return ij * Tile.SIZE;
    }

    private int ij_for_xy(int xy) {
        boolean neg = (xy < 0);
        if (neg) xy = -xy;
        int ij = xy / Tile.SIZE;
        return neg ? -ij-1 : ij;
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
    private void invalidateTile(Tile tile) {
        if (tile == null) return;
        if (mTileView != null) {
            mViewNeedsInvalidate = false;
            final int SZ = Tile.SIZE;
            int x = tile.getVirtualX() + mMiddleX + mPanningX;
            int y = tile.getVirtualY() + mMiddleY + mPanningY;
            logd("Invalidate %s @ (%d,%d)", tile.toString(), x, y);
            int x1 = x + SZ;
            int y1 = y + SZ;
            if (x < 0) x = 0;
            if (y < 0) y = 0;
            mTileView.postInvalidate(x, y, x1, y1);
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
