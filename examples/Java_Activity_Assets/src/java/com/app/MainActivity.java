package com.app;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.os.Bundle;
import android.view.View;

import java.io.InputStream;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(new ImageView(this));
    }

    private class ImageView extends View {

        private Bitmap bitmap;

        public ImageView(Context context) {
            super(context);

            try {
                InputStream is = getAssets().open("example.png");
                bitmap = BitmapFactory.decodeStream(is);
                is.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            if (bitmap != null) {
                canvas.drawBitmap(
                    bitmap,
                    null,
                    canvas.getClipBounds(),
                    null
                );
            }
        }
    }
}