package com.example.wojciechliebert.movieswiper;

/**
 * Created by wojciech.liebert on 08.01.2018.
 */

class CirclePosition {
    public final CircleMessage message;
    public float x;
    public float y;
    public int state;

    public CirclePosition(float x, float y, int state) {
        this.x = x;
        this.y = y;
        this.state = state;
        this.message = null;
    }

    public CirclePosition(float x, float y, int state, CircleMessage message) {
        this.x = x;
        this.y = y;
        this.state = state;
        this.message = message;
    }

    public static class CircleState {
        public static final int SHOW = 1;
        public static final int HIDE = 0;
    }
}
