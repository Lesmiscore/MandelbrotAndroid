/*
 * (c) ralfoide gmail com, 2008
 * Project: Mandelbrot
 * License TBD
 */

package com.alfray.mandelbrot.tiles;

import com.alfray.mandelbrot.NativeMandel;
import com.alfray.mandelbrot.R;

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
        
        TextView t = (TextView)findViewById(R.id.text);
        
        try {
        	boolean loaded = false; //NativeMandel.loadDirect();
	        if (loaded) {
	        	long a = 0; // NativeMandel.add(42, 24);
	        
	        	t.setText((loaded ? "" : "un") + "loaded... " + Long.toString(a));
	        }
        } catch (Throwable tr) {
        	Log.d("mandeltest", "Throwable: " + tr.getMessage());
        	t.setText("Throwable: " + tr.getMessage());
        }
    }
}
