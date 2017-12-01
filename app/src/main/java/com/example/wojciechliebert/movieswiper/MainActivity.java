package com.example.wojciechliebert.movieswiper;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity {

    @BindView(R.id.button_interact)
    Button interactBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ButterKnife.bind(this);

        interactBtn.setOnClickListener((View view) -> {
            Intent intent = new Intent(this, SwiperActivity.class);
            startActivity(intent);
        });
    }
}
