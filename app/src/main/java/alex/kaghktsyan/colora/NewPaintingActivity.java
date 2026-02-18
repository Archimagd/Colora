package alex.kaghktsyan.colora;

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

public class NewPaintingActivity extends AppCompatActivity {

    private DrawingView drawingView;
    private Button btnUndo, btnRedo, btnClear, btnEraser;
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