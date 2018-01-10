package com.example.wojciechliebert.movieswiper;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.VideoView;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity {

    @BindView(R.id.main_vid)
    VideoView mVideoView;

    @BindView(R.id.button_interact)
    Button interactBtn;

    @BindView(R.id.play_pause_button)
    Button mPlayPauseButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ButterKnife.bind(this);

        String pathToVideo = "android.resource://" + getPackageName() + "/" + R.raw.out1;
        mVideoView.setVideoURI(Uri.parse(pathToVideo));
        mVideoView.setOnPreparedListener(mp -> mp.setLooping(true));

        mPlayPauseButton.setOnClickListener(view -> {
            if (mVideoView.isPlaying()) {
                mVideoView.pause();
            } else {
                mVideoView.start();
            }
        });

        mVideoView.start();

        interactBtn.setOnClickListener((View view) -> {
            Intent intent = new Intent(this, SwiperActivity.class);
            startActivity(intent);
        });

    }
}
