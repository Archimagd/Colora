package alex.kaghktsyan.colora;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.List;

public class MainActivity extends AppCompatActivity {

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
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Новый рисунок")
                    .setMessage("Вы хотите создать новый рисунок?")
                    .setPositiveButton("Создать", (dialog, which) -> {
                        startActivity(new Intent(MainActivity.this, NewPaintingActivity.class));
                    })
                    .setNegativeButton("Отмена", null)
                    .show();
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
            }
            return false;
        });

        loadRecentDrawings();
    }

    public void loadRecentDrawings() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("paintings")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(5)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (isFinishing() || isDestroyed()) return;
                    
                    recentContainer.removeAllViews();
                    FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                    
                    List<Fragment> currentFragments = getSupportFragmentManager().getFragments();
                    for (Fragment f : currentFragments) {
                        if (f instanceof DrawingItemFragment) {
                            transaction.remove(f);
                        }
                    }

                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        String title = doc.getString("title");
                        String data = doc.getString("image_data");
                        if (title != null && data != null) {
                            DrawingItemFragment fragment = DrawingItemFragment.newInstanceFromCloud(doc.getId(), title, data, true);
                            transaction.add(recentContainer.getId(), fragment);
                        }
                    }
                    transaction.commitAllowingStateLoss();
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        BottomNavigationView navView = findViewById(R.id.nav_view);
        navView.setSelectedItemId(R.id.navigation_home);
        loadRecentDrawings();
    }
}
