/*
 * (c) ralfoide, http://gamez.googlecode.com/, 2008
 * Project: gamez
 * License TBD
 */


package com.alfray.mandelbrot.util;

import java.io.Writer;
import java.util.ArrayList;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;
import javax.microedition.khronos.opengles.GL10;

import android.opengl.GLU;
import android.util.Log;
import android.view.SurfaceHolder;


//-----------------------------------------------

public abstract class GLBaseThread extends BaseThread {

    private static final String TAG = "GLBaseThread";

    private int mWidth;
	private int mHeight;

	private String mLastError = "";
	private ArrayList<Runnable> mPendingTask;

	private GL10 mGl;
	private EGL10 mEGL;
	private EGLDisplay mEGLDisplay;
	private EGLContext mEGLContext;
	private EGLSurface mEGLSurface;

    private GL10 mGlWrap;

    private long mRenderTimeMs;

    private long mPostTimeMs;

    private long mTotalRenderTimeMs;

    private final SurfaceHolder mSurfaceHolder;

    private int mUpdates;

    private boolean mSetupSceneNeeded;

    private boolean mDirty;

    public GLBaseThread(SurfaceHolder surfaceHolder) {
        super("RenderGL");

        mSurfaceHolder = surfaceHolder;
        surfaceHolder.addCallback(new SurfaceViewCallback());
        
        mPendingTask = new ArrayList<Runnable>();

        this.setPriority(Thread.currentThread().getPriority()+1);
    }
    
    public void invalidate() {
        mDirty = true;
    }
    
    public String getLastError() {
    	return mLastError;
    }

    /**
     * Add a pending task which will be executed in the GL thread right before the
     * start of the next scene rendering.
     * <p/>
     * The purpose of this is to make sure we can change scene state synchronously.
     * The tasks should be very short.
     * 
     * @param task A runnable to run in the GL thread.
     */
    public void addPendingTask(Runnable task) {
    	synchronized(mPendingTask) {
    		mPendingTask.add(task);
    	}
    }
    
    class LogCatWriter extends Writer {
		@Override
		public void close() {
			// pass
		}

		@Override
		public void flush() {
			// pass
		}

		@Override
		public void write(char[] buf, int offset, int count) {
			String s = new String(buf, offset, count);
			Log.d("GLBaseThread", s);
		}    	
    }

    @Override
    protected void startRun() {
    	mLastError = " Setup GL";
    	mGl = setupGLContext();
    	mGlWrap = mGl;
        // GL10 mGlWrap = (GL10) GLDebugHelper.wrap(mGl,
        //		false /* enableErrorChecking */,
        //		new LogCatWriter());
	    mLastError = "";
    	processErrors(mGl);
    }
    
    @Override
    protected void runIteration() {
        mDirty = false;
    	runPendingTasks();
            	
        long start_time = System.currentTimeMillis();
        renderScene(mGl, mGlWrap);

        long render_time_ms = System.currentTimeMillis();
        postRender();

        long post_time_ms = System.currentTimeMillis() - render_time_ms;
        render_time_ms -= start_time;

        mRenderTimeMs = render_time_ms;
        mPostTimeMs = post_time_ms;
        mTotalRenderTimeMs += render_time_ms + post_time_ms;
	            
        processErrors(mGl);
        
        if (!mDirty) {
            waitForALongTime();
        }
    }

    @Override
    protected void endRun() {
    	releaseGLContext();
    }

	/** Run all pending tasks in the GL thread before rendering the scene */
	private void runPendingTasks() {
		if (mPendingTask.size() > 0) {
			synchronized(mPendingTask) {
				for (Runnable task : mPendingTask) {
					task.run();
				}
				mPendingTask.clear();
			}
		}
	}

    private void processErrors(GL10 gl) {
        String s = "";
        while(true) {
        	int e = gl.glGetError();
        	switch(e) {
        	case GL10.GL_NO_ERROR:
                if (s.length() > 0) {
                	mLastError += s;
                }
                return;
            default:
                String err = GLU.gluErrorString(e);
                if (err != null) {
                    s += ", " + err;
                }
        		break;
        	}
        }
	}

	/**
     * Called by the main activity when it's time to stop.
     * <p/>
     * Requirement: this MUST be called from another thread, not from GLBaseThread.
     * The activity thread is supposed to called this.
     * <p/>
     * This lets the thread complete it's render loop and wait for it
     * to fully stop using a join.
     */
    @Override
	public void waitForStop() {
    	// Not synchronized. Setting one boolean is assumed to be atomic.
        mContinue = false;

		try {
	    	assert Thread.currentThread() != this;
			join();
		} catch (InterruptedException e) {
			Log.e(TAG, "Thread.join failed", e);
		}
    }

    /**
     * Notify the thread that a redraw is needed.
     * If the thread is blocked in the waiting loop, this breaks the wait.
     * <p/>
     * Note that this does not invalidate the board (i.e. the scene content).
     * You may want to call Board.invalidateAll() before.
     */
    public void notifyRedraw() {
        synchronized (this) {
            notify();
        }
    }
    

