package com.alfray.mandelbrot2;

import android.content.Context;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.util.Log;

/*
 * Mandelbrot wrapper for RenderScript.
 * Requires API 11 to work, will fail with VerifyError on previous versions.
 */
public class Mandel_RS {

    private static final String TAG = Mandel_RS.class.getSimpleName();
    private static final boolean DEBUG = true;

    private static RenderScript mRs;
    private static ScriptC_mandel mScript;
    private static Allocation mAlloc;

    /** Returns true if RS is supported. */
    public synchronized static boolean init(Context context) {
        try {
            mRs = RenderScript.create(context);
            mScript = new ScriptC_mandel(mRs, context.getResources(), R.raw.mandel);
            return true;
        } catch (Throwable t) {
            Log.w(TAG, "RenderScript init error", t);
            return false;
        }
    }

    public synchronized static void dispose() {
        if (mAlloc != null) {
            mAlloc.destroy();
            mAlloc = null;
        }
        if (mScript != null) {
            mScript.destroy();
            mScript = null;
        }
        if (mRs != null) {
            mRs.destroy();
            mRs = null;
        }
    }

    // ------------------------------------------------------------------------

    public static void mandelbrot2_RS(
            float x_start, float x_step,
            float y_start, float y_step,
            int sx, int sy,
            int max_iter,
            int size, int[] result) {

        if (mAlloc == null || mAlloc.getType().getCount() != size) {
            if (mAlloc != null) mAlloc.destroy();
            mAlloc = Allocation.createSized(mRs,
                    Element.I32(mRs),
                    size,
                    Allocation.USAGE_SCRIPT);

            if (DEBUG) Log.d(TAG,
                String.format("Create alloc of size %d", mAlloc.getType().getCount()));

            mScript.bind_result(mAlloc);
        }

        mScript.invoke_mandel2(x_start, x_step, y_start, y_step, sx, sy, max_iter);
        mAlloc.copyTo(result);
    }

}

