package com.example.wojciechliebert.movieswiper.Adapters;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.support.constraint.ConstraintLayout;
import android.support.v7.widget.RecyclerView;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.FutureTarget;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.util.LruCache;
import com.example.wojciechliebert.movieswiper.R;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

/**
 * Created by wojciech.liebert on 01.12.2017.
 */

public class SwiperAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public static final String IMAGE_INDEX_EXTRA = "image-index-extra";

    private Context mContext;
    private ArrayList<Integer> mImageList;

    private RequestManager glide;
    private LruCache<String, Bitmap> memCache;
    private ArrayList<FutureTarget<Bitmap>> imageViewFutureTargets;

    public SwiperAdapter(Context context, ArrayList<Integer> imageList) {
        mContext = context;
        mImageList = imageList;

        glide = Glide.with(mContext);

        memCache = new LruCache<String, Bitmap>(mImageList.size()) {
            @Override
            protected int getSize(Bitmap item) {
                return item.getByteCount()/1024;
            }
        };

        imageViewFutureTargets = new ArrayList<>();

        Display display = ((Activity) mContext).getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x; //width of screen in pixels
        int height = size.y;//height of screen in pixels
        final RequestOptions requestOptions = new RequestOptions().diskCacheStrategy(DiskCacheStrategy.ALL);
        for (int i = 0; i < mImageList.size() - 1; i++) {
            final int index = i;
            glide.load(mImageList.get(i))
                    .apply(requestOptions)
                    .preload();

//            imageViewFutureTargets.add(
//                    glide.asBitmap().load(mImageList.get(i)).submit()
//            );

//            glide
//                    .asBitmap()
//                    .load(mImageList.get(i))
//                    .submit(width, height)
////                .apply(requestOptions) //fits given dimensions maintaining ratio
////                    .into(new SimpleTarget(width, height) {
////                        @Override
////                        public void onResourceReady(Object resource, Transition transition) {
////                            memCache.put(String.valueOf(index), ((Bitmap) resource));
////                        }
//                    });
        }


    }

    private class SwiperImageViewHolder extends RecyclerView.ViewHolder {
        private ImageView galleryImage;

        public SwiperImageViewHolder(View itemView) {
            super(itemView);
            galleryImage = itemView.findViewById(R.id.swiper_frame_iv);
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.swiper_frame_vh, parent, false);
        int height = ((View)parent.getParent()).getHeight();
        itemView.setMinimumHeight(height);
        ((ConstraintLayout)itemView).setMaxHeight(height);
        return new SwiperImageViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        ImageView imageView = ((SwiperImageViewHolder) holder).galleryImage;
//        Glide.with(mContext)
//                .load(mImageList.get(position))
//                .pr
//        Bitmap image = memCache.get(String.valueOf(position));
        Bitmap image = null;
        if (image != null) {
            //Bitmap exists in cache.
            imageView.setImageBitmap(image);
        } else {
            //Bitmap not found in cache reload it
            glide
                    .load(mImageList.get(position))
                    .into(imageView);
        }
//        glide.load(mImageList.get(position))
//                .into(imageView);
    }

    @Override
    public int getItemCount() {
        return mImageList.size();
    }
}

