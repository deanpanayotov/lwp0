package com.dpanayotov.wallpaper;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import android.content.SharedPreferences;
import android.graphics.Canvas;
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

        private static final byte FRAMES_PER_SECOND = 100;
        private static final byte FRAME = 1000 / FRAMES_PER_SECOND; //in milliseconds;
        private short CIRCLE_RADIUS = 160; //in pixels
        private short CIRCLE_DIAMETER = (short) (CIRCLE_RADIUS * 2);
        private short ROW_MAX_SPEED = (short) (CIRCLE_DIAMETER * 0.2); //per second
        private short ROW_MIN_SPEED = (short) (CIRCLE_DIAMETER * 0.05);

        Random rand = new Random();
        private Paint paint = new Paint();

        private List<List<Circle>> circles;
        private float[] rowSpeeds;
        private boolean[] rowSpeedsInverted;

        //dimensions
        private short width;
        private short height;
        private byte arrayW;
        private byte arrayH;

        //magic values
        private short borderRight;
        private short borderLeft = (short) -CIRCLE_RADIUS;
        private byte lastElementIndex;

        //lifecycle
        private boolean visible = true;
        private boolean restart;
        private float delta;
        private long then;
        private long now;

        public MyWallpaperEngine() {
            getPreferences();
            paint.setAntiAlias(true);
            paint.setStyle(Paint.Style.FILL);
        }

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

        private void getPreferences() {
//            maxNumber = Integer
//                    .valueOf(prefs.getString("numberOfCircles", "4"));
//            touchEnabled = prefs.getBoolean("touch", false);
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            this.visible = visible;
            if (visible) {
                then = System.currentTimeMillis();
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
            onVisibilityChanged(false);
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format,
                                     int width, int height) {
            super.onSurfaceChanged(holder, format, width, height);
            this.width = (short) width;
            this.height = (short) height;
            this.borderRight = (short) (this.width + CIRCLE_RADIUS);
            init(false);
        }

        /**
         * Initialize the whole drawing process
         */
        private void init(boolean force) {
            if (force || restart) {
                restart = false;
                circles = new ArrayList<>();
                List<Circle> row;
                arrayH = (byte) (Math.ceil(((float) height) / CIRCLE_DIAMETER) + 2);
                arrayW = (byte) (Math.ceil(((float) width) / CIRCLE_DIAMETER) + 1);
                lastElementIndex = (byte) (arrayW - 1);

                short startingOffset = (short) ((width % CIRCLE_DIAMETER + CIRCLE_DIAMETER) / 2);
                ColorManager.init();
                for (int i = 0; i < arrayH; i++) {
                    row = new ArrayList<>();
                    for (int j = 0; j < arrayW; j++) {
                        row.add(new Circle(j * CIRCLE_DIAMETER - startingOffset, i *
                                CIRCLE_DIAMETER, ColorManager.getNextColor()));
                    }
                    circles.add(row);
                }
                rowSpeeds = new float[arrayH];
                rowSpeedsInverted = new boolean[arrayH];
                for (int j = 0; j < arrayH; j++) {
                    rowSpeedsInverted[j] = rand.nextBoolean();
                    rowSpeeds[j] = ( rand.nextFloat() * (ROW_MAX_SPEED - ROW_MIN_SPEED ) +
                            ROW_MIN_SPEED ) * (rowSpeedsInverted[j] ? 1 : -1);
                }
                then = System.currentTimeMillis();
                handler.post(drawRunner);
            }
        }

        private final Handler handler = new Handler();
        private final Runnable drawRunner = new Runnable() {
            @Override
            public void run() {
                step();
            }
        };

        private void step(){
            now = System.currentTimeMillis();
            delta = (now - then) / 1000f;
            then = now;
            update();
            draw();
        }

        private void update() {
            int rowIndex = 0;
            float speed;
            Circle edgeCircle, otherEdgeCircle;
            for (List<Circle> row : circles) {
                speed = rowSpeeds[rowIndex] * delta;
                for (Circle circle : row) {
                    circle.x += speed;
                }
                if (rowSpeedsInverted[rowIndex]) {
                    edgeCircle = row.get(lastElementIndex);
                    if (edgeCircle.x > borderRight) {
                        otherEdgeCircle = row.get(0);
                        row.remove(edgeCircle);
                        edgeCircle.x = otherEdgeCircle.x - CIRCLE_DIAMETER;
                        row.add(0, edgeCircle);
                    }
                } else {
                    edgeCircle = row.get(0);
                    if (edgeCircle.x < borderLeft) {
                        otherEdgeCircle = row.get(lastElementIndex);
                        row.remove(edgeCircle);
                        edgeCircle.x = otherEdgeCircle.x + CIRCLE_DIAMETER;
                        row.add(edgeCircle);
                    }
                }
                rowIndex++;
            }
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
                handler.postDelayed(drawRunner, FRAME);
            }
        }

        private void drawCircles(Canvas canvas) {
            for (List<Circle> row : circles) {
                for (Circle circle : row) {
                    paint.setColor(circle.c);
                    canvas.drawCircle(circle.x, circle.y, CIRCLE_RADIUS, paint);
                }
            }
        }
    }
}
