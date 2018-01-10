package com.example.wojciechliebert.movieswiper;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.animation.FlingAnimation;
import android.support.animation.FloatValueHolder;
import android.support.annotation.NonNull;
import android.support.annotation.RawRes;
import android.support.constraint.ConstraintLayout;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.LruCache;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.VideoView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import butterknife.BindView;
import butterknife.ButterKnife;
import rx.Observable;
import rx.Subscription;
import rx.schedulers.Schedulers;
import wseemann.media.FFmpegMediaMetadataRetriever;

public class SwiperActivity extends AppCompatActivity {

    private static final float FLING_FRICTION = 0.5f;

    @BindView(R.id.root_constraint_layout)
    FrameLayout rootLayout;

    @BindView(R.id.swiper_rv)
    RecyclerView swiperRv;

    @BindView(R.id.tester)
    TextView testerTv;

    @BindView(R.id.swiper_video)
    VideoView videoView;

    @BindView(R.id.swiper_img)
    ImageView mImageView;

    @BindView(R.id.open_cam_button)
    Button mOpenCamBtn;

    @BindView(R.id.info_cl)
    ConstraintLayout mInfoCl;

    @BindView(R.id.info_header)
    TextView mInfoHeader;

    @BindView(R.id.info_body)
    TextView mInfoBody;

    FFmpegMediaMetadataRetriever metadataRetriever;
    private long mCurrentTime;
    private ImageView mCircle;

    public long getCurrentTime() {
        return mCurrentTime;
    }

    public void setCurrentTime(long newTime) {
        mCurrentTime = newTime;
        Log.d("AA", String.format("setCurrentTime: %s", mCurrentTime));
        updateCirclePosition(mCurrentTime);
    }

    private GestureDetectorCompat mDetector;

    private LruCache<Long, Bitmap> mCache;
    private TreeMap<Long, CirclePosition> mMarkPositions;
    private long videoDuration;
    private long FRAME_REQUEST_DENSITY = 50000L;
    private Uri mVideoUri;
    private FlingAnimation flingAnimation;
    private Subscription subscription;
    private int partitionSize = 3;
    private Observable<Map<Long, Bitmap>> observable;
    private AtomicInteger currentThreadsNumber;

    private void addCircle() {
        ViewGroup layout = rootLayout;
        mCircle = new ImageView(this);
        final FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(100, 100);
        mCircle.setLayoutParams(params);
        params.leftMargin = 50;
        params.topMargin = 60;
//        mImageView.getLayoutParams().height = 20;
//        mImageView.getLayoutParams().width = 20;
//        mImageView.requestLayout();
        mCircle.setX(60);
        mCircle.setY(20);
//        iv.setBackgroundColor(getResources().getColor(R.color.colorTransparent));
        mCircle.setOnClickListener(view -> {
            final Map.Entry<Long, CirclePosition> floorEntry = mMarkPositions.floorEntry(mCurrentTime);
            Long key = floorEntry.getKey();
            CircleMessage message = floorEntry.getValue().message;
            while (message == null) {
                if (key != null) {
                    key = mMarkPositions.lowerKey(key);
                }
                message = mMarkPositions.floorEntry(key).getValue().message;
            }
            mInfoCl.setVisibility(View.VISIBLE);
            mInfoHeader.setText(message.header);
            mInfoBody.setText(message.body);
//            Toast.makeText(getApplicationContext(), String.format("%s\n%s", header, body), Toast.LENGTH_SHORT).show();
        });
//        Glide.with(this)
//                .load(R.drawable.circle)
//                .into(mCircle);
        mCircle.setImageResource(R.drawable.ic_circle1);
        layout.addView(mCircle);
        mCircle.setVisibility(View.GONE);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_swiper);

        ButterKnife.bind(this);

        mOpenCamBtn.setOnClickListener(view -> {
            dispatchTakeVideoIntent();
        });

        mDetector = new GestureDetectorCompat(this, new MyGestureListener());
        mCache = new LruCache<>(500);

        File f = getFile("/out1.webm", R.raw.out1);

        metadataRetriever = new FFmpegMediaMetadataRetriever();
        metadataRetriever.setDataSource(f.getPath());
        showFrameAfter(1);

