/*
 * Copyright 2008 (c) ralfoide gmail com, 2008
 * Project: Mandelbrot
 * License: GPL version 3 or any later version
 */

package com.alfray.mandelbrot2.tiles;

import android.util.SparseArray;

//-----------------------------------------------

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


