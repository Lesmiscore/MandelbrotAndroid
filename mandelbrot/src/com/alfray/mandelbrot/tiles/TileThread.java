/*
 * (c) ralfoide gmail com, 2008
 * Project: Mandelbrot
 * License: GPL version 3 or any later version
 */

package com.alfray.mandelbrot.tiles;

import java.util.LinkedList;

import android.util.Log;

import com.alfray.mandelbrot.util.BaseThread;

public class TileThread extends BaseThread {

    private static final String TAG = "TileContext";

    private LinkedList<Tile> mLifoList;
    private ITileCompleted mTileCompleted;

    public TileThread() {
        super("TileThread");
        
        mLifoList = new LinkedList<Tile>();
    }
    
    public void setCompletedCallback(ITileCompleted callback) {
        mTileCompleted = callback;
    }

    public void schedule(Tile t) {
        if (t != null) {
            Log.d(TAG, "schedule: " + t.toString());
            synchronized(mLifoList) {
            	mLifoList.addFirst(t);
            }
            wakeUp();
        }
    }

    @Override
    public void clear() {
        synchronized(mLifoList) {
        	mLifoList.clear();
        }
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
    	Tile t = null;
        synchronized(mLifoList) {
        	t = mLifoList.poll();
        }
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
