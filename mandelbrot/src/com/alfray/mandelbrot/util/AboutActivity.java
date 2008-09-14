/*
 * (c) ralfoide gmail com, 2008
 * Project: asqare
 * License TBD
 */

package com.alfray.mandelbrot.util;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;

import com.alfray.mandelbrot.R;

public class AboutActivity extends Activity {

	private static final String TAG = "AboutActivity";

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	
	    setContentView(R.layout.about);
	    setTitle(R.string.about_title);
	    WebView wv = (WebView) findViewById(R.id.web);

	    wv.loadUrl("file:///android_asset/about.html");
	    wv.setFocusable(true);
	    wv.setFocusableInTouchMode(true);
        wv.requestFocus();
	}
}
