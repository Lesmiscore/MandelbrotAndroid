package com.alfray.mandelbrot2.tests;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;

import com.alfray.mandelbrot2.JavaMandel;
import com.alfray.mandelbrot2.Mandel_RS;
import com.alfray.mandelbrot2.R;

//-----------------------------------------------

public class TestActivity extends Activity {

    private static final String TAG = "TestActivity";

    private TextView mText;
    private NativeTests mTestThread;
    private Button mStart;
    private Button mPause;
    private ScrollView mScroller;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        JavaMandel.init(this);

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

    /** Thread that actually runs the tests and controls their lifecycle. */
    class NativeTests extends TestThread {

        private AccessWrapper mWrapper;

        public NativeTests() {
            super("nativeTestsThread");
            mWrapper = new AccessWrapper();
        }

        @Override
        protected void runIteration() {
            mWrapper.runIteration();
        }
    }

    /** Utility wrapper to access the protected internal methods of JavaMandel. */
    public class AccessWrapper extends JavaMandel {

        private static final int SIZE = 128;

        private static final float FULL_STEP = 1.0f / SIZE;
        private static final float FULL_X_START = -1f;
        private static final float FULL_Y_START = -1f;

        private static final float BLACK_STEP = 0.5f / SIZE;
        private static final float BLACK_X_START = -0.5f;
        private static final float BLACK_Y_START = -0.5f;

        private int mState;
        private int[] mResults2;
        private byte[] mResults3;


        public AccessWrapper() {
            mState = 1;
            mResults2 = new int[SIZE*SIZE];
            mResults3 = new byte[SIZE*SIZE];
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

        public void runIteration() {
            switch(mState) {
            case 1:
                test_full_java2(20);
                break;
            case 2:
                test_full_java3(20);
                break;
            case 3:
                test_full_java4(20);
                break;
            case 4:
                test_black_java2(20);
                break;
            case 5:
                test_full_rs2(20);
                break;
            case 6:
                test_black_rs2(20);
                break;
            default:
                mState = 0; // loop
                writeResult("-------");
            }

            mState++;
        }

        private void test_full_java2(int max_iter) {
            long start = System.currentTimeMillis();

            final int N=10;
            for (int k = 0; k < N; ++k) {
                JavaMandel.mandelbrot2_java(
                        FULL_X_START, FULL_STEP,
                        FULL_Y_START, FULL_STEP,
                        SIZE, SIZE,
                        max_iter, mResults2.length, mResults2);
            }

            long end = System.currentTimeMillis();
            end -= start;

            writeResult("Full Java 2 [%dx%dx%d] = %.2f ms/call", SIZE, SIZE, max_iter, (double)end/N);
        }

        private void test_black_java2(int max_iter) {
            long start = System.currentTimeMillis();

            final int N=10;
            for (int k = 0; k < N; ++k) {
                JavaMandel.mandelbrot2_java(
                        BLACK_X_START, BLACK_STEP,
                        BLACK_Y_START, BLACK_STEP,
                        SIZE, SIZE,
                        max_iter, mResults2.length, mResults2);
            }

            long end = System.currentTimeMillis();
            end -= start;

            writeResult("Black Java 2 [%dx%dx%d] = %.2f ms/call", SIZE, SIZE, max_iter, (double)end/N);
        }

        private void test_full_rs2(int max_iter) {
            if (!JavaMandel.hasRs()) {
                writeResult("Rs Mandel 2 not supported");
                return;
            }

            long start = System.currentTimeMillis();

            final int N=10;
            for (int k = 0; k < N; ++k) {
                Mandel_RS.mandelbrot2_RS(
                        FULL_X_START, FULL_STEP,
                        FULL_Y_START, FULL_STEP,
                        SIZE, SIZE,
                        max_iter, mResults2.length, mResults2);
            }

            long end = System.currentTimeMillis();
            end -= start;

            writeResult("Full RS 2 [%dx%dx%d] = %.2f ms/call", SIZE, SIZE, max_iter, (double)end/N);
        }

        private void test_black_rs2(int max_iter) {
            if (!JavaMandel.hasRs()) {
                writeResult("Rs Mandel 2 not supported");
                return;
            }

            long start = System.currentTimeMillis();

            final int N=10;
            for (int k = 0; k < N; ++k) {
                Mandel_RS.mandelbrot2_RS(
                        BLACK_X_START, BLACK_STEP,
                        BLACK_Y_START, BLACK_STEP,
                        SIZE, SIZE,
                        max_iter, mResults2.length, mResults2);
            }

            long end = System.currentTimeMillis();
            end -= start;

            writeResult("Black RS 2 [%dx%dx%d] = %.2f ms/call", SIZE, SIZE, max_iter, (double)end/N);
        }

        private void test_full_java3(int max_iter) {
            long start = System.currentTimeMillis();

            final int N=10;
            for (int k = 0; k < N; ++k) {
                JavaMandel.mandelbrot3_java(
                        FULL_X_START, FULL_STEP,
                        FULL_Y_START, FULL_STEP,
                        SIZE, SIZE,
                        (byte)(max_iter - 128),
                        mResults3.length, mResults3);
            }

            long end = System.currentTimeMillis();
            end -= start;

            writeResult("Full Java 3 [%dx%dx%d] = %.2f ms/call", SIZE, SIZE, max_iter, (double)end/N);
        }

        private void test_full_java4(int max_iter) {
            long start = System.currentTimeMillis();

            final int N=10;
            for (int k = 0; k < N; ++k) {
                JavaMandel.mandelbrot3_java(
                        FULL_X_START, FULL_STEP,
                        FULL_Y_START, FULL_STEP,
                        SIZE, SIZE,
                        (byte)(max_iter - 128),
                        mResults3.length, mResults3);
            }

            long end = System.currentTimeMillis();
            end -= start;

            writeResult("Full Java 4 [%dx%dx%d] = %.2f ms/call", SIZE, SIZE, max_iter, (double)end/N);
        }

    }

}


