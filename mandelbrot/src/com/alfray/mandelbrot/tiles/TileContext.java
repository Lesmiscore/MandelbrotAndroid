/*
 * (c) ralfoide gmail com, 2008
 * Project: Mandelbrot
 * License TBD
 */

package com.alfray.mandelbrot.tiles;

import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.util.SparseArray;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;
import android.widget.ZoomControls;

public class TileContext {
    
    private static final String TAG = "TileContext";

    private static final int ZOOM_HIDE_DELAY_MS = 3000;

    private static final float sInterestingPlaces[] = {
    	-1.77f, 0
    };
    
    private static class TileCache extends SparseArray<Tile> {
    }
    
    private int mZoomLevel;
    private int mViewWidth;
    private int mViewHeight;
    private int mPanningX;
    private int mPanningY;
    private SparseArray<TileCache> mLevelTileCaches;
    private Tile[] mVisibleTiles;

    private TileView mTileView;
    private ZoomControls mZoomer;
    private Handler mHandler;
    private TileThread mTileThread;

    /** lock to synchronize on zoom level change between tile thread and context */
    private Object mZoomLock = new Object();
    
    private int mMaxIter;
    private boolean mViewNeedsInvalidate;
    
    private int mMiddleX;
    private int mMiddleY;

	private int mCurrentI;
	private int mCurrentJ;

    private long mHideZoomAfterMs;
    private HideZoomRunnable mHideZoomRunnable;
	private TextView mTextView;
	private boolean mNeedUpdateCaption;
	private UpdateCaptionRunnable mUpdateCaptionRunnable;

    public TileContext() {

    	mLevelTileCaches = new SparseArray<TileCache>(16);
    	
        if (mTileThread == null) {
            mTileThread = new TileThread();
            mTileThread.setCompletedCallback(new TileCompletedCallback());
            mTileThread.start();
        }
        
        mHandler = new Handler();
        mHideZoomRunnable = new HideZoomRunnable();
        mUpdateCaptionRunnable = new UpdateCaptionRunnable();
    }

