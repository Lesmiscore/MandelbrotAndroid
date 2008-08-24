package com.alfray.mandelbrot.old;

import android.app.TabActivity;
import android.os.Bundle;
import android.view.Window;

//-----------------------------------------------

/**
 * Accessory activity class used to display help and about information
 * for the Mandelbrot activity.
 */
public class InfoActivity extends TabActivity {

    private static final String TAB_HELP = "tab_help";
    private static final String TAB_ABOUT = "tab_about";

    /**
     * Called when the activity is created.
     * 
     * Simply initialized the tab host with the 2 default tabs and
     * selects the first one.
     */
    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        // TODO missing in 28797?
        // setFloatingWindow(true);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        /* Needs to be rewritten for TC2-RC6
        TabHost tb = getTabHost();
        getViewInflate().inflate(R.layout.info, tb.getTabContentView());
        
        tb.addTab(TAB_HELP,  R.id.tab_help,  "Help");
        tb.addTab(TAB_ABOUT, R.id.tab_about, "About");
        tb.setCurrentTabByTag(TAB_HELP);
        */
    }
}


