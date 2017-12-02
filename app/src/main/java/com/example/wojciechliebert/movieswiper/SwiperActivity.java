package com.example.wojciechliebert.movieswiper;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.LruCache;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.VideoView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import butterknife.BindView;
import butterknife.ButterKnife;
import wseemann.media.FFmpegMediaMetadataRetriever;

public class SwiperActivity extends AppCompatActivity {

    @BindView(R.id.swiper_rv)
    RecyclerView swiperRv;

    @BindView(R.id.tester)
    TextView testerTv;

    @BindView(R.id.swiper_video)
    VideoView videoView;

    @BindView(R.id.swiper_img)
    ImageView mImageView;

    FFmpegMediaMetadataRetriever metadataRetriever;
    long mCurrent;

    private GestureDetectorCompat mDetector;

    private LruCache<Long, Bitmap> mCache;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_swiper);

        ButterKnife.bind(this);

        mDetector = new GestureDetectorCompat(this, new MyGestureListener());
        mCache = new LruCache<Long, Bitmap>(500);

        File f = new File(getCacheDir()+"/out1.mp4");
        if (!f.exists()) try {

            InputStream is = getResources().openRawResource(R.raw.out1);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();


            FileOutputStream fos = new FileOutputStream(f);
            fos.write(buffer);
            fos.close();
        } catch (Exception e) { throw new RuntimeException(e); }


        metadataRetriever = new FFmpegMediaMetadataRetriever();
        metadataRetriever.setDataSource(f.getPath());
        showFrameAt(1);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mDetector.onTouchEvent(event);
        return super.onTouchEvent(event);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        float x = ev.getRawX();
        float y = ev.getRawY();
        if (isViewContains(videoView, ((int) x), ((int) y))) {
            switch (ev.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    touch_start(x, y);
                    break;
                case MotionEvent.ACTION_MOVE:
                    touch_move(x, y);
                    break;
                case MotionEvent.ACTION_UP:
//                    touch_up();
                    break;
            }
//            return false;
        }
        return super.dispatchTouchEvent(ev);
    }

    private boolean isViewContains(View view, int rx, int ry) {
        int[] l = new int[2];
        view.getLocationOnScreen(l);
        int x = l[0];
        int y = l[1];
        int w = view.getWidth();
        int h = view.getHeight();

        if (rx < x || rx > x + w || ry < y || ry > y + h) {
            return false;
        }
        return true;
    }

    private float mX, mY;
    private static long delta = 5_000;
    private static float TOUCH_TOLERANCE = 0.01f;
    private static float SPEED = 1000f;

    private void touch_start(float x, float y) {
        mX = x;
        mY = y;
    }

    private void touch_move(float x, float y) {
        float dx = Math.abs(x - mX);
        float dy = Math.abs(y - mY);
        if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
            long d = delta;
            d = (long) ((((SPEED * dx) / d) + 1) * d);
            if (x >= mX) {
                // moving finger to right
                d = -d;
            } else {
                // moving finger to left
                // OK
            }
//            It's kinda like a density
//            d = d / 100 * 100;
            showFrameAt(d);
            mX = x;
            mY = y;
        }
    }

    private void showFrameAt(final long d) {
//            It's kinda like a density
        long time = (mCurrent + d) / 50000L * 50000L;

        final Bitmap bmpFromCache = mCache.get(time);
        if (bmpFromCache == null) {
            Bitmap bitmap = metadataRetriever.getFrameAtTime(time,
                    FFmpegMediaMetadataRetriever.OPTION_CLOSEST);
            if (bitmap != null) {
                mCache.put(time, bitmap);
                loadBitmapIntoImage(bitmap);
                mCurrent += d;
//                Log.d("HEHERE", String.format("showFrameAt%09d: Not from cache/nCurrent=%09d", time, mCurrent));
            }
        } else {
            loadBitmapIntoImage(bmpFromCache);
            mCurrent += d;
//            Log.d("HEHERE", String.format("showFrameAt%09d: Loaded from cache/nCurrent=%09d", time, mCurrent));
        }

    }

    private void loadBitmapIntoImage(Bitmap bitmap) {
        mImageView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        mImageView.setImageBitmap(bitmap);
        new Handler(getMainLooper()).post(() ->
                mImageView.setLayerType(View.LAYER_TYPE_NONE, null));
    }

    private class MyGestureListener extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            Log.d("HEHERE", "onFling: velocityX=" + velocityX);
            return super.onFling(e1, e2, velocityX, velocityY);
        }
    }
}
