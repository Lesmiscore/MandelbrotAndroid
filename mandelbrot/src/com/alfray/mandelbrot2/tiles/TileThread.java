/*
 * Copyright 2008 (c) ralfoide gmail com, 2008
 * Project: Mandelbrot
 * License: GPL version 3 or any later version
 */

package com.alfray.mandelbrot2.tiles;

import java.util.LinkedList;
import java.util.ListIterator;

import android.graphics.Bitmap;
import android.util.Log;

import com.alfray.mandelbrot2.util.BaseThread;

public class TileThread extends BaseThread {

    private static final String TAG = "TileContext";
    private static boolean DEBUG = false;

    private static class ImgZoomEntry {
        private final Tile mCurrentTile;
        private final Tile mLargerTile;

        public ImgZoomEntry(Tile currentTile, Tile largerTile) {
            mCurrentTile = currentTile;
            mLargerTile = largerTile;
        }

        public Tile getCurrentTile() {
            return mCurrentTile;
        }

        public Tile getLargerTile() {
            return mLargerTile;
        }
    }

    /** List of pending tiles to compute */
    private LinkedList<Tile> mPendingList;
    /** List of pending titles for quick image zoom */
    private LinkedList<ImgZoomEntry> mImgZoomList;
    /** List of all tiles created here that have memory to reclaim */
    private LinkedList<Tile> mMemoryList;
    /** Callback to call when a tile computation is completed */
    private ITileCompleted mTileCompleted;

    public TileThread() {
        super("TileThread");

        mImgZoomList = new LinkedList<ImgZoomEntry>();
        mPendingList = new LinkedList<Tile>();
        mMemoryList = new LinkedList<Tile>();
    }

    public boolean hasPending() {
        synchronized (mPendingList) {
            return !mPendingList.isEmpty();
        }
    }

    public void setCompletedCallback(ITileCompleted callback) {
        mTileCompleted = callback;
    }

    public void scheduleImgZoom(Tile t, Tile largerTile) {
        if (t != null && largerTile != null) {
            synchronized(mImgZoomList) {
                mImgZoomList.addFirst(new ImgZoomEntry(t, largerTile));
            }
            wakeUp();
        }
    }

    public void schedule(Tile t) {
        if (t != null) {
            if (DEBUG) Log.d(TAG, "schedule: " + t.toString());
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
        // -- process pending image zoom tiles
        try {
            // Reclaim some memory.
            if (mMemoryList.size() > 300) {
                reclaimTiles(-1);
            }

            Tile t = null;
            ImgZoomEntry z = null;
            synchronized(mImgZoomList) {
                z = mImgZoomList.poll();
            }
            if (z != null) {
                for (int i = 0 ; i < 2; i++) {
                    try {
                        t = z.getCurrentTile();
                        t.zoomForLowerLevel(z.getLargerTile());
                        if (mTileCompleted != null) {
                            mTileCompleted.onTileCompleted(t);
                        }
                        if (t.isInMemory()) mMemoryList.remove(t);
                        mMemoryList.add(t);
                        t.setInMemory(true);
                        return;
                    } catch (RuntimeException e) {
                        reclaimTiles(t.getZoomLevel());
                    } catch (OutOfMemoryError e) {
                        reclaimTiles(t.getZoomLevel());
                    }
                }
            }

            // -- process pending tile computations
            synchronized(mPendingList) {
            	t = mPendingList.poll();
            }
            if (t != null) {
            	if (DEBUG) Log.d(TAG, "compute: " + t.toString());

                for (int i = 0 ; i < 2; i++) {
                    try {
                        t.compute();
                        if (mTileCompleted != null) {
                            mTileCompleted.onTileCompleted(t);
                        }
                        if (t.isInMemory()) mMemoryList.remove(t);
                        mMemoryList.add(t);
                        t.setInMemory(true);
                        return;
                    } catch (RuntimeException e) {
                        reclaimTiles(t.getZoomLevel());
                    } catch (OutOfMemoryError e) {
                        reclaimTiles(t.getZoomLevel());
                    }
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "Uncatched Exception ", e);
        } catch (Throwable th) {
            Log.e(TAG, "Uncatched Throwable : " + th.getMessage());
        }

        waitForALongTime();
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
        Tile t = mMemoryList.poll();
        if (t != null) {
            t.setInMemory(false);
            t.reclaimBitmap();
        }

        // now free some more
        int n = mMemoryList.size() / 2;
        int r = 1;
        for (ListIterator<Tile> it = mMemoryList.listIterator();
                it.hasNext() && r < n;
                ) {
            t = it.next();
            if (level < 0 || t.getZoomLevel() != level) {
                Bitmap b = t.reclaimBitmap();
                t.setInMemory(false);
                it.remove();
                if (b != null) r++;
            }
        }

        // always free at least two of them
        if (r == 1 && !mMemoryList.isEmpty()) {
            t = mMemoryList.poll();
            if (t != null) {
                t.setInMemory(false);
                t.reclaimBitmap();
            }
            r++;
        }

        Log.d(TAG, "Reclaimed: " + Integer.toString(r) + " tiles");

        // now is a good time to GC
        System.gc();
    }
}
