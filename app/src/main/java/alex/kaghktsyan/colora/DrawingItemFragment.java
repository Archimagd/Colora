package alex.kaghktsyan.colora;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DrawingItemFragment extends Fragment {

    private static final String ARG_IMAGE_PATH = "image_path";
    private static final String ARG_IS_SMALL = "is_small";
    private String imagePath;
    private boolean isSmall;
    private FavoritesManager favoritesManager;

    public static DrawingItemFragment newInstance(String imagePath) {
        return newInstance(imagePath, false);
    }

    public static DrawingItemFragment newInstance(String imagePath, boolean isSmall) {
        DrawingItemFragment fragment = new DrawingItemFragment();
        Bundle args = new Bundle();
        args.putString(ARG_IMAGE_PATH, imagePath);
        args.putBoolean(ARG_IS_SMALL, isSmall);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            imagePath = getArguments().getString(ARG_IMAGE_PATH);
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
        View btnShare = view.findViewById(R.id.btnShare);

        // Настройка размеров для "маленького" режима на главной
        if (isSmall) {
            ViewGroup.LayoutParams rootParams = root.getLayoutParams();
            if (rootParams != null) {
                rootParams.width = dpToPx(180); // Ширина маленькой карточки
                root.setLayoutParams(rootParams);
            }

            ViewGroup.LayoutParams cardParams = cardImage.getLayoutParams();
            if (cardParams != null) {
                cardParams.height = dpToPx(120); // Высота картинки на главной
                cardImage.setLayoutParams(cardParams);
            }
            
            txtTitle.setTextSize(12);
            txtDate.setTextSize(10);
            
            // Скрываем кнопки лайка и поделиться на главной
            if (btnLike != null) btnLike.setVisibility(View.GONE);
            if (btnShare != null) btnShare.setVisibility(View.GONE);
            
            // Уменьшаем отступы
            root.setPadding(0, 0, dpToPx(12), 0);
        }

        if (imagePath != null) {
            File imgFile = new File(imagePath);
            if (imgFile.exists()) {
                Bitmap thumbBitmap = decodeSampledBitmapFromFile(imgFile.getAbsolutePath(), isSmall ? 300 : 600, isSmall ? 200 : 400);
                imageView.setImageBitmap(thumbBitmap);
                
                txtTitle.setText(getFileNameWithoutExtension(imgFile));
                
                long lastModified = imgFile.lastModified();
                SimpleDateFormat sdf = new SimpleDateFormat("d MMMM", new Locale("ru"));
                txtDate.setText(sdf.format(new Date(lastModified)));

                if (btnLike != null && !isSmall) {
                    updateLikeIcon(btnLike);
                    btnLike.setOnClickListener(v -> {
                        favoritesManager.toggleFavorite(imagePath);
                        updateLikeIcon(btnLike);
                        // If we are in GalleryActivity, we might want to refresh if filter is active
                        if (getActivity() instanceof GalleryActivity) {
                            ((GalleryActivity) getActivity()).onFavoriteToggled();
                        }
                    });
                }

                if (btnShare != null && !isSmall) {
                    btnShare.setOnClickListener(v -> {
                        Uri uri = FileProvider.getUriForFile(requireContext(), 
                                requireContext().getPackageName() + ".fileprovider", imgFile);
                        Intent intent = new Intent(Intent.ACTION_SEND);
                        intent.setType("image/*");
                        intent.putExtra(Intent.EXTRA_STREAM, uri);
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        startActivity(Intent.createChooser(intent, "Поделиться рисунком"));
                    });
                }

                view.setOnClickListener(v -> {
                    Intent intent = new Intent(getActivity(), NewPaintingActivity.class);
                    intent.putExtra("image_path", imagePath);
                    startActivity(intent);
                });
            }
        }
        
        return view;
    }

    private void updateLikeIcon(ImageButton btnLike) {
        if (favoritesManager.isFavorite(imagePath)) {
            btnLike.setImageResource(android.R.drawable.btn_star_big_on);
            btnLike.setColorFilter(ContextCompat.getColor(requireContext(), R.color.purple_main));
        } else {
            btnLike.setImageResource(android.R.drawable.btn_star_big_off);
            btnLike.setColorFilter(ContextCompat.getColor(requireContext(), R.color.text_grey));
        }
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round((float) dp * density);
    }

    private String getFileNameWithoutExtension(File file) {
        String fileName = file.getName();
        int pos = fileName.lastIndexOf(".");
        return (pos > 0) ? fileName.substring(0, pos) : fileName;
    }

    private Bitmap decodeSampledBitmapFromFile(String path, int reqWidth, int reqHeight) {
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(path, options);
    }

    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;
        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }
}
