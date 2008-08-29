package com.alfray.mandelbrot;

import android.util.Log;

public class NativeMandel {
    static {
        try {
            Log.i("JNI", "Trying to load libNativeAdd.so");
            System.loadLibrary("NativeAdd");
        }
        catch (UnsatisfiedLinkError ule) {
            Log.e("JNI", "WARNING: Could not load libNativeAdd.so");
        }
    }

    public static native long add(long a, long b);
}

