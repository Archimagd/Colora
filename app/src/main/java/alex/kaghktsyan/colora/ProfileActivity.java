package alex.kaghktsyan.colora;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.switchmaterial.SwitchMaterial;

public class ProfileActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "theme_prefs";
    private static final String KEY_IS_NIGHT_MODE = "is_night_mode";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean isNightMode = prefs.getBoolean(KEY_IS_NIGHT_MODE, false);

        SwitchMaterial switchTheme = findViewById(R.id.switch_theme);
        switchTheme.setChecked(isNightMode);

        View loadingOverlay = findViewById(R.id.loading_overlay);

        switchTheme.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Показываем индикатор загрузки
            loadingOverlay.setVisibility(View.VISIBLE);
            loadingOverlay.setAlpha(0f);
            loadingOverlay.animate().alpha(1f).setDuration(200).start();

            // Небольшая задержка для плавности анимации
            buttonView.postDelayed(() -> {
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean(KEY_IS_NIGHT_MODE, isChecked);
                editor.apply();

                if (isChecked) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                } else {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                }
            }, 300);
        });

        BottomNavigationView navView = findViewById(R.id.nav_view);
        navView.setSelectedItemId(R.id.navigation_profile);

        navView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.navigation_home) {
                Intent intent = new Intent(this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
                return true;
            } else if (id == R.id.navigation_profile) {
                return true;
            }
            return false;
        });
    }
}