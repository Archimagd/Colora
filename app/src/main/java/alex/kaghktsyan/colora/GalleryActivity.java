package alex.kaghktsyan.colora;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class GalleryActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_STORAGE = 1;
    private LinearLayout galleryContainer;
    private TextView txtCount;
    private EditText editSearch;
    private List<File> allFiles = new ArrayList<>();
    private FavoritesManager favoritesManager;
    
    private CheckBox chipAll, chipRecent, chipFavorites;
    private String currentSearchQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_gallery);

        favoritesManager = new FavoritesManager(this);
        galleryContainer = findViewById(R.id.gallery_container);
        txtCount = findViewById(R.id.txtCount);
        editSearch = findViewById(R.id.editSearch);
        
        chipAll = findViewById(R.id.chipAll);
        chipRecent = findViewById(R.id.chipRecent);
        chipFavorites = findViewById(R.id.chipFavorites);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnAddNew).setOnClickListener(v -> {
            startActivity(new Intent(this, NewPaintingActivity.class));
        });

        setupChips();

        editSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s.toString();
                applyFilters();
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        if (checkPermission()) {
            loadDrawings();
        } else {
            requestPermission();
        }
    }

    private void setupChips() {
        chipAll.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                chipRecent.setChecked(false);
                chipFavorites.setChecked(false);
                applyFilters();
            } else if (!chipRecent.isChecked() && !chipFavorites.isChecked()) {
                chipAll.setChecked(true); // Keep at least one checked
            }
        });

        chipRecent.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                chipAll.setChecked(false);
                chipFavorites.setChecked(false);
                applyFilters();
            } else if (!chipAll.isChecked() && !chipFavorites.isChecked()) {
                chipAll.setChecked(true);
            }
        });

        chipFavorites.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                chipAll.setChecked(false);
                chipRecent.setChecked(false);
                applyFilters();
            } else if (!chipAll.isChecked() && !chipRecent.isChecked()) {
                chipAll.setChecked(true);
            }
        });
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
                Toast.makeText(this, "Permission denied.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void loadDrawings() {
        File directory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Colora");
        
        if (!directory.exists()) {
            txtCount.setText("0 работ");
            return;
        }

        File[] files = directory.listFiles(file -> file.isFile() && (file.getName().endsWith(".png") || file.getName().endsWith(".jpg")));
        
        if (files != null) {
            allFiles = new ArrayList<>(Arrays.asList(files));
            // Sort by last modified descending by default
            allFiles.sort((f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified()));
            applyFilters();
        }
    }

    public void onFavoriteToggled() {
        if (chipFavorites.isChecked()) {
            applyFilters();
        }
    }

    private void applyFilters() {
        List<File> filtered = new ArrayList<>();
        for (File file : allFiles) {
            boolean matchesSearch = file.getName().toLowerCase().contains(currentSearchQuery.toLowerCase());
            boolean matchesChip = true;

            if (chipFavorites.isChecked()) {
                matchesChip = favoritesManager.isFavorite(file.getAbsolutePath());
            } else if (chipRecent.isChecked()) {
                // For "Recent", we could limit to last 7 days or just keep the default sort
                // Here let's assume "Recent" is just the same list sorted by date (which it already is)
                // but maybe limited in count? Or just the default view.
                // Let's keep it as is for now.
            }

            if (matchesSearch && matchesChip) {
                filtered.add(file);
            }
        }
        displayFiles(filtered);
    }

    private void displayFiles(List<File> files) {
        galleryContainer.removeAllViews();
        txtCount.setText(String.format(Locale.getDefault(), "%d %s", files.size(), getWorkWord(files.size())));

        for (File file : files) {
            DrawingItemFragment fragment = DrawingItemFragment.newInstance(file.getAbsolutePath());
            getSupportFragmentManager().beginTransaction()
                    .add(galleryContainer.getId(), fragment)
                    .commit();
        }
    }

    private String getWorkWord(int count) {
        int lastDigit = count % 10;
        int lastTwoDigits = count % 100;
        if (lastTwoDigits >= 11 && lastTwoDigits <= 19) return "работ";
        if (lastDigit == 1) return "работа";
        if (lastDigit >= 2 && lastDigit <= 4) return "работы";
        return "работ";
    }
}
