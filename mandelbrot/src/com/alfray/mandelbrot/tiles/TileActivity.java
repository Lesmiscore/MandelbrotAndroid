/*
 * Copyright 2008 (c) ralfoide gmail com, 2008
 * Project: Mandelbrot
 * License: GPL version 3 or any later version
 */

package com.alfray.mandelbrot.tiles;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.media.MediaScannerConnection;
import android.media.MediaScannerConnection.MediaScannerConnectionClient;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ZoomControls;

import com.alfray.mandelbrot.R;
import com.alfray.mandelbrot.tests.TestActivity;
import com.alfray.mandelbrot.tiles.TileContext.ImageGenerator;
import com.alfray.mandelbrot.util.AboutActivity;


public class TileActivity extends Activity {

    private static final String TAG = "TileActivity";

    private static final int DLG_SAVE_IMG = 0;
    private static final int DLG_WALLPAPER = 1;
    
	private static final int MENU_GRP_IMG = 1;

    private TileContext mTileContext;
	private ImageGenerator mImageGenerator;

	private TileActivity mActivity;

	private boolean mOrientLandscape;

	private boolean mOrientSensor;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle inState) {
        super.onCreate(inState);

        if (inState != null) {
        	mOrientLandscape = inState.getBoolean("orient-land");
        	mOrientSensor = inState.getBoolean("orient-sensor");
        	setOrientation();
        }

        setContentView(R.layout.tiles);

        mActivity = this;
        
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
    	outState.putBoolean("orient-land", mOrientLandscape);
    	outState.putBoolean("orient-sensor", mOrientSensor);
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
        menu.add(2, R.string.flip_orient,      0, R.string.flip_orient).setCheckable(true);
        menu.add(3, R.string.sensor_orient,      0, R.string.sensor_orient).setCheckable(true);
        return super.onCreateOptionsMenu(menu);
    }
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
    	menu.setGroupEnabled(MENU_GRP_IMG, mImageGenerator == null);
    	menu.findItem(R.string.flip_orient).setChecked(mOrientLandscape);
    	menu.findItem(R.string.sensor_orient).setChecked(mOrientSensor);
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
            startSaveImage();
            break;
        case R.string.wallpaper:
            startSaveWallpaper();
            break;
        case R.string.sensor_orient:
    		mOrientLandscape = false;
    		mOrientSensor = !mOrientSensor;
    		setOrientation();
    		break;
        case R.string.flip_orient:
    		mOrientLandscape = !mOrientLandscape;
    		mOrientSensor = false;
    		setOrientation();
    		break;
        }
        return super.onOptionsItemSelected(item);
    }

	private void setOrientation() {
		if (mOrientSensor) {
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
		} else if (mOrientLandscape) {
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		} else {
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);        		
		}
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_T && event.isShiftPressed()) {
			startActivity(new Intent(this, TestActivity.class));
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	// ---- wallpaper and images -------------------------
	
    private void startSaveImage() {
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

    private void startSaveWallpaper() {
        showDialog(DLG_WALLPAPER);
    }
    
    @Override
    protected Dialog onCreateDialog(final int id) {
    	final Activity activity = this;
        final ProgressDialog dialog = new ProgressDialog(this);
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
        		activity, new ImageGeneratorDone(dialog, id));

        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
			public void onCancel(DialogInterface dialog_interface) {
				dialog.setMessage("Aborting...");
				ImageGenerator t = mImageGenerator;
				mImageGenerator = null;
				if (t != null) t.waitForStop();
				removeDialog(id);
			}
        });

        mImageGenerator.start();
        return dialog;
    }

    /**
     * This runs on the UI thread to save the bitmap actually generated:
     * - Save as a wallpaper.
     * - Save as a PNG and tell the media scanner to scan the file.
     */
    private class ImageGeneratorDone implements Runnable {
    	private final ProgressDialog mDialog;
		private final int mId;
		private MediaScannerConnection mScanner;

		public ImageGeneratorDone(ProgressDialog dialog, int id) {
			mDialog = dialog;
			mId = id;
		}

		public void run() {
			String toastResult = null;
			try {
				Bitmap bmp = mImageGenerator.getBitmap();
				if (mId == DLG_WALLPAPER) {
					mDialog.setMessage("Setting wallpaper...");
					try {
						setWallpaper(bmp);
						toastResult = "Wallpaper set";
					} catch (IOException e) {
						toastResult = "Set wallpaper failed";
						Log.e(TAG, "Set wallpaper failed", e);
					}
				} else if (mId == DLG_SAVE_IMG) {
					mDialog.setMessage("Saving image...");
					
					final String name = String.format("/sdcard/mandelbrot/%d.png", System.currentTimeMillis());
					FileOutputStream fos;
					try {
						fos = new FileOutputStream(name);
						BufferedOutputStream bos = new BufferedOutputStream(fos, 8192);

						boolean ok = bmp.compress(Bitmap.CompressFormat.PNG, 100 /*quality*/, bos);
						
						try {
							bos.close();
							fos.close();
						} catch (IOException e) {
							ok = false;
						}
						
						if (ok) {
							mScanner = new MediaScannerConnection(mActivity,
								new MediaScannerConnectionClient() {
									public void onMediaScannerConnected() {
										mScanner.scanFile(name, null /*mimeType*/);
									}
	
									public void onScanCompleted(String path, Uri uri) {
										if (path.equals(name)) {
											mActivity.runOnUiThread(new Runnable() {
												public void run() {
													Toast
														.makeText(getApplicationContext(),
															"Image now available in Home > Pictures",
															Toast.LENGTH_SHORT)
														.show();
												}
											});
											mScanner.disconnect();
										}
									}
								
							});
							mScanner.connect();
						}

						toastResult = ok ? "Image saved successfully" : "Failed to save image";
					} catch (FileNotFoundException e) {
						toastResult = "Could not write to file";
						Log.e(TAG, "Can't open file for writing: " + name, e);
					}
				}
			} finally {
				mImageGenerator = null;
				removeDialog(mId);
				if (toastResult != null) {
					Toast
						.makeText(mActivity, toastResult, Toast.LENGTH_SHORT)
						.show();;
				}
			}
		}
	}
}
