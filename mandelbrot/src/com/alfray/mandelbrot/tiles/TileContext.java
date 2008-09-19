/*
 * (c) ralfoide gmail com, 2008
 * Project: Mandelbrot
 * License: GPL version 3 or any later version
 */

package com.alfray.mandelbrot.tiles;

import java.util.Iterator;
import java.util.LinkedList;

import com.alfray.mandelbrot.util.BaseThread;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Bundle;
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
    	-1.77f, 0,
    	-1.25565f, 0.38156f
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

	private int mInterestingPlaceIndex;
	
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

    /** Runs from the UI thread */
    public void resetState(Bundle inState) {
        if (inState == null) {
            mZoomLevel = 0;
            mPanningX  = 0;
            mPanningY  = 0;
        } else {
            mZoomLevel = inState.getInt("mandelbrot.zoom");
            mPanningX  = inState.getInt("mandelbrot.panX");
            mPanningY  = inState.getInt("mandelbrot.panY");

            int nn = inState.getInt("mandelbrot.nbtiles");
            if (nn > 0) {
                if (mVisibleTiles == null || mVisibleTiles.length != nn) {
                    mVisibleTiles = new Tile[nn];
                }
                for (int k = 0; k < nn; k++) {
                    int[] a = inState.getIntArray(String.format("mandelbrot.tile_%02d", k));
                    if (a != null) {
                        Tile t = new Tile(a);
                        mVisibleTiles[k] = t;
                        cacheTile(t);
                    }
                }
            }
            
        }
        updateMaxIter();
        updateCaption();
        updateAll(false /*force*/);
        invalidateView();
    }


    /** Runs from the UI thread */
    public void saveState(Bundle outState) {
        outState.putInt("mandelbrot.zoom", mZoomLevel);
        outState.putInt("mandelbrot.panX", mPanningX);
        outState.putInt("mandelbrot.panY", mPanningY);
        
        // we're note going to save all tiles since this is just for the
        // transient state save (i.e. the activity is momentarily paused
        // because another one has precedence.) However in this case to
        // restore the activity quickly it would be nice to have all the
        // *current* visible tiles saved.
        
        int nn = mVisibleTiles.length;
        outState.putInt("mandelbrot.nbtiles", nn);
        for (int k = 0; k < nn; k++) {
            Tile t = mVisibleTiles[k];
            if (t != null) {
                outState.putIntArray(String.format("mandelbrot.tile_%02d", k),
                        t.serialize());
            }
        }
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

    public void panToInterestingPlace() {
        int index = mInterestingPlaceIndex;
        
        float zoom = (float)Tile.getZoomFp8(mZoomLevel);
        float x = sInterestingPlaces[index++] * zoom;
        float y = sInterestingPlaces[index++] * zoom;
        mPanningX = 0 - (int)x;
        mPanningY = 0 - (int)y;
        updateCaption();
        updateAll(true /*force*/);
        invalidateView();
        
        mInterestingPlaceIndex = index == sInterestingPlaces.length ? 0 : index;
    }
    
    public void zoom(boolean zoom_in) {
        if (zoom_in) {
            changeZoomBy(1);
        } else {
            changeZoomBy(-1);
        }
    }

	/**
	 * Constructs a new ImageGenThread that can generated a new image.
	 *
	 * @param width  The width in pixels of the image to generate. Use 0 for the view size.
	 * @param height The height in pixels of the image to generate. Use 0 for the view size.
	 * @param activity The activity on which to run the callback (in the UI thread)
	 * @param callback If non-null, this runnable will run once the generation is
	 *        complete, whether the actual image generation succeeded or not.
	 */
    public ImageGenerator newImageGenerator(int sx, int sy, Activity activity, Runnable callback) {
    	return new ImageGenerator(sx, sy, activity, callback);
    }
    
    /**
     * A thread that knows how to generate an image of the current view.
     * 
	 * This is similar to updateAll except that it is run from
	 * a different thread. That means we must prevent stuff like
	 * panning or zooming.
     */
	public class ImageGenerator extends BaseThread {
		
		private final int mWidth;
		private final int mHeight;
		private final Activity mActivity;
		private final Runnable mCallback;
		private Bitmap mBitmap;
		private LinkedList<Tile> mTiles;
		private int mX1;
		private int mY1;
		private Bitmap mDestBmp;
		private Canvas mCanvas;

		/**
		 * Constructs a new ImageGenThread that can generated a new image.
		 *
		 * @param width  The width in pixels of the image to generate. Use 0 for the view size.
		 * @param height The height in pixels of the image to generate. Use 0 for the view size.
		 * @param runOnCompletion If non-null, this runnable will run once the generation is
		 *        complete, whether the actual image generation succeeded or not.
		 */
		public ImageGenerator(int width, int height, Activity activity, Runnable callback) {
			super("ImageGenThread");
			mWidth  = width;
			mHeight = height;
			mActivity = activity;
			mCallback = callback;
			mContinue = true;
		}

		/**
		 * Returns the computed bitmap.
		 * Null as long as the image as not been successfully completed.
		 */
		public Bitmap getBitmap() {
			return mBitmap;
		}


		@Override
		public void clear() {
		}

		@Override
		protected void startRun() {
			int sx = mWidth <= 0 ? mViewWidth : mWidth;
			int sy = mHeight <= 0 ? mViewHeight : mHeight;

			logd("Generating Image %d,%d", sx, sy);
			
			mDestBmp = Bitmap.createBitmap(sx, sy, Tile.BMP_CONFIG);
			mCanvas = new Canvas(mDestBmp);
			
			int sx2 = sx / 2;
			int sy2 = sy / 2;

			final int SZ = Tile.SIZE;

			mTiles = new LinkedList<Tile>();
			synchronized(mZoomLock) {
		        // boundaries in the virtual-screen space
		        mX1 = -mPanningX - sx2;
		        mY1 = -mPanningY - sy2;
		        
		        int x2 = -mPanningX + sx2;
		        int y2 = -mPanningY + sy2;
	
		        int i = ij_for_xy(mX1);
		        int j = ij_for_xy(mY1);
	
		        int xs = xy_for_ij(i);
		        int ys = xy_for_ij(j);
	
		        // get the list of tiles we need
		        for (int y = ys; y < y2; y += SZ, j++) {
		            for (int i1 = i, x = xs; x < x2; x += SZ, i1++) {
		                Tile t = requestTile(i1, j);
		                mTiles.add(t);
		            }
		        }
			}
		}

		/**
		 * Transfer all completed tiles to the destination bitmap.
		 * Loop whilst tiles are not completed (they are built asynchronously.
		 */
		@Override
		protected void runIteration() {

			for (Iterator<Tile> it = mTiles.iterator();
					it.hasNext();
					) {
				Tile t = it.next();
				if (!t.isReady()) continue;

				// if a tile is ready, remove it from the list and blit it
				// into the dest bitmap
				it.remove();

				Bitmap bmp = t.getBitmap();
				if (bmp == null) continue; // should not happen
				
				int x = t.getVirtualX() - mX1;
				int y = t.getVirtualY() - mY1;
				mCanvas.drawBitmap(bmp, x, y, null /*paint*/);
				
				logd("ImageGen: apply tile %d,%d", x, y);
			}
			
			if (mTiles.size() == 0) {
				// job completed! set the final bitmap
				mBitmap = mDestBmp;
				logd("ImageGen: completed.");
				setCompleted();
			} else {
				// Wait a bit for the remaining tiles to complete.
				// The 10 milliseconds per tile should be optimistic.
				logd("ImageGen: Waiting for %d tiles", mTiles.size());
				waitFor(mTiles.size() * 10 /*ms*/);
			}
		}

		@Override
		protected void endRun() {
			if (mActivity != null && mCallback != null) {
				logd("ImageGen: run completion.");
				mActivity.runOnUiThread(mCallback);
			}
		}
	}

    //----
    
	private void logd(String format, Object...args) {
        Log.d(TAG, String.format(format, args));
    }

    /**
     * Runs from the UI thread.
     * This means stuff like panning or zoom cannot change while this executes.  
     */
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

        int x2 = -mPanningX + sx2;
        int y2 = -mPanningY + sy2;

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
    	TileCache cache;
		synchronized (mLevelTileCaches) {
			cache = mLevelTileCaches.get(mZoomLevel);
			if (cache == null) {
    			mLevelTileCaches.put(mZoomLevel, cache = new TileCache());
    		}
    	}

    	int key = Tile.computeKey(i, j);
        Tile t = cache.get(key);

        if (t == null) {
            t = new Tile(key, mZoomLevel, i, j, mMaxIter);
            cache.put(key, t);
        }
        if (!t.isReady()) mTileThread.schedule(t);

        return t;
    }

    /** Runs from the UI thread. Called when restoring state. */
    private void cacheTile(Tile t) {
    	TileCache cache;
		synchronized (mLevelTileCaches) {
			cache = mLevelTileCaches.get(mZoomLevel);
			if (cache == null) {
				mLevelTileCaches.put(mZoomLevel, cache = new TileCache());
			}
		}
        cache.put(t.hashCode(), t);
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
}
