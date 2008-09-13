/*
 * (c) ralfoide gmail com, 2008
 * Project: Mandelbrot
 * License TBD
 */

package com.alfray.mandelbrot.tiles;

import java.util.HashMap;

import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.widget.ZoomControls;

public class TileContext {
    
    private static final String TAG = "TileContext";

    private static final int MIN_ZOOM_FP8 = Tile.FP8_1 / 2;
    
    private static final int ZOOM_HIDE_DELAY_MS = 3000;
    
    private int mZoomFp8;
    private int mViewWidth;
    private int mViewHeight;
    private int mPanningX;
    private int mPanningY;
    private HashMap<String, Tile> mTileCache = new HashMap<String, Tile>();
    private Tile[] mVisibleTiles;
    private TileView mTileView;
    private int mMaxIter;
    private boolean mViewNeedsInvalidate;
    private TileThread mTileThread;

    private int mMiddleX;

    private int mMiddleY;

	private int mCurrentI;

	private int mCurrentJ;

    private ZoomControls mZoomer;

    private Handler mHandler;

    private long mHideZoomAfterMs;

    private HideZoomRunnable mHideZoomRunnable;

    public TileContext() {

        if (mTileThread == null) {
            mTileThread = new TileThread();
            mTileThread.setCompletedCallback(new TileCompletedCallback());
            mTileThread.start();
        }
        
        mHandler = new Handler();
        mHideZoomRunnable = new HideZoomRunnable();
    }

    public void resetScreen() {
        mZoomFp8 = MIN_ZOOM_FP8;
        mMaxIter = 17;
        mPanningX = 0;
        mPanningY = 0;
    }
    
    /** Runs from the UI thread */
    public Tile[] getVisibleTiles() {
        return mVisibleTiles;
    }
    
    public int getPanningX() {
        return mPanningX;
    }
    
    public int getPanningY() {
        return mPanningY;
    }
    
    public int getOffsetX() {
        return mMiddleX + mPanningX;
    }
    
    public int getOffsetY() {
        return mMiddleY + mPanningY;
    }

    /** Runs from the UI thread */
    public void onSizeChanged(int viewWidth, int viewHeight) {
        logd("onSizeChanged: %dx%d", viewWidth, viewHeight);

        mViewWidth  = viewWidth;
        mViewHeight = viewHeight;
        
        mMiddleX = viewWidth/2;
        mMiddleY = viewHeight/2;
        
        updateAll(true /*force*/);
        invalidateView();
    }

    /** Runs from the UI (activity) thread */
    public void setZoomer(ZoomControls zoomer) {
        mZoomer = zoomer;
        changeZoomBy(0);
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
    public void onPanTo(int x, int y) {
        if (x != mPanningX || y != mPanningY) {
            mPanningX = x;
            mPanningY = y;
            updateAll(false /*force*/);
            invalidateView();
            changeZoomBy(0); // to display zoomer
        }
    }

    //----
    
    private void logd(String format, Object...args) {
        Log.d(TAG, String.format(format, args));
    }

    /** Runs from the UI thread */
    private void updateAll(boolean force) {
        final int SZ = Tile.SIZE;

        final int nx = (mViewWidth  / SZ) + 2;
        final int ny = (mViewHeight / SZ) + 2;
        final int nn = nx * ny;
        if (mVisibleTiles == null || mVisibleTiles.length != nn) {
        	mVisibleTiles = new Tile[nn];
        	force = true;
        }
        
        final int sx2 = mMiddleX;
        final int sy2 = mMiddleY;

        // boundaries in the virtual-screen space
        int x1 = -mPanningX - sx2;
        int y1 = -mPanningY - sy2;

        int i = ij_for_xy(x1);
        int j = ij_for_xy(y1);
        
        if (!force) {
        	if (mCurrentI == i && mCurrentJ == j) {
        		return;
        	}
        	mCurrentI = i;
        	mCurrentJ = j;
        }

        int xs = xy_for_ij(i);
        int ys = xy_for_ij(j);
        
        logd("UpdateAll: (%d,%d) px(%d,%d)", i, j, xs, ys);

        int x2 = -mPanningX + sx2;
        int y2 = -mPanningY + sy2;

        int k = 0;
        for (int y = ys; y < y2; y += SZ, j++) {
            for (int i1 = i, x = xs; x < x2; x += SZ, i1++, k++) {
                Tile t = requestTile(i1, j);
                mVisibleTiles[k] = t;
            }
        }
        
        for (; k < nn; k++) {
        	mVisibleTiles[k] = null;
        }
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

    private void changeZoomBy(int delta) {
        if (delta != 0) {
            
        }
        if (mZoomer != null) {
            mZoomer.setIsZoomOutEnabled(mZoomFp8 > MIN_ZOOM_FP8);
            mZoomer.show();
            mHideZoomAfterMs = SystemClock.uptimeMillis() + ZOOM_HIDE_DELAY_MS;
            mHandler.postAtTime(mHideZoomRunnable, mHideZoomAfterMs + 10);
        }
    }
    
    private class HideZoomRunnable implements Runnable {
        public void run() {
            if (mZoomer != null && SystemClock.uptimeMillis() >= mHideZoomAfterMs) {
                mZoomer.hide();
            }
        }
        
    }

}
