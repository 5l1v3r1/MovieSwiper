package com.example.wojciechliebert.movieswiper.Adapters;

import android.content.Context;
import android.content.Intent;
import android.support.constraint.ConstraintLayout;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.example.wojciechliebert.movieswiper.R;

import java.util.ArrayList;

/**
 * Created by wojciech.liebert on 01.12.2017.
 */

public class SwiperAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public static final String IMAGE_INDEX_EXTRA = "image-index-extra";

    private Context mContext;
    private ArrayList<Integer> mImageList;

    public SwiperAdapter(Context context, ArrayList<Integer> imageList) {
        mContext = context;
        mImageList = imageList;
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
        ImageView imageView = ((SwiperImageViewHolder)holder).galleryImage;
        Glide.with(mContext)
                .load(mImageList.get(position))
                .into(imageView);
    }

    @Override
    public int getItemCount() {
        return mImageList.size();
    }
}

