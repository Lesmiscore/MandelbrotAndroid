package com.alfray.mandelbrot;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;

public class MandelbrotActivity extends Activity
{
    private static final int SCREEN_UPDATES_IN_MS = 1000;
    private static final int ID_CONTINUE = Menu.FIRST;
    private static final int ID_PAUSE = Menu.FIRST + 1;
    private static final int ID_SELECT = Menu.FIRST + 2;
    private static final int ID_BACK = Menu.FIRST + 3;
    private static final int ID_OPTIONS = Menu.FIRST + 4;
    private static final int ID_MORE = Menu.FIRST + 5;

    private static final String TAG = "MandelbrotActivity";
    
    /** The builder thread controller */
    private MandelbrotBuilder mBuilder;
    /** The bitmap view that displays the result */
    private BitmapView mBitmapView;
    /** Callbacks called by the bitmap view (onSizeKnown, selection, etc) */
    private ViewCallback mViewCallback;
    /** Flag indicating if the thread was explicitely paused by the user via menu */
    private boolean mPausedByUser;
    /** Handler to generate bitmap view updates */
    private Handler mHandler;
    protected boolean mContinueScreenUpdates;

    /**
     * Called when the activity is first created.
     * The icicle map possibly contains data saved by onFreeze.
     * */
    @Override
    public void onCreate(Bundle bundle)
    {
        super.onCreate(bundle);

        setTitle(R.string.app_title);
        // request a progress bar and hide it
        requestWindowFeature(Window.FEATURE_PROGRESS);
        getWindow().setFeatureInt(Window.FEATURE_PROGRESS, 10000);

        mBitmapView = new BitmapView(getApplication());
        setContentView(mBitmapView);
        mBitmapView.setFocusable(true);
        mBitmapView.setFocusableInTouchMode(true);
        mBitmapView.requestFocus();

        mViewCallback = new ViewCallback();
        mBitmapView.setCallback(mViewCallback);

        mHandler = new Handler();

        if (bundle != null) {
            mBitmapView.restoreState(bundle);
            mBuilder = MandelbrotBuilder.createFromState(bundle);
        }
    }

