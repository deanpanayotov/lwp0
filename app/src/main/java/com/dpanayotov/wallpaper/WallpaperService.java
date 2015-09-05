package com.dpanayotov.wallpaper;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.SurfaceHolder;

/**
 * Created by Dean Panayotov Local on 2.9.2015
 */
public class WallpaperService extends android.service.wallpaper.WallpaperService {

    @Override
    public android.service.wallpaper.WallpaperService.Engine onCreateEngine() {
        return new MyWallpaperEngine();
    }

    private class MyWallpaperEngine extends android.service.wallpaper.WallpaperService.Engine {

        private static final byte FRAME = 30; //in milliseconds;
        private short CIRCLE_RADIUS = 48;
        private short CIRCLE_DIAMETER = (short) (CIRCLE_RADIUS * 2);
        private short ROW_MAX_SPEED = (short) (CIRCLE_DIAMETER * 1); //per second

        private final Handler handler = new Handler();
        private final Runnable drawRunner = new Runnable() {
            @Override
            public void run() {
                now = System.currentTimeMillis();
                delta = (now - then) / 1000d;
                then = now;
                update();
                draw();
            }
        };
        Random rand = new Random();
        private List<List<Circle>> circles;
        private float[] rowSpeeds;
        private Paint paint = new Paint();
        private short width;
        private short height;
        private boolean visible = true;
        private boolean restart;

        private double delta;
        private long then;
        private long now;


        private SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(WallpaperService.this);
        private SharedPreferences.OnSharedPreferenceChangeListener prefsListener =
                new SharedPreferences.OnSharedPreferenceChangeListener() {
                    @Override
                    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                        getPreferences();
                        init(true);
                    }
                };

        public MyWallpaperEngine() {
            getPreferences();
            circles = new ArrayList<>();
            paint.setAntiAlias(true);
            paint.setColor(Color.WHITE);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeJoin(Paint.Join.ROUND);
            paint.setStrokeWidth(10f);
            paint.setStyle(Paint.Style.FILL);
        }

        private void getPreferences() {
//            maxNumber = Integer
//                    .valueOf(prefs.getString("numberOfCircles", "4"));
//            touchEnabled = prefs.getBoolean("touch", false);
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            this.visible = visible;
            if (visible) {
                handler.post(drawRunner);
            } else {
                handler.removeCallbacks(drawRunner);
            }
        }

        @Override
        public void onSurfaceCreated(SurfaceHolder holder) {
            super.onSurfaceCreated(holder);
            prefs.registerOnSharedPreferenceChangeListener(prefsListener);
            restart = true;
        }

        @Override
        public void onSurfaceDestroyed(SurfaceHolder holder) {
            super.onSurfaceDestroyed(holder);
            prefs.unregisterOnSharedPreferenceChangeListener(prefsListener);
            this.visible = false;
            handler.removeCallbacks(drawRunner);
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format,
                                     int width, int height) {
            this.width = (short) width;
            this.height = (short) height;
            init(false);
            super.onSurfaceChanged(holder, format, width, height);
        }

        private void draw() {
            if (visible) {
                SurfaceHolder holder = getSurfaceHolder();
                Canvas canvas = null;
                try {
                    canvas = holder.lockCanvas();
                    if (canvas != null) {
                        canvas.drawColor(ColorManager.BACKGROUND);
                        drawCircles(canvas);
                    }
                } finally {
                    if (canvas != null)
                        holder.unlockCanvasAndPost(canvas);
                }
                handler.removeCallbacks(drawRunner);
                handler.postDelayed(drawRunner, FRAME);
            }
        }

        // Surface view requires that all elements are drawn completely
        private void drawCircles(Canvas canvas) {
            for (List<Circle> row : circles) {
                for (Circle circle : row) {
                    paint.setColor(circle.c);
                    canvas.drawCircle(circle.x, circle.y, CIRCLE_RADIUS, paint);
                }
            }
        }

        /**
         * Initialize the whole drawing process
         */
        private void init(boolean force) {
            if (force || restart) {
                restart = false;
                circles = new ArrayList<>();
                List<Circle> row;
                short h = (short) (Math.ceil(((float) height) / CIRCLE_DIAMETER) + 1);
                short w = (short) (Math.ceil(((float) width) / CIRCLE_DIAMETER) + 1);
                short startingOffset = (short) ((width % CIRCLE_DIAMETER + CIRCLE_DIAMETER) / 2);
                ColorManager.init();

                for (int i = 0; i < h; i++) {
                    row = new ArrayList<>();
                    for (int j = 0; j < w; j++) {
                        row.add(new Circle(j * CIRCLE_DIAMETER - startingOffset, i *
                                CIRCLE_DIAMETER + CIRCLE_RADIUS, ColorManager.getNextColor()));
                    }
                    circles.add(row);
                }
                rowSpeeds = new float[h];
                for (int j = 0; j < h; j++) {
                    rowSpeeds[j] = rand.nextFloat() * ROW_MAX_SPEED * (rand.nextBoolean() ? 1 : -1);
                }
                then = System.currentTimeMillis();
                handler.post(drawRunner);
            }
        }

        private void update() {
            int rowIndex = 0;
            float speed;
            Circle edgeCircle, otherEdgeCircle;
            for (List<Circle> row : circles) {
                speed = (float) (rowSpeeds[rowIndex] * delta);
                for (Circle circle : row) {
                    circle.x += speed;
                }
                if (rowSpeeds[rowIndex] > 0) {
                    edgeCircle = row.get(row.size() - 1);
                    if (edgeCircle.x > width + CIRCLE_RADIUS) {
                        otherEdgeCircle = row.get(0);
                        row.remove(edgeCircle);
                        edgeCircle.x = otherEdgeCircle.x - CIRCLE_DIAMETER;
                        row.add(0, edgeCircle);
                    }
                } else {
                    edgeCircle = row.get(0);
                    if (edgeCircle.x < -CIRCLE_RADIUS) {
                        otherEdgeCircle = row.get(row.size() - 1);
                        row.remove(edgeCircle);
                        edgeCircle.x = otherEdgeCircle.x + CIRCLE_DIAMETER;
                        row.add(edgeCircle);
                    }
                }
                rowIndex++;
            }
        }
    }
}
