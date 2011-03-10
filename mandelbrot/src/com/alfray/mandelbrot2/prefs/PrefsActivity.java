package com.alfray.mandelbrot2.prefs;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.Preference.OnPreferenceChangeListener;

import com.alfray.mandelbrot2.JavaMandel;
import com.alfray.mandelbrot2.R;

/**
 * Displays preferences
 */
public class PrefsActivity extends PreferenceActivity {

    protected boolean mDataChanged = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.prefs);

        String appName = getString(R.string.app_name);
        setTitle(getString(R.string.prefs_title).replaceAll("\\$APP", appName));


        Preference useDataTogglePref = findPreference("use_rs");
        if (useDataTogglePref != null) {
            useDataTogglePref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    mDataChanged  = true;
                    return true;
                }
            });
        }
    }

    @Override
    protected void onPause() {
        if (mDataChanged) {
            JavaMandel.prefsChanged(PrefsActivity.this);
        }
        super.onPause();
    }
}
