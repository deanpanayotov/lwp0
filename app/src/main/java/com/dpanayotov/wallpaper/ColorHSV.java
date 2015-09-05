package com.dpanayotov.wallpaper;

import java.util.Random;

/**
 * Created by Dean Panayotov Local on 5.9.2015
 */
public class ColorHSV {
    public float h;
    public float s;
    public float v;
    public float hf;
    public float sf;
    public float vf;
    private Random rand = new Random();

    public ColorHSV(float h, float s, float v, float hf, float sf, float vf) {
        this.h = h;
        this.s = s;
        this.v = v;
        this.hf = hf;
        this.sf = sf;
        this.vf = vf;
    }

    public int produceColor() {
        return android.graphics.Color.HSVToColor(new float[]{fiddle(h, hf, 360), fiddle(s, sf, 1),
                fiddle(v, vf, 1)});
    }

    private float fiddle(float val, float fiddle, float max) {
        if(fiddle == 0){
            return val;
        }
        return Math.min(max, val + (int) (rand.nextFloat() * (fiddle) - fiddle / 2));
    }
}
