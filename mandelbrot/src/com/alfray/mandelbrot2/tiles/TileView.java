/*
 * Copyright 2008 (c) ralfoide gmail com, 2008
 * Project: Mandelbrot
 * License: GPL version 3 or any later version
 */


package com.alfray.mandelbrot2.tiles;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;

import com.alfray.mandelbrot2.R;

/**
 * Workflow:
 * - created from XML
 * - activity assigns TileContext upon creation (no size yet)
 * - onSizeChanged =>TileContext.onSizeChanged to recompute tiles
 * - onWindowVisibilityChanged => TileContext.onViewLostVisibility to pause
 *    rendering threads
 * - trackball/mouse => change offset for panning
 * -
 */
public class TileView extends View {

    private static final String TAG = "TileView";

    private static boolean DEBUG = false;

    private enum GridMode {
    	NONE,
    	LINES,
    	TEXT
    }

    private TileContext mTileContext;
    private Rect mTempBounds = new Rect();
    private Rect mTempRect = new Rect(0, 0, Tile.SIZE, Tile.SIZE);
    private Drawable mNoTile;
    private Paint mRed;
    private GridMode mGridMode = GridMode.LINES;
    private float mDownX;
    private float mDownY;
    private int mDownOffsetX;
    private int mDownOffsetY;

	private Drawable mLogo;

    public TileView(Context context, AttributeSet attrs) {
        super(context, attrs);

        mNoTile = context.getResources().getDrawable(R.drawable.android_maps_no_tile_128);

        mRed = new Paint();
        mRed.setColor(0xFFFF0000);
        mRed.setStyle(Paint.Style.STROKE);
        mRed.setTextAlign(Paint.Align.CENTER);

        mGridMode = DEBUG ? GridMode.LINES : GridMode.NONE;
    }

    public void setTileContext(TileContext tileContext) {
        mTileContext = tileContext;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (mTileContext != null) mTileContext.onSizeChanged(w, h);
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        if (mTileContext != null) mTileContext.pause(visibility != VISIBLE);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        Tile[] tiles = null;

        if (mTileContext != null) tiles = mTileContext.getVisibleTiles();

        if (tiles == null || tiles.length == 0 || mTileContext == null) {
            // fake display for design-time
        	if (mLogo == null) {
        		mLogo = getContext().getResources().getDrawable(R.drawable.icon);
        	}
            if (mLogo != null) {
                mTempRect.set(0, 0, getWidth(), getHeight());
            	mLogo.setBounds(mTempRect);
            	mLogo.draw(canvas);
            }
            if (mRed != null) {
            	String s = "Custom view: " + this.getClass().getSimpleName();
            	// The layout editor currently doesn't support the "CENTER" alignment
            	// on the Paint.
            	canvas.drawText(s,
            			getWidth()/2,
            			getHeight()/2, mRed);
            }
            return;
        }

        Rect bounds = mTempBounds ;
        boolean useBounds = canvas.getClipBounds(bounds);

        if (DEBUG) {
        	logd("Bounds %s", (useBounds ? bounds.toString() : "no"));
        }

        final int ofx = mTileContext.getOffsetX();
        final int ofy = mTileContext.getOffsetY();

        Rect rect = mTempRect;
        for (Tile t : tiles) {
            if (t == null) continue;

            int x = t.getVirtualX() + ofx;
            int y = t.getVirtualY() + ofy;
            rect.offsetTo(x, y);

            if (useBounds && !Rect.intersects(bounds, rect)) continue;

            Bitmap bmp = t.getBitmap();

            if (bmp != null) {
                canvas.drawBitmap(bmp, x, y, null /*paint*/);
            } else {
                mNoTile.setBounds(rect);
                mNoTile.draw(canvas);
            }

            if (mGridMode != GridMode.NONE) {
                canvas.drawRect(rect, mRed);
            }
            if (mGridMode == GridMode.TEXT) {
                String s = t.toString();
                final int SZ2 = Tile.SIZE / 2;
                canvas.drawText(s, x + SZ2, y + SZ2, mRed);
            }
        }
    }

    @Override
    public boolean onTrackballEvent(MotionEvent event) {
        switch (event.getAction()) {
        case MotionEvent.ACTION_DOWN:
        	// trackball selected... ignore
        	break;
        case MotionEvent.ACTION_UP:
        case MotionEvent.ACTION_CANCEL:
        	// trackball selected... ignore
        case MotionEvent.ACTION_MOVE:
            if (mTileContext != null) {
            	float x = event.getX() * event.getXPrecision();
            	float y = event.getY() * event.getYPrecision();
            	if (DEBUG) logd("Trackback MOVE: getX/Y: %f, %f => %f, %f", event.getX(), event.getY(), x ,y);
            	// trackball events are delta motion
                mDownOffsetX = mTileContext.getPanningX();
                mDownOffsetY = mTileContext.getPanningY();
            	int newOfx = mDownOffsetX + (int)x;
            	int newOfy = mDownOffsetY + (int)y;
                if (DEBUG) {
                	logd("Move: to-of7(%d,%d)", newOfx, newOfy);
                }
                mTileContext.onPanTo(newOfx, newOfy);
                return true;
            }
        }
        return super.onTrackballEvent(event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
        case MotionEvent.ACTION_DOWN:
            if (mTileContext != null) {
            	// touch events are absolute positions
                mDownX = event.getX();
                mDownY = event.getY();
                mDownOffsetX = mTileContext.getPanningX();
                mDownOffsetY = mTileContext.getPanningY();
                if (DEBUG) {
                	logd("Down: down(%.2f,%.2f), of7(%d,%d)", mDownX, mDownY, mDownOffsetX, mDownOffsetY);
                }
                mTileContext.onPanStarted();
            }
            return (mTileContext != null);
        case MotionEvent.ACTION_MOVE:
            if (mTileContext != null) {
            	// touch events are absolute positions, make relative to start position
                int newOfx = mDownOffsetX + (int)(event.getX() - mDownX);
                int newOfy = mDownOffsetY + (int)(event.getY() - mDownY);
                if (DEBUG) {
                	logd("Move: to-of7(%d,%d)", newOfx, newOfy);
                }
                mTileContext.onPanTo(newOfx, newOfy);
                return true;
            }
        case MotionEvent.ACTION_UP:
        case MotionEvent.ACTION_CANCEL:
        	if (mTileContext != null) {
        		mTileContext.onPanFinished();
        		return true;
        	}
        }
        return super.onTouchEvent(event);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
    	if (keyCode == KeyEvent.KEYCODE_G) {
    		if (mGridMode == GridMode.NONE) {
				mGridMode = GridMode.LINES;
			} else if (mGridMode == GridMode.LINES) {
				mGridMode = GridMode.TEXT;
			} else {
				mGridMode = GridMode.NONE;
			}
    		invalidate();
    	} else if (mTileContext != null && mTileContext.onKeyDown(event)) {
			return true;
		}
    	return super.onKeyDown(keyCode, event);
    }

    // ----

    private void logd(String format, Object...args) {
        Log.d(TAG, String.format(format, args));
    }

}
