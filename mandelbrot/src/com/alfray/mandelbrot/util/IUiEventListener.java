/*
 * (c) ralfoide, http://gamez.googlecode.com/, 2008
 * Project: gamez
 * License: GPL version 3 or any later version
 */

package com.alfray.mandelbrot.util;

import android.view.MotionEvent;
import android.view.KeyEvent.Callback;
import android.view.View.OnClickListener;

//-----------------------------------------------

/**
 * Interface definition for callback invoked to handle user input:
 * - onTouchEvent from the View
 * - onClick from the View.OnClickListener
 * - onKeyDown/Up/Multiple from View.KeyEvent.Callback
 */
public interface IUiEventListener extends OnClickListener, Callback {

	/**
	 * Implements the method to handle touch motion events.
	 * <p/>
	 * @param event The motion event.
	 * @return True if the event was handled by the method.
	 */
	public boolean onTouchEvent(MotionEvent event);

    public boolean onTrackballEvent(MotionEvent event);
}
