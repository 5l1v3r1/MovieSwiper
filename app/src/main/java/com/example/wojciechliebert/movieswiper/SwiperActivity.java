package com.example.wojciechliebert.movieswiper;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.VideoView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.example.wojciechliebert.movieswiper.Adapters.SwiperAdapter;

import java.io.File;
import java.io.FileDescriptor;
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
    ImageView imageView;

    FFmpegMediaMetadataRetriever metadataRetriever;
    long current;

    RequestManager glide;
    private RequestOptions requestOptions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_swiper);

        ButterKnife.bind(this);

        glide = Glide.with(this);
        requestOptions = new RequestOptions();
        requestOptions.diskCacheStrategy(DiskCacheStrategy.ALL);

//        final LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
//        linearLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
//        swiperRv.setLayoutManager(linearLayoutManager);
//        swiperRv.setAdapter(new SwiperAdapter(this, Utils.getImagesIdentifiers(this)));
//        swiperRv.setHasFixedSize(true);
//        swiperRv.setItemViewCacheSize(50);
//        swiperRv.setDrawingCacheEnabled(true);
//        swiperRv.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);
//        swiperRv.addOnScrollListener(new RecyclerView.OnScrollListener() {
//            @Override
//            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
//                recyclerView.getLayoutManager().fin
//                super.onScrolled(recyclerView, dx, dy);
//            }
//        });
//        Uri video_uri = Uri.parse(r);
//        videoView.setMediaController(new MediaController(this));
//        videoView.setVideoURI(Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.out1));
//        videoView.seekTo(100);
//        videoView.start();
//        videoView.setBackgroundColor(Color.TRANSPARENT);
//        videoView.setZOrderOnTop(true);

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

//            if (ev.getAction() == MotionEvent.ACTION_MOVE) {
//                //if move event occurs
//                testerTv.setText("AA");

//            listview.dispatchTouchEvent(motionEvent); //send event to listview
            return true;
//            }
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
    private static float TOUCH_TOLERANCE = 5.0f;
    private static float MNOZNIK = 1000f;

    private void touch_start(float x, float y) {
        mX = x;
        mY = y;
    }
    private void touch_move(float x, float y) {
        float dx = Math.abs(x - mX);
        float dy = Math.abs(y - mY);
        if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
            long d = delta;
            d = (long)((((MNOZNIK * dx)/d)+1) * d);
            if (x >= mX) {
                // moving finger to right
                d = -d;
            } else {
                // moving finger to left
                // OK
            }
            d = d/100*100;
            Bitmap bitmap = metadataRetriever.getFrameAtTime(current + d,
                   FFmpegMediaMetadataRetriever.OPTION_CLOSEST);
            if (bitmap != null) {
                imageView.setImageBitmap(bitmap);
//                glide.asBitmap().load(bitmap)
//                        .apply(requestOptions)
//                        .into(imageView);
                current += d;
            }
            mX = x;
            mY = y;
        }
    }

}
