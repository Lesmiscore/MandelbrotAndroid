/*************************
 * The file TestActivity.java does...
 */
package com.alfray.mandelbrot.tests;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;

import com.alfray.mandelbrot.NativeMandel;
import com.alfray.mandelbrot.R;

//-----------------------------------------------

/*************************
 * The class  does...
 *
 */
public class TestActivity extends Activity {

	private TextView mText;
	private NativeTests mTestThread;
    private Button mStart;
    private Button mPause;
    private ScrollView mScroller;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);

	    NativeMandel.init(getAssets());
	    
	    setContentView(R.layout.tests);
	    
	    mScroller = (ScrollView) findViewById(R.id.scroller);
	    mText = (TextView) findViewById(R.id.text);
        mStart = (Button) findViewById(R.id.start);
        mPause = (Button) findViewById(R.id.pause);
	    
	    mTestThread = new NativeTests();
	    
	    mStart.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                mTestThread.start();
            }
	    });
        
	    mPause.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                mTestThread.pauseThread(true);
            }
        });
	}
	
	@Override
	protected void onResume() {
		super.onResume();
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		mTestThread.pauseThread(true);
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		mTestThread.waitForStop();
	}
	
	class NativeTests extends TestThread {

		private static final int sx = 320;
		private static final int sy = 480;
		private static final float _xcenter = -0.5f;
		private static final float xy_width = 3.0f;
		private static final float x_step = xy_width / sx;
		private static final float y_step = xy_width / sy;
		private static final float x_start = _xcenter - xy_width/2;
		private static final float y_start = 0.0f - xy_width/2;
		private static final float y_end = 0.0f + xy_width/2;
		
		
		private int mState;
		private int[] mResults1;
		private int[] mResults2;

		public NativeTests() {
			super("nativeTestsThread");
			mState = 1;
			mResults1 = new int[sx];
			mResults2 = new int[sx*sy];
		}
		
	    public void writeResult(String format, Object...params) {
	        String msg = String.format(format, params);
	        Log.d(TAG, msg);
	        
	        final String msg2 = msg.endsWith("\n") ? msg : msg + "\n";
	        
	        mText.post(new Runnable() {
	            public void run() {
	                mText.append(msg2);
	                mScroller.scrollTo(0, mText.getHeight());
	            }
	        });
	    }

		@Override
		protected void runIteration() {
			switch(mState) {
			case 1:
				test_nothing();
				break;
			case 2:
				test_java1(20);
				break;
			case 3:
				test_native1(20);
				break;
            case 4:
                test_java2(100);
                break;
            case 5:
                test_native2(100);
                break;
			default:
				mState = 0; // loop
			    writeResult("-------");
			}
			
			mState++;
		}

		private void test_nothing() {
			long start = System.currentTimeMillis();

			for (int i = 0; i < 100000; ++i) {
				// calls native with size=0 => does nothing, returns asap
				NativeMandel.mandelbrot1_native(0, 0, 0, 20, 0, mResults1);
			}
			
			long end = System.currentTimeMillis();
			end -= start;
			
			writeResult("Empty [100,000] = %.2fs", end/1000.0f);
		}

		private void test_native1(int max_iter) {
			long start = System.currentTimeMillis();

			for (float y = y_start; y < y_end; y += y_step) {
				NativeMandel.mandelbrot1_native(x_start, x_step, y, 20, sx, mResults1);
			}
			
			long end = System.currentTimeMillis();
			end -= start;
			
			writeResult("Native 1 [%dx%dx%d] = %.2fs", sx, sy, max_iter, end/1000.0f);
		}

		private void test_java1(int max_iter) {
			long start = System.currentTimeMillis();

			for (float y = y_start; y < y_end; y += y_step) {
				NativeMandel.mandelbrot1_java(x_start, x_step, y, 20, sx, mResults1);
			}
			
			long end = System.currentTimeMillis();
			end -= start;
			
			writeResult("Java 1 [%dx%dx%d] = %.2fs", sx, sy, max_iter, end/1000.0f);
		}

		private void test_native2(int max_iter) {
			long start = System.currentTimeMillis();

			NativeMandel.mandelbrot2_native(
					x_start, x_step,
					y_start, y_start,
					sx, sy,
					max_iter, mResults2.length, mResults2);
			
			long end = System.currentTimeMillis();
			end -= start;
			
			writeResult("Native 2 [%dx%dx%d] = %.2fs", sx, sy, max_iter, end/1000.0f);
		}

		private void test_java2(int max_iter) {
			long start = System.currentTimeMillis();

			NativeMandel.mandelbrot2_java(
					x_start, x_step,
					y_start, y_start,
					sx, sy,
					max_iter, mResults2.length, mResults2);
			
			long end = System.currentTimeMillis();
			end -= start;
			
			writeResult("Java 2 [%dx%dx%d] = %.2fs", sx, sy, max_iter, end/1000.0f);
		}
	}

}


