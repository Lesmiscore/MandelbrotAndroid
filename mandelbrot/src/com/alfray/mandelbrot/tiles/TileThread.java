/*
 * (c) ralfoide gmail com, 2008
 * Project: Mandelbrot
 * License TBD
 */

package com.alfray.mandelbrot.tiles;

import java.util.concurrent.ConcurrentLinkedQueue;

import android.util.Log;

import com.alfray.mandelbrot.util.BaseThread;

public class TileThread extends BaseThread {

    private static final String TAG = "TileContext";

    private ConcurrentLinkedQueue<Tile> mQueue;
    private ITileCompleted mTileCompleted;

    public TileThread() {
        super("TileThread");
        
        mQueue = new ConcurrentLinkedQueue<Tile>();
    }
    
    public void setCompletedCallback(ITileCompleted callback) {
        mTileCompleted = callback;
        
    }

    public void schedule(Tile t) {
        if (t != null) {
            Log.d(TAG, "schedule: " + t.toString());
            mQueue.add(t);
            wakeUp();
        }
    }

    @Override
    public void clear() {
        mQueue.clear();
    }

    @Override
    protected void startRun() {
        Log.d(TAG, "Start");
    }

    @Override
    protected void endRun() {
        Log.d(TAG, "End");
    }

    @Override
    protected void runIteration() {
        Tile t = mQueue.poll();
        if (t != null) {
            Log.d(TAG, "Compute " + t.toString());
            t.compute();
            if (mTileCompleted != null) {
                mTileCompleted.onTileCompleted(t);
            }
        } else {
            waitForALongTime();
        }
    }
}