    public void resetScreen() {
        mZoomLevel = 0;
        updateMaxIter();
        mPanningX = 0;
        mPanningY = 0;
        updateCaption();
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
	public void setText(TextView textView) {
		mTextView = textView;
	}

    /** Runs from the UI (activity) thread */
    public void setZoomer(ZoomControls zoomer) {
        mZoomer = zoomer;
        if (zoomer != null) {
            changeZoomBy(0);
            showZoomer(true /*force*/);

            zoomer.setOnZoomInClickListener(new OnClickListener() {
				public void onClick(View v) {
					changeZoomBy(1);
				}
            });

            zoomer.setOnZoomOutClickListener(new OnClickListener() {
				public void onClick(View v) {
					changeZoomBy(-1);
				}
            });
        }
    }
    
    /** Runs from the UI (activity) thread */
    public void setView(TileView tileView) {
        mTileView = tileView;
        if (tileView != null && mViewNeedsInvalidate) {
            invalidateView();
        }
    }

    /** Runs from the UI (activity) thread */
	public void pause(boolean shouldPause) {
        if (mTileThread != null) {
            logd("Pause TileThread: %s", shouldPause ? "yes" : "no");
            mTileThread.pauseThread(shouldPause);
        }
        runUpdateCaption(false);
	}

    /** Runs from the UI thread */
    public void onPanTo(int x, int y) {
        if (x != mPanningX || y != mPanningY) {
            mPanningX = x;
            mPanningY = y;
            updateAll(false /*force*/);
            invalidateView();
            updateCaption();
        }
    }

    /** Runs from the UI thread */
	public void onPanStarted() {
        showZoomer(false /*force*/);
        runUpdateCaption(true);
	}

    /** Runs from the UI thread */
	public void onPanFinished() {
        runUpdateCaption(false);
	}

    /** Runs from the UI thread */
	public boolean onKeyDown(KeyEvent event) {
		switch(event.getKeyCode()) {
		case KeyEvent.KEYCODE_PLUS:
		case KeyEvent.KEYCODE_I:
			changeZoomBy(1);
			break;
		case KeyEvent.KEYCODE_MINUS:
		case KeyEvent.KEYCODE_O:
			changeZoomBy(-1);
			break;
		case KeyEvent.KEYCODE_S:
			panToInterestingPlace();
			break;
		default:
			return false;
		}
		return true;
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
        
        if (!force && mCurrentI == i && mCurrentJ == j) {
    		return;
        }
    	mCurrentI = i;
    	mCurrentJ = j;

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
    	TileCache cache = mLevelTileCaches.get(mZoomLevel);
    	if (cache == null) mLevelTileCaches.put(mZoomLevel, cache = new TileCache());

    	int key = Tile.computeKey(i, j);
        Tile t = cache.get(key);

        if (t == null) {
            t = new Tile(key, mZoomLevel, i, j, mMaxIter);
            cache.put(key, t);

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
        	// the callback may be fired just after a zoom level change, in which case
        	// we'll ignore the update. however it cannot happen during a zoom change.
        	synchronized(mZoomLock) {
	            if (mZoomLevel == tile.getZoomLevel()) {
		            invalidateTile(tile);
	
		            // do we want the mirror?
		            int mirrorKey = tile.computeMirrorKey();
		            TileCache cache = mLevelTileCaches.get(mZoomLevel);
		            if (cache != null) {
			            Tile mirror = cache.get(mirrorKey);
			            if (mirror != null && !mirror.isReady()) {
			            	mirror.fromMirror(tile);
				            invalidateTile(mirror);
			            }
		            }
	            }
        	}
        }
    }

    /**
     * Change zoom.
     *
     * @param delta 1 for zoom in, -1 for zoom out, 0 for no zooming
     */
    private void changeZoomBy(int delta) {
    	if (delta != 0) {
    		int oldZoomLevel = mZoomLevel;
	        if (delta > 0) {
	        	// zoom in by 1 (i.e. x2)
	        	synchronized(mZoomLock) {
	        		if (mZoomLevel == 0) {
	        			mZoomLevel = 1;
	        		} else {
	        			mZoomLevel *= 2;
	        		}
	        	}
	        } else if (delta < 0 && mZoomLevel > 0) {
	        	// zoom out by 1 (i.e. x0.5)
	        	synchronized(mZoomLock) {
	        		if (mZoomLevel > 1) {
	        			mZoomLevel /= 2;
	        		} else {
	        			mZoomLevel = 0;
	        		}
	        	}
	        }
	        if (mZoomLevel != oldZoomLevel) {
	        	float oldZoom = Tile.getZoomFp8(oldZoomLevel);
	        	float newZoom = Tile.getZoomFp8(mZoomLevel);
	        	float factor = newZoom / oldZoom;
	        	mPanningX *= factor;
	        	mPanningY *= factor;
	        	// clear the tile thread pending queue when changing levels
	    		if (mTileThread != null) {
	    			mTileThread.clear();
	    		}
	        	updateMaxIter();
	            updateCaption();
	        	updateAll(true /*force*/);
	    		invalidateView();
	        }
    	}

        if (mZoomer != null) {
            mZoomer.setIsZoomOutEnabled(mZoomLevel > 0);
        }
    }

	private void updateMaxIter() {
        // Dynamically adapt the number of iterations to the width:
        // width 3..1 => 20 iter
        // width 0.1 => 60 iter
        // width 0.01 => 120
        // int max_iter = Math.max(mPrefMinIter, (int)(mPrefStepIter * Math.log10(1.0 / w)));

		mMaxIter = 15 + (int)(10*Math.log1p(mZoomLevel));
	}

	private void showZoomer(boolean force) {
		if (force || mZoomer.getVisibility() != View.VISIBLE) {
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

    /** This MUST be used from the UI thread */
    private void setTextCaption(String format, Object...args) {
    	if (mTextView != null) {
    		String s = String.format(format, args);
    		mTextView.setText(s);
    	}
    }
    
    /** This MUST be used from the UI thread */
    private void updateCaption() {
    	if (!mNeedUpdateCaption) {
    		mUpdateCaptionRunnable.run();
    	}
    }
    
    private void runUpdateCaption(boolean run) {
    	boolean start = run && !mNeedUpdateCaption;
		mNeedUpdateCaption = run;
		if (start) mHandler.post(mUpdateCaptionRunnable);
    }
    
    private class UpdateCaptionRunnable implements Runnable {
		public void run() {
	    	float zoom = 0-(float)Tile.getZoomFp8(mZoomLevel);
	    	setTextCaption("x%1$d, Iter:%2$d, c:%3$.5f, %4$.5f, ",
	    			mZoomLevel,
	    			mMaxIter,
	    			mPanningX / zoom,
	    			mPanningY / zoom
				);
	    	if (mNeedUpdateCaption && mHandler != null) {
	    		mHandler.post(mUpdateCaptionRunnable);
	    	}
		}
    }

    private void panToInterestingPlace() {
		// TODO use x=places[mCurrentInterestingPlaceIndex++] ,y=[index++]
    	// and wrap at places.length

    	float zoom = (float)Tile.getZoomFp8(mZoomLevel);
    	float x = sInterestingPlaces[0] * zoom;
		float y = sInterestingPlaces[1] * zoom;
		mPanningX = 0 - (int)x;
		mPanningY = 0 - (int)y;
		updateCaption();
		updateAll(true /*force*/);
		invalidateView();
	}
}
