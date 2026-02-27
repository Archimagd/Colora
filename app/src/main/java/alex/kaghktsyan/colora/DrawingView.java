package alex.kaghktsyan.colora;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class DrawingView extends View {

    public static class Layer {
        public Bitmap bitmap;
        public Canvas canvas;
        public String name;
        public boolean isVisible = true;
        public float opacity = 1.0f;

        public Layer(int w, int h, String name) {
            this.bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            this.canvas = new Canvas(this.bitmap);
            this.name = name;
        }

        public void recycle() {
            if (bitmap != null && !bitmap.isRecycled()) {
                bitmap.recycle();
            }
        }
    }

    private Paint paint;
    private Path currentPath;
    private List<Layer> layers = new ArrayList<>();
    private int currentLayerIndex = -1;

    private float lastX, lastY;
    private boolean isEraserMode = false;
    private int currentColor = Color.BLACK;
    private float currentStrokeWidth = 10;
    private int backgroundColor = Color.WHITE;

    private List<Bitmap[]> undoStack = new ArrayList<>();
    private List<Bitmap[]> redoStack = new ArrayList<>();
    private static final int MAX_UNDO_STEPS = 10;

    public DrawingView(Context context) {
        super(context);
        init();
    }

    public DrawingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        currentPath = new Path();
        paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setStrokeWidth(10);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setAntiAlias(true);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w > 0 && h > 0) {
            if (layers.isEmpty()) {
                addLayer("Background Layer");
            } else {
                // Resize existing layers if needed (usually happens on rotation)
                for (Layer layer : layers) {
                    Bitmap newBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
                    Canvas newCanvas = new Canvas(newBitmap);
                    newCanvas.drawBitmap(layer.bitmap, 0, 0, null);
                    layer.recycle();
                    layer.bitmap = newBitmap;
                    layer.canvas = newCanvas;
                }
            }
        }
    }

    public void addLayer(String name) {
        if (getWidth() > 0 && getHeight() > 0) {
            saveStateToUndo();
            Layer layer = new Layer(getWidth(), getHeight(), name);
            layers.add(layer);
            currentLayerIndex = layers.size() - 1;
            invalidate();
        }
    }

    public void removeLayer(int index) {
        if (layers.size() > 1 && index >= 0 && index < layers.size()) {
            saveStateToUndo();
            layers.get(index).recycle();
            layers.remove(index);
            currentLayerIndex = Math.min(currentLayerIndex, layers.size() - 1);
            invalidate();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(backgroundColor);

        for (int i = 0; i < layers.size(); i++) {
            Layer layer = layers.get(i);
            if (layer.isVisible) {
                Paint layerPaint = new Paint();
                layerPaint.setAlpha((int) (layer.opacity * 255));
                canvas.drawBitmap(layer.bitmap, 0, 0, layerPaint);

                if (i == currentLayerIndex && !currentPath.isEmpty()) {
                    canvas.drawPath(currentPath, paint);
                }
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (currentLayerIndex == -1 || !layers.get(currentLayerIndex).isVisible) return false;

        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                saveStateToUndo();
                currentPath.moveTo(x, y);
                lastX = x;
                lastY = y;
                invalidate();
                return true;

            case MotionEvent.ACTION_MOVE:
                float midX = (x + lastX) / 2;
                float midY = (y + lastY) / 2;
                currentPath.quadTo(lastX, lastY, midX, midY);
                lastX = x;
                lastY = y;
                invalidate();
                break;

            case MotionEvent.ACTION_UP:
                Layer layer = layers.get(currentLayerIndex);
                layer.canvas.drawPath(currentPath, paint);
                currentPath.reset();
                invalidate();
                break;
        }
        return true;
    }

    private void saveStateToUndo() {
        redoStack.clear();
        Bitmap[] state = new Bitmap[layers.size()];
        for (int i = 0; i < layers.size(); i++) {
            state[i] = layers.get(i).bitmap.copy(Bitmap.Config.ARGB_8888, true);
        }
        undoStack.add(state);
        if (undoStack.size() > MAX_UNDO_STEPS) {
            Bitmap[] oldState = undoStack.remove(0);
            for (Bitmap b : oldState) b.recycle();
        }
    }

    public void undo() {
        if (!undoStack.isEmpty()) {
            Bitmap[] currentState = new Bitmap[layers.size()];
            for (int i = 0; i < layers.size(); i++) {
                currentState[i] = layers.get(i).bitmap.copy(Bitmap.Config.ARGB_8888, true);
            }
            redoStack.add(currentState);

            Bitmap[] previousState = undoStack.remove(undoStack.size() - 1);
            
            // Adjust layer count if necessary
            while (layers.size() > previousState.length) {
                layers.remove(layers.size() - 1).recycle();
            }
            while (layers.size() < previousState.length) {
                layers.add(new Layer(getWidth(), getHeight(), "Layer " + (layers.size() + 1)));
            }

            for (int i = 0; i < layers.size(); i++) {
                layers.get(i).recycle();
                layers.get(i).bitmap = previousState[i];
                layers.get(i).canvas = new Canvas(layers.get(i).bitmap);
            }
            currentLayerIndex = Math.min(currentLayerIndex, layers.size() - 1);
            invalidate();
        }
    }

    public void redo() {
        if (!redoStack.isEmpty()) {
            Bitmap[] currentState = new Bitmap[layers.size()];
            for (int i = 0; i < layers.size(); i++) {
                currentState[i] = layers.get(i).bitmap.copy(Bitmap.Config.ARGB_8888, true);
            }
            undoStack.add(currentState);

            Bitmap[] nextState = redoStack.remove(redoStack.size() - 1);

            while (layers.size() > nextState.length) {
                layers.remove(layers.size() - 1).recycle();
            }
            while (layers.size() < nextState.length) {
                layers.add(new Layer(getWidth(), getHeight(), "Layer " + (layers.size() + 1)));
            }

            for (int i = 0; i < layers.size(); i++) {
                layers.get(i).recycle();
                layers.get(i).bitmap = nextState[i];
                layers.get(i).canvas = new Canvas(layers.get(i).bitmap);
            }
            currentLayerIndex = Math.min(currentLayerIndex, layers.size() - 1);
            invalidate();
        }
    }

    public void clearCanvas() {
        saveStateToUndo();
        for (Layer layer : layers) {
            layer.bitmap.eraseColor(Color.TRANSPARENT);
        }
        invalidate();
    }

    public void setColor(int color) {
        currentColor = color;
        if (!isEraserMode) {
            paint.setColor(color);
            paint.setXfermode(null);
        }
    }

    public int getColor() {
        return currentColor;
    }

    public void setStrokeWidth(float width) {
        currentStrokeWidth = width;
        paint.setStrokeWidth(width);
    }

    public float getStrokeWidth() {
        return currentStrokeWidth;
    }

    public Bitmap getBitmap() {
        Bitmap result = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(result);
        canvas.drawColor(backgroundColor);
        for (Layer layer : layers) {
            if (layer.isVisible) {
                Paint p = new Paint();
                p.setAlpha((int) (layer.opacity * 255));
                canvas.drawBitmap(layer.bitmap, 0, 0, p);
            }
        }
        return result;
    }

    public void loadBitmap(Bitmap loadedBitmap) {
        // Clear existing layers and load this as the first layer
        for (Layer l : layers) l.recycle();
        layers.clear();
        
        Layer layer = new Layer(loadedBitmap.getWidth(), loadedBitmap.getHeight(), "Imported Layer");
        layer.canvas.drawBitmap(loadedBitmap, 0, 0, null);
        layers.add(layer);
        currentLayerIndex = 0;
        invalidate();
    }

    public void setEraserMode(boolean enabled) {
        isEraserMode = enabled;
        if (enabled) {
            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        } else {
            paint.setXfermode(null);
            paint.setColor(currentColor);
        }
    }

    public boolean isEraserMode() {
        return isEraserMode;
    }

    public void toggleEraser() {
        setEraserMode(!isEraserMode);
    }

    public void setBackgroundColor(int color) {
        backgroundColor = color;
        invalidate();
    }

    public List<Layer> getLayers() {
        return layers;
    }

    public void setCurrentLayerIndex(int index) {
        if (index >= 0 && index < layers.size()) {
            this.currentLayerIndex = index;
        }
    }

    public int getCurrentLayerIndex() {
        return currentLayerIndex;
    }
}
