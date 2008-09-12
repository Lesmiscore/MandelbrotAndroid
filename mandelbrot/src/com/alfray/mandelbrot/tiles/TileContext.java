/*
 * (c) ralfoide gmail com, 2008
 * Project: Mandelbrot
 * License TBD
 */

package com.alfray.mandelbrot.tiles;

public class TileContext {
    
    private int mZoomFp8;
    private Tile mTile1;

    public TileContext() {
    }

    public void resetScreen() {
        mZoomFp8 = Tile.FP8_1;
        if (mTile1 == null) {
            mTile1 = new Tile(mZoomFp8, 0, 0, 20 /*iter*/);
            //mTile1.compute();
        }
    }
    
    public Tile getVisibleTiles() {
        return mTile1;
    }
}
