package com.alfray.mandelbrot2;

import android.content.Context;
import android.util.Log;

import com.alfray.mandelbrot2.prefs.BasePrefsValues;

/**
 * Performs the Mandelbrot computation, either using a native wrapper or via pure Java calls.
 * <p/>
 * The protected static methods are called from TestActivity.AccessWrapper for speed measurements.
 * For "real" code, rely on the public methods that abstract the native-vs-java calls.
 * <p/>
 * Note that JNI code has been disabled. It's quite obsolete and probably buggy.
 */
public class JavaMandel {

    private static String TAG = JavaMandel.class.getSimpleName();
    private static boolean mHasRs = false;
    private static boolean mUseRs = true;

    public synchronized static void init(Context context) {
        try {
            mHasRs  = Mandel_RS.init(context);
        } catch (VerifyError e) {
            Log.d(TAG, "RenderScript API not present");

        } catch (Throwable t) {
            Log.w(TAG, "RenderScript init error", t);
        }

        prefsChanged(context);
    }

    public static void dispose() {
    }

    public static boolean hasRs() {
        return mHasRs;
    }

    public static boolean useRs() {
        return mHasRs && mUseRs;
    }

    public static void prefsChanged(Context context) {
        BasePrefsValues pv = new BasePrefsValues(context.getApplicationContext());
        mUseRs = pv.useRenderScript();
    }

    // ------------------------------------------------------------------------

    /**
     * Native or java rendering using a classic double algorithm with no fancy optim tricks.
     * <p/>
     * Version 2: computes a block using x_start+x_step / y_start+y_step.
     * maxIter is an int, returns SX*SY int ranging [1..maxIter].
     * Uses the "classic" double version, no fancy optims.
     * <p/>
     * Aborts if maxIter or sx or sy <= 0.
     */
    public static void mandelbrot2(
            double x_start, double x_step,
            double y_start, double y_step,
            int sx, int sy,
            int max_iter,
            int size, int[] result) {

        if (mHasRs && mUseRs) {
            Mandel_RS.mandelbrot2_RS(x_start, x_step, y_start, y_step, sx, sy, max_iter, size, result);
        } else {
            mandelbrot2_java(x_start, x_step, y_start, y_step, sx, sy, max_iter, size, result);
        }
    }

    protected static void mandelbrot2_java(
            double x_start, double x_step,
            double y_start, double y_step,
            int sx, int sy,
            int max_iter,
            int size, int[] result) {
        if (max_iter <= 0) return;
        double x_begin = x_start;
        for(int j = 0, k = 0; j < sy; ++j, y_start += y_step) {
            x_start = x_begin;
            for(int i = 0; i < sx; ++i, ++k, x_start += x_step) {
                // the "naive" mandelbrot computation. nothing fancy.
                double x = x_start;
                double y = y_start;
                double x2 = x * x;
                double y2 = y * y;
                int iter = 0;
                while (x2 + y2 < 4 && iter < max_iter) {
                    double xt = x2 - y2 + x_start;
                  y = 2 * x * y + y_start;
                  x = xt;
                  x2 = xt * xt;
                  y2 = y * y;
                  ++iter;
                }

                result[k] = iter;
            } // i
        } // j
    }

    // ------------------------------------------------------------------------

    /**
     * Native or java rendering using a classic fp16 algorithm with no fancy optim tricks.
     * <p/>
     * Version 3: Similar to version 2, except its in fixed-point 16 bits (8.8).
     * MaxIter is a byte (1..255 except java doesn't have unsigned types so
     * we use -128..127 instead to mean 0..255).
     * Returns SX*SY bytes, ranging [-128..127] but really meaning [0..255].
     *
     * Returns false if there isn't enough precision to use fp16 or
     * if maxter is -128 (which represents 0 here).
     */
    public static boolean mandelbrot3(
            double x_start, double x_step,
            double y_start, double y_step,
            int sx, int sy,
            byte max_iter,
            int size, byte[] result) {
        return mandelbrot3_java(x_start, x_step, y_start, y_step, sx, sy, max_iter, size, result);
    }

