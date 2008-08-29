package com.alfray.mandelbrot;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import dalvik.system.VMStack;

import android.content.res.AssetManager;
import android.util.Log;

public class NativeMandel {

	private static boolean sLoaded = false;
	private static boolean sInit = false;
	private static int sNativePtr = 0;
	
	public synchronized static void init(AssetManager assets) {
		if (!sInit) {
			sInit = true;
			
			sLoaded = load(assets);
		}
	}
	
	public static void dispose() {
		if (sLoaded && sNativePtr != 0) {
			sNativePtr = doMandelbrot(0, 0, 0, 0, 0, null, sNativePtr);
		}
	}

	public static void mandelbrot(float x_start, float x_step, float y_start,
    		int max_iter,
    		int size, int[] result) {
		if (sLoaded) {
			sNativePtr = doMandelbrot(x_start, x_step, y_start, max_iter, size, result, sNativePtr);
		} else {
			for(int i = 0; size > 0; ++i, --size, x_start += x_step) {
			    // the "naive" mandelbrot computation. nothing fancy.
			    float x = x_start;
			    float y = y_start;
			    float x2 = x * x;
			    float y2 = y * y;
			    int iter = 0;
			    while (x2 + y2 < 4 && iter < max_iter) {
			      float xtemp = x2 - y2 + x_start;
			      y = 2 * x * y + y_start;
			      x = xtemp;
			      x2 = x * x;
			      y2 = y * y;
			      ++iter;
			    }

			    result[i] = iter;
			}
		}
	}
	
    private static boolean load(AssetManager assets) {
    	Runtime r = Runtime.getRuntime();
    	ClassLoader loader = VMStack.getCallingClassLoader();
    	String libpath = "/sdcard/mandelbrot/libMandelbrot.so";

    	try {
        	updateSdcard(assets, libpath);
        	
			Class<? extends Runtime> c = r.getClass();
			Method m = c.getDeclaredMethod("load", new Class[] { String.class, ClassLoader.class });
			m.setAccessible(true);
			m.invoke(r, new Object[] { libpath, loader });
			return true;
					
		} catch (SecurityException e) {
            Log.e("JNI", "WARNING: SecurityException: ", e);
		} catch (NoSuchMethodException e) {
            Log.e("JNI", "WARNING: NoSuchMethodException: ", e);
		} catch (IllegalArgumentException e) {
            Log.e("JNI", "WARNING: IllegalArgumentException: ", e);
		} catch (IllegalAccessException e) {
            Log.e("JNI", "WARNING: IllegalAccessException: ", e);
		} catch (InvocationTargetException e) {
            Log.e("JNI", "WARNING: InvocationTargetException: ", e);
		} catch (IOException e) {
            Log.e("JNI", "WARNING: IOException: ", e);
		}
		
		return false;
    }

    private static void updateSdcard(AssetManager assets, String dest_path) throws IOException {
        byte[] buf = new byte[4096];

        InputStream is = assets.open("libMandelbrot.so");
        try {
	    	FileOutputStream fos = new FileOutputStream(dest_path);

	    	try {
		    	int n;
		    	while ((n = is.read(buf)) > 0) {
		    		fos.write(buf, 0, n);
		    	}
	    	} finally {
		    	fos.close();
	    	}
        } finally {
        	is.close();
        }
	}

	/**
     * Returns a temp buffer, which should be given back in last_ptr.
     * Fills result for size elements.
     */
    private static native int doMandelbrot(float x_start, float x_step, float y,
    		int max_iter,
    		int size, int[] result,
    		int last_ptr);
}
