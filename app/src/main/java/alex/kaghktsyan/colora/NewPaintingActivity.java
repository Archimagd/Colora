package alex.kaghktsyan.colora;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Base64;
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
import android.widget.ProgressBar;
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
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class NewPaintingActivity extends AppCompatActivity {

    private DrawingView drawingView;
    private ImageButton btnUndo, btnRedo, btnSave, btnLayers, btnBack, btnHand, btnZoom, btnReference, btnTimelapse;
    private ImageButton toolBrush, toolEraser, toolFill, toolPicker, toolShapes, toolText, toolSelect, toolSymmetry;
    private View shapesPanel, symmetryPanel, referencePanel;
    private ImageView imgReference;
    private ImageButton btnCloseReference;
    private ImageButton shapeLine, shapeRect, shapeCircle, shapeTriangle;
    private ImageButton symNone, symVertical, symHorizontal, symRadial;
    private SeekBar sbSize, sbOpacity, sbHardness;
    private TextView tvSizeValue, tvOpacityValue, tvHardnessValue;
    private View currentColorPreview;
    private RecyclerView rvPalette;
    private ColorSwatchAdapter paletteAdapter;

    private float dX, dY;

    private final ActivityResultLauncher<String> pickReferenceLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    loadReferenceImage(uri);
                }
            }
    );

    private final List<Integer> paletteColors = new ArrayList<>(Arrays.asList(
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

        // БЕЗОПАСНАЯ ЗАГРУЗКА: Используем статическое поле вместо Intent
        String cloudImageData = DrawingItemFragment.pendingImageData;
        DrawingItemFragment.pendingImageData = null; // Сразу очищаем для экономии памяти

        if (cloudImageData != null) {
            try {
                byte[] decodedString = Base64.decode(cloudImageData, Base64.DEFAULT);
                Bitmap cloudBitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                if (cloudBitmap != null) {
                    drawingView.loadBitmap(cloudBitmap);
                }
            } catch (Exception e) {
                Toast.makeText(this, "Ошибка загрузки рисунка", Toast.LENGTH_SHORT).show();
            }
        }

        selectTool(toolBrush);
        
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
        toolSymmetry = findViewById(R.id.toolSymmetry);

        shapesPanel = findViewById(R.id.shapesPanel);
        shapeLine = findViewById(R.id.shapeLine);
        shapeRect = findViewById(R.id.shapeRect);
        shapeCircle = findViewById(R.id.shapeCircle);
        shapeTriangle = findViewById(R.id.shapeTriangle);

        symmetryPanel = findViewById(R.id.symmetryPanel);
        symNone = findViewById(R.id.symNone);
        symVertical = findViewById(R.id.symVertical);
        symHorizontal = findViewById(R.id.symHorizontal);
        symRadial = findViewById(R.id.symRadial);

        sbSize = findViewById(R.id.sbSize);
        sbOpacity = findViewById(R.id.sbOpacity);
        sbHardness = findViewById(R.id.sbHardness);
        tvSizeValue = findViewById(R.id.tvSizeValue);
        tvOpacityValue = findViewById(R.id.tvOpacityValue);
        tvHardnessValue = findViewById(R.id.tvHardnessValue);

        currentColorPreview = findViewById(R.id.currentColorPreview);
        rvPalette = findViewById(R.id.rvPalette);
        
        referencePanel = findViewById(R.id.referencePanel);
        imgReference = findViewById(R.id.imgReference);
        btnCloseReference = findViewById(R.id.btnCloseReference);
        
        drawingView.setBackgroundColor(Color.WHITE);
        findViewById(R.id.btnColorPicker).setOnClickListener(v -> showColorPickerBottomSheet());
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

        btnZoom.setOnClickListener(v -> drawingView.resetTransform());

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

        referencePanel.setOnTouchListener((view, event) -> {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    dX = view.getX() - event.getRawX();
                    dY = view.getY() - event.getRawY();
                    break;
                case MotionEvent.ACTION_MOVE:
                    view.setX(event.getRawX() + dX);
                    view.setY(event.getRawY() + dY);
                    break;
                default: return false;
            }
            return true;
        });

        toolBrush.setOnClickListener(v -> { selectTool(toolBrush); drawingView.setEraserMode(false); });
        toolEraser.setOnClickListener(v -> { selectTool(toolEraser); drawingView.setEraserMode(true); });
        toolFill.setOnClickListener(v -> { selectTool(toolFill); drawingView.setFillMode(true); });
        toolPicker.setOnClickListener(v -> { selectTool(toolPicker); drawingView.setPickerMode(true); });
        toolShapes.setOnClickListener(v -> {
            if (shapesPanel.getVisibility() == View.VISIBLE) shapesPanel.setVisibility(View.GONE);
            else { selectTool(toolShapes); shapesPanel.setVisibility(View.VISIBLE); }
        });
        toolText.setOnClickListener(v -> { selectTool(toolText); drawingView.setTextMode(true); });
        toolSelect.setOnClickListener(v -> { selectTool(toolSelect); drawingView.setSelectMode(true); });
        toolSymmetry.setOnClickListener(v -> {
             if (symmetryPanel.getVisibility() == View.VISIBLE) symmetryPanel.setVisibility(View.GONE);
             else { selectTool(toolSymmetry); updateSymmetryUI(); symmetryPanel.setVisibility(View.VISIBLE); }
        });

        shapeLine.setOnClickListener(v -> selectShape(DrawingView.ShapeType.LINE, shapeLine));
        shapeRect.setOnClickListener(v -> selectShape(DrawingView.ShapeType.RECT, shapeRect));
        shapeCircle.setOnClickListener(v -> selectShape(DrawingView.ShapeType.CIRCLE, shapeCircle));
        shapeTriangle.setOnClickListener(v -> selectShape(DrawingView.ShapeType.TRIANGLE, shapeTriangle));

        symNone.setOnClickListener(v -> {
            drawingView.setSymmetryType(DrawingView.SymmetryType.NONE);
            symmetryPanel.setVisibility(View.GONE);
            selectTool(toolBrush);
        });
        symVertical.setOnClickListener(v -> selectSymmetry(DrawingView.SymmetryType.VERTICAL, symVertical));
        symHorizontal.setOnClickListener(v -> selectSymmetry(DrawingView.SymmetryType.HORIZONTAL, symHorizontal));
        symRadial.setOnClickListener(v -> selectSymmetry(DrawingView.SymmetryType.RADIAL, symRadial));

        sbSize.setOnSeekBarChangeListener(new SimpleSeekBarListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                drawingView.setStrokeWidth(Math.max(1, progress));
                tvSizeValue.setText(String.valueOf(Math.max(1, progress)));
            }
        });

        sbOpacity.setOnSeekBarChangeListener(new SimpleSeekBarListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                drawingView.setBrushAlpha(progress);
                tvOpacityValue.setText(String.valueOf(progress));
            }
        });

        sbHardness.setOnSeekBarChangeListener(new SimpleSeekBarListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                drawingView.setHardness(progress);
                tvHardnessValue.setText(String.valueOf(progress));
            }
        });

        drawingView.setOnColorPickedListener(this::updateCurrentColorUI);
        drawingView.setOnTextRequestListener(this::showTextInputDialog);
    }

    private void showTimelapseOptionsDialog() {
        if (drawingView.getFrameCount() < 2) return;
        new MaterialAlertDialogBuilder(this)
                .setTitle("Таймлапс готов")
                .setMessage("Сохранить процесс (" + drawingView.getFrameCount() + " кадров)?")
                .setPositiveButton("Экспорт", (dialog, which) -> startTimelapseExport())
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void startTimelapseExport() {
        ProgressBar progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(100);
        progressBar.setPadding(40, 40, 40, 40);

        AlertDialog progressDialog = new MaterialAlertDialogBuilder(this)
                .setTitle("Экспорт видео")
                .setMessage("Пожалуйста, подождите...")
                .setView(progressBar)
                .setCancelable(false)
                .create();
        progressDialog.show();

        TimelapseExporter.export(this, drawingView.getTimelapseDir(), new TimelapseExporter.ExportListener() {
            @Override public void onProgress(int progress) { progressBar.setProgress(progress); }
            @Override public void onComplete(Uri uri) {
                progressDialog.dismiss();
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("video/mp4");
                shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
                startActivity(Intent.createChooser(shareIntent, "Поделиться"));
            }
            @Override public void onError(String error) { progressDialog.dismiss(); }
        });
    }

    private void showSaveDialog() {
        final EditText input = new EditText(this);
        input.setHint("Название рисунка");
        int padding = dpToPx(20);
        input.setPadding(padding, padding, padding, padding);

        new MaterialAlertDialogBuilder(this)
                .setTitle("Сохранить")
                .setView(input)
                .setPositiveButton("ОК", (dialog, which) -> {
                    String name = input.getText().toString().trim();
                    drawingView.saveToFirebase(name.isEmpty() ? "Colora_" + System.currentTimeMillis() : name);
                    Toast.makeText(this, "Сохранение...", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void loadReferenceImage(Uri uri) {
        try (InputStream inputStream = getContentResolver().openInputStream(uri)) {
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            imgReference.setImageBitmap(bitmap);
            referencePanel.setVisibility(View.VISIBLE);
        } catch (Exception ignored) {}
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
        for (ImageButton b : shapeButtons) b.setImageTintList(ColorStateList.valueOf(iconColor));
    }

    private void selectSymmetry(DrawingView.SymmetryType type, ImageButton button) {
        drawingView.setSymmetryType(type);
        resetSymmetryButtons();
        button.setSelected(true);
        button.setImageTintList(ColorStateList.valueOf(getColor(R.color.purple_main)));
    }

    private void resetSymmetryButtons() {
        int iconColor = getThemeColor(R.attr.mainTextColor);
        ImageButton[] symButtons = {symNone, symVertical, symHorizontal, symRadial};
        for (ImageButton b : symButtons) b.setImageTintList(ColorStateList.valueOf(iconColor));
    }

    private void updateSymmetryUI() {
        resetSymmetryButtons();
        ImageButton toSelect;
        switch (drawingView.getSymmetryType()) {
            case VERTICAL: toSelect = symVertical; break;
            case HORIZONTAL: toSelect = symHorizontal; break;
            case RADIAL: toSelect = symRadial; break;
            default: toSelect = symNone; break;
        }
        toSelect.setImageTintList(ColorStateList.valueOf(getColor(R.color.purple_main)));
    }

    private void updateCurrentColorUI(int color) {
        drawingView.setColor(color);
        currentColorPreview.setBackgroundTintList(ColorStateList.valueOf(color));
        sbOpacity.setProgress(Color.alpha(color));
        if (paletteAdapter != null) paletteAdapter.setSelectedColor(color);
    }

    private void showTextInputDialog(float x, float y) {
        final EditText input = new EditText(this);
        input.setPadding(40, 40, 40, 40);
        new MaterialAlertDialogBuilder(this).setTitle("Текст").setView(input)
                .setPositiveButton("OK", (d, w) -> drawingView.drawText(input.getText().toString(), x, y))
                .show();
    }

    private void resetTopButtons() {
        int iconColor = getThemeColor(R.attr.mainTextColor);
        btnHand.setImageTintList(ColorStateList.valueOf(iconColor));
        btnZoom.setImageTintList(ColorStateList.valueOf(iconColor));
    }

    private void resetTools() {
        int iconColor = getThemeColor(R.attr.mainTextColor);
        ImageButton[] tools = {toolBrush, toolEraser, toolFill, toolPicker, toolShapes, toolText, toolSelect, toolSymmetry};
        for (ImageButton t : tools) t.setImageTintList(ColorStateList.valueOf(iconColor));
        shapesPanel.setVisibility(View.GONE);
        symmetryPanel.setVisibility(View.GONE);
    }

    private void selectTool(ImageButton selected) {
        resetTopButtons();
        resetTools();
        selected.setImageTintList(ColorStateList.valueOf(getColor(R.color.purple_main)));
    }

    private void setupPalette() {
        rvPalette.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        paletteAdapter = new ColorSwatchAdapter(paletteColors, this::updateCurrentColorUI);
        rvPalette.setAdapter(paletteAdapter);
        paletteAdapter.setSelectedColor(drawingView.getColor());
    }

    private int getThemeColor(int attr) {
        TypedValue typedValue = new TypedValue();
        getTheme().resolveAttribute(attr, typedValue, true);
        return typedValue.data;
    }

    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }

    private void showLayersBottomSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.bottom_sheet_layers, null);
        ListView listView = view.findViewById(R.id.listLayers);
        LayerAdapter adapter = new LayerAdapter(drawingView.getLayers());
        listView.setAdapter(adapter);
        view.findViewById(R.id.btnAddLayer).setOnClickListener(v -> {
            drawingView.addLayer("Слой " + (drawingView.getLayers().size() + 1));
            adapter.notifyDataSetChanged();
        });
        dialog.setContentView(view);
        dialog.show();
    }

    private void showColorPickerBottomSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.bottom_sheet_color_picker, null);
        ColorPickerView cpv = view.findViewById(R.id.colorPickerView);
        cpv.setColor(drawingView.getColor());
        view.findViewById(R.id.btnSelectColor).setOnClickListener(v -> {
            updateCurrentColorUI(cpv.getColor());
            dialog.dismiss();
        });
        dialog.setContentView(view);
        dialog.show();
    }

    private abstract static class SimpleSeekBarListener implements SeekBar.OnSeekBarChangeListener {
        @Override public void onStartTrackingTouch(SeekBar seekBar) {}
        @Override public void onStopTrackingTouch(SeekBar seekBar) {}
    }

    private class LayerAdapter extends ArrayAdapter<DrawingView.Layer> {
        public LayerAdapter(List<DrawingView.Layer> layers) { super(NewPaintingActivity.this, R.layout.item_layer, layers); }
        @NonNull @Override public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) convertView = getLayoutInflater().inflate(R.layout.item_layer, parent, false);
            DrawingView.Layer layer = getItem(position);
            ((TextView)convertView.findViewById(R.id.txtLayerName)).setText(layer.name);
            ((CheckBox)convertView.findViewById(R.id.checkVisibility)).setChecked(layer.isVisible);
            ((ImageView)convertView.findViewById(R.id.imgLayerPreview)).setImageBitmap(layer.bitmap);
            convertView.setOnClickListener(v -> { drawingView.setCurrentLayerIndex(position); notifyDataSetChanged(); });
            return convertView;
        }
    }
}
