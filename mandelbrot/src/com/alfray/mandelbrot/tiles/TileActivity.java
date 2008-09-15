/*
 * (c) ralfoide gmail com, 2008
 * Project: Mandelbrot
 * License TBD
 */

package com.alfray.mandelbrot.tiles;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.ZoomControls;

import com.alfray.mandelbrot.R;
import com.alfray.mandelbrot.util.AboutActivity;


public class TileActivity extends Activity {

    private TileContext mTileContext;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle inState) {
        super.onCreate(inState);
    
        setContentView(R.layout.tiles);
        
        TextView textView = (TextView) findViewById(R.id.text);

        TileView tileView = (TileView) findViewById(R.id.tile_view);
        tileView.requestFocus();
        
        ZoomControls zoomer = (ZoomControls) findViewById(R.id.zoomer);
        
        mTileContext = new TileContext();
        mTileContext.setView(tileView);
        mTileContext.setZoomer(zoomer);
        mTileContext.setText(textView);
        tileView.setTileContext(mTileContext);
        mTileContext.resetState(inState);
    }
    
    @Override
    protected void onResume() {
        super.onResume();
    }
    
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        mTileContext.saveState(outState);
        super.onSaveInstanceState(outState);
    }
    
    @Override
    protected void onPause() {
        super.onPause();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, R.string.about,         0, R.string.about);
        menu.add(0, R.string.interesting,   0, R.string.interesting);
        menu.add(0, R.string.reset,         0, R.string.reset);
        menu.add(0, R.string.zoom_in,       0, R.string.zoom_in);
        menu.add(0, R.string.zoom_out,      0, R.string.zoom_out);
        menu.add(0, R.string.capture,       0, R.string.capture).setEnabled(false);
        menu.add(0, R.string.wallpaper,     0, R.string.wallpaper).setEnabled(false);
        return super.onCreateOptionsMenu(menu);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        switch(item.getItemId()) {
        case R.string.reset:
            mTileContext.resetState(null /*bundle*/);
            break;
        case R.string.interesting:
            mTileContext.panToInterestingPlace();
            break;
        case R.string.zoom_in:
            mTileContext.zoom(true);
            break;
        case R.string.zoom_out:
            mTileContext.zoom(false);
            break;
        case R.string.about:
            intent = new Intent(this, AboutActivity.class);
            startActivity(intent);
            break;
        case R.string.capture:
            break;
        case R.string.wallpaper:
            break;
        }
        return super.onOptionsItemSelected(item);
    }
}