    /**
     * Prepares the GL context.
     */
    public GL10 setupGLContext() {
        /*
         * Get an EGL instance
         */
        EGL10 egl = (EGL10)EGLContext.getEGL();
        
        /*
         * Get to the default display.
         */
        EGLDisplay dpy = egl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);

        /*
         * We can now initialize EGL for that display
         */
        int[] version = new int[2];
        egl.eglInitialize(dpy, version);
        
        /*
         * Specify a configuration for our opengl session
         * and grab the first configuration that matches is
         */
        int[] configSpec = {
                EGL10.EGL_DEPTH_SIZE,   16,
                EGL10.EGL_NONE
        };
        EGLConfig[] configs = new EGLConfig[1];
        int[] num_config = new int[1];
        egl.eglChooseConfig(dpy, configSpec, configs, 1, num_config);
        EGLConfig config = configs[0];

        /* 
         * Create an OpenGL ES context. This must be done only once, an
         * OpenGL context is a somewhat heavy object.
         */
        EGLContext context = egl.eglCreateContext(dpy, config, EGL10.EGL_NO_CONTEXT, null);

        if (mEGLSurface != null) {
            
            /*
             * Unbind and destroy the old EGL surface, if
             * there is one.
             */
            egl.eglMakeCurrent(dpy, egl.EGL_NO_SURFACE, egl.EGL_NO_SURFACE, context);
            egl.eglDestroySurface(dpy, mEGLSurface);
        }

        /* 
         * Create an EGL surface we can render into.
         */
        EGLSurface surface = egl.eglCreateWindowSurface(dpy, config, mSurfaceHolder, null);

        /*
         * Before we can issue GL commands, we need to make sure 
         * the context is current and bound to a surface.
         */
        egl.eglMakeCurrent(dpy, surface, surface, context);
        
        /*
         * Get to the appropriate GL interface.
         * This is simply done by casting the GL context to either
         * GL10 or GL11.
         */
        mGl = (GL10)context.getGL();

        mEGL = egl;
        mEGLDisplay = dpy;
        mEGLContext = context;
        mEGLSurface = surface;
        return mGl;
    }


    public void releaseGLContext() {
        /*
         * clean-up everything...
         */
        mEGL.eglMakeCurrent(mEGLDisplay, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_SURFACE, mEGLContext);
        mEGL.eglDestroySurface(mEGLDisplay, mEGLSurface);
        mEGL.eglDestroyContext(mEGLDisplay, mEGLContext);
        mEGL.eglTerminate(mEGLDisplay);
        mEGLDisplay = null;
        mEGLContext = null;
        mEGLSurface = null;
        mEGL = null;
    }
    

    /**
     * Renders the OpenGL scene. Called for every frame.
	 * This methods deffers the rendering to the gameplay engine.
     */
    public void renderScene(GL10 gl, GL10 gl_wrap) {
        
        // Cache the asynchronous state (window size)
        int w, h;
        synchronized(this) {
            w = mWidth;
            h = mHeight;
        }
        
        // setup scene once at startup or when size has changed
        if (mSetupSceneNeeded) {
        	mLastError = " Setup Scene";
        	setupScene(gl_wrap, w, h);
        	mLastError = "";
            processErrors(gl);
        }
        
        // draw a frame here
        drawScene(gl, w, h);
    }

    /**
     * Finishes the OpenGL scene rendering.
     */
    public void postRender() {
        /*
         * Once we're done with GL, we need to call swapBuffers()
         * to instruct the system to display the rendered frame
         */
        mEGL.eglSwapBuffers(mEGLDisplay, mEGLSurface);
        
        // Update counters
        ++mUpdates;
    }

    
    /**
     * Called by the main activity when the view is resized.
     */
    public void onWindowResize(int w, int h) {
        synchronized(this) {
            mWidth = w;
            mHeight = h;
            mSetupSceneNeeded = true;
        }
    }

    protected void setupScene(GL10 gl, int w, int h) {
        mSetupSceneNeeded = false;
    }

    protected abstract void drawScene(GL10 gl, int w, int h);

    // -----------------------------

    /**
     * Callback for the SurfaceView.
     * <p/>
     * It basically starts everything when the view is created and makes sure everything
     * is stopped before the view is going to be destroyed.
     * <p/>
     * Note that when the activity is created, the surface view is created and
     * this is what starts everything.
     */
    private class SurfaceViewCallback implements SurfaceHolder.Callback {

        /**
         * Tell GL to resize its viewport if the view is resized.
         */
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            onWindowResize(width, height);
        }

        /**
         * Starts the thread once the surface view has been created.
         * <p/>
         * This is what makes the game start.
         */
        public void surfaceCreated(SurfaceHolder holder) {
            start();
        }

        /**
         * Stops the threads before the surface view is destroyed.
         */
        public void surfaceDestroyed(SurfaceHolder holder) {
            pauseThread(true);
        }       
    }
}


