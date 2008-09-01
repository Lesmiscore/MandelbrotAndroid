/*
 * (c) ralfoide, http://gamez.googlecode.com/, 2008
 * Project: gamez
 * License TBD
 */


package com.alfray.mandelbrot.util;

import java.util.Map;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;

//-----------------------------------------------

/**
 * A custom derived surface view.
 * <p/>
 * A custom version is needed to handle some events such as onMotionEvent
 * and to implement size restriction via onMeasure.
 */
public class GLSurfaceView extends SurfaceView implements OnClickListener {

	private IUiEventListener mUiEventListener;

	/**
	 * Constructs a new GLSurfaceView from info in a resource layout XML file.
	 */
	@SuppressWarnings("unchecked")
	public GLSurfaceView(Context context, AttributeSet attrs) {
		super(context, attrs);
		setFocusable(true);
		setFocusableInTouchMode(true);
		setClickable(true);
		setOnClickListener(this);
	}
	
	void setUiEventHandler(IUiEventListener listener) {
		mUiEventListener = listener;
	}

	/**
	 * Implements size restriction.
	 * <p/>
	 * TODO: The goal is to ensure the view is square. Maybe later.
	 * Right now we don't use it.
	 */
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
	}

	/**
	 * Implements the method to handle touch motion events.
	 * <p/>
	 * @param event The motion event.
	 * @return True if the event was handled by the method.
	 */
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (mUiEventListener != null &&
				mUiEventListener.onTouchEvent(event)) {
			return true;
		}
		return super.onTouchEvent(event);
	}
    
    @Override
    public boolean onTrackballEvent(MotionEvent event) {
      if (mUiEventListener != null &&
            mUiEventListener.onTrackballEvent(event)) {
          return true;
      }
      return super.onTrackballEvent(event);
    }

	/**
	 * Implements the method to handle a click event.
	 */
	public void onClick(View view) {
		if (mUiEventListener != null) {
			mUiEventListener.onClick(view);
		}
	}

	/**
	 * Called when a key down event has occurred.
	 * <p/>
	 * This base implementation does nothing and returns false.
	 * 
	 * @param keyCode The value in event.getKeyCode().
	 * @param event Description of the key event.
	 * @return If you handled the event, return true.
	 *         If you want to allow the event to be handled by the next receiver, return false.
	 */
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (mUiEventListener != null &&
				mUiEventListener.onKeyDown(keyCode, event)) {
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}
	
	/**
	 * Called when multiple down/up pairs of the same key have occurred in a row.
	 * <p/>
	 * This base implementation does nothing and returns false.
	 * 
	 * @param keyCode The value in event.getKeyCode().
	 * @param count Number of pairs as returned by event.getRepeatCount().
	 * @param event Description of the key event.
	 * @return If you handled the event, return true.
	 *         If you want to allow the event to be handled by the next receiver, return false.
	 */
	@Override
	public boolean onKeyMultiple(int keyCode, int repeatCount, KeyEvent event) {
		if (mUiEventListener != null &&
				mUiEventListener.onKeyMultiple(keyCode, repeatCount, event)) {
			return true;
		}
		return super.onKeyMultiple(keyCode, repeatCount, event);
	}
	
	/**
	 * Called when a key up event has occurred.
	 * <p/>
	 * This base implementation does nothing and returns false.
	 * 
	 * @param keyCode The value in event.getKeyCode().
	 * @param event Description of the key event.
	 * @return If you handled the event, return true.
	 *         If you want to allow the event to be handled by the next receiver, return false.
	 */
	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if (mUiEventListener != null &&
				mUiEventListener.onKeyUp(keyCode, event)) {
			return true;
		}
		return super.onKeyUp(keyCode, event);
	}
}
