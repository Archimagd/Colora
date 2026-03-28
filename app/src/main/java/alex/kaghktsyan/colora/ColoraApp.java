package alex.kaghktsyan.colora;

import android.app.Application;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;

public class ColoraApp extends Application {
    private static final String PREFS_NAME = "theme_prefs";
    private static final String KEY_IS_NIGHT_MODE = "is_night_mode";

    @Override
    public void onCreate() {
        super.onCreate();
        
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean isNightMode = prefs.getBoolean(KEY_IS_NIGHT_MODE, false);
        
        if (isNightMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }
}