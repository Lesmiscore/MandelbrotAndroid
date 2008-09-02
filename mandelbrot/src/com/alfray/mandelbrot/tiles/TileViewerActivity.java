/*
 * (c) ralfoide gmail com, 2008
 * Project: Mandelbrot
 * License TBD
 */

package com.alfray.mandelbrot.tiles;

import android.app.Activity;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.widget.TextView;

import com.alfray.mandelbrot.R;
import com.alfray.mandelbrot.util.GLSurfaceView;


public class TileViewerActivity extends Activity {

    private RenderThread mRenderThread;
    private TileContext mTileContext;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    
        setContentView(R.layout.tiles);
        
        TextView t = (TextView) findViewById(R.id.text);
        GLSurfaceView gl_view = (GLSurfaceView) findViewById(R.id.gl_view);
        gl_view.requestFocus();
        
        SurfaceHolder holder = gl_view.getHolder();
        holder.setType(SurfaceHolder.SURFACE_TYPE_GPU);

        // Note: the GL thread start itself when the SurfaceView is created.
        mRenderThread = new RenderThread(holder);
        
        mTileContext = new TileContext();
        mRenderThread.setTileContext(mTileContext);
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        mTileContext.resetScreen(); // HACK
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        mRenderThread.pauseThread(true);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        mRenderThread.waitForStop();
    }
}
