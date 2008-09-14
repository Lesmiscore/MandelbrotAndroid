/*
 * (c) ralfoide gmail com, 2008
 * Project: Mandelbrot
 * License TBD
 */

package com.alfray.mandelbrot.tiles;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.ZoomControls;

import com.alfray.mandelbrot.R;


public class TileViewerActivity extends Activity {

    private TileContext mTileContext;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    
        setContentView(R.layout.tiles);
        
        TextView t = (TextView) findViewById(R.id.text);

        TileView tile_view = (TileView) findViewById(R.id.tile_view);
        tile_view.requestFocus();
        
        ZoomControls zoomer = (ZoomControls) findViewById(R.id.zoomer);
        
        mTileContext = new TileContext();
        mTileContext.setView(tile_view);
        mTileContext.setZoomer(zoomer);
        tile_view.setTileContext(mTileContext);
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        mTileContext.resetScreen(); // HACK
    }
    
    @Override
    protected void onPause() {
        super.onPause();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
