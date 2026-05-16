package alex.kaghktsyan.colora;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class GalleryActivity extends AppCompatActivity {

    private LinearLayout galleryContainer;
    private TextView txtCount;
    private EditText editSearch;
    private List<CloudDrawing> cloudDrawings = new ArrayList<>();
    private FavoritesManager favoritesManager;
    
    private CheckBox chipAll, chipRecent, chipFavorites;
    private String currentSearchQuery = "";

    public static class CloudDrawing {
        String id;
        String title;
        String data;
        long timestamp;

        CloudDrawing(String id, String title, String data, long timestamp) {
            this.id = id;
            this.title = title;
            this.data = data;
            this.timestamp = timestamp;
        }
    }

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
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Новый рисунок")
                    .setMessage("Вы хотите создать новый рисунок?")
                    .setPositiveButton("Создать", (dialog, which) -> {
                        startActivity(new Intent(this, NewPaintingActivity.class));
                    })
                    .setNegativeButton("Отмена", null)
                    .show();
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

        loadCloudDrawings();
    }

    private void setupChips() {
        chipAll.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                chipRecent.setChecked(false);
                chipFavorites.setChecked(false);
                applyFilters();
            } else if (!chipRecent.isChecked() && !chipFavorites.isChecked()) {
                chipAll.setChecked(true);
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

    public void loadCloudDrawings() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("paintings")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (isFinishing() || isDestroyed()) return;
                    cloudDrawings.clear();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        String title = doc.getString("title");
                        String data = doc.getString("image_data");
                        com.google.firebase.Timestamp ts = doc.getTimestamp("timestamp");
                        if (title != null && data != null && ts != null) {
                            cloudDrawings.add(new CloudDrawing(doc.getId(), title, data, ts.getSeconds()));
                        }
                    }
                    applyFilters();
                });
    }

    private void applyFilters() {
        if (isFinishing() || isDestroyed()) return;

        // Удаляем старые фрагменты
        FragmentTransaction removeTransaction = getSupportFragmentManager().beginTransaction();
        List<Fragment> currentFragments = getSupportFragmentManager().getFragments();
        for (Fragment f : currentFragments) {
            if (f instanceof DrawingItemFragment) {
                removeTransaction.remove(f);
            }
        }
        removeTransaction.commitNowAllowingStateLoss();

        galleryContainer.removeAllViews();
        int totalCount = 0;

        FragmentTransaction addTransaction = getSupportFragmentManager().beginTransaction();

        for (CloudDrawing cd : cloudDrawings) {
            boolean matchesSearch = cd.title.toLowerCase().contains(currentSearchQuery.toLowerCase());
            boolean matchesChip = true;

            if (chipFavorites.isChecked()) {
                matchesChip = favoritesManager.isFavorite(cd.title);
            }

            if (matchesSearch && matchesChip) {
                DrawingItemFragment fragment = DrawingItemFragment.newInstanceFromCloud(cd.id, cd.title, cd.data, false);
                addTransaction.add(galleryContainer.getId(), fragment);
                totalCount++;
            }
        }
        
        addTransaction.commitAllowingStateLoss();
        txtCount.setText(String.format(Locale.getDefault(), "%d %s", totalCount, getWorkWord(totalCount)));
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
