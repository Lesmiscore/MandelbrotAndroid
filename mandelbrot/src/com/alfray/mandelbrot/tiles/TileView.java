/*
 * (c) ralfoide gmail com, 2008
 * Project: Mandelbrot
 * License TBD
 */


package com.alfray.mandelbrot.tiles;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.alfray.mandelbrot.R;

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

    private static boolean DEBUG = true;
    
    private TileContext mTileContext;
    private Rect mTempBounds = new Rect();
    private Rect mTempRect = new Rect(0, 0, Tile.SIZE, Tile.SIZE);
    private Drawable mNoTile;
    private Paint mRed;

    public TileView(Context context, AttributeSet attrs) {
        super(context, attrs);
        
        mNoTile = context.getResources().getDrawable(R.drawable.android_maps_no_tile_128);
        
        if (DEBUG) {
            mRed = new Paint();
            mRed.setColor(0xFFFF0000);
            mRed.setStyle(Paint.Style.STROKE);
            mRed.setTextAlign(Paint.Align.CENTER);
        }
    }

    public void setTileContext(TileContext tileContext) {
        mTileContext = tileContext;
        mTileContext.setView(this);
    }
    
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (mTileContext != null) mTileContext.onSizeChanged(w, h);
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        if (mTileContext != null) {
            mTileContext.setView(visibility == VISIBLE ? this : null);
        }
    }
    
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mTileContext == null) return;
        
        Tile[] tiles = mTileContext.getVisibleTiles();

        Rect bounds = mTempBounds ;
        boolean useBounds = canvas.getClipBounds(bounds);

        Log.d(TAG, "Bounds " + (useBounds ? bounds.toString() : "no"));

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
            
            if (DEBUG) {
                canvas.drawRect(rect, mRed);
                String s = t.toString();
                final int SZ2 = Tile.SIZE / 2;
                canvas.drawText(s, x + SZ2, y + SZ2, mRed);
                Log.d(TAG, "Draw " + s);
            }
        }
    }
}
