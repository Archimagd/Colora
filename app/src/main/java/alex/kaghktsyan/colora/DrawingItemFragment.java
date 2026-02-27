package alex.kaghktsyan.colora;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.io.File;

public class DrawingItemFragment extends Fragment {

    private static final String ARG_IMAGE_PATH = "image_path";
    private String imagePath;

    public static DrawingItemFragment newInstance(String imagePath) {
        DrawingItemFragment fragment = new DrawingItemFragment();
        Bundle args = new Bundle();
        args.putString(ARG_IMAGE_PATH, imagePath);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            imagePath = getArguments().getString(ARG_IMAGE_PATH);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_drawing_item, container, false);
        ImageView imageView = view.findViewById(R.id.imgDrawingPreview);
        TextView txtTitle = view.findViewById(R.id.txtDrawingTitle);
        
        if (imagePath != null) {
            File imgFile = new File(imagePath);
            if (imgFile.exists()) {
                // Optimized image loading
                Bitmap thumbBitmap = decodeSampledBitmapFromFile(imgFile.getAbsolutePath(), 300, 300);
                imageView.setImageBitmap(thumbBitmap);
                
                txtTitle.setText(getFileNameWithoutExtension(imgFile));

                view.setOnClickListener(v -> {
                    Intent intent = new Intent(getActivity(), NewPaintingActivity.class);
                    intent.putExtra("image_path", imagePath);
                    startActivity(intent);
                });
            }
        }
        
        return view;
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
