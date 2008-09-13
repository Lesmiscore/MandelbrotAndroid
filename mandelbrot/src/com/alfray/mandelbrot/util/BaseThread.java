/*
 * (c) ralfoide gmail com, 2008
 * Project: asqare
 * License TBD
 */

package com.alfray.mandelbrot.util;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import android.util.Log;


//-----------------------------------------------

public abstract class BaseThread extends Thread {

    private static final String TAG = "BaseThread";

	protected boolean mContinue = true;
	protected boolean mIsPaused = false;
	protected boolean mWaitAtPauseBarrier = false;
	protected CyclicBarrier mPauseBarrier;
	
	public BaseThread(String name) {
		super(name);
        this.setPriority(Thread.currentThread().getPriority()+1);
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
    public void waitForStop() {
    	// Not synchronized. Setting one boolean is assumed to be atomic.
        mContinue = false;

		try {
	    	assert Thread.currentThread() != this;
			wakeUp();
			join();
		} catch (InterruptedException e) {
			Log.e(TAG, "Thread.join failed", e);
		}
    }
    
    /**
     * Starts the thread if it wasn't already started.
     * Does nothing if started.
     */
    @Override
    public synchronized void start() {
    	if (getState() == State.NEW) {
    		super.start();
    	}
    	if (mIsPaused) {
    		mIsPaused = false;
    		wakeUp();
    	}
    }

    /**
     * Pauses or unpauses the thread.
     * <p/>
     * When pausing, this blocks till the thread is actually paused.
     */
	public synchronized void pauseThread(boolean pause) {
		// ignore same-state
		if (mIsPaused == pause) return;
			
		if (pause) {
			// when pausing, we must first create the barrier the first time
			// and wait at it.
			if (mPauseBarrier == null) {
				mPauseBarrier = new CyclicBarrier(2, new Runnable() {
					public void run() {
						// Once the barrier has run, it must reset the
						// flag indicating to wait at it. That's because the
						// pause loop is not infinite, it loops and calls
						// waitAtPauseBarrier + waitForALongTime repeatedly so
						// we use the boolean to wait for the barrier the
						// first pause loop time only.
						mWaitAtPauseBarrier = false;
					}
				});
			}
			
			mWaitAtPauseBarrier = true;
		}

		mIsPaused = pause;
		
		if (pause) {
			waitAtPauseBarrier();
		} else {
			wakeUp();
		}
	}

	/**
	 * Resets the circular buffer of actions and also clears every single
	 * action to make sure no outside object reference is kept behind.
	 */
	public abstract void clear();
	
	// -----------------

	/**
	 * Base implementation of the thread loop.
	 * Derived classes can re-implement this, as long as they follow this
	 * contract:
	 * - the loop must continue whilst mContinue is true
	 * - each iteration must invoke runIteration() when not paused.
	 * - the loop must pause when mIsPaused is true by first waiting for
	 *   the pause barrier and do a waitForALongTime() until mIsPaused is
	 *   released.
	 */
	@Override
    public void run() {
	    
	    try {
	        startRun();
	        
            while (mContinue) {
            	if (mIsPaused) {
            		waitAtPauseBarrier();
            		waitForALongTime();
            		continue;
            	}
    
            	runIteration();
            }
        } catch (Exception e) {
            Log.e(TAG, "Run-Loop Exception, stopping thread", e);
	    } finally {
	        endRun();
	    }
	}
	
    protected abstract void startRun();

    /**
	 * Performs one iteration of the thread run loop.
	 * Implementations must implement this and not throw exceptions from it.
	 */
	protected abstract void runIteration();

	protected abstract void endRun();


	protected void wakeUp() {
		this.interrupt();
	}
	
	protected void waitForALongTime() {
		try {
			synchronized(this) {
				wait(5000 /* ms */);
			}
		} catch (InterruptedException e) {
			// pass
		}
	}
	
	protected void waitFor(long time_ms) {
		try {
			synchronized(this) {
				if (time_ms > 0) wait(time_ms);
			}
		} catch (InterruptedException e) {
			// pass
		}
		
	}

	/**
	 * Waits at the pause barrier if it exists and it has been requested
	 * to do so. See comments in pauseThread().
	 */
	protected void waitAtPauseBarrier() {
		if (mWaitAtPauseBarrier && mPauseBarrier != null) {
			try {
				mPauseBarrier.await(1000, TimeUnit.MILLISECONDS);
			} catch (InterruptedException e) {
				// pass
			} catch (BrokenBarrierException e) {
				// pass
			} catch (TimeoutException e) {
				// pass
			}
		}
	}
}


