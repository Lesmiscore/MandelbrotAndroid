package com.alfray.mandelbrot.old;

import com.alfray.mandelbrot.R;
import com.alfray.mandelbrot.R.id;
import com.alfray.mandelbrot.R.layout;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.widget.EditText;

//-----------------------------------------------

/**
 * Activity than handles preferences for the Mandelbrot activity.
 */
public class OptionsActivity extends Activity {

    private static final String TAG = "Mandelbrot-Options";

    public static final int DEFAULT_HISTORY_DEPTH = 10;
    public static final int DEFAULT_STEP_ITER = 60;
    public static final int DEFAULT_MIN_ITER = 20;
    public final static String PREFERENCES = "MandelbrotOptions";
    public final static String MIN_ITER = "min-iter";
    public final static String STEP_ITER = "max-iter";
    public final static String HISTORY_DEPTH = "hist-depth";

    private EditText mMinIterEdit;
    private EditText mMaxIterEdit;
    private EditText mHistoryDepthEdit;

    /**
     * Called when the activity is created.
     */
    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.options);
        
        mMinIterEdit = (EditText)findViewById(R.id.min_iter);
        mMaxIterEdit = (EditText)findViewById(R.id.step_iter);
        mHistoryDepthEdit = (EditText)findViewById(R.id.hist_depth);
    }
    
    /**
     * Called when the activity is resumed.
     * Read the current preferences and set them in the view's controls.
     */
    @Override
    protected void onResume() {
        super.onResume();

        SharedPreferences prefs = getSharedPreferences(PREFERENCES, 0 /* mode */);
        int min_iter = prefs.getInt(MIN_ITER, DEFAULT_MIN_ITER);
        int step_iter = prefs.getInt(STEP_ITER, DEFAULT_STEP_ITER);
        int hist_depth = prefs.getInt(HISTORY_DEPTH, DEFAULT_HISTORY_DEPTH);
        mMinIterEdit.setText(Integer.toString(min_iter));
        mMaxIterEdit.setText(Integer.toString(step_iter));
        mHistoryDepthEdit.setText(Integer.toString(hist_depth));
    }
    
    /**
     * Called when the activity is paused (i.e. switched away or dismissed).
     * Reads the view's controls values and write preferences.
     */
    @Override
    protected void onPause() {
        super.onPause();
        
        SharedPreferences prefs = getSharedPreferences(PREFERENCES, 0 /* mode */);
        Editor pref_edit = prefs.edit();
        int min_iter = Integer.valueOf(mMinIterEdit.getText().toString());
        int step_iter = Integer.valueOf(mMaxIterEdit.getText().toString());
        int hist_depth = Integer.valueOf(mHistoryDepthEdit.getText().toString());
        pref_edit.putInt(MIN_ITER, min_iter);
        pref_edit.putInt(STEP_ITER, step_iter);
        pref_edit.putInt(HISTORY_DEPTH, hist_depth);
        pref_edit.commit();
    }
}

