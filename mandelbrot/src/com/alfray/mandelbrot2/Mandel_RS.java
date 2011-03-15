package com.alfray.mandelbrot2;

import android.content.Context;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.Type;
import android.renderscript.Type.Builder;
import android.util.Log;

/*
 * Mandelbrot wrapper for RenderScript.
 * Requires API 11 to work, will likely fail with VerifyError on previous versions.
 */
public class Mandel_RS {

    private static final String TAG = Mandel_RS.class.getSimpleName();
    private static final boolean DEBUG = true;

    private static RenderScript mRs;
    private static ScriptC_mandel_foreach mScriptForEach;
    private static ScriptC_mandel_root mScriptRoot;
    private static Allocation mAlloc;
    private static Allocation mAlloc2;

    /** Returns true if RS is supported. */
    public synchronized static boolean init(Context context) {
        try {
            if (mRs == null) {
                mRs = RenderScript.create(context);
                mScriptForEach = new ScriptC_mandel_foreach(mRs, context.getResources(), R.raw.mandel_foreach);
                mScriptRoot = new ScriptC_mandel_root(mRs, context.getResources(), R.raw.mandel_root);
            }
            return true;
        } catch (Throwable t) {
            Log.w(TAG, "RenderScript init error", t);
            return false;
        }
    }

    public synchronized static void dispose() {
        if (mRs != null) {
            mRs.destroy();
            mRs = null;
            mAlloc = null;
            mScriptForEach = null;
            mScriptRoot = null;
        }
    }

    // ------------------------------------------------------------------------

    public static void mandelbrot2_RS(
            double x_start, double x_step,
            double y_start, double y_step,
            int sx, int sy,
            int max_iter,
            int size, int[] result) {

        if (mAlloc == null || mAlloc.getType().getCount() != size) {
            if (mAlloc != null) mAlloc.destroy();

            Builder t = new Type.Builder(mRs, Element.I32(mRs));
            t.setX(sx);
            t.setY(sy);
            mAlloc = Allocation.createTyped(mRs, t.create());
            mAlloc2 = Allocation.createTyped(mRs, t.create());

            if (DEBUG) Log.d(TAG,
                String.format("Create alloc of size %d", mAlloc.getType().getCount()));

            mScriptForEach.set_gScriptRoot(mScriptRoot);
            mScriptForEach.set_gResult(mAlloc);
            mScriptForEach.set_gIn(mAlloc2);
        }

        mScriptForEach.invoke_mandel2(x_start, x_step, y_start, y_step, max_iter);
        mAlloc.copyTo(result);
    }

}

