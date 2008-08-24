package com.alfray.mandelbrot.old;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Paint.Style;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Computes a Mandelbrot set
 */
public class MandelbrotBuilder {

    private static final String TAG = "Mandelbrot-Builder";

    /** Number of passes for the multi-pass algorithm */
    private static final int NUM_PASSES = 6;

    /** Width in pixels */
    private int mWidth;
    /** Height in pixels */
    private int mHeight;
    /** Builder thread */
    private BuilderThread mBuilderThread;

    /** Current state (i.e. area to compute, result & intermediaries */
    State mCurrentState = null;
    /** Area history. */
    private ArrayList<State> mHistoryStack;
    private int mPrefMinIter;
    private int mPrefStepIter;
    private int mPrefHistoryDepth;

    /**
     * States defines a Mandelbrot area to be plotted.
     * It contains information about which part of the Mandelbrot complex
     * plane must be plotted as well as the current state of the computation.
     */
    static class State implements Parcelable {
        /** X center of the area */
        public double mXCenter;
        /** Y center of the area */
        public double mYCenter;
        /** Width of the area */
        public double mWidth;
        /** Max number of iterations */
        public int mMaxIter;
        /** Destination bitmap */
        public Bitmap mBitmap;
        /** Current computation start line */
        public int mStartLine;
        /** Current computation start column */
        public int mStartColumn;
        public int mMaxPass;
        public int mStartPass;
        public int mProgress;

        /**
         * Constructs a new Mandelbrot area with the given parameters.
         * Coordinates are in the complex plane space.
         * Note that the height is not given. It is automatically computed
         * depending on the aspect ratio of the view.
         * 
         * @param xCenter X center of the area.
         * @param yCenter Y center of the area.
         * @param width Width of the area.
         * @param max_iter Max number of iterations. Must be > 0.
         */
        public State(double xCenter, double yCenter, double width, int max_iter,
                        int pixelWidth, int pixelHeight) {
            assert max_iter > 0;
            mXCenter = xCenter;
            mYCenter = yCenter;
            mWidth = width;
            mMaxIter = max_iter;
            mStartColumn = 0;
            mStartLine = 0;
            mStartPass = mMaxPass = NUM_PASSES;
            mProgress = 0;
            mBitmap = Bitmap.createBitmap(pixelWidth, pixelHeight, Bitmap.Config.RGB_565);
        }

        /**
         * Serializes this state object to a bundle.
         * Use State.createFromBundle to recreate the object.
         */
        public void saveToBundle(String prefix, Bundle bundle) {
            int w = mBitmap.getWidth();
            int h = mBitmap.getHeight();
            bundle.putInt(prefix + "bw", new Integer(w));
            bundle.putInt(prefix + "bh", new Integer(h));
            bundle.putDouble(prefix + "xc", new Double(mXCenter));
            bundle.putDouble(prefix + "yc", new Double(mYCenter));
            bundle.putDouble(prefix + "w",  new Double(mWidth));
            bundle.putInt(prefix + "mi", new Integer(mMaxIter));
            bundle.putInt(prefix + "sc", new Integer(mStartColumn));
            bundle.putInt(prefix + "sl", new Integer(mStartLine));
            bundle.putInt(prefix + "sp", new Integer(mStartPass));
            bundle.putInt(prefix + "mp", new Integer(mMaxPass));
            bundle.putInt(prefix + "pg", new Integer(mProgress));
            int[] pixels = new int[w * h];
            mBitmap.getPixels(pixels, 0, w, 0, 0, w, h);
            bundle.putIntArray(prefix + "bmp", pixels);
        }

