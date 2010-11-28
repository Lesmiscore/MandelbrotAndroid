/*
 * Copyright 2008 (c) ralfoide gmail com, 2008
 * Project: Mandelbrot
 * License: GPL version 3 or any later version
 */

package com.alfray.mandelbrot2.tiles;

import android.util.SparseArray;

//-----------------------------------------------

/** @deprecated not used yet.
 * The idea was to extract the tile cache that is intermixed in {@link TileContext}.
 * I never got around to it, so this is left as an exercise to the reader :-)
 */
public class LevelTileCache {

    public static class TileCache extends SparseArray<Tile> {
    }

    private static LevelTileCache sThis;

    private SparseArray<TileCache> mLevelCache;


    private LevelTileCache() {
    }

    public static LevelTileCache getInstance() {
        sThis = new LevelTileCache();
        return sThis;
    }

}


