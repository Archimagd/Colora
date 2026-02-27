package alex.kaghktsyan.colora;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.android.material.bottomsheet.BottomSheetDialog;

public class NewPaintingActivity extends AppCompatActivity {

    private DrawingView drawingView;
    private Button btnUndo, btnRedo, btnClear, btnEraser, btnColor;
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
        seekBarStroke = findViewById(R.id.seekBarStroke);
        txtStrokeWidth = findViewById(R.id.txtStrokeWidth);
        btnColor = findViewById(R.id.btnColor);

        btnUndo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawingView.undo();
            }
        });

        btnRedo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawingView.redo();
            }
        });

        btnClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawingView.clearCanvas();
            }
        });

        btnEraser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawingView.toggleEraser();
                updateEraserButton();
            }
        });

        btnColor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showColorPickerBottomSheet();
            }
        });

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
        
        // real time color update
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
        if (hsv[2] > 0.7) {
            btnColor.setTextColor(Color.BLACK);
        } else {
            btnColor.setTextColor(Color.WHITE);
        }
    }

    private void updateEraserButton() {
        if (drawingView.isEraserMode()) {
            btnEraser.setText("Eraser on");
            Toast.makeText(this, "Eraser enabled", Toast.LENGTH_SHORT).show();
        } else {
            btnEraser.setText("Eraser off");
            Toast.makeText(this, "Drawing mode enabled", Toast.LENGTH_SHORT).show();
        }
    }
}