        String time = metadataRetriever.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_DURATION);
        videoDuration = TimeUnit.MILLISECONDS.toMicros(Integer.parseInt(time));
//        FRAME_REQUEST_DENSITY = TimeUnit.SECONDS.toMicros(1) /
//                Integer.parseInt(metadataRetriever.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_FRAMERATE));
        FRAME_REQUEST_DENSITY = TimeUnit.SECONDS.toMicros(1) / 24;
//        new Thread(() -> {
//            for (long i = 0; (i < videoDuration) && (i < 7791542); i += FRAME_REQUEST_DENSITY) {
//                if (mCache.get(i) == null) {
//                    mCache.put(i, metadataRetriever.getFrameAtTime(i,
//                            FFmpegMediaMetadataRetriever.OPTION_CLOSEST));
//                    Log.d("Loading in bg", String.format("onCreate: i:%d, cache size:%d", i, mCache.size()));
//                }
////                synchronized (mCache) {
////                }
//            }
//            Log.d("A", "DONE");
//        }).start();
        calculateVidLengthAndFps();
        createObservable();

        createMarkPositionMap();
        addCircle();

//        try {
//            File file = getFile("q", R.raw.out1);
//            FrameGrab grab = null;
//            grab = FrameGrab.createFrameGrab(NIOUtils.readableChannel(file));
//            Picture picture;
//            long i = 0;
//            while (null != (picture = grab.getNativeFrame())) {
//                mCache.put(i, AndroidUtil.toBitmap(picture));
//                i += 1_000_000;
////                System.out.println(picture.getWidth() + "x" + picture.getHeight() + " " + picture.getColor());
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        } catch (JCodecException e) {
//            e.printStackTrace();
//        }

    }

    private void calculateVidLengthAndFps() {
        String time = metadataRetriever.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_DURATION);
        videoDuration = TimeUnit.MILLISECONDS.toMicros(Integer.parseInt(time));
        FRAME_REQUEST_DENSITY = TimeUnit.SECONDS.toMicros(1) / 24;
    }

    private void createMarkPositionMap() {
        mMarkPositions = new TreeMap<>();
        mMarkPositions.put(0L, new CirclePosition(0f, 0f, CirclePosition.CircleState.HIDE,
                new CircleMessage("STUB", "STUB")));
        mMarkPositions.put(800000L, new CirclePosition(300f, 600f, CirclePosition.CircleState.SHOW,
                new CircleMessage("Tiliwizor", getString(R.string.lorem_ipsum))));
        mMarkPositions.put(2000000L, new CirclePosition(100f, 300f, CirclePosition.CircleState.SHOW));
        mMarkPositions.put(2700000L, new CirclePosition(100f, 300f, CirclePosition.CircleState.HIDE));
        mMarkPositions.put(4000000L, new CirclePosition(900f, 200f, CirclePosition.CircleState.SHOW,
                new CircleMessage("GWIATEG", getString(R.string.lorem_ipsum))));
        mMarkPositions.put(4500000L, new CirclePosition(600f, 400f, CirclePosition.CircleState.SHOW));
        mMarkPositions.put(4800000L, new CirclePosition(550f, 450f, CirclePosition.CircleState.SHOW));
        mMarkPositions.put(5000000L, new CirclePosition(500f, 450f, CirclePosition.CircleState.SHOW));
        mMarkPositions.put(5800000L, new CirclePosition(500f, 480f, CirclePosition.CircleState.SHOW));
        mMarkPositions.put(6700000L, new CirclePosition(400f, 480f, CirclePosition.CircleState.SHOW));
        mMarkPositions.put(7200000L, new CirclePosition(100f, 480f, CirclePosition.CircleState.SHOW));
        mMarkPositions.put(7200001L, new CirclePosition(100f, 480f, CirclePosition.CircleState.HIDE));
        mMarkPositions.put(videoDuration, new CirclePosition(0f, 0f, CirclePosition.CircleState.HIDE));
    }

    private void updateCirclePosition(long time) {
        if (time < 0) {
            time = 0;
        }
        final Map.Entry<Long, CirclePosition> prevKey = mMarkPositions.floorEntry(time);
        final CirclePosition prevPos = prevKey.getValue();
        Map.Entry<Long, CirclePosition> nextKey = mMarkPositions.ceilingEntry(time);
        if (nextKey == null) {
            nextKey = prevKey;
        }
        final CirclePosition nextPos = nextKey.getValue();

        if (prevPos.state == CirclePosition.CircleState.HIDE) {
            mCircle.setVisibility(View.GONE);
            mInfoCl.setVisibility(View.GONE);
        }
        if (prevPos.state == CirclePosition.CircleState.SHOW) {
            float tp = (float)(time - prevKey.getKey()) / (nextKey.getKey() - prevKey.getKey());
            float dx = nextPos.x - prevPos.x;
            float dy = nextPos.y - prevPos.y;
            final float newX = prevPos.x + tp * dx;
            final float newY = prevPos.y + tp * dy;
            mCircle.setX(newX);
            mCircle.setY(newY);

            mCircle.setVisibility(View.VISIBLE);
        }
    }

    private Long getPreviousEntry(NavigableMap<Long, Object> map, Long key) {
        Long before = map.floorKey(key);
        Long after = map.ceilingKey(key);
        if (before == null) return after;
        if (after == null) return before;
        return (key - before < after - key
                || after - key < 0) && key - before > 0
                    ? before : after;
    }

    private void showCircle(float x, float y) {
        mCircle.setX(x);
        mCircle.setY(y);
        showCircle();
    }

    private void showCircle() {
        mCircle.setVisibility(View.VISIBLE);
    }

    private void hideCircle() {
        mCircle.setVisibility(View.GONE);
    }

    @NonNull
    private File getFile(String nameInCache, @RawRes int raw) {
        File f = new File(getCacheDir()+ nameInCache);
        if (!f.exists()) try {

            InputStream is = getResources().openRawResource(raw);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();


            FileOutputStream fos = new FileOutputStream(f);
            fos.write(buffer);
            fos.close();
        } catch (Exception e) { throw new RuntimeException(e); }
        return f;
    }

    static final int REQUEST_VIDEO_CAPTURE = 1;

    private void dispatchTakeVideoIntent() {
        Intent takeVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        if (takeVideoIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takeVideoIntent, REQUEST_VIDEO_CAPTURE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == REQUEST_VIDEO_CAPTURE && resultCode == RESULT_OK) {
            mVideoUri = intent.getData();
        }
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
        if (isViewContains(mImageView, ((int) x), ((int) y))) {
            switch (ev.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    cancelAnimations();
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
//            long d = delta;
            long d = (long) ((((SPEED * dx) / delta) + 1) * delta);
            if (x >= mX) {
                // moving finger to right
                d = -d;
            } else {
                // moving finger to left
                // OK
            }
//            It's kinda like a density
//            d = d / 100 * 100;
            showFrameAfter(d);
            mX = x;
            mY = y;
        }
    }

    private void showFrameAfter(final long delta) {
        long time = (getCurrentTime() + delta) / FRAME_REQUEST_DENSITY * FRAME_REQUEST_DENSITY;
        if (time - FRAME_REQUEST_DENSITY > videoDuration || time < 0) {
            return;
        }
        Bitmap bmpFromCache;
        synchronized (mCache) {
            bmpFromCache = mCache.get(time);
            if (bmpFromCache == null) {
                Bitmap bitmap = metadataRetriever.getFrameAtTime(time,
                        FFmpegMediaMetadataRetriever.OPTION_CLOSEST);
                if (bitmap != null) {
//                    Log.d("A", "showFrameAfter: AAAAAAAAAAAAa");
                    mCache.put(time, bitmap);
                }
            }
        }
        if (bmpFromCache != null) {
            loadBitmapIntoImage(bmpFromCache);
            setCurrentTime(getCurrentTime() + delta);
        }

    }

    private void loadBitmapIntoImage(Bitmap bitmap) {
        mImageView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        mImageView.setImageBitmap(bitmap);
        new Handler(getMainLooper()).post(() ->
                mImageView.setLayerType(View.LAYER_TYPE_NONE, null));
    }

    private void cancelAnimations() {
        if (flingAnimation != null) {
            flingAnimation.cancel();
        }
    }

    private class MyGestureListener extends GestureDetector.SimpleOnGestureListener {
        private ValueAnimator valueAnimator;

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            Log.d("HEHERE", "onFling: velocityX=" + velocityX);

//            if(velocityX < 0) {
//                return false;
//            }
            cancelAnimations();
            flingAnimation = new FlingAnimation(new FloatValueHolder(e1.getX()));
            flingAnimation.setStartVelocity(velocityX)
//                    .setMaxValue(200000f)
                    .setFriction(FLING_FRICTION)
                    .addUpdateListener((dynamicAnimation, val, velocity) -> {
//                        Log.d("A", String.format("onFling: val:%f, vel:%f", val, velocity));
                        showFrameAfter((long) -velocity * 5);
                    })
                    .addEndListener((dynamicAnimation, canceled, val, velocity) -> {})
                    .start();

            return true;
//            return super.onFling(e1, e2, velocityX, velocityY);
        }
    }

    final int threadPoolSize = Runtime.getRuntime().availableProcessors() + 1;
    ExecutorService threadPoolExecutor = Executors.newFixedThreadPool(threadPoolSize);

    public static void log(String msg) {
        String threadName = Thread.currentThread().getName();
        System.out.println(String.format("%s - %s", threadName, msg));
    }

    @SuppressLint("DefaultLocale")
    private void createObservable() {

        List<List<Long>> inputs = partitionVideo();

        currentThreadsNumber = new AtomicInteger(0);

        observable = Observable.from(inputs)
//                .observeOn(Schedulers.computation())
                .flatMap((in) ->
                        Observable.defer(() -> {
                            while (!(currentThreadsNumber.get() <= threadPoolSize)) { /*wait*/ }
                            log(String.format("Starting from: %d, n. of threads: %d", in.get(0), currentThreadsNumber.get()));
                            currentThreadsNumber.incrementAndGet();
                            return Observable.just(in);
                        }).subscribeOn(Schedulers.from(threadPoolExecutor))
                                .map(this::doHeavyWeightStuff))
                .subscribeOn(Schedulers.computation())
                .doOnError((e) -> {
                    log("error:\n");
                    e.printStackTrace();
                });
        subscription = subscribeToBeginLoading();

    }

    private Subscription subscribeToBeginLoading() {
        return observable
                .subscribe((lis) -> {
                    currentThreadsNumber.decrementAndGet();
                    log(String.format("Done, n. of threads: %d", currentThreadsNumber.get()));
                    synchronized (mCache) {
                        for (long key : lis.keySet()) {
                            final Bitmap bitmap = lis.get(key);
                            if (bitmap != null) {
                                mCache.put(key, bitmap);
                                Log.d("INS", String.format("subscribe: null bitmap at %d", key));
                            }
                            Log.d("INS", String.format("subscribe: inserted at %d", key));
                        }
                    }
                });
    }

    @NonNull
    private List<List<Long>> partitionVideo() {
        List<Long> frameTimes = new LinkedList<>();
        for (long i = 0; (i < videoDuration); i += FRAME_REQUEST_DENSITY) {
            frameTimes.add(i);
        }
        List<List<Long>> inputs = new LinkedList<>();
        for (int i = 0; i < frameTimes.size(); i += partitionSize) {
            inputs.add(frameTimes.subList(i,
                    Math.min(i + partitionSize, frameTimes.size())));
        }
        return inputs;
    }

    private  Map<Long, Bitmap> doHeavyWeightStuff(List<Long> input) {
        Map<Long, Bitmap> toRet = new LinkedHashMap<>();
        for (long key : input) {
            toRet.put(key, metadataRetriever.getFrameAtTime(key,
                    FFmpegMediaMetadataRetriever.OPTION_CLOSEST));
            Log.d("Loading in bg",
                    String.format("onCreate: i:%d(%d), cache size:%d, my part is %d of %d",
                            key,
                            TimeUnit.MICROSECONDS.toMillis(key),
                            mCache.size(),
                            toRet.keySet().size(),
                            input.size()));
            log("loadnig in bg...");
        }
        return toRet;
    }

}
