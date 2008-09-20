/*
 * Copyright 2008 (c) ralfoide gmail com, 2008
 * Project: Mandelbrot
 * License: GPL version 3 or any later version
 */

package com.alfray.mandelbrot.tiles;

import java.util.LinkedList;
import java.util.ListIterator;

import android.util.Log;

import com.alfray.mandelbrot.util.BaseThread;

public class TileThread extends BaseThread {

    private static final String TAG = "TileContext";

    /** List of pending tiles to compute */
    private LinkedList<Tile> mPendingList;
    /** List of all tiles created here that have memory to reclaim */
    private LinkedList<Tile> mMemoryList;
    /** Callback to call when a tile computation is completed */
    private ITileCompleted mTileCompleted;

    public TileThread() {
        super("TileThread");
        
        mPendingList = new LinkedList<Tile>();
        mMemoryList = new LinkedList<Tile>();
    }
    
    public void setCompletedCallback(ITileCompleted callback) {
        mTileCompleted = callback;
    }

    public void schedule(Tile t) {
        if (t != null) {
            Log.d(TAG, "schedule: " + t.toString());
            synchronized(mPendingList) {
            	mPendingList.addFirst(t);
            }
            wakeUp();
        }
    }

    @Override
    public void clear() {
        synchronized(mPendingList) {
        	mPendingList.clear();
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
        synchronized(mPendingList) {
        	t = mPendingList.poll();
        }
        if (t != null) {
            Log.d(TAG, "compute: " + t.toString());

            try {
                for (int i = 0 ; i < 2; i++) {
                    try {
                        t.compute();
                        if (mTileCompleted != null) {
                            mTileCompleted.onTileCompleted(t);
                        }
                        mMemoryList.add(t);
                        return;
                    } catch (OutOfMemoryError e) {
                        reclaimTiles(t.getZoomLevel());
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Uncatched Exception during tile " + t.toString(), e);
            } catch (Throwable th) {
                Log.e(TAG, "Uncatched Throwable during tile " + t.toString() + " : " + th.getMessage());
            }
        } else {
            waitForALongTime();
        }
    }

    /**
     * Frees all tiles from a different level,
     * at most half of the tiles,
     * and at least 2 tiles.
     */
    private void reclaimTiles(int level) {
        if (mMemoryList == null) return;
        if (mMemoryList.isEmpty()) return;

        // always free the first tile
        mMemoryList.poll().reclaimBitmap();

        // now is a good time to GC
        System.gc();

        // now free some more
        int n = mMemoryList.size() / 2;
        int r = 1;
        for (ListIterator<Tile> it = mMemoryList.listIterator();
                it.hasNext() && r < n;
                ) {
            Tile t = it.next();
            if (t.getZoomLevel() != level && t.reclaimBitmap()) {
                it.remove();
                r++;
            }
        }

        // always free at least two of them
        if (r == 1 && !mMemoryList.isEmpty()) {
            mMemoryList.poll().reclaimBitmap();
            r++;
        }
        
        Log.d(TAG, "Reclaimed: " + Integer.toString(r) + " tiles");

        // now is a good time to GC
        System.gc();
    }
}
