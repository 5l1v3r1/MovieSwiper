package com.example.wojciechliebert.movieswiper;

import android.content.Context;

import java.util.ArrayList;

/**
 * Created by wojciech.liebert on 01.12.2017.
 */

public class Utils {

    public static ArrayList<Integer> getImagesIdentifiers(Context context) {
        int resID;
        int imgnum = 0;
        ArrayList<Integer> images = new ArrayList<>();

        do {
            resID = context.getResources()
                    .getIdentifier("room" + String.format("2110%3d", imgnum),
                            "drawable", context.getPackageName());
            if (resID != 0){
                images.add(resID);
            }
            imgnum++;
        }
        while (resID != 0);

        return images;
    }

}