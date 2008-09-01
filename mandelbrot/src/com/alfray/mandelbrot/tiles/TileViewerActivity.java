/*
 * (c) ralfoide gmail com, 2008
 * Project: Mandelbrot
 * License TBD
 */

package com.alfray.mandelbrot.tiles;

import com.alfray.mandelbrot.NativeMandel;
import com.alfray.mandelbrot.R;
import com.alfray.mandelbrot.util.GLSurfaceView;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;


public class TileViewerActivity extends Activity {

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    
        setContentView(R.layout.tiles);
        
        TextView t = (TextView) findViewById(R.id.text);
        GLSurfaceView gl_view = (GLSurfaceView) findViewById(R.id.gl_view);
    }
}
