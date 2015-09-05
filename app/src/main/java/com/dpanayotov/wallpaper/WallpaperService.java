package com.dpanayotov.wallpaper;

import java.util.Arrays;
import java.util.Random;

import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
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
                delta = (now - then) / 1000f;
                Log.d("zxc", "zxc delta: " + delta);
                then = now;
                update();
                draw();
            }
        };
        Random rand = new Random();
        private Circle[][] circles;
        private byte[] circlesLastIndex;
        private float[] rowSpeeds;
        private boolean[] rowSpeedsInverted;

        private Paint paint = new Paint();
        private short width;
        private short height;
        private short borderRight;
        private short borderLeft = (short) -CIRCLE_RADIUS;
        private byte arrayW;
        private byte arrayH;
        private byte lastElementIndex;
        private boolean visible = true;
        private boolean restart;

        private float delta;
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
            this.borderRight = (short) (this.width + CIRCLE_RADIUS);
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
            for (int i = 0; i < arrayH; i++) {
                for (int j = 0; j < arrayW; j++) {
                    paint.setColor(circles[i][j].c);
                    canvas.drawCircle(circles[i][j].x, circles[i][j].y, CIRCLE_RADIUS, paint);
                }
            }
        }

        /**
         * Initialize the whole drawing process
         */
        private void init(boolean force) {
            if (force || restart) {
                restart = false;
                arrayH = (byte) (Math.ceil(((float) height) / CIRCLE_DIAMETER) + 1);
                arrayW = (byte) (Math.ceil(((float) width) / CIRCLE_DIAMETER) + 1);
                lastElementIndex = (byte) (arrayW - 1);
                short startingOffset = (short) ((width % CIRCLE_DIAMETER + CIRCLE_DIAMETER) / 2);
                ColorManager.init();
                circles = new Circle[arrayH][arrayW];

                for (int i = 0; i < arrayH; i++) {
                    for (int j = 0; j < arrayW; j++) {
                        circles[i][j] = new Circle(j * CIRCLE_DIAMETER - startingOffset, i *
                                CIRCLE_DIAMETER + CIRCLE_RADIUS, ColorManager.getNextColor());
                    }
                }

                rowSpeeds = new float[arrayH];
                rowSpeedsInverted = new boolean[arrayH];
                circlesLastIndex = new byte[arrayH];
                for (int j = 0; j < arrayH; j++) {
                    rowSpeedsInverted[j] = rand.nextBoolean();
                    circlesLastIndex[j] = rowSpeedsInverted[j] ? 0 : lastElementIndex;
                    rowSpeeds[j] = rand.nextFloat() * ROW_MAX_SPEED * (rowSpeedsInverted[j] ? -1 :
                            1);
                }
                then = System.currentTimeMillis();
                handler.post(drawRunner);
            }
        }

        private void update() {
            float speed;
            for (int i = 0; i < arrayH; i++) {
                speed = rowSpeeds[i] * delta;
                for (int j = 0; j < arrayW; j++) {
                    circles[i][j].x += speed;
                }
                if (!rowSpeedsInverted[i]) {
                    if (circles[i][circlesLastIndex[i]].x > borderRight) {
                        if (circlesLastIndex[i] == lastElementIndex) {
                            circles[i][circlesLastIndex[i]].x = circles[i][0].x
                                    - CIRCLE_DIAMETER;

                        } else {
                            circles[i][circlesLastIndex[i]].x =
                                    circles[i][circlesLastIndex[i] + 1].x
                                            - CIRCLE_DIAMETER;
                        }
                        if (circlesLastIndex[i] == 0) {
                            circlesLastIndex[i] = lastElementIndex;
                        } else {
                            circlesLastIndex[i]--;
                        }
                    }
                } else {
                    if (circles[i][circlesLastIndex[i]].x < borderLeft) {
                        if (circlesLastIndex[i] == 0) {
                            circles[i][circlesLastIndex[i]].x =
                                    circles[i][lastElementIndex].x +
                                            CIRCLE_DIAMETER;
                        } else {
                            circles[i][circlesLastIndex[i]].x =
                                    circles[i][circlesLastIndex[i] - 1].x +
                                            CIRCLE_DIAMETER;
                        }

                        if (circlesLastIndex[i] == lastElementIndex) {
                            circlesLastIndex[i] = 0;
                        } else {
                            circlesLastIndex[i]++;
                        }
                    }
                }
            }
        }
    }
}