        /**
         * Experiment: it would be nice for this struct to be Parceleable
         * and then we could use Bundle.put(Parceleable) directly. However
         * I currently don't see a reverse operation such as Bundle.getParcel(). 
         */
        public void writeToParcel(Parcel parcel, int flags) {
            int w = mBitmap.getWidth();
            int h = mBitmap.getHeight();
            parcel.writeInt(w);
            parcel.writeInt(h);
            parcel.writeDouble(mXCenter);
            parcel.writeDouble(mYCenter);
            parcel.writeDouble(mWidth);
            parcel.writeInt(mMaxIter);
            parcel.writeInt(mStartColumn);
            parcel.writeInt(mStartLine);
            parcel.writeInt(mStartPass);
            parcel.writeInt(mMaxPass);
            parcel.writeInt(mProgress);
            parcel.writeParcelable(mBitmap, 0);
        }
        
        //--how?--public static State createFromParcel()

        /**
         * Serializes this state object to a bundle.
         * Use State.createFromBundle to recreate the object.
         */ 
        public static State createFromBundle(String prefix, Bundle bundle) {
            int w = bundle.getInt(prefix + "bw");
            int h = bundle.getInt(prefix + "bh");
            State s = new State(
                            bundle.getDouble(prefix + "xc"),
                            bundle.getDouble(prefix + "yc"),
                            bundle.getDouble(prefix + "w"),
                            bundle.getInt(prefix + "mi"),
                            w, h);
            s.mStartColumn = bundle.getInt(prefix + "sc");
            s.mStartLine = bundle.getInt(prefix + "sl");
            s.mStartPass = bundle.getInt(prefix + "sp");
            s.mMaxPass = bundle.getInt(prefix + "mp");
            s.mProgress = bundle.getInt(prefix + "pg");
            s.mBitmap.setPixels(bundle.getIntArray(prefix + "bmp"), 0, w, 0, 0, w, h);
            return s;
        }

        /** for 0.9_r1 SDK */
		public int describeContents() {
			// TODO Auto-generated method stub
			return 0;
		}

        /**
         * Serializes this state object to a map.
         * Use State.createFromMap to recreate the object.
         * 
         * Deprecated. This was used with builds <= 27454.
         */
        /*
        @Deprecated
        public Map<String, Object> saveToMap() {
            Map<String, Object> map = new HashMap<String, Object>();
            map.put("xc", new Double(mXCenter));
            map.put("yc", new Double(mYCenter));
            map.put("w",  new Double(mWidth));
            map.put("mi", new Integer(mMaxIter));
            map.put("sc", new Integer(mStartColumn));
            map.put("sl", new Integer(mStartLine));
            map.put("sp", new Integer(mStartPass));
            map.put("mp", new Integer(mMaxPass));
            map.put("pg", new Integer(mProgress));
            int w = mBitmap.getWidth();
            int h = mBitmap.getHeight();
            map.put("bw", new Integer(w));
            map.put("bh", new Integer(h));
            map.put("bmp", mBitmap);
            return map;
        }
        */


        /**
         * Creates a new State object from a saved map.
         * 
         * Deprecated. This was used with builds <= 27454.
         */
        /*
        @Deprecated
        public static State createFromMap(Map<String, Object> map) {
            int w = ((Integer)map.get("bw")).intValue();
            int h = ((Integer)map.get("bh")).intValue();
            State s = new State(
                            ((Double)map.get("xc")).doubleValue(),
                            ((Double)map.get("yc")).doubleValue(),
                            ((Double)map.get("w")).doubleValue(),
                            ((Integer)map.get("mi")).intValue(),
                            w, h);
            s.mStartColumn = ((Integer)map.get("sc")).intValue();
            s.mStartLine = ((Integer)map.get("sl")).intValue();
            s.mStartPass = ((Integer)map.get("sp")).intValue();
            s.mMaxPass = ((Integer)map.get("mp")).intValue();
            s.mProgress = ((Integer)map.get("pg")).intValue();
            s.mBitmap.setPixels((int[])map.get("bmp"), 0, w, 0, 0, w, h);
            return s;
        }
        */
    }

