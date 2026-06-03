package com.gles.bluescreen;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class MainActivity extends Activity {

    static {
        System.loadLibrary("native");
    }

    private native void nativeInit(Surface surface);
    private native void nativeResize(int w, int h);
    private native void nativeStep();

    private final Handler handler = new Handler(Looper.getMainLooper());

    private final Runnable renderRunnable = new Runnable() {
        @Override
        public void run() {
            nativeStep();
            handler.postDelayed(this, 16); // ~60 FPS
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SurfaceView view = new SurfaceView(this);
        setContentView(view);

        view.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                nativeInit(holder.getSurface());
                handler.post(renderRunnable);
            }

            @Override
            public void surfaceChanged(
                    SurfaceHolder holder,
                    int format,
                    int width,
                    int height
            ) {
                nativeResize(width, height);
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                handler.removeCallbacks(renderRunnable);
            }
        });
    }
}