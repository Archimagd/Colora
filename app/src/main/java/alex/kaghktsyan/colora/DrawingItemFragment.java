package alex.kaghktsyan.colora;

import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

public class DrawingItemFragment extends Fragment {

    private static final String ARG_IS_SMALL = "is_small";
    private static final String ARG_CLOUD_DATA = "cloud_data";
    private static final String ARG_CLOUD_TITLE = "cloud_title";

    private String cloudData;
    private String cloudTitle;
    private boolean isSmall;
    private FavoritesManager favoritesManager;

    public static DrawingItemFragment newInstanceFromCloud(String title, String base64Data, boolean isSmall) {
        DrawingItemFragment fragment = new DrawingItemFragment();
        Bundle args = new Bundle();
        args.putString(ARG_CLOUD_TITLE, title);
        args.putString(ARG_CLOUD_DATA, base64Data);
        args.putBoolean(ARG_IS_SMALL, isSmall);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            cloudData = getArguments().getString(ARG_CLOUD_DATA);
            cloudTitle = getArguments().getString(ARG_CLOUD_TITLE);
            isSmall = getArguments().getBoolean(ARG_IS_SMALL);
        }
        favoritesManager = new FavoritesManager(requireContext());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_drawing_item, container, false);
        
        ImageView imageView = view.findViewById(R.id.imgDrawingPreview);
        TextView txtTitle = view.findViewById(R.id.txtDrawingTitle);
        TextView txtDate = view.findViewById(R.id.txtDrawingDate);
        View cardImage = view.findViewById(R.id.cardImage);
        View root = view.findViewById(R.id.itemRoot);
        ImageButton btnLike = view.findViewById(R.id.btnLike);
        ImageButton btnShare = view.findViewById(R.id.btnShare);
        ImageButton btnDownload = view.findViewById(R.id.btnDownload);

        if (isSmall) {
            if (root != null) {
                ViewGroup.LayoutParams rootParams = root.getLayoutParams();
                if (rootParams != null) {
                    rootParams.width = dpToPx(180);
                    root.setLayoutParams(rootParams);
                }
                root.setPadding(0, 0, dpToPx(12), 0);
            }
            if (cardImage != null) {
                ViewGroup.LayoutParams cardParams = cardImage.getLayoutParams();
                if (cardParams != null) {
                    cardParams.height = dpToPx(120);
                    cardImage.setLayoutParams(cardParams);
                }
            }
            if (txtTitle != null) txtTitle.setTextSize(12);
            if (txtDate != null) txtDate.setTextSize(10);
            if (btnLike != null) btnLike.setVisibility(View.GONE);
            if (btnShare != null) btnShare.setVisibility(View.GONE);
            if (btnDownload != null) btnDownload.setVisibility(View.GONE);
        }

        if (cloudData != null) {
            setupCloudItem(view, imageView, txtTitle, txtDate, btnLike, btnShare, btnDownload);
        }
        
        return view;
    }

    private void setupCloudItem(View root, ImageView imageView, TextView txtTitle, TextView txtDate, ImageButton btnLike, ImageButton btnShare, ImageButton btnDownload) {
        txtTitle.setText(cloudTitle);
        txtDate.setText("В облаке");
        
        byte[] decodedString = Base64.decode(cloudData, Base64.DEFAULT);
        Bitmap bitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
        imageView.setImageBitmap(bitmap);

        if (btnLike != null && !isSmall) {
            btnLike.setOnClickListener(v -> {
                favoritesManager.toggleFavorite(cloudTitle);
            });
        }
        
        if (btnDownload != null && !isSmall) {
            btnDownload.setOnClickListener(v -> {
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Скачивание")
                        .setMessage("Вы хотите сохранить этот рисунок в галерею устройства?")
                        .setPositiveButton("Скачать", (dialog, which) -> saveCloudImageToGallery(bitmap, cloudTitle))
                        .setNegativeButton("Отмена", null)
                        .show();
            });
        }

        if (btnShare != null && !isSmall) {
            btnShare.setOnClickListener(v -> shareImage(bitmap, cloudTitle));
        }

        root.setOnClickListener(v -> {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Открыть рисунок")
                    .setMessage("Вы хотите перейти к редактированию этого рисунка?")
                    .setPositiveButton("Открыть", (dialog, which) -> {
                        Intent intent = new Intent(getActivity(), NewPaintingActivity.class);
                        intent.putExtra("cloud_image_data", cloudData);
                        startActivity(intent);
                    })
                    .setNegativeButton("Отмена", null)
                    .show();
        });
    }

    private void shareImage(Bitmap bitmap, String title) {
        try {
            File cachePath = new File(requireContext().getCacheDir(), "images");
            cachePath.mkdirs();
            File file = new File(cachePath, title + ".png");
            FileOutputStream stream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            stream.close();

            Uri uri = FileProvider.getUriForFile(requireContext(), 
                    requireContext().getPackageName() + ".fileprovider", file);

            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("image/*");
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, "Поделиться"));
        } catch (Exception ignored) {
        }
    }

    private void saveCloudImageToGallery(Bitmap bitmap, String fileName) {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName + ".png");
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Colora");
        }

        Uri uri = requireContext().getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        if (uri != null) {
            try (OutputStream out = requireContext().getContentResolver().openOutputStream(uri)) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                Toast.makeText(requireContext(), "Сохранено в галерею", Toast.LENGTH_SHORT).show();
            } catch (Exception ignored) {
            }
        }
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round((float) dp * density);
    }
}
