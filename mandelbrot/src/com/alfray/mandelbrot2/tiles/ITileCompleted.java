/*
 * Copyright 2008 (c) ralfoide gmail com, 2008
 * Project: Mandelbrot
 * License: GPL version 3 or any later version
 */


package com.alfray.mandelbrot2.tiles;

/**
 * Provides a callback used by the tile thread when a tile is done being computed.
 */
public interface ITileCompleted {
    public void onTileCompleted(Tile tile);
}
