package com.alfray.mandelbrot;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.Paint.Style;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;

import com.alfray.mandelbrot.MandelbrotActivity.ViewCallback;

/**
 * A simple view that draws a bitmap on screen.
 */
public class BitmapView extends View {

    private final static int STROKE_WIDTH = 2;
    
    private Bitmap mBitmap;

    private String mStatusText;
    private String mDetailsText;
    private Paint mWhiteTextPaint;
    private Paint mBlackTextPaint;
    private Paint mDetailsTextPaint;
    private int mStatusPosX = 0;
    private int mStatusPosY = 0;
    private int mDetailsPosX = 0;
    private int mDetailsPosY = 0;

    private boolean mPrepared = false;
    private boolean mSelectionVisible = false;
    private RectF mSelectionRectF;
    private Paint mCursorPaint;
    private int mBitmapWidth;
    private int mBitmapHeight;
    private ViewCallback mCallback;

	private long mStartTouch;

    public BitmapView(Context context) {
        super(context);
    }

    /**
     * Returns the width in pixels. This will be 0 before sizeChanged is called
     * at least once on the view by the layout engine.
     */
    public int getBitmapWidth() {
        return mBitmapWidth;
    }
    
    /**
     * Returns the height in pixels. This will be 0 before sizeChanged is called
     * at least once on the view by the layout engine.
     */
    public int getBitmapHeight() {
        return mBitmapHeight;
    }

    /**
     * Sets the bitmap this view should draw.
     */
    public void setSourceBitmap(Bitmap bitmap) {
        mBitmap = bitmap;
    }
    
    /**
     * Changes the status text. This does not update the display,
     * force an invalidate with invalidateText() if you really mean it.
     */
    public void setStatusText(String status) {
        mStatusText = status;
    }
    
    /**
     * Changes the default text. Does not force an invalidate.
     */
    public void setDetailsText(String details) {
        mDetailsText = details;
    }

    public void setCallback(ViewCallback callback) {
        mCallback = callback;
    }

    /**
     * Invalidate only the area where the status text is.
     */
    public void invalidateText() {
        if (mWhiteTextPaint != null) {
            int y = mStatusPosY - (int)(mWhiteTextPaint.ascent() +
                                        mWhiteTextPaint.descent());
            Rect drawing_rect = new Rect();
            getDrawingRect(drawing_rect);
            invalidate(0, y, drawing_rect.right, mStatusPosY);
        }
    }
    
    /**
     * Reset the cursor. Makes it match the full view.
     * 
     * @param visible Should the cursor be visible?
     */
    public void resetSelection(boolean visible) {
    	int w = mBitmapWidth;
    	int h = mBitmapHeight;
        mSelectionRectF = new RectF(w/4, w/4, w-w/4, h-h/4);
        mSelectionVisible = visible;
    }
    
    /** Indicates if selection is visible */
    public boolean isSelectionVisible() {
        return mSelectionVisible;
    }

    /**
     * User selected area to draw
     */
    public void activateSelection() {
        if (mSelectionVisible) {
            mCallback.changeZoomArea(mSelectionRectF);
        }
    }

    /**
     * User wants to go back to a previous area to draw
     * @return 
     */
    public boolean backHistory() {
        return mCallback.backHistory();
    }
    
    // ----------------------------------------------------------------------
    // Protected SDK overrides