    /**
     * Creates a new builder for the given set characteristics.
     * 
     * @param width Desired width in pixels.
     * @param height Desired height in pixels.
     * @param max_iter Max number of iterations (> 0).
     */
    public MandelbrotBuilder(int width, int height) {
        mWidth = width;
        mHeight = height;
        mBuilderThread = null;
        mHistoryStack = new ArrayList<State>();
        // We need to setup the state only after mWidht/mHeight have been
        // defined, as it will be used to setup the bitmap.
        mCurrentState = new State(-0.5, 0.0, 3.0, 20, getWidth(), getHeight());
    }

    public void updatePrefs(Activity activity) {
        SharedPreferences prefs = activity.getSharedPreferences(OptionsActivity.PREFERENCES, 0 /* mode */);
        mPrefMinIter = prefs.getInt(OptionsActivity.MIN_ITER, OptionsActivity.DEFAULT_MIN_ITER);
        mPrefStepIter = prefs.getInt(OptionsActivity.STEP_ITER, OptionsActivity.DEFAULT_STEP_ITER);
        mPrefHistoryDepth = prefs.getInt(OptionsActivity.HISTORY_DEPTH, OptionsActivity.DEFAULT_HISTORY_DEPTH);
    }
    

    /** Returns the width in pixels */
    public int getWidth() {
        return mWidth;
    }

    /** Returns the height in pixels */
    public int getHeight() {
        return mHeight;
    }

    /** Returns the current max iterations */
    public int getMaxIter() {
        return mCurrentState.mMaxIter;
    }   

    /** Returns the current output bitmap */
    public Bitmap getBitmap() {
        return mCurrentState.mBitmap;
    }

    /** Indicates if the builder thread is actually running */
    public boolean isRunning() {
        return mBuilderThread != null && mBuilderThread.isAlive();
    }

    /** Indicates if the current computation has completed */
    public boolean  isCompleted() {
        int num_pixels = getWidth() * getHeight();
        return mCurrentState.mProgress >= num_pixels;
    }

    /** Returns a progress status between [0..1] */ 
    public float getProgress() {
        int num_pixels = getWidth() * getHeight();
        return (float)mCurrentState.mProgress / (float)num_pixels;
    }

    /** Gets the width of the current Mandelbrot area */
    public double getAreaWidth() {
        return mCurrentState.mWidth;
    }
    
    /** Indicates if the current history stack is empty */
    public boolean isHistoryEmpty() {
        return mHistoryStack.size() == 0;
    }
    
    public String getDescription() {
        // No support for java.util.Formatter or String.format yet :(
        StringBuilder sb = new StringBuilder();
        sb.append("Iter: " + cut(Integer.toString(mCurrentState.mMaxIter)));
        sb.append(", X:" + cut(Double.toString(mCurrentState.mXCenter)));
        sb.append(", Y:" + cut(Double.toString(mCurrentState.mYCenter)));
        sb.append(", Width:" + cut(Double.toString(mCurrentState.mWidth)));
        return sb.toString();
    }
    
    private String cut(String num) {
        return num.substring(0, Math.min(6, num.length()));
    }

    /**
     * Starts an asynchronous thread to perform the computation. Do not call
     * twice in a row -- you must stop a current thread before starting a new
     * one.
     */
    public void start() {
        if (mBuilderThread ==  null) {
            mBuilderThread = new BuilderThread();
            // We're going to do a lot of computation so let's not starve the
            // limited CPU resource.
            mBuilderThread.setPriority(Thread.MIN_PRIORITY);
        }
        if (!isCompleted()) {
            mBuilderThread.start();
        }
    }
    
