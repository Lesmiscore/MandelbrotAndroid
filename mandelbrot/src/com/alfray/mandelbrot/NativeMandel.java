package com.alfray.mandelbrot;

import java.io.File;
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
	private static int sNativePtr1 = 0;
	private static int sNativePtr2 = 0;
	
	public synchronized static void init(AssetManager assets) {
		if (!sInit) {
			sInit = true;
			
			sLoaded = load(assets);
		}
	}
	
	public static void dispose() {
		if (sLoaded ) {
			sNativePtr1 = doMandelbrot1(0, 0, 0, 0, 0, null, sNativePtr1);
			sNativePtr2 = doMandelbrot2(0, 0, 0, 0, 0, 0, 0, 0, null, sNativePtr2);
		}
	}

	public static void mandelbrot(float x_start, float x_step, float y_start,
    		int max_iter,
    		int size, int[] result) {
		if (sLoaded) {
			sNativePtr1 = doMandelbrot1(x_start, x_step, y_start, max_iter, size, result, sNativePtr1);
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


	// for benchmark purposes
	public static void mandelbrot1_native(float x_start, float x_step, float y_start,
    		int max_iter,
    		int size, int[] result) {
		if (sLoaded) {
			sNativePtr1 = doMandelbrot1(x_start, x_step, y_start, max_iter, size, result, sNativePtr1);
		}
	}

	// for benchmark purposes
	public static void mandelbrot1_java(float x_start, float x_step, float y_start,
    		int max_iter,
    		int size, int[] result) {
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

	public static void mandelbrot(
			float x_start, float x_step,
			float y_start, float y_step,
			int sx, int sy,
    		int max_iter,
    		int size, int[] result) {
		if (false && sLoaded) {
			sNativePtr2 = doMandelbrot2(
					x_start, x_step,
					y_start, y_step,
					sx, sy,
					max_iter,
					size, result,
					sNativePtr2);
		} else {
			mandelbrot2_java(x_start, x_step, y_start, y_step, sx, sy, max_iter, size, result);
		}
	}
	
	// for benchmark purposes
	public static void mandelbrot2_native(
			float x_start, float x_step,
			float y_start, float y_step,
			int sx, int sy,
    		int max_iter,
    		int size, int[] result) {
		if (sLoaded) {
			sNativePtr2 = doMandelbrot2(
					x_start, x_step,
					y_start, y_step,
					sx, sy,
					max_iter,
					size, result,
					sNativePtr2);
		}
	}

	// for benchmark purposes
	public static void mandelbrot2_java(
			float x_start, float x_step,
			float y_start, float y_step,
			int sx, int sy,
    		int max_iter,
    		int size, int[] result) {
		float x_begin = x_start;
		for(int j = 0, k = 0; j < sy; ++j, y_start += y_step) {
			x_start = x_begin;
			for(int i = 0; i < sx; ++i, ++k, x_start += x_step) {
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
	
			    result[k] = iter;
			} // i
		} // j
	}

    private static boolean load(AssetManager assets) {
    	Runtime r = Runtime.getRuntime();
    	ClassLoader loader = VMStack.getCallingClassLoader();
    	String libpath = "/data/data/com.alfray.mandelbrot/libMandelbrot.so";

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

        File f = new File(dest_path);
        if (!f.exists()) {
            File d = new File(f.getParent());
            if (!d.exists()) {
                boolean worked = d.mkdirs();
                if (!worked) {
                    throw new IOException("Mkdir failed for " + f.getParent());
                }
            }
        }
        
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
    private static native int doMandelbrot1(float x_start, float x_step, float y,
    		int max_iter,
    		int size, int[] result,
    		int last_ptr);

	/**
     * Returns a temp buffer, which should be given back in last_ptr.
     * Fills result for size elements.
     */
    private static native int doMandelbrot2(
    		float x_start, float x_step,
    		float y_start, float y_step,
    		int sx, int sy,
    		int max_iter,
    		int size, int[] result,
    		int last_ptr);
}

