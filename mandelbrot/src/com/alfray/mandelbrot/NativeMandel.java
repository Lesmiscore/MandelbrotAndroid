package com.alfray.mandelbrot;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import android.content.res.AssetManager;
import android.util.Log;
import dalvik.system.VMStack;

/**
 * Performs the Mandelbrot computation, either using a native wrapper or via pure Java calls.
 * <p/>
 * The protected static methods are called from TestActivity.AccessWrapper for speed measurements.
 * For "real" code, rely on the public methods that abstract the native-vs-java calls.
 */
public class NativeMandel {

	private static String TAG = "NativeMandel";
	
	private static boolean sLoaded = false;
	private static boolean sInit = false;
	private static int sNativePtr2 = 0;
	private static int sNativePtr3 = 0;
	
	public synchronized static void init(AssetManager assets) {
		if (!sInit) {
			sInit = true;
			
			sLoaded = load(assets);
		}
	}
	
	public static void dispose() {
		if (sLoaded ) {
			sNativePtr2 = doMandelbrot2(0, 0, 0, 0, 0, 0, 0, 0, null, sNativePtr2);
			//--TODO--sNativePtr3 = doMandelbrot3(0, 0, 0, 0, 0, 0, 0, 0, null, sNativePtr3);
		}
	}

	// ------------------------------------------------------------------------

