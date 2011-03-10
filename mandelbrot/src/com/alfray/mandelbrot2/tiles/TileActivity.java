/*
 * Copyright 2008 (c) ralfoide gmail com, 2008
 * Project: Mandelbrot
 * License: GPL version 3 or any later version
 */

package com.alfray.mandelbrot2.tiles;

import com.alfray.mandelbrot2.JavaMandel;
import com.alfray.mandelbrot2.R;
import com.alfray.mandelbrot2.prefs.PrefsActivity;
import com.alfray.mandelbrot2.tests.TestActivity;
import com.alfray.mandelbrot2.tiles.TileContext.ImageGenerator;
import com.alfray.mandelbrot2.util.AboutActivity;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.AlertDialog.Builder;
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
import android.view.SubMenu;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ZoomControls;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;


public class TileActivity extends Activity {

    private static final String TAG = "TileActivity";

    private static final int DLG_SAVE_IMG = 0;
    private static final int DLG_WALLPAPER = 1;

    private static final int MENU_GRP_IMG = 1;

    private TileContext mTileContext;
    private ImageGenerator mImageGenerator;

    private TileActivity mActivity;

    private int mOrientation;


    private static final int ORIENT_MAX = 3;
    private static final int[] ORIENT_SET = {
        ActivityInfo.SCREEN_ORIENTATION_USER,
        ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
        ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE,
        ActivityInfo.SCREEN_ORIENTATION_SENSOR,
        };
    private static final int[] ORIENT_STR = {
        R.string.orient_default,
        R.string.orient_portrait,
        R.string.orient_land,
        R.string.orient_sensor,
        };

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle inState) {
        super.onCreate(inState);

        JavaMandel.init(this);

        if (inState != null) {
            mOrientation = inState.getInt("orient");
            setOrientation();
        }

        setContentView(R.layout.tiles);

        mActivity = this;

        TextView textView = (TextView) findViewById(R.id.text);

        TileView tileView = (TileView) findViewById(R.id.tile_view);
        tileView.requestFocus();

        ZoomControls zoomer = (ZoomControls) findViewById(R.id.zoomer);

        mTileContext = new TileContext(getLastNonConfigurationInstance());
        mTileContext.setView(tileView);
        mTileContext.setZoomer(zoomer);
        mTileContext.setText(textView);
        tileView.setTileContext(mTileContext);
        mTileContext.resetState(inState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mTileContext.pause(false);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        mTileContext.saveState(outState);
        outState.putInt("orient", mOrientation);
        super.onSaveInstanceState(outState);
    }

    @Override
    public Object onRetainNonConfigurationInstance() {
        return mTileContext.getNonConfigurationInstance();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mTileContext.pause(true);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mTileContext.destroy();
        mTileContext = null;
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

        SubMenu sub = menu.addSubMenu(R.string.orient);
        sub.add(0, R.string.orient_default,  0, R.string.orient_default).setCheckable(true);
        sub.add(0, R.string.orient_portrait, 0, R.string.orient_portrait).setCheckable(true);
        sub.add(0, R.string.orient_land,     0, R.string.orient_land).setCheckable(true);
        sub.add(0, R.string.orient_sensor,   0, R.string.orient_sensor).setCheckable(true);

        menu.add(0, R.string.fly_mode, 0, R.string.fly_mode).setCheckable(true);
        menu.add(0, R.string.test_mode, 0, R.string.test_mode);
        menu.add(0, R.string.settings, 0, R.string.settings);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.setGroupEnabled(MENU_GRP_IMG, mImageGenerator == null);
        for (int orient = 0; orient <= ORIENT_MAX; orient++) {
            menu.findItem(ORIENT_STR[orient]).setChecked(mOrientation == orient);
        }

        menu.findItem(R.string.fly_mode).setChecked(mTileContext.inFlyMode());

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        int id = item.getItemId();
        switch(id) {
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
        case R.string.fly_mode:
            toggleFlyMode();
            break;
        case R.string.test_mode:
            intent = new Intent(this, TestActivity.class);
            startActivity(intent);
            break;
        case R.string.settings:
            intent = new Intent(this, PrefsActivity.class);
            startActivity(intent);
            break;
        }

        for (int orient = 0; orient <= ORIENT_MAX; orient++) {
            if (id == ORIENT_STR[orient]) {
                mOrientation = orient;
                setOrientation();
                break;
            }
        }

        return super.onOptionsItemSelected(item);
    }

    private void setOrientation() {
        if (mOrientation >= 0 && mOrientation <= ORIENT_MAX) {
            setRequestedOrientation(ORIENT_SET[mOrientation]);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_T && event.isShiftPressed()) {
            startActivity(new Intent(this, TestActivity.class));
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_F) {
            toggleFlyMode();
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

        mImageGenerator = mTileContext.newImageGenerator(sx, sy, activity,
                        new ImageGeneratorDone(dialog, id));

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

                    final String name = String.format(
                                    "/sdcard/mandelbrot/%d.png", System
                                                    .currentTimeMillis());
                    FileOutputStream fos;
                    try {
                        fos = new FileOutputStream(name);
                        BufferedOutputStream bos = new BufferedOutputStream(
                                        fos, 8192);

                        boolean ok = bmp.compress(Bitmap.CompressFormat.PNG,
                                        100 /* quality */, bos);

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
                                        mScanner.scanFile(name,
                                                          null /* mimeType */);
                                    }

                                public void onScanCompleted(String path, Uri uri) {
                                    if (path.equals(name)) {
                                        mActivity.runOnUiThread(new Runnable() {
                                            public void run() {
                                                Toast.makeText(
                                                    getApplicationContext(),
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

                        toastResult = ok ? "Image saved successfully"
                                        : "Failed to save image";
                    } catch (FileNotFoundException e) {
                        toastResult = "Could not write to file";
                        Log.e(TAG, "Can't open file for writing: " + name, e);
                    }
                }
            } finally {
                mImageGenerator = null;
                removeDialog(mId);
                if (toastResult != null) {
                    Toast.makeText(mActivity, toastResult, Toast.LENGTH_SHORT)
                                    .show();
                }
            }
        }
    }

    // ---------- fly mode ------------------------------

    private void toggleFlyMode() {

        if (mTileContext.inFlyMode()) {
            mTileContext.stopFlyMode();
        } else {
            mTileContext.startFlyMode(this, new Runnable() {
                public void run() {
                    // Stop was called (either end of fly mode or aborted)
                    String s = String.format("Fly mode finished in %.1f seconds.",
                                    mTileContext.getFlyModeTime());
                    Log.d(TAG, s);

                    Builder b = new AlertDialog.Builder(TileActivity.this);
                    b.setMessage(s);
                    b.setPositiveButton("Dismiss", null);

                    AlertDialog d = b.create();
                    d.show();
                }
            });
        }
    }
}
