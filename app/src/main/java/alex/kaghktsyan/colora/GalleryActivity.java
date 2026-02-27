package alex.kaghktsyan.colora;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.widget.GridLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class GalleryActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_STORAGE = 1;
    private GridLayout galleryContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_gallery);

        galleryContainer = findViewById(R.id.gallery_container);

        if (checkPermission()) {
            loadDrawings();
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
                loadDrawings();
            } else {
                Toast.makeText(this, "Permission denied. Cannot load drawings.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void loadDrawings() {
        File directory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Colora");
        
        if (!directory.exists()) {
            Toast.makeText(this, "Directory not found. Try saving an image first.", Toast.LENGTH_SHORT).show();
            return;
        }

        File[] files = directory.listFiles();
        if (files != null && files.length > 0) {
            galleryContainer.removeAllViews(); // Clear before loading
            for (File file : files) {
                if (file.isFile() && (file.getName().endsWith(".png") || file.getName().endsWith(".jpg"))) {
                    DrawingItemFragment fragment = DrawingItemFragment.newInstance(file.getAbsolutePath());
                    getSupportFragmentManager().beginTransaction()
                            .add(galleryContainer.getId(), fragment)
                            .commit();
                }
            }
        } else {
            Toast.makeText(this, "No drawings found in Colora folder.", Toast.LENGTH_SHORT).show();
        }
    }
}
