/*************************
 * The file TestActivity.java does...
 */
package com.alfray.mandelbrot.tests;

import com.alfray.mandelbrot.NativeMandel;
import com.alfray.mandelbrot.R;
import com.alfray.mandelbrot.R.id;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

//-----------------------------------------------

/*************************
 * The class  does...
 *
 */
public class TestActivity extends Activity {

	private TextView mText;
	private NativeTests mTestThread;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);

	    NativeMandel.init(getAssets());
	    
	    setContentView(R.layout.tests);
	    
	    mText = (TextView) findViewById(R.id.text);
	    
	    mTestThread = new NativeTests(mText);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		mTestThread.start();
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
	
	static class NativeTests extends TestThread {

		private int mState;
		private int[] mResults;

		public NativeTests(TextView textView) {
			super("nativeTestsThread", textView);
			mState = 1;
			mResults = new int[320];
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
                test_java1(100);
                break;
            case 5:
                test_native1(100);
                break;
			default:
				mState = 0; // loop
			}
			
			mState++;
		}

		private void test_nothing() {
			long start = System.currentTimeMillis();

			int nb = mResults.length;
			float step = 4.0f / 200;
			for (float y = -2.0f; y < 2.0f; y += step) {
				// calls native with size=0 => does nothing, returns asap
				NativeMandel.mandelbrot1_native(-2.0f, step, y, 20, 0, mResults);
			}
			
			long end = System.currentTimeMillis();
			end -= start;
			
			writeResult("Empty [200] = %.2fs", end/1000.0f);
		}

		private void test_native1(int max_iter) {
			long start = System.currentTimeMillis();

			int nb = mResults.length;
			float ystep = 4.0f / 200;
			float xstep = 4.0f / nb;
			for (float y = -2.0f; y < 2.0f; y += ystep) {
				NativeMandel.mandelbrot1_native(-2.0f, xstep, y, max_iter, nb, mResults);
			}
			
			long end = System.currentTimeMillis();
			end -= start;
			
			writeResult("Native1 [320x200x%d] = %.2fs", max_iter, end/1000.0f);
		}

		private void test_java1(int max_iter) {
			long start = System.currentTimeMillis();

			int nb = mResults.length;
			float ystep = 4.0f / 200;
			float xstep = 4.0f / nb;
			for (float y = -2.0f; y < 2.0f; y += ystep) {
				NativeMandel.mandelbrot1_java(-2.0f, xstep, y, max_iter, nb, mResults);
			}
			
			long end = System.currentTimeMillis();
			end -= start;
			
			writeResult("Java1 [320x200x%d] = %.2fs", max_iter, end/1000.0f);
		}
	}

}


