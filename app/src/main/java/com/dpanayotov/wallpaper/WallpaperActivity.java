package com.dpanayotov.wallpaper;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.WallpaperManager;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

/**
 * Created by Dean Panayotov Local on 2.9.2015
 */
public class WallpaperActivity extends Activity{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wallpaper);
        findViewById(R.id.apply_wallpaper).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                if (Build.VERSION.SDK_INT >= 16) {
                    intent.setAction(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER);
                    intent.putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                            new ComponentName(WallpaperActivity.this, WallpaperService.class));
                } else {
                    Toast.makeText(WallpaperActivity.this, R.string.selection_message, Toast
                            .LENGTH_SHORT).show();
                    intent.setAction(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER);
                }
                startActivity(intent);
            }
        });

        findViewById(R.id.preferences).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentManager fragmentManager = getFragmentManager();
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                Fragment fragment = new PreferencesFragment();
                fragmentTransaction.add(R.id.frame, fragment, "-");
                fragmentTransaction.commit();
            }
        });
    }
}