    protected static boolean mandelbrot3_java(
            final double x_start, final double x_step,
            final double y_start, final double y_step,
            final int sx, final int sy,
            final byte max_iter,
            final int size, byte[] result) {
        if (max_iter == -128) return false;
        final int ix_step = (int)(x_step  * 256);
        final int iy_step = (int)(y_step  * 256);
        if (ix_step <= 0 || iy_step <= 0) return false;
        int ix_start = (int)(x_start * 256);
        int iy_start = (int)(y_start * 256);

        int ix_begin = ix_start;
        for(int j = 0, k = 0; j < sy; ++j, iy_start += iy_step) {
            ix_start = ix_begin;
            for(int i = 0; i < sx; ++i, ++k, ix_start += ix_step) {
                int ix = ix_start;
                int iy = iy_start;
                int ix2 = (ix * ix) >> 8;
                int iy2 = (iy * iy) >> 8;
                byte iter = -128;
                while (ix2 + iy2 < (4<<8) && iter < max_iter) {
                  int ixt = (ix2 - iy2) + ix_start;
                  iy = ((2 * ix * iy) >> 8) + iy_start;
                  ix = ixt;
                  ix2 = (ixt * ixt) >> 8;
                  iy2 = (iy * iy) >> 8;
                  ++iter;
                }

                result[k] = iter;
            } // i
        } // j
        return true;
    }

    // ------------------------------------------------------------------------

    /**
     * Native or java rendering using a classic fp32 algorithm with no fancy optim tricks.
     * <p/>
     * Version 4: Similar to version 2, except its in fixed-point 32 bits (16.16).
     * MaxIter is a byte (1..255 except java doesn't have unsigned types so
     * we use -128..127 instead to mean 0..255).
     * Returns SX*SY bytes, ranging [-128..127] but really meaning [0..255].
     *
     * Returns false if there isn't enough precision to use fp32 or
     * if maxter is -128 (which represents 0 here).
     */
    public static boolean mandelbrot4(
            double x_start, double x_step,
            double y_start, double y_step,
            int sx, int sy,
            byte max_iter,
            int size, byte[] result) {
        return mandelbrot4_java(x_start, x_step, y_start, y_step, sx, sy, max_iter, size, result);
    }

    protected static boolean mandelbrot4_java(
            final double x_start, final double x_step,
            final double y_start, final double y_step,
            final int sx, final int sy,
            final byte max_iter,
            final int size, byte[] result) {
        if (max_iter == -128) return false;
        final int ix_step = (int)(x_step  * 65536);
        final int iy_step = (int)(y_step  * 65536);
        if (ix_step <= 0 || iy_step <= 0) return false;
        int ix_start = (int)(x_start * 65536);
        int iy_start = (int)(y_start * 65536);

        int ix_begin = ix_start;
        for(int j = 0, k = 0; j < sy; ++j, iy_start += iy_step) {
            ix_start = ix_begin;
            for(int i = 0; i < sx; ++i, ++k, ix_start += ix_step) {
                long Lx = (long)ix_start;
                long Ly = (long)iy_start;
                int ix2 = (int)((Lx * Lx) >> 16);
                int iy2 = (int)((Ly * Ly) >> 16);
                byte iter = -128;
                while (ix2 + iy2 < (4<<16) && iter < max_iter) {
                  int ixt = (ix2 - iy2) + ix_start;
                  Ly = ((2 * Lx * Ly) >> 16) + iy_start;
                  Lx = (long)ixt;
                  ix2 = (int)((Lx * Lx) >> 16);
                  iy2 = (int)((Ly * Ly) >> 16);
                  ++iter;
                }

                result[k] = iter;
            } // i
        } // j
        return true;
    }
}

