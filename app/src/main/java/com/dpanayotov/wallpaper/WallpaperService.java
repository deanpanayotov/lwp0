package com.dpanayotov.wallpaper;

import java.util.ArrayList;
import java.util.List;

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

        private static final char FRAME = 30; //in milliseconds;

        private final Handler handler = new Handler();
        private final Runnable drawRunner = new Runnable() {
            @Override
            public void run() {
                draw();
            }

        };
        private List<List<Circle>> circles;
        private Paint paint = new Paint();
        private short width;
        private short height;
        private boolean visible = true;
        private boolean restart;

        private short circleRadius = 144;

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
            handler.post(drawRunner);
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
                        drawCircles(canvas);
//                        if (circles.size() >= maxNumber) {
//                            circles.clear();
//                        }
//                        int x = (int) (width * Math.random());
//                        int y = (int) (height * Math.random());
//                        circles.add(new Circle(x, y));
//                        drawCircles(canvas, circles);
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
            for (List<Circle> column : circles) {
                for(Circle circle : column){
                    canvas.drawColor(circle.c);
                    canvas.drawCircle(circle.x, circle.y, 20.0f, paint);
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
                short halfRadius = (short) (circleRadius / 2);
                List<Circle> column;
                short w = (short) Math.ceil(((float) width) / circleRadius);
                short h = (short) Math.ceil(((float) height) / circleRadius);
                for (int i = 0; i < w; i++) {
                    column = new ArrayList();
                    for (int j = 0; j < h; j++) {
                        column.add(new Circle(i * circleRadius + halfRadius, j * circleRadius +
                                halfRadius, generateColor()));
                    }
                    circles.add(column);
                }
            }
        }

        private int generateColor(){
            return 1;
        }
    }
}