    /**
     * Creates the items for the options menu.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        
        menu.add(0, ID_SELECT, 0, R.string.selection);
        menu.add(0, ID_BACK, 0, R.string.back);
        menu.add(0, ID_PAUSE, 0, R.string.pause).setShortcut(
                (char) KeyEvent.KEYCODE_1, (char) KeyEvent.KEYCODE_P);
        menu.add(0, ID_CONTINUE, 0, R.string._continue).setShortcut(
        		(char) KeyEvent.KEYCODE_2, (char) KeyEvent.KEYCODE_C);
        /* TC2-RC6 re-enable when ready
        menu.add(0, ID_OPTIONS, 0, R.string.options).setShortcut(
        		(char) KeyEvent.KEYCODE_3, (char) KeyEvent.KEYCODE_O);
        menu.add(0, ID_MORE, 0, R.string.more).setShortcut(
        		(char) KeyEvent.KEYCODE_4, (char) KeyEvent.KEYCODE_M);
        */
        return true;
    }
   
    /**
     * Updates the menu items as needed.
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        menu.findItem(ID_BACK).setEnabled(mBuilder != null && !mBuilder.isHistoryEmpty());
        menu.findItem(ID_CONTINUE).setEnabled(mBuilder == null || !mBuilder.isRunning());
        menu.findItem(ID_PAUSE).setEnabled(mBuilder != null && !mBuilder.isCompleted());
        menu.findItem(ID_PAUSE).setChecked(mPausedByUser);
        return true;
    }

    /**
     * Takes action when a menu item is selected.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case ID_SELECT:
                select();
                break;
            case ID_BACK:
                mViewCallback.backHistory();
                break;
            case ID_CONTINUE:
                start();
                break;
            case ID_PAUSE:
                mPausedByUser = true;
                stop();
                break;
            case ID_OPTIONS:
                showOptions();
                break;
            case ID_MORE:
                showMore();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Called when the activity is frozen or evicted.
     * The state saved here will be given back in onCreate.
     * 
     * @param outState Where the activity must save its state.
     */
    @Override
    protected void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        stop();
        if (mBuilder != null)    mBuilder.saveState(bundle);
        if (mBitmapView != null) mBitmapView.saveState(bundle);
    }
    
    /**
     * Called when the activity is paused i.e. when the user switched away.
     * To do: write prefs.
     */
    @Override
    protected void onPause() {
        super.onPause();
        stop();
    }

    /**
     * Called when an activity returns from the paused or frozen state.
     * To do: read the prefs, continue whatever we were doing before.
     */
    @Override
    protected void onResume() {
        super.onResume();
        mBitmapView.requestFocus();
        if (mBuilder != null) mBuilder.updatePrefs(this);
        start();
    }

    // ----------------------------------------------------------------------
    // Logic specific to this app

    /**
     * Display the more information screen
     */
    private void showMore() {
        Intent intent = new Intent(this, InfoActivity.class);
        try {
            startActivityForResult(intent, 0 /* requestCode */);
        } catch(ActivityNotFoundException e) {
            Log.e(TAG, e.getMessage() + ": " + intent.toString());
        }                
    }

    /**
     * Displays the options screen.
     */
    private void showOptions() {
        Intent intent = new Intent(this, OptionsActivity.class);
        //--M5 intent.addCategory(Intent.PREFERENCE_CATEGORY);
        try {
        	startActivityForResult(intent, 0 /* requestCode */);
        } catch(ActivityNotFoundException e) {
            Log.e(TAG, e.getMessage() + ": " + intent.toString());
        }        
    }
    
    /**
     * Start the computation for the current Mandelbrot, unless it has completed.
     */
    private void start() {
        int w = mBitmapView.getBitmapWidth();
        int h = mBitmapView.getBitmapHeight();
        if (w == 0 || h == 0 || mBuilder == null) return; // Abort if too early

        mPausedByUser = false;
        mBitmapView.setSourceBitmap(mBuilder.getBitmap());
        mBitmapView.setDetailsText(mBuilder.getDescription());
        mBitmapView.invalidate();

        mBuilder.updatePrefs(this);
        mBuilder.start();
        startScreenUpdates();
    }

    /**
     * Stops the current Mandelbrot computation.
     */
    private void stop() {        
        if (mBuilder != null) mBuilder.stop();
        // Hide progress bar
        getWindow().setFeatureInt(Window.FEATURE_PROGRESS, 10000);        
        stopScreenUpdates();
    }

    /**
     * Handles the action when the context menu Select item is used:
     * - if the bitmap selection is not visible, make it visible
     * - if it is visible, activate it.
     */
    private void select() {
        if (!mBitmapView.isSelectionVisible()) {
            mBitmapView.resetSelection(true /* visible */);
        } else {
            mBitmapView.activateSelection();
        }
    }

    private void startScreenUpdates() {
        mContinueScreenUpdates = true;
        mHandler.postDelayed(new Runnable() {
            public void run() {
                updateBitmap();
                if (mContinueScreenUpdates) startScreenUpdates();
            }
        }, SCREEN_UPDATES_IN_MS);
    }
    
    private void stopScreenUpdates() {
        mContinueScreenUpdates = false;
    }
    
    /**
     * Updates the bitmap view when a line has been completed
     */
    private void updateBitmap() {
        //mBitmapView.setStatusText("Running... " + Integer.toString((int)(mBuilder.getProgress() * 100)) + "%");
        getWindow().setFeatureInt(Window.FEATURE_PROGRESS,
                        (int)(10000 * mBuilder.getProgress()));
        mBitmapView.invalidate();
    }

    /**
     * Callback methods called by the bitmap view
     */
    protected class ViewCallback {

        /**
         * Called by the bitmap view when its size is first known.
         * 
         * @param width The width of the bitmap view in pixels
         * @param height The height of the bitmap view in pixels
         */
        public void sizeKnown(int width, int height) {
            if (mBuilder == null) {
                mBuilder = new MandelbrotBuilder(width, height);
                start();
            }
        }         

        /**
         * Tells the Mandelbrot builder to change its computation area.
         * This is called by the bitmap view. Since the bitmap view has no knowledge
         * of the area currently covered by the builder, it just used fractions
         * relative to the screen, i.e. to the current area. It's up to the builder
         * to convert that to meaningful values.
         * 
         * This also starts building the Mandelbrot.
         * 
         * @param centerX The new X center, 0=left, 1=right.
         * @param centerY The new Y center, 0=top, 1=bottom.
         * @param width The new width, as a percentage of the screen width
         * @param height The new height, as a percentage of the screen height 
         */
        public void changeZoomArea(RectF selection) {
            if (mBuilder == null) return; // abort if too early
            stop();
            mBitmapView.resetSelection(false /* not visible */);
            mBuilder.changeArea(selection);
            start();
        }

        /**
         * Tells the Mandelbrot builder to pop the area stack and select the previous
         * zoom area. If there's one, it also starts recomputing it.
         * 
         * Returns true if there was something to pop up the stack
         * @return 
         */
        public boolean backHistory() {
            if (mBuilder == null) return false; // abort if too early
            stop();
            if (mBuilder.popHistory()) {
                mBitmapView.setDetailsText(mBuilder.getDescription());
                start();
                return true;
            }
            return false;
        }
    }
}