	/**
	 * Native or java rendering using a classic float algorithm with no fancy optim tricks.
	 * <p/>
	 * Version 2: computes a block using x_start+x_step / y_start+y_step.
	 * maxIter is an int, returns SX*SY int ranging [1..maxIter].
	 * Uses the "classic" float version, no fancy optims.
	 * <p/> 
	 * Aborts if maxIter or sx or sy <= 0.
	 */
	public static void mandelbrot2(
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
		} else {
			mandelbrot2_java(x_start, x_step, y_start, y_step, sx, sy, max_iter, size, result);
		}
	}

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

	protected static void mandelbrot2_native(
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

	protected static void mandelbrot2_java(
			float x_start, float x_step,
			float y_start, float y_step,
			int sx, int sy,
    		int max_iter,
    		int size, int[] result) {
	    if (max_iter <= 0) return;
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
			      float xt = x2 - y2 + x_start;
			      y = 2 * x * y + y_start;
			      x = xt;
			      x2 = xt * xt;
			      y2 = y * y;
			      ++iter;
			    }
	
			    result[k] = iter;
			} // i
		} // j
	}

	/** direction vectors, going 'to the right' */
	//                            dir: ->, v, <-,  ^
   private static final int DIR_X[] = { 1, 0, -1,  0 };
   private static final int DIR_Y[] = { 0, 1,  0, -1 };

	
	/** boundary detection with float */
    protected static void mandelbrot2b_java(
            final float x_start, final float x_step,
            final float y_start, final float y_step,
            final int sx, final int sy,
            final int max_iter,
            final int size, int[] result) {
        if (max_iter <= 0) return;
        
        // visited bits, one per pixel
        int n_vis = size >> 5;
        int i_vis = sx >> 5;
        int[] visited = new int[n_vis];
        
        // stats per line: 16 LSB==min fill, 16 MSB=max fill
        short[] fill = new short[sy*2];

        int todo = size;
        
        while (todo > 0) {
            int i, j, k;
        _find_todo:
            for (i = -1, j = 0, k = 0; j < sy; j++) {
                for (int ik = 0; ik < i_vis; ik++, k++) {
                    int a = visited[k];
                    if (a != -1) {
                        for (i = ik << 5; (a&1) != 0; i++) {
                            a = a>>1;
                        }
                        break _find_todo;
                    }
                }
            }
            
            if (i < 0 || j >= sy) {
                // nothing found to do... not expected. abort anyway.
                break; // breaks white todo
            }
            
            // start boundary-detection in (i,j)

            int i0 = i, j0 = j;
            int dir = 0;
            int minJfill = j;
            int k_res = j0 * sx + i0;
            
            // mark i0,j0 as visited
            int vk = (j0 * i_vis) + (i0 >> 5);
            visited[vk] |= (1 << (i0 & 31));

            // compute i0,j0
            int iter0 = result[k_res];
            if (iter0 != 0) {
                iter0 = z2c(x_start + i0 * x_step, y_start + j0 * y_step, max_iter);
                result[k_res] = iter0;
            }
            
            // set (i0,j0) fill
            fill[j*2 + 0] = (short) i0;
            fill[j*2 + 1] = (short) i0;

            _do_bounds:
            while (true) {
                // check points at dir -1/0/+1
                for (int delta_dir = -1; delta_dir <= 1; delta_dir++) {
                    int ddir = dir;
                    if (delta_dir != 0) ddir = (dir + delta_dir + 4) & 3;
                    
                    int i1 = i + DIR_X[ddir];
                    int j1 = j + DIR_Y[ddir];
                    
                    // finished if back to i0,j0
                    if (i1 == i0 && j1 == j0) break _do_bounds;
                    
                    // discard if out of bounds
                    if (i1 < 0 || j1 < 0 || i1 >= sx || j1 >= sy) continue;
                    
                    // discard if visited
                    vk = (j1 * i_vis) + (i1 >> 5);
                    if ((visited[vk] & (1 << (i0 & 31))) != 0) continue;
                    
                    // compute i,j
                    k_res = j * sx + i;
                    int iter = result[k_res];
                    if (iter != 0) {
                        iter = z2c(x_start + i * x_step, y_start + j * y_step, max_iter);
                        result[k_res] = iter;
                    }
                    
                    // CONTINUE HERE
                }
            }
        }
    }

    private static int z2c(final float xs, final float ys, final int max_iter) {
        int iter = 0;
        float x = xs;
        float y = ys;
        float x2 = x * x;
        float y2 = y * y;
        while (x2 + y2 < 4 && iter < max_iter) {
          float xt = x2 - y2 + xs;
          y = 2 * x * y + ys;
          x = xt;
          x2 = xt * xt;
          y2 = y * y;
          ++iter;
        }
        return iter;
    }

	// ------------------------------------------------------------------------

	/**
	 * Native or java rendering using a classic fp16 algorithm with no fancy optim tricks.
	 * <p/>
	 * Version 3: Similar to version 2, except its in fixed-point 16 bits (8.8).
	 * MaxIter is a byte (1..255 except java doesn't have unsigned types so
	 * we use -128..127 instead to mean 0..255).
	 * Returns SX*SY bytes, ranging [-128..127] but really meaning [0..255].
	 * 
	 * Returns false if there isn't enough precision to use fp16 or
	 * if maxter is -128 (which represents 0 here).
	 */
    public static boolean mandelbrot3(
            float x_start, float x_step,
            float y_start, float y_step,
            int sx, int sy,
            byte max_iter,
            int size, byte[] result) {
        /* if (sLoaded) {
            sNativePtr3 = doMandelbrot3(
                    x_start, x_step,
                    y_start, y_step,
                    sx, sy,
                    max_iter,
                    size, result,
                    sNativePtr3);
        } else */ {
            return mandelbrot3_java(x_start, x_step, y_start, y_step, sx, sy, max_iter, size, result);
        }
    }

    protected static boolean mandelbrot3_java(
            final float x_start, final float x_step,
            final float y_start, final float y_step,
            final int sx, final int sy,
            final byte max_iter,
            final int size, byte[] result) {
        if (max_iter == -128) return false;
        final int ix_step = (int)(x_step  * 256);
        final int iy_step = (int)(y_step  * 256);
        if (ix_step <= 0 || iy_step <= 0) return false;
        int ix_start = (int)(x_start * 256);
        int iy_start = (int)(y_start * 256);
        
        int ix_begin = ix_start;
        for(int j = 0, k = 0; j < sy; ++j, iy_start += iy_step) {
            ix_start = ix_begin;
            for(int i = 0; i < sx; ++i, ++k, ix_start += ix_step) {
                int ix = ix_start;
                int iy = iy_start;
                int ix2 = (ix * ix) >> 8;
                int iy2 = (iy * iy) >> 8;
                byte iter = -128;
                while (ix2 + iy2 < (4<<8) && iter < max_iter) {
                  int ixt = (ix2 - iy2) + ix_start;
                  iy = ((2 * ix * iy) >> 8) + iy_start;
                  ix = ixt;
                  ix2 = (ixt * ixt) >> 8;
                  iy2 = (iy * iy) >> 8;
                  ++iter;
                }
    
                result[k] = iter;
            } // i
        } // j
        return true;
    }

	// ------------------------------------------------------------------------

	/**
	 * Native or java rendering using a classic fp32 algorithm with no fancy optim tricks.
	 * <p/>
	 * Version 4: Similar to version 2, except its in fixed-point 32 bits (16.16).
	 * MaxIter is a byte (1..255 except java doesn't have unsigned types so
	 * we use -128..127 instead to mean 0..255).
	 * Returns SX*SY bytes, ranging [-128..127] but really meaning [0..255].
	 * 
	 * Returns false if there isn't enough precision to use fp32 or
	 * if maxter is -128 (which represents 0 here).
	 */
    public static boolean mandelbrot4(
            float x_start, float x_step,
            float y_start, float y_step,
            int sx, int sy,
            byte max_iter,
            int size, byte[] result) {
        /* if (sLoaded) {
            sNativePtr3 = doMandelbrot4(
                    x_start, x_step,
                    y_start, y_step,
                    sx, sy,
                    max_iter,
                    size, result,
                    sNativePtr3);
        } else */ {
            return mandelbrot4_java(x_start, x_step, y_start, y_step, sx, sy, max_iter, size, result);
        }
    }

    protected static boolean mandelbrot4_java(
            final float x_start, final float x_step,
            final float y_start, final float y_step,
            final int sx, final int sy,
            final byte max_iter,
            final int size, byte[] result) {
        if (max_iter == -128) return false;
        final int ix_step = (int)(x_step  * 65536);
        final int iy_step = (int)(y_step  * 65536);
        if (ix_step <= 0 || iy_step <= 0) return false;
        int ix_start = (int)(x_start * 65536);
        int iy_start = (int)(y_start * 65536);
        
        int ix_begin = ix_start;
        for(int j = 0, k = 0; j < sy; ++j, iy_start += iy_step) {
            ix_start = ix_begin;
            for(int i = 0; i < sx; ++i, ++k, ix_start += ix_step) {
                long Lx = (long)ix_start;
                long Ly = (long)iy_start;
                int ix2 = (int)((Lx * Lx) >> 16);
                int iy2 = (int)((Ly * Ly) >> 16);
                byte iter = -128;
                while (ix2 + iy2 < (4<<16) && iter < max_iter) {
                  int ixt = (ix2 - iy2) + ix_start;
                  Ly = ((2 * Lx * Ly) >> 16) + iy_start;
                  Lx = (long)ixt;
                  ix2 = (int)((Lx * Lx) >> 16);
                  iy2 = (int)((Ly * Ly) >> 16);
                  ++iter;
                }
    
                result[k] = iter;
            } // i
        } // j
        return true;
    }

    // ------------------------------------------------------------------------
    private static boolean load(AssetManager assets) {
        // This is an UGLY HACK initially done to see whether the system
        // can be abused or not. The answer was "not really".
        // *** Please do not reuse this ugly hack or I shall taunt you a second time! ***
        
    	Runtime r = Runtime.getRuntime();
    	ClassLoader loader = VMStack.getCallingClassLoader();
    	// TODO use data path as given by context
    	String libpath = "/data/data/com.alfray.mandelbrot/libMandelbrot.so";

    	try {
        	setup(assets, libpath);
        	
			Class<? extends Runtime> c = r.getClass();
			Method m = c.getDeclaredMethod("load", new Class[] { String.class, ClassLoader.class });
			m.setAccessible(true);
			m.invoke(r, new Object[] { libpath, loader });
			Log.d(TAG, "libMandelbrot.so loaded");
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
		
		Log.d(TAG, "libMandelbrot.so *NOT* loaded");
		return false;
    }

    private static void setup(AssetManager assets, String dest_path) throws IOException {

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

    //------------------------------------------------------------------

}

