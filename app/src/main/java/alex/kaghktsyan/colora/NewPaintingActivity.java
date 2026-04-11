package alex.kaghktsyan.colora;

import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class NewPaintingActivity extends AppCompatActivity {

    private DrawingView drawingView;
    private ImageButton btnUndo, btnRedo, btnSave, btnLayers, btnBack, btnHand, btnZoom, btnReference, btnTimelapse;
    private ImageButton toolBrush, toolEraser, toolFill, toolPicker, toolShapes, toolText, toolSelect;
    private View shapesPanel, referencePanel;
    private ImageView imgReference;
    private ImageButton btnCloseReference;
    private ImageButton shapeLine, shapeRect, shapeCircle, shapeTriangle;
    private SeekBar sbSize, sbOpacity, sbHardness;
    private TextView tvSizeValue, tvOpacityValue, tvHardnessValue;
    private View currentColorPreview;
    private RecyclerView rvPalette;
    private ImageButton btnColorPicker;
    private ColorSwatchAdapter paletteAdapter;

    private float dX, dY; // For reference window dragging

    private final ActivityResultLauncher<String> pickReferenceLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    loadReferenceImage(uri);
                }
            }
    );

    private List<Integer> paletteColors = new ArrayList<>(Arrays.asList(
            Color.BLACK, Color.DKGRAY, Color.GRAY, Color.LTGRAY, Color.WHITE,
            Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW, Color.CYAN, Color.MAGENTA,
            Color.parseColor("#FF5722"), Color.parseColor("#4CAF50"), Color.parseColor("#2196F3"),
            Color.parseColor("#9C27B0"), Color.parseColor("#3F51B5"), Color.parseColor("#009688")
    ));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_new_painting);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            getWindow().setNavigationBarContrastEnforced(false);
        }
        getWindow().setNavigationBarColor(Color.TRANSPARENT);
        getWindow().setStatusBarColor(Color.TRANSPARENT);

        initViews();
        setupListeners();
        setupPalette();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });

        // Load image if passed
        String imagePath = getIntent().getStringExtra("image_path");
        if (imagePath != null) {
            File imgFile = new File(imagePath);
            if (imgFile.exists()) {
                Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                drawingView.loadBitmap(myBitmap);
            }
        }

        // Initial UI states
        selectTool(toolBrush);
        
        // Initial values for SeekBars
        sbSize.setProgress((int) drawingView.getStrokeWidth());
        tvSizeValue.setText(String.valueOf(sbSize.getProgress()));
        
        sbOpacity.setMax(255);
        sbOpacity.setProgress(drawingView.getBrushAlpha());
        tvOpacityValue.setText(String.valueOf(sbOpacity.getProgress()));

        sbHardness.setMax(100);
        sbHardness.setProgress(drawingView.getHardness());
        tvHardnessValue.setText(String.valueOf(sbHardness.getProgress()));
    }

    private void initViews() {
        drawingView = findViewById(R.id.drawingView);
        btnBack = findViewById(R.id.btnBack);
        btnUndo = findViewById(R.id.btnUndo);
        btnRedo = findViewById(R.id.btnRedo);
        btnLayers = findViewById(R.id.btnLayers);
        btnSave = findViewById(R.id.btnSave);
        btnHand = findViewById(R.id.btnHand);
        btnZoom = findViewById(R.id.btnZoom);
        btnReference = findViewById(R.id.btnReference);
        btnTimelapse = findViewById(R.id.btnTimelapse);

        toolBrush = findViewById(R.id.toolBrush);
        toolEraser = findViewById(R.id.toolEraser);
        toolFill = findViewById(R.id.toolFill);
        toolPicker = findViewById(R.id.toolPicker);
        toolShapes = findViewById(R.id.toolShapes);
        toolText = findViewById(R.id.toolText);
        toolSelect = findViewById(R.id.toolSelect);

        shapesPanel = findViewById(R.id.shapesPanel);
        shapeLine = findViewById(R.id.shapeLine);
        shapeRect = findViewById(R.id.shapeRect);
        shapeCircle = findViewById(R.id.shapeCircle);
        shapeTriangle = findViewById(R.id.shapeTriangle);

        sbSize = findViewById(R.id.sbSize);
        sbOpacity = findViewById(R.id.sbOpacity);
        sbHardness = findViewById(R.id.sbHardness);
        tvSizeValue = findViewById(R.id.tvSizeValue);
        tvOpacityValue = findViewById(R.id.tvOpacityValue);
        tvHardnessValue = findViewById(R.id.tvHardnessValue);

        currentColorPreview = findViewById(R.id.currentColorPreview);
        rvPalette = findViewById(R.id.rvPalette);
        btnColorPicker = findViewById(R.id.btnColorPicker);
        
        referencePanel = findViewById(R.id.referencePanel);
        imgReference = findViewById(R.id.imgReference);
        btnCloseReference = findViewById(R.id.btnCloseReference);
        
        drawingView.setBackgroundColor(Color.WHITE);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());
        btnUndo.setOnClickListener(v -> drawingView.undo());
        btnRedo.setOnClickListener(v -> drawingView.redo());
        btnSave.setOnClickListener(v -> showSaveDialog());
        btnLayers.setOnClickListener(v -> showLayersBottomSheet());

        btnHand.setOnClickListener(v -> {
            if (drawingView.isTransformMode()) {
                selectTool(toolBrush);
                drawingView.setEraserMode(false);
            } else {
                resetTopButtons();
                resetTools();
                drawingView.setTransformMode(true);
                btnHand.setImageTintList(ColorStateList.valueOf(getColor(R.color.purple_main)));
            }
        });

        btnZoom.setOnClickListener(v -> {
            drawingView.resetTransform();
        });

        btnReference.setOnClickListener(v -> {
            if (referencePanel.getVisibility() == View.GONE) {
                pickReferenceLauncher.launch("image/*");
            } else {
                referencePanel.setVisibility(View.GONE);
            }
        });

        btnTimelapse.setOnClickListener(v -> {
            if (drawingView.isRecordingTimelapse()) {
                drawingView.stopTimelapse();
                btnTimelapse.setImageTintList(ColorStateList.valueOf(getThemeColor(R.attr.mainTextColor)));
                showTimelapseOptionsDialog();
            } else {
                drawingView.startTimelapse();
                btnTimelapse.setImageTintList(ColorStateList.valueOf(Color.RED));
            }
        });

        btnCloseReference.setOnClickListener(v -> referencePanel.setVisibility(View.GONE));

        // Make reference window draggable
        referencePanel.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        dX = view.getX() - event.getRawX();
                        dY = view.getY() - event.getRawY();
                        break;
                    case MotionEvent.ACTION_MOVE:
                        view.animate()
                                .x(event.getRawX() + dX)
                                .y(event.getRawY() + dY)
                                .setDuration(0)
                                .start();
                        break;
                    default:
                        return false;
                }
                return true;
            }
        });

        // Tool listeners
        toolBrush.setOnClickListener(v -> {
            selectTool(toolBrush);
            drawingView.setEraserMode(false);
        });
        toolEraser.setOnClickListener(v -> {
            selectTool(toolEraser);
            drawingView.setEraserMode(true);
        });
        toolFill.setOnClickListener(v -> {
            selectTool(toolFill);
            drawingView.setFillMode(true);
        });
        toolPicker.setOnClickListener(v -> {
            selectTool(toolPicker);
            drawingView.setPickerMode(true);
        });
        toolShapes.setOnClickListener(v -> {
            if (shapesPanel.getVisibility() == View.VISIBLE) {
                shapesPanel.setVisibility(View.GONE);
            } else {
                selectTool(toolShapes);
                shapesPanel.setVisibility(View.VISIBLE);
            }
        });
        toolText.setOnClickListener(v -> {
            selectTool(toolText);
            drawingView.setTextMode(true);
        });
        toolSelect.setOnClickListener(v -> {
            selectTool(toolSelect);
            drawingView.setSelectMode(true);
        });

        // Shape buttons listeners
        shapeLine.setOnClickListener(v -> selectShape(DrawingView.ShapeType.LINE, shapeLine));
        shapeRect.setOnClickListener(v -> selectShape(DrawingView.ShapeType.RECT, shapeRect));
        shapeCircle.setOnClickListener(v -> selectShape(DrawingView.ShapeType.CIRCLE, shapeCircle));
        shapeTriangle.setOnClickListener(v -> selectShape(DrawingView.ShapeType.TRIANGLE, shapeTriangle));

        // SeekBar listeners
        sbSize.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int size = Math.max(1, progress);
                drawingView.setStrokeWidth(size);
                tvSizeValue.setText(String.valueOf(size));
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        sbOpacity.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                drawingView.setBrushAlpha(progress);
                tvOpacityValue.setText(String.valueOf(progress));
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        sbHardness.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                drawingView.setHardness(progress);
                tvHardnessValue.setText(String.valueOf(progress));
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        btnColorPicker.setOnClickListener(v -> showColorPickerBottomSheet());

        drawingView.setOnColorPickedListener(color -> {
            updateCurrentColorUI(color);
        });

        drawingView.setOnTextRequestListener((x, y) -> showTextInputDialog(x, y));
    }

    private void showTimelapseOptionsDialog() {
        if (drawingView.getFrameCount() < 2) {
            Toast.makeText(this, "Слишком короткий процесс для видео", Toast.LENGTH_SHORT).show();
            return;
        }
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Таймлапс готов");
        builder.setMessage("Вы хотите сохранить процесс создания арта (" + drawingView.getFrameCount() + " кадров)?");
        builder.setPositiveButton("Экспорт", (dialog, which) -> startTimelapseExport());
        builder.setNegativeButton("Отмена", null);
        builder.show();
    }

    private void startTimelapseExport() {
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Экспорт видео");
        progressDialog.setMessage("Пожалуйста, подождите...");
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setCancelable(false);
        progressDialog.setMax(100);
        progressDialog.show();

        TimelapseExporter.export(this, drawingView.getTimelapseDir(), new TimelapseExporter.ExportListener() {
            @Override
            public void onProgress(int progress) {
                progressDialog.setProgress(progress);
            }

            @Override
            public void onComplete(Uri uri) {
                progressDialog.dismiss();
                Toast.makeText(NewPaintingActivity.this, "Видео сохранено в галерею!", Toast.LENGTH_LONG).show();
                
                // Предложить поделиться видео
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("video/mp4");
                shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
                startActivity(Intent.createChooser(shareIntent, "Поделиться таймлапсом"));
            }

            @Override
            public void onError(String error) {
                progressDialog.dismiss();
                Toast.makeText(NewPaintingActivity.this, "Ошибка: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showSaveDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Сохранить рисунок");

        final EditText input = new EditText(this);
        input.setHint("Введите название");
        builder.setView(input);

        builder.setPositiveButton("Сохранить", (dialog, which) -> {
            String fileName = input.getText().toString().trim();
            if (fileName.isEmpty()) {
                fileName = "Colora_" + System.currentTimeMillis();
            }
            saveImageToGallery(fileName);
        });
        builder.setNegativeButton("Отмена", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void saveImageToGallery(String fileName) {
        Bitmap bitmap = drawingView.getBitmap();
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName + ".png");
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Colora");
            values.put(MediaStore.Images.Media.IS_PENDING, 1);
        }

        Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        if (uri != null) {
            try (OutputStream out = getContentResolver().openOutputStream(uri)) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    values.clear();
                    values.put(MediaStore.Images.Media.IS_PENDING, 0);
                    getContentResolver().update(uri, values, null, null);
                }
                
                Toast.makeText(this, "Рисунок '" + fileName + "' сохранен!", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void loadReferenceImage(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            imgReference.setImageBitmap(bitmap);
            referencePanel.setVisibility(View.VISIBLE);
        } catch (Exception e) {
            Toast.makeText(this, "Ошибка загрузки изображения", Toast.LENGTH_SHORT).show();
        }
    }

    private void selectShape(DrawingView.ShapeType type, ImageButton button) {
        drawingView.setShapeType(type);
        resetShapeButtons();
        button.setSelected(true);
        button.setImageTintList(ColorStateList.valueOf(getColor(R.color.purple_main)));
    }

    private void resetShapeButtons() {
        int iconColor = getThemeColor(R.attr.mainTextColor);
        ImageButton[] shapeButtons = {shapeLine, shapeRect, shapeCircle, shapeTriangle};
        for (ImageButton b : shapeButtons) {
            b.setSelected(false);
            b.setImageTintList(ColorStateList.valueOf(iconColor));
        }
    }

    private void updateCurrentColorUI(int color) {
        drawingView.setColor(color);
        currentColorPreview.setBackgroundTintList(ColorStateList.valueOf(color));
        int alpha = Color.alpha(color);
        sbOpacity.setProgress(alpha);
        tvOpacityValue.setText(String.valueOf(alpha));
        if (paletteAdapter != null) {
            paletteAdapter.setSelectedColor(color);
        }
    }

    private void showTextInputDialog(float x, float y) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Введите текст");

        final EditText input = new EditText(this);
        builder.setView(input);

        builder.setPositiveButton("OK", (dialog, which) -> {
            String text = input.getText().toString();
            if (!text.isEmpty()) {
                drawingView.drawText(text, x, y);
            }
        });
        builder.setNegativeButton("Отмена", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void resetTopButtons() {
        int iconColor = getThemeColor(R.attr.mainTextColor);
        btnHand.setImageTintList(ColorStateList.valueOf(iconColor));
        btnZoom.setImageTintList(ColorStateList.valueOf(iconColor));
        btnTimelapse.setImageTintList(ColorStateList.valueOf(drawingView.isRecordingTimelapse() ? Color.RED : iconColor));
    }

    private void resetTools() {
        int iconColor = getThemeColor(R.attr.mainTextColor);
        ImageButton[] tools = {toolBrush, toolEraser, toolFill, toolPicker, toolShapes, toolText, toolSelect};
        for (ImageButton t : tools) {
            t.setSelected(false);
            t.setImageTintList(ColorStateList.valueOf(iconColor));
        }
        shapesPanel.setVisibility(View.GONE);
        resetShapeButtons();
    }

    private void selectTool(ImageButton selected) {
        resetTopButtons();
        resetTools();
        selected.setSelected(true);
        selected.setImageTintList(ColorStateList.valueOf(getColor(R.color.purple_main)));
    }

    private void setupPalette() {
        rvPalette.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        paletteAdapter = new ColorSwatchAdapter(paletteColors, color -> {
            updateCurrentColorUI(color);
        });
        rvPalette.setAdapter(paletteAdapter);
        paletteAdapter.setSelectedColor(drawingView.getColor());
    }

    private int getThemeColor(int attr) {
        TypedValue typedValue = new TypedValue();
        getTheme().resolveAttribute(attr, typedValue, true);
        return typedValue.data;
    }

    private void showLayersBottomSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        try {
            View view = getLayoutInflater().inflate(R.layout.bottom_sheet_layers, null);
            ListView listView = view.findViewById(R.id.listLayers);
            Button btnAddLayer = view.findViewById(R.id.btnAddLayer);

            LayerAdapter adapter = new LayerAdapter(drawingView.getLayers());
            listView.setAdapter(adapter);

            btnAddLayer.setOnClickListener(v -> {
                drawingView.addLayer("Слой " + (drawingView.getLayers().size() + 1));
                adapter.notifyDataSetChanged();
            });

            dialog.setContentView(view);
            dialog.show();
        } catch (Exception e) {
            Toast.makeText(this, "Ошибка слоев: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void showColorPickerBottomSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.bottom_sheet_color_picker, null);
        
        ColorPickerView colorPickerView = view.findViewById(R.id.colorPickerView);
        Button btnSelectColor = view.findViewById(R.id.btnSelectColor);
        
        colorPickerView.setColor(drawingView.getColor());
        
        btnSelectColor.setOnClickListener(v -> {
            int selectedColor = colorPickerView.getColor();
            updateCurrentColorUI(selectedColor);
            
            if (!paletteColors.contains(selectedColor)) {
                paletteColors.add(0, selectedColor);
                paletteAdapter.notifyItemInserted(0);
                rvPalette.scrollToPosition(0);
                paletteAdapter.setSelectedColor(selectedColor);
            }
            
            dialog.dismiss();
        });
        
        dialog.setContentView(view);
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
                convertView.setBackgroundTintList(ColorStateList.valueOf(getThemeColor(R.attr.cardBackgroundColor)));
                convertView.setAlpha(0.7f);
            } else {
                convertView.setBackgroundTintList(null);
                convertView.setAlpha(1.0f);
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
}
