package alex.kaghktsyan.colora;

import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.io.File;
import java.util.List;

public class NewPaintingActivity extends AppCompatActivity {

    private DrawingView drawingView;
    private Button btnUndo, btnRedo, btnClear, btnEraser, btnColor, btnSave, btnLayers;
    private SeekBar seekBarStroke;
    private TextView txtStrokeWidth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_new_painting);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        drawingView = findViewById(R.id.drawingView);
        btnUndo = findViewById(R.id.btnUndo);
        btnRedo = findViewById(R.id.btnRedo);
        btnClear = findViewById(R.id.btnClear);
        btnEraser = findViewById(R.id.btnEraser);
        btnSave = findViewById(R.id.btnSave);
        btnLayers = findViewById(R.id.btnLayers);
        seekBarStroke = findViewById(R.id.seekBarStroke);
        txtStrokeWidth = findViewById(R.id.txtStrokeWidth);
        btnColor = findViewById(R.id.btnColor);

        String imagePath = getIntent().getStringExtra("image_path");
        if (imagePath != null) {
            File imgFile = new File(imagePath);
            if (imgFile.exists()) {
                Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                drawingView.loadBitmap(myBitmap);
            }
        }

        btnUndo.setOnClickListener(v -> drawingView.undo());
        btnRedo.setOnClickListener(v -> drawingView.redo());
        btnClear.setOnClickListener(v -> drawingView.clearCanvas());
        btnEraser.setOnClickListener(v -> {
            drawingView.toggleEraser();
            updateEraserButton();
        });

        btnSave.setOnClickListener(v -> {
            Bitmap bitmap = drawingView.getBitmap();
            ImageUtils.saveBitmapToGallery(this, bitmap, "Drawing_" + System.currentTimeMillis());
        });

        btnLayers.setOnClickListener(v -> showLayersBottomSheet());

        btnColor.setOnClickListener(v -> showColorPickerBottomSheet());

        seekBarStroke.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int strokeWidth = Math.max(1, progress);
                drawingView.setStrokeWidth(strokeWidth);
                txtStrokeWidth.setText(String.valueOf(strokeWidth));
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void showLayersBottomSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(android.R.layout.list_content, null);
        
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(20, 20, 20, 20);

        TextView title = new TextView(this);
        title.setText("Layers");
        title.setTextSize(20);
        title.setPadding(20, 20, 20, 20);
        layout.addView(title);

        ListView listView = new ListView(this);
        layout.addView(listView);

        Button btnAddLayer = new Button(this);
        btnAddLayer.setText("+ Add Layer");
        btnAddLayer.setOnClickListener(v -> {
            drawingView.addLayer("Layer " + (drawingView.getLayers().size() + 1));
            dialog.dismiss();
            showLayersBottomSheet(); // Refresh
        });
        layout.addView(btnAddLayer);

        LayerAdapter adapter = new LayerAdapter(drawingView.getLayers());
        listView.setAdapter(adapter);

        dialog.setContentView(layout);
        dialog.show();
    }

    private class LayerAdapter extends ArrayAdapter<DrawingView.Layer> {
        public LayerAdapter(List<DrawingView.Layer> layers) {
            super(NewPaintingActivity.this, R.layout.item_layer, layers);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.item_layer, parent, false);
            }

            DrawingView.Layer layer = getItem(position);
            CheckBox checkVisibility = convertView.findViewById(R.id.checkVisibility);
            ImageView imgPreview = convertView.findViewById(R.id.imgLayerPreview);
            TextView txtName = convertView.findViewById(R.id.txtLayerName);
            ImageButton btnDelete = convertView.findViewById(R.id.btnDeleteLayer);

            txtName.setText(layer.name);
            checkVisibility.setChecked(layer.isVisible);
            imgPreview.setImageBitmap(layer.bitmap);

            if (position == drawingView.getCurrentLayerIndex()) {
                convertView.setBackgroundColor(Color.LTGRAY);
            } else {
                convertView.setBackgroundColor(Color.TRANSPARENT);
            }

            convertView.setOnClickListener(v -> {
                drawingView.setCurrentLayerIndex(position);
                notifyDataSetChanged();
                drawingView.invalidate();
            });

            checkVisibility.setOnCheckedChangeListener((buttonView, isChecked) -> {
                layer.isVisible = isChecked;
                drawingView.invalidate();
            });

            btnDelete.setOnClickListener(v -> {
                drawingView.removeLayer(position);
                notifyDataSetChanged();
            });

            return convertView;
        }
    }

    private void showColorPickerBottomSheet() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(40, 40, 40, 80);

        TextView title = new TextView(this);
        title.setText("Select Color");
        title.setTextSize(20);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setPadding(0, 0, 0, 30);
        layout.addView(title);

        ColorPickerView colorPickerView = new ColorPickerView(this);
        colorPickerView.setColor(drawingView.getColor());
        colorPickerView.setOnColorChangedListener(color -> {
            updateColorButton(color);
            drawingView.setColor(color);
        });
        layout.addView(colorPickerView);

        Button btnSelect = new Button(this);
        btnSelect.setText("Select");
        android.widget.LinearLayout.LayoutParams params = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
        params.topMargin = 40;
        btnSelect.setLayoutParams(params);
        btnSelect.setOnClickListener(v -> {
            int selectedColor = colorPickerView.getColor();
            drawingView.setColor(selectedColor);
            updateColorButton(selectedColor);
            bottomSheetDialog.dismiss();
        });
        layout.addView(btnSelect);

        bottomSheetDialog.setContentView(layout);
        bottomSheetDialog.show();
    }

    private void updateColorButton(int color) {
        btnColor.setBackgroundTintList(ColorStateList.valueOf(color));
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        btnColor.setTextColor(hsv[2] > 0.7 ? Color.BLACK : Color.WHITE);
    }

    private void updateEraserButton() {
        btnEraser.setText(drawingView.isEraserMode() ? "Eraser on" : "Eraser off");
    }
}
