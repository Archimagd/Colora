package alex.kaghktsyan.colora;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.io.File;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_STORAGE = 1;
    private LinearLayout recentContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recentContainer = findViewById(R.id.recent_container);

        View btnNewCanvas = findViewById(R.id.btn_new_canvas);
        View btnGalleryCard = findViewById(R.id.btn_gallery_card);
        View btnSeeAll = findViewById(R.id.btn_see_all);

        btnNewCanvas.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, NewPaintingActivity.class));
        });

        View.OnClickListener openGallery = v -> {
            startActivity(new Intent(MainActivity.this, GalleryActivity.class));
        };
        btnGalleryCard.setOnClickListener(openGallery);
        btnSeeAll.setOnClickListener(openGallery);

        BottomNavigationView navView = findViewById(R.id.nav_view);
        navView.setSelectedItemId(R.id.navigation_home);

        navView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.navigation_home) {
                return true;
            } else if (id == R.id.navigation_profile) {
                startActivity(new Intent(MainActivity.this, ProfileActivity.class));
                return true;
            }
            return false;
        });

        if (checkPermission()) {
            loadRecentDrawings();
        } else {
            requestPermission();
        }
    }

    private boolean checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED;
        } else {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_MEDIA_IMAGES}, REQUEST_CODE_STORAGE);
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_CODE_STORAGE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadRecentDrawings();
            }
        }
    }

    private void loadRecentDrawings() {
        new Thread(() -> {
            File directory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Colora");
            
            if (!directory.exists()) {
                return;
            }

            File[] files = directory.listFiles(file -> file.isFile() && (file.getName().endsWith(".png") || file.getName().endsWith(".jpg")));
            
            if (files != null && files.length > 0) {
                Arrays.sort(files, (f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified()));
                
                final File[] finalFiles = files;
                runOnUiThread(() -> {
                    recentContainer.removeAllViews();
                    int limit = Math.min(finalFiles.length, 10);
                    for (int i = 0; i < limit; i++) {
                        File file = finalFiles[i];
                        DrawingItemFragment fragment = DrawingItemFragment.newInstance(file.getAbsolutePath(), true);
                        getSupportFragmentManager().beginTransaction()
                                .add(recentContainer.getId(), fragment)
                                .commitAllowingStateLoss();
                    }
                });
            }
        }).start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        BottomNavigationView navView = findViewById(R.id.nav_view);
        navView.setSelectedItemId(R.id.navigation_home);
        if (checkPermission()) {
            loadRecentDrawings();
        }
    }
}