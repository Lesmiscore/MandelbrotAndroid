/*
 * (c) ralfoide gmail com, 2008
 * Project: Mandelbrot
 * License: GPL version 3 or any later version
 */

package com.alfray.mandelbrot.tiles;

import java.io.File;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ZoomControls;

import com.alfray.mandelbrot.R;
import com.alfray.mandelbrot.tiles.TileContext.ImageGenerator;
import com.alfray.mandelbrot.util.AboutActivity;


public class TileActivity extends Activity {

    private static final int DLG_SAVE_IMG = 0;
    private static final int DLG_WALLPAPER = 1;
    
	private static final int MENU_GRP_IMG = 1;

    private TileContext mTileContext;
	private ImageGenerator mImageGenerator;

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
        menu.add(0, R.string.about,         0, R.string.about)
            .setIcon(R.drawable.ic_menu_info_details);
        menu.add(0, R.string.interesting,   0, R.string.interesting)
            .setIcon(R.drawable.ic_menu_myplaces);
        menu.add(0, R.string.reset,         0, R.string.reset)
            .setIcon(R.drawable.ic_menu_mapmode);
        menu.add(0, R.string.zoom_in,       0, R.string.zoom_in)
            .setIcon(R.drawable.btn_flicker_plus);
        menu.add(0, R.string.zoom_out,      0, R.string.zoom_out)
            .setIcon(R.drawable.btn_flicker_minus);
        menu.add(MENU_GRP_IMG, R.string.save_image,    0, R.string.save_image)
            .setIcon(R.drawable.ic_menu_save);
        menu.add(MENU_GRP_IMG, R.string.wallpaper,     0, R.string.wallpaper)
            .setIcon(R.drawable.ic_menu_save);
        return super.onCreateOptionsMenu(menu);
    }
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
    	menu.setGroupEnabled(MENU_GRP_IMG, mImageGenerator == null);
    	return super.onPrepareOptionsMenu(menu);
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
        case R.string.save_image:
            saveImage();
            break;
        case R.string.wallpaper:
            saveWallpaper();
            break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void saveImage() {
        // create dir on sdcard and complain if it can't be found or created
        File d = new File("/sdcard/mandelbrot");
        if (!d.isDirectory() && !d.mkdir()) {
            Toast t = Toast.makeText(this,
                    "Cannot save image.\nIs the SD Card available?",
                    Toast.LENGTH_SHORT);
            t.show();
            return;
        }
        
        showDialog(DLG_SAVE_IMG);
    }

    private void saveWallpaper() {
        showDialog(DLG_WALLPAPER);
    }
    
    @Override
    protected Dialog onCreateDialog(final int id) {
        ProgressDialog dialog = new ProgressDialog(this);
        dialog.setMessage("Please wait while the image gets generated...");
        dialog.setIndeterminate(true);
        dialog.setCancelable(true);

    	int sx = 0;
    	int sy = 0;
        if (id == DLG_WALLPAPER) {
    		sx = getWallpaperDesiredMinimumWidth();
    		sy = getWallpaperDesiredMinimumHeight();
            dialog.setTitle("Generating Wallpaper");
        } else {
            dialog.setTitle("Generating Image");
        }

        mImageGenerator = mTileContext.newImageGenerator(sx, sy,
        		new Runnable() {
					public void run() {
						mImageGenerator = null;
						removeDialog(id);
					}
        });

        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
			public void onCancel(DialogInterface dialog) {
				ImageGenerator t = mImageGenerator;
				mImageGenerator = null;
				if (t != null) t.cancel();
				removeDialog(id);
			}
        });

        mImageGenerator.start();
        return dialog;
    }
}
