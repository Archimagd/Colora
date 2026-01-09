package alex.kaghktsyan.colora;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class NewPaintingActivity extends AppCompatActivity {

    private DrawingView drawingView;
    private Button btnUndo, btnRedo, btnClear;

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
    }
}