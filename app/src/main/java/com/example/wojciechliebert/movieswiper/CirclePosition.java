package com.example.wojciechliebert.movieswiper;

/**
 * Created by wojciech.liebert on 08.01.2018.
 */

class CirclePosition {
//    public T time;
    public float x;
    public float y;
    public int state;

    public CirclePosition(float x, float y, int state) {
//        time = t;
        this.x = x;
        this.y = y;
        this.state = state;
    }

    public class CircleState {
        public static final int SHOW = 1;
        public static final int HIDE = 0;
    }
}