    /**
     * Stops the asynchronous thread if running.
     * @throws InterruptedException if the thread can't be stopped.
     */
    public void stop() {
        if (mBuilderThread != null) {
            mBuilderThread.setShouldStop(true);
            try {
                mBuilderThread.join();
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            mBuilderThread = null;
        }
    }

    /**
     * Tells the Mandelbrot builder to change its computation area.
     * This is called by the bitmap view. Since the bitmap view has no knowledge
     * of the area currently covered by the builder, it just used fractions
     * relative to the screen, i.e. to the current area. It's up to the builder
     * to convert that to meaningful values.
     * 
     * Note that current the desired height is ignored. Instead we just keep
     * an aspect ratio that matches the final bitmap assuming pixels are square.
     * 
     * @param centerX The new X center, 0=left, 1=right.
     * @param centerY The new Y center, 0=top, 1=bottom.
     * @param width The new width, as a percentage of the screen width
     * @param height The new height, as a percentage of the screen height 
     */
    public void changeArea(RectF selection) {        
        // Stop current computation, save current state so that we can restore
        // it later
        stop();
        while (mHistoryStack.size() >= mPrefHistoryDepth) {
            mHistoryStack.remove(0);
        }
        mHistoryStack.add(mCurrentState);
        
        // The new center is the left-top coord + half of the current size
        // plus a ratio of the size to the new center.
        double sx = getWidth();
        double sy = getHeight();
        double curw = mCurrentState.mWidth;
        double curh = curw * sy / sx;
        double xc = selection.centerX() / sx;
        xc = mCurrentState.mXCenter - curw / 2 + curw * xc;
        double yc = selection.centerY() / sy;
        yc = mCurrentState.mYCenter - curh / 2 + curh * yc;
        double w = curw * selection.width() / sx;

        // Dynamically adapt the number of iterations to the width:
        // width 3..1 => 20 iter
        // width 0.1 => 60 iter
        // width 0.01 => 120
        int max_iter = Math.max(mPrefMinIter, (int)(mPrefStepIter * Math.log10(1.0 / w)));

        mCurrentState = new State(xc, yc, w, max_iter, getWidth(), getHeight());
    }
    
    /**
     * Restore the previous zoom area from the stack.
     * 
     * @return True if stack was not empty and an old zoom area has been restored.
     */
    public boolean popHistory() {
        if (mHistoryStack.size() > 0) {
            mCurrentState = mHistoryStack.remove(mHistoryStack.size() - 1);
            return true;
        }
        return false;
    }

    /**
     * Saves the current state of the builder.
     * This will be restored via createFromState().
     */
    public void saveState(Bundle bundle) {
        bundle.putInt("w", new Integer(mWidth));
        bundle.putInt("h", new Integer(mHeight));
        mCurrentState.saveToBundle("s-", bundle);
        bundle.putInt("hl", mHistoryStack.size());
        for (int i = 0; i < mHistoryStack.size(); i++) {
            mHistoryStack.get(i).saveToBundle(Integer.toString(i) + "-", bundle); 
        }
    }

    public static MandelbrotBuilder createFromState(Bundle bundle) {
        int w = bundle.getInt("w");
        int h = bundle.getInt("h");        
        int hl = bundle.getInt("hl");
        MandelbrotBuilder mb = new MandelbrotBuilder(w, h);
        mb.mCurrentState = State.createFromBundle("s-", bundle);
        for (int i = 0; i < hl; i++) {
            mb.mHistoryStack.add(State.createFromBundle(Integer.toString(i) + "-", bundle)); 
        }
        return mb;
    }
    
    // -----------------------------------------------------------------------

    /**
     * Thread that does the actual computation.
     * 
     * Implements the builder thread using a multiple pass
     * computation over the complex plane.
     */
    private class BuilderThread extends Thread {
        private boolean mContinue;
        private Bitmap mBitmap;
        private Canvas mCanvas;
        private Paint mPaint;
        private Rect mRect;

        /**
         * Starts the thread for the given builder.
         */
        public BuilderThread() {
            mContinue = true;
        }
        
        /**
         * Set to true to indicate the worker thread should stop as soon
         * as possible. The thread will terminate its current computation and
         * leave the set in a correct state.
         */
        public void setShouldStop(boolean shouldStop) {
            mContinue = !shouldStop;
        }

        /**
         * Computes the Mandelbrot set using a multiple pass method.
         * For a step N > 1, computes:
         * 0,0 for first pass and fills a N*N square
         * 1,1 and fills a N/2 * N/2 square
         * 1,0 ''
         * 0,1 ''
         * N is 1 << pass_number so f.ex. if we start at pass 8, we fill
         * a 256x256 square and at pass 1 we fill a 2x2 square. Since we do
         * always four quadrants there's no pass 0.
         */
        @Override
		public void run() {
            mBitmap = mCurrentState.mBitmap;
            mCanvas = new Canvas(mBitmap);
            mPaint = new Paint();
            mPaint.setStyle(Style.FILL);

            int sx = getWidth();
            int sy = getHeight();
            int max_iter = mCurrentState.mMaxIter;
            double x0 = mCurrentState.mXCenter - mCurrentState.mWidth / 2.0;
            double sx1 = mCurrentState.mWidth / sx;
            double sy1 = sx1;  // Assumes pixels are square
            double y0 = mCurrentState.mYCenter - 0.5 * sy1 * sy;
            int i1 = mCurrentState.mStartColumn;
            int j1 = mCurrentState.mStartLine;
            int pn = mCurrentState.mMaxPass;
            int pc = mCurrentState.mStartPass;
            for ( ; pc > 0 && mContinue; pc--) {
                int n = 1 << pc;
                int n2 = n >> 1;
                boolean is_firt_pass = (pn == pc);
                mRect = new Rect(0, 0, n2, n2);

                for (int j2 = j1 + n2; j1 < sy && mContinue; j1 += n, j2 += n) {
                    double y1 = y0 + j1 * sy1;
                    double y2 = y0 + j2 * sy1;
                    for (int i2 = i1 + n2; i1 < sx && mContinue; i1 += n, i2 += n) {
                        double x1 = x0 + i1 * sx1;
                        double x2 = x0 + i2 * sx1;
                        if (is_firt_pass) square(x1, y1, i1, j1, max_iter, n2);
                        square(x2, y1, i2, j1, max_iter, n2);
                        square(x1, y2, i1, j2, max_iter, n2);
                        square(x2, y2, i2, j2, max_iter, n2);
                        mCurrentState.mProgress += (is_firt_pass ? 4 : 3);
                    } // for i
                    if (mContinue) i1 = 0;
                } // for j
                if (mContinue) j1 = 0;
            } // for pc
            mCurrentState.mStartColumn = i1;
            mCurrentState.mStartLine = j1;
            mCurrentState.mStartPass = pc;

            // Tell the listener if we finished building without interruptions
            // TODO if (mContinue && mFinishListener != null) mFinishListener.run();
        }

        private void square(double x, double y, int i, int j, int max_iter, int n) {
            int c = computePoint(x, y, max_iter);
            c = colorIndex(c, max_iter);
            
            if (n > 1) {
                mRect.offsetTo(i, j);
                mPaint.setColor(c);
                mCanvas.drawRect(mRect, mPaint);
            } else {
                mBitmap.setPixel(i, j, c);
            }
        }

        /**
         * Computes a given Mandelbrot point.
         */
        private int computePoint(double x0, double y0, int max_iter) {
            double x = x0;
            double y = y0;
            double x2 = x * x;
            double y2 = y * y;
            int iter = 0;
            while (x2 + y2 < 4 && iter < max_iter) {
                double xtemp = x2 - y2 + x0;
                y = 2 * x * y + y0;
                x = xtemp;
                x2 = x * x;
                y2 = y * y;
                ++iter;
            }
            
            return iter;
        }

        // We'll hack a quick fixed palette with a gradient.
        // TODO Make palette customizable
        // color is ARGB with A=FF
        // we'll do B=0x80 => 0xFF
        // and RG=0x00 => 0xFF
        private int colorIndex(int iter, int max_iter) {
            float col_factor1 = 255.0f / max_iter;
            float col_factor2 = 223.0f * 2/ max_iter; // 0xFF-0x20=xDF=255-32=223
            int rg, b;
            if (iter >= max_iter) {
                rg = b = 0;
            } else  {
                rg = (int)(iter * col_factor1);
                b  = (int)(0x20 + iter * col_factor2);
            }
            return 0xFF000000 | (rg << 16) | (rg << 8) | (b);
        }
    }

}