    @Override
    protected void onDraw(Canvas canvas) {
        if (!mPrepared) prepare();
        if (mBitmap != null) {
            canvas.drawBitmap(mBitmap, 0, 0, null);
        } else {
            canvas.drawColor(0xFF000020); // dark blue
            super.onDraw(canvas);
        }
        
        if (mDetailsText != null) {
            canvas.drawText(mDetailsText, mDetailsPosX, mDetailsPosY,
                            mDetailsTextPaint);
        }

        if (mSelectionVisible) {
            canvas.drawRoundRect(mSelectionRectF, 3, 3, mCursorPaint);
        }
 
        if (mStatusText != null) {
            canvas.drawText(mStatusText, mStatusPosX + 1, mStatusPosY + 1,
                            mBlackTextPaint);
            canvas.drawText(mStatusText, mStatusPosX, mStatusPosY,
                            mWhiteTextPaint);
        }
    }
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch(keyCode) {
            case KeyEvent.KEYCODE_DPAD_UP:
            case KeyEvent.KEYCODE_I:
                moveSelection(0, -2, 1.0f, true /* redraw */, true /* relative */);
                break;
            case KeyEvent.KEYCODE_DPAD_DOWN:
            case KeyEvent.KEYCODE_M:
                moveSelection(0, 2, 1.0f, true /* redraw */, true /* relative */);
                break;
            case KeyEvent.KEYCODE_DPAD_LEFT:
            case KeyEvent.KEYCODE_J:
                moveSelection(-2, 0, 1.0f, true /* redraw */, true /* relative */);
                break;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
            case KeyEvent.KEYCODE_K:
                moveSelection(2, 0, 1.0f, true /* redraw */, true /* relative */);
                break;
            case KeyEvent.KEYCODE_LEFT_BRACKET:
            case KeyEvent.KEYCODE_O:
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                moveSelection(0, 0, 0.5f, true /* redraw */, true /* relative */);
                break;
            case KeyEvent.KEYCODE_RIGHT_BRACKET:
            case KeyEvent.KEYCODE_P:
            case KeyEvent.KEYCODE_VOLUME_UP:
                moveSelection(0, 0, 2.0f, true /* redraw */, true /* relative */);
                break;
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
                activateSelection();
                break;
            case KeyEvent.KEYCODE_BACK:
                if (!backHistory()) {
                    return super.onKeyDown(keyCode, event);
                }
                break;
            default:
                return super.onKeyDown(keyCode, event);
        }
        return true;
    }
    
    @Override
    public boolean onTrackballEvent(MotionEvent event) {
    	if (event.getAction() == MotionEvent.ACTION_MOVE) {
    		float y = event.getY();
    		if (y < 0) {
    			moveSelection(0, 0, 1.1f, true /* redraw */, true /* relative */);
    		} else {
    			moveSelection(0, 0, 0.9f, true /* redraw */, true /* relative */);
    		}
    		return true;
    	}
    	return super.onTrackballEvent(event);
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent event) {
    	if (event.getAction() == MotionEvent.ACTION_DOWN) {
    		// return true so that we get the move/up actions
    		mStartTouch = System.currentTimeMillis();
    		return true;
    	} else if (event.getAction() == MotionEvent.ACTION_MOVE) {
    		moveSelection(event.getX(), event.getY(), 1.0f, true /* redraw */, false /* relative */);
    		return true;
    	} else if (event.getAction() == MotionEvent.ACTION_UP) {
    		if (System.currentTimeMillis() - mStartTouch < 100) {
	            activateSelection();
    		}
    		return true;
    	}
    	return super.onTouchEvent(event);
    }

    // ----------------------------------------------------------------------
    // Private methods

    /**
     * Moves or resize the area cursor and asks for redraws.
     * @param relative True if relative deltaX/Y, false is absolute post X/Y 
     */
    private void moveSelection(float inX, float inY, float zoom, boolean redraw, boolean relative) {
        RectF old = new RectF(mSelectionRectF);
        boolean was_visible = mSelectionVisible;

        mSelectionVisible = true;
        if (inX != 0 || inY != 0) {
        	if (relative) {
        		mSelectionRectF.offset(inX, inY);
        	} else {
        		float xc = mSelectionRectF.centerX();
        		float yc = mSelectionRectF.centerY();
        		mSelectionRectF.offset(inX - xc, inY - yc);
        	}
        }
        if (zoom != 1.0) {
            float sx1 = mSelectionRectF.width();
            float sy1 = mSelectionRectF.height();
            float sx2 = sx1 * zoom;
            float sy2 = sy1 * zoom;
            sx2 = Math.max(2, Math.min(sx2, mBitmapWidth - 1));
            float min_y = 2 * mBitmapHeight / mBitmapWidth;
            sy2 = Math.max(min_y, Math.min(sy2, mBitmapHeight - 1));
            mSelectionRectF.inset((sx1 - sx2) / 2, (sy1 - sy2) / 2);
        }
        
       if (redraw && was_visible) {
           invalidate((int)old.left  - STROKE_WIDTH,  (int)old.top   - STROKE_WIDTH,
                      (int)old.right + STROKE_WIDTH, (int)old.bottom + STROKE_WIDTH);
       }
       if (redraw) {
           invalidate((int)mSelectionRectF.left  - STROKE_WIDTH,  (int)mSelectionRectF.top   - STROKE_WIDTH,
                      (int)mSelectionRectF.right + STROKE_WIDTH, (int)mSelectionRectF.bottom + STROKE_WIDTH);
       }
    }

    /**
     * Prepare the text position and paint.
     * Done right during the first draw to take into account the current screen size.
     */
    private void prepare() {
        mPrepared = true;

        Rect drawing_rect = new Rect();
        getDrawingRect(drawing_rect);
        mBitmapWidth = drawing_rect.width();
        mBitmapHeight = drawing_rect.height();

        mCallback.sizeKnown(mBitmapWidth, mBitmapHeight);
        
        mWhiteTextPaint = new Paint();
        mWhiteTextPaint.setColor(Color.WHITE);
        mWhiteTextPaint.setTextSize(16);
        mWhiteTextPaint.setAntiAlias(true);
        mWhiteTextPaint.setTypeface(Typeface.create(
                Typeface.SANS_SERIF.SANS_SERIF, Typeface.BOLD));
        
        mBlackTextPaint = new Paint(mWhiteTextPaint);
        mBlackTextPaint.setColor(Color.BLACK);

        mDetailsTextPaint = new Paint();
        mDetailsTextPaint.setColor(Color.WHITE);
        mDetailsTextPaint.setTextSize(8);
        mDetailsTextPaint.setAntiAlias(true);
        mDetailsTextPaint.setTypeface(Typeface.DEFAULT);

        mCursorPaint = new Paint();
        mCursorPaint.setStrokeWidth(STROKE_WIDTH);
        mCursorPaint.setColor(Color.WHITE);
        mCursorPaint.setStyle(Style.STROKE);
        mCursorPaint.setPathEffect(new DashPathEffect(new float[] {5, 2}, 0));

        mStatusPosX = 5;
        mStatusPosY = mBitmapHeight - 5 - (int)(Math.ceil(mWhiteTextPaint.descent()));

        mDetailsPosX = 5;
        mDetailsPosY = 5 - (int)mDetailsTextPaint.ascent();

        // The selection rectangle may have already been set by restoreState.
        if (mSelectionRectF == null) {
            resetSelection(false);
        }
    }

    /**
     * Saves the state of the bitmap view in the given map.
     * Used when the activity is frozen.
     * State is restored in restoreState().
     */
    public void saveState(Bundle bundle) {
        bundle.putBoolean("sv", new Boolean(mSelectionVisible));
        if (mSelectionVisible) {
            bundle.putFloat("sL", new Float(mSelectionRectF.left));
            bundle.putFloat("sT", new Float(mSelectionRectF.top));
            bundle.putFloat("sR", new Float(mSelectionRectF.right));
            bundle.putFloat("sB", new Float(mSelectionRectF.bottom));
        }
        if (mStatusText != null) bundle.putString("st", mStatusText);
        if (mDetailsText != null) bundle.putString("dt", mDetailsText);
    }

    /**
     * Restores the state that was previously saved by saveState().
     * The bitmap view has already been created but it has no source bitmap
     * attached yet. Also the layout has probably not been constructed yet
     * and the bitmap view will be prepared later before being actually used.
     */
    public void restoreState(Bundle bundle) {
        mSelectionVisible = bundle.getBoolean("sv");
        if (mSelectionVisible) {
            mSelectionRectF = new RectF(bundle.getFloat("sL"),
                                        bundle.getFloat("sT"),
                                        bundle.getFloat("sR"),
                                        bundle.getFloat("sB"));
        }
        if (bundle.containsKey("st")) mStatusText = bundle.getString("st");
        if (bundle.containsKey("dt")) mDetailsText = bundle.getString("dt");
    }
}
