package com.dpanayotov.wallpaper;

import android.graphics.Color;

import java.util.Random;

/**
 * Created by Dean Panayotov Local on 5.9.2015
 */
public class ColorManager {

    public static final int BACKGROUND = Color.HSVToColor(new float[]{300, 1, 0.09f});

    private static final ColorHSV[] colors = {
            new ColorHSV(300, 1, 0.14f, 12, 0.08f, 0.08f),
            new ColorHSV(320, 0.6f, 0.24f, 12, 0.08f, 0.08f),
            new ColorHSV(0, 0.49f, 0.52f, 12, 0.08f, 0.08f),
            new ColorHSV(17, 0.85f, 0.60f, 12, 0.08f, 0.08f),
    };

    private static final float[] colorsProbability = {
            0.37f,
            0.58f,
            0.79f,
            1f
    };

    private static final short BUFFER_SIZE = 2048;
    private static int[] colorBuffer = new int[BUFFER_SIZE];
    private static Random rand;

    public static void init() {
        rand = new Random();
        for (int i = 0; i < BUFFER_SIZE; i++) {
            colorBuffer[i] = generateColor();
        }
    }



    private static int generateColor() {
        float val = rand.nextFloat();
        float border;
        for (int i = 0; i < colorsProbability.length; i++) {
            border = colorsProbability[i];
            if (val < border) {
                return colors[i].produceColor();
            }
        }
        return 1;
    }

    private static short colorIndex = 0;

    public static int getNextColor(){
        if(colorIndex == BUFFER_SIZE){
            colorIndex = 0;
        }
        return colorBuffer[colorIndex++];
    }
}
