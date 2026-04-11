package alex.kaghktsyan.colora;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class DrawingView extends View {

    public enum ShapeType {
        NONE, LINE, RECT, CIRCLE, TRIANGLE
    }

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

    public interface OnColorPickedListener {
        void onColorPicked(int color);
    }

    public interface OnTextRequestListener {
        void onTextRequest(float x, float y);
    }

    private Paint paint;
    private Paint textPaint;
    private Paint selectionPaint;
    private Path currentPath = new Path();
    private List<Layer> layers = new ArrayList<>();
    private int currentLayerIndex = -1;

    private float lastX, lastY;
    private float startX, startY;
    private boolean isEraserMode = false;
    private boolean isFillMode = false;
    private boolean isPickerMode = false;
    private boolean isTextMode = false;
    private boolean isSelectMode = false;
    private boolean isMoveMode = false;
    private boolean isTransformMode = false;
    private ShapeType currentShape = ShapeType.NONE;

    private RectF selectionRect = null;
    private Bitmap selectedBitmap = null;
    private float moveStartX, moveStartY;
    private float selectedBitmapX, selectedBitmapY;

    private int currentColor = Color.BLACK;
    private int currentAlpha = 255;
    private float currentStrokeWidth = 10;
    private int currentHardness = 100;
    private int backgroundColor = Color.WHITE;
    private OnColorPickedListener colorPickedListener;
    private OnTextRequestListener textRequestListener;

    private List<Bitmap[]> undoStack = new ArrayList<>();
    private List<Bitmap[]> redoStack = new ArrayList<>();
    private static final int MAX_UNDO_STEPS = 10;

    // Timelapse members
    private boolean isRecordingTimelapse = false;
    private File timelapseDir;
    private int frameCounter = 0;

    // Transformation members
    private Matrix drawMatrix = new Matrix();
    private Matrix inverseMatrix = new Matrix();
    private ScaleGestureDetector scaleGestureDetector;
    private RotationGestureDetector rotationGestureDetector;
    private float lastTouchX, lastTouchY;

    public DrawingView(Context context) {
        super(context);
        init(context);
    }

    public DrawingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);

        paint = new Paint();
        paint.setColor(currentColor);
        paint.setAlpha(currentAlpha);
        paint.setStrokeWidth(currentStrokeWidth);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setAntiAlias(true);
        updateHardness();

        textPaint = new Paint();
        textPaint.setColor(Color.BLACK);
        textPaint.setTextSize(50);
        textPaint.setAntiAlias(true);

        selectionPaint = new Paint();
        selectionPaint.setColor(Color.BLUE);
        selectionPaint.setStyle(Paint.Style.STROKE);
        selectionPaint.setStrokeWidth(2);
        selectionPaint.setPathEffect(new DashPathEffect(new float[]{10, 10}, 0));

        setupGestures(context);
        
        timelapseDir = new File(context.getCacheDir(), "timelapse_frames");
    }

    private void setupGestures(Context context) {
        scaleGestureDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                float scaleFactor = detector.getScaleFactor();
                drawMatrix.postScale(scaleFactor, scaleFactor, detector.getFocusX(), detector.getFocusY());
                invalidate();
                return true;
            }
        });

        rotationGestureDetector = new RotationGestureDetector(rotationDetector -> {
            float angle = rotationDetector.getAngle();
            drawMatrix.postRotate(angle, getWidth() / 2f, getHeight() / 2f);
            invalidate();
        });
    }

    public void startTimelapse() {
        if (isRecordingTimelapse) return;
        isRecordingTimelapse = true;
        frameCounter = 0;
        if (timelapseDir.exists()) {
            File[] files = timelapseDir.listFiles();
            if (files != null) {
                for (File f : files) f.delete();
            }
        } else {
            timelapseDir.mkdirs();
        }
        captureTimelapseFrame();
    }

    public void stopTimelapse() {
        isRecordingTimelapse = false;
    }

    public boolean isRecordingTimelapse() {
        return isRecordingTimelapse;
    }

    public File getTimelapseDir() {
        return timelapseDir;
    }

    public int getFrameCount() {
        return frameCounter;
    }

    private void captureTimelapseFrame() {
        if (!isRecordingTimelapse) return;
        
        Bitmap frame = getBitmap();
        File frameFile = new File(timelapseDir, String.format("frame_%05d.jpg", frameCounter++));
        try (FileOutputStream out = new FileOutputStream(frameFile)) {
            frame.compress(Bitmap.CompressFormat.JPEG, 80, out);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            frame.recycle();
        }
    }

    public void setTransformMode(boolean enabled) {
        resetModes();
        isTransformMode = enabled;
    }

    public boolean isTransformMode() {
        return isTransformMode;
    }

    public void resetTransform() {
        drawMatrix.reset();
        invalidate();
    }

    public void setOnColorPickedListener(OnColorPickedListener listener) {
        this.colorPickedListener = listener;
    }

    public void setOnTextRequestListener(OnTextRequestListener listener) {
        this.textRequestListener = listener;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w > 0 && h > 0) {
            if (layers.isEmpty()) {
                addLayer("Background Layer");
            } else {
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
            captureTimelapseFrame();
        }
    }

    public void removeLayer(int index) {
        if (layers.size() > 1 && index >= 0 && index < layers.size()) {
            saveStateToUndo();
            layers.get(index).recycle();
            layers.remove(index);
            currentLayerIndex = Math.min(currentLayerIndex, layers.size() - 1);
            invalidate();
            captureTimelapseFrame();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(backgroundColor);

        canvas.save();
        canvas.concat(drawMatrix);

        for (int i = 0; i < layers.size(); i++) {
            Layer layer = layers.get(i);
            if (layer.isVisible) {
                Paint layerPaint = new Paint();
                layerPaint.setAlpha((int) (layer.opacity * 255));
                canvas.drawBitmap(layer.bitmap, 0, 0, layerPaint);

                if (i == currentLayerIndex) {
                    if (currentShape != ShapeType.NONE) {
                        drawShape(canvas, paint, startX, startY, lastX, lastY, currentShape);
                    } else if (!currentPath.isEmpty() && !isEraserMode && !isFillMode && !isPickerMode && !isTextMode && !isSelectMode && !isMoveMode) {
                        canvas.drawPath(currentPath, paint);
                    }
                }
            }
        }

        if (isSelectMode && selectionRect != null) {
            canvas.drawRect(selectionRect, selectionPaint);
        }

        if (isMoveMode && selectedBitmap != null) {
            canvas.drawBitmap(selectedBitmap, selectedBitmapX, selectedBitmapY, null);
            canvas.drawRect(selectedBitmapX, selectedBitmapY, 
                           selectedBitmapX + selectedBitmap.getWidth(), 
                           selectedBitmapY + selectedBitmap.getHeight(), selectionPaint);
        }

        canvas.restore();
    }

    private void drawShape(Canvas canvas, Paint p, float x1, float y1, float x2, float y2, ShapeType shape) {
        switch (shape) {
            case LINE:
                canvas.drawLine(x1, y1, x2, y2, p);
                break;
            case RECT:
                canvas.drawRect(Math.min(x1, x2), Math.min(y1, y2), Math.max(x1, x2), Math.max(y1, y2), p);
                break;
            case CIRCLE:
                float radius = (float) Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
                canvas.drawCircle(x1, y1, radius, p);
                break;
            case TRIANGLE:
                Path path = new Path();
                path.moveTo(x1, y2);
                path.lineTo(x2, y2);
                path.lineTo((x1 + x2) / 2, y1);
                path.close();
                canvas.drawPath(path, p);
                break;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        scaleGestureDetector.onTouchEvent(event);
        rotationGestureDetector.onTouchEvent(event);

        if (event.getPointerCount() > 1 || isTransformMode) {
            handlePanning(event);
            return true;
        }

        if (currentLayerIndex == -1 || !layers.get(currentLayerIndex).isVisible) return false;

        drawMatrix.invert(inverseMatrix);
        float[] pts = {event.getX(), event.getY()};
        inverseMatrix.mapPoints(pts);
        float x = pts[0];
        float y = pts[1];

        Layer layer = layers.get(currentLayerIndex);

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (isPickerMode) {
                    pickColor((int) x, (int) y);
                    return true;
                }
                if (isFillMode) {
                    saveStateToUndo();
                    int targetColor = layer.bitmap.getPixel((int) Math.max(0, Math.min(x, layer.bitmap.getWidth()-1)), (int) Math.max(0, Math.min(y, layer.bitmap.getHeight()-1)));
                    performFloodFill((int) x, (int) y, targetColor, currentColor);
                    captureTimelapseFrame();
                    return true;
                }
                if (isTextMode) {
                    if (textRequestListener != null) {
                        textRequestListener.onTextRequest(x, y);
                    }
                    return true;
                }
                if (isSelectMode) {
                    selectionRect = new RectF(x, y, x, y);
                    invalidate();
                    return true;
                }
                if (isMoveMode) {
                    if (selectedBitmap != null && 
                        x >= selectedBitmapX && x <= selectedBitmapX + selectedBitmap.getWidth() &&
                        y >= selectedBitmapY && y <= selectedBitmapY + selectedBitmap.getHeight()) {
                        moveStartX = x;
                        moveStartY = y;
                        return true;
                    }
                    return false;
                }
                saveStateToUndo();
                startX = x;
                startY = y;
                lastX = x;
                lastY = y;
                if (currentShape == ShapeType.NONE) {
                    currentPath.moveTo(x, y);
                }
                invalidate();
                return true;

            case MotionEvent.ACTION_MOVE:
                if (isPickerMode || isFillMode || isTextMode) return true;
                
                if (isSelectMode) {
                    selectionRect.right = x;
                    selectionRect.bottom = y;
                    invalidate();
                    return true;
                }

                if (isMoveMode) {
                    if (selectedBitmap != null) {
                        float dx = x - moveStartX;
                        float dy = y - moveStartY;
                        selectedBitmapX += dx;
                        selectedBitmapY += dy;
                        moveStartX = x;
                        moveStartY = y;
                        invalidate();
                    }
                    return true;
                }

                if (currentShape != ShapeType.NONE) {
                    lastX = x;
                    lastY = y;
                } else {
                    float midX = (x + lastX) / 2;
                    float midY = (y + lastY) / 2;
                    currentPath.quadTo(lastX, lastY, midX, midY);
                    
                    if (isEraserMode) {
                        layer.canvas.drawPath(currentPath, paint);
                        currentPath.reset();
                        currentPath.moveTo(midX, midY);
                    }
                    lastX = x;
                    lastY = y;
                }
                invalidate();
                break;

            case MotionEvent.ACTION_UP:
                if (isPickerMode || isFillMode || isTextMode) return true;
                
                if (isSelectMode) {
                    if (selectionRect != null) {
                        selectionRect.sort();
                        if (selectionRect.width() > 5 && selectionRect.height() > 5) {
                            cutSelection();
                        } else {
                            selectionRect = null;
                        }
                    }
                    invalidate();
                    return true;
                }

                if (isMoveMode) return true;

                if (currentShape != ShapeType.NONE) {
                    drawShape(layer.canvas, paint, startX, startY, x, y, currentShape);
                } else {
                    layer.canvas.drawPath(currentPath, paint);
                    currentPath.reset();
                }
                invalidate();
                captureTimelapseFrame();
                break;
        }
        return true;
    }

    private void handlePanning(MotionEvent event) {
        float sumX = 0, sumY = 0;
        int count = event.getPointerCount();
        int action = event.getActionMasked();

        boolean isPointerUp = action == MotionEvent.ACTION_POINTER_UP;
        int skipIndex = isPointerUp ? event.getActionIndex() : -1;

        int effectiveCount = 0;
        for (int i = 0; i < count; i++) {
            if (i == skipIndex) continue;
            sumX += event.getX(i);
            sumY += event.getY(i);
            effectiveCount++;
        }

        if (effectiveCount == 0) return;

        float focusX = sumX / effectiveCount;
        float focusY = sumY / effectiveCount;

        switch (action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
            case MotionEvent.ACTION_POINTER_UP:
                lastTouchX = focusX;
                lastTouchY = focusY;
                break;

            case MotionEvent.ACTION_MOVE:
                float dx = focusX - lastTouchX;
                float dy = focusY - lastTouchY;

                drawMatrix.postTranslate(dx, dy);

                lastTouchX = focusX;
                lastTouchY = focusY;
                invalidate();
                break;
        }
    }

    private void cutSelection() {
        if (selectionRect == null || currentLayerIndex == -1) return;
        saveStateToUndo();
        Layer layer = layers.get(currentLayerIndex);
        
        int left = (int) Math.max(0, selectionRect.left);
        int top = (int) Math.max(0, selectionRect.top);
        int right = (int) Math.min(layer.bitmap.getWidth(), (int)selectionRect.right);
        int bottom = (int) Math.min(layer.bitmap.getHeight(), (int)selectionRect.bottom);
        
        if (right <= left || bottom <= top) return;

        selectedBitmap = Bitmap.createBitmap(layer.bitmap, left, top, right - left, bottom - top);
        selectedBitmapX = left;
        selectedBitmapY = top;

        Paint clearPaint = new Paint();
        clearPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        layer.canvas.drawRect(selectionRect, clearPaint);
        
        selectionRect = null;
        isSelectMode = false;
        isMoveMode = true;
    }

    public void applyMove() {
        if (selectedBitmap != null && currentLayerIndex != -1) {
            Layer layer = layers.get(currentLayerIndex);
            layer.canvas.drawBitmap(selectedBitmap, selectedBitmapX, selectedBitmapY, null);
            selectedBitmap.recycle();
            selectedBitmap = null;
            isMoveMode = false;
            invalidate();
            captureTimelapseFrame();
        }
    }

    public void drawText(String text, float x, float y) {
        if (currentLayerIndex != -1) {
            saveStateToUndo();
            Layer layer = layers.get(currentLayerIndex);
            textPaint.setColor(currentColor);
            textPaint.setAlpha(currentAlpha);
            textPaint.setTextSize(currentStrokeWidth * 5);
            layer.canvas.drawText(text, x, y, textPaint);
            invalidate();
            captureTimelapseFrame();
        }
    }

    private void pickColor(int x, int y) {
        Bitmap fullBitmap = getBitmap();
        int safeX = Math.max(0, Math.min(x, fullBitmap.getWidth() - 1));
        int safeY = Math.max(0, Math.min(y, fullBitmap.getHeight() - 1));
        int color = fullBitmap.getPixel(safeX, safeY);
        fullBitmap.recycle();
        
        setColor(color);
        if (colorPickedListener != null) {
            colorPickedListener.onColorPicked(color);
        }
    }

    private void performFloodFill(int x, int y, int targetColor, int replacementColor) {
        if (targetColor == replacementColor) return;

        Bitmap bitmap = layers.get(currentLayerIndex).bitmap;
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        
        if (x < 0 || x >= width || y < 0 || y >= height) return;

        int[] pixels = new int[width * height];
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);

        Stack<Point> stack = new Stack<>();
        stack.push(new Point(x, y));

        while (!stack.isEmpty()) {
            Point p = stack.pop();
            int px = p.x;
            int py = p.y;

            if (pixels[py * width + px] != targetColor) continue;

            int left = px;
            while (left > 0 && pixels[py * width + left - 1] == targetColor) {
                left--;
            }

            int right = px;
            while (right < width - 1 && pixels[py * width + right + 1] == targetColor) {
                right++;
            }

            for (int i = left; i <= right; i++) {
                pixels[py * width + i] = replacementColor;

                if (py > 0 && pixels[(py - 1) * width + i] == targetColor) {
                    if (i == left || pixels[(py - 1) * width + i - 1] != targetColor) {
                        stack.push(new Point(i, py - 1));
                    }
                }
                if (py < height - 1 && pixels[(py + 1) * width + i] == targetColor) {
                    if (i == left || pixels[(py + 1) * width + i - 1] != targetColor) {
                        stack.push(new Point(i, py + 1));
                    }
                }
            }
        }
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
        invalidate();
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
            captureTimelapseFrame();
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
            captureTimelapseFrame();
        }
    }

    public void clearCanvas() {
        saveStateToUndo();
        for (Layer layer : layers) {
            layer.bitmap.eraseColor(Color.TRANSPARENT);
        }
        invalidate();
        captureTimelapseFrame();
    }

    public void setColor(int color) {
        currentColor = color;
        if (!isEraserMode) {
            paint.setColor(color);
            paint.setAlpha(currentAlpha);
            paint.setXfermode(null);
        }
    }

    public int getColor() {
        return currentColor;
    }

    public void setBrushAlpha(int alpha) {
        currentAlpha = alpha;
        if (!isEraserMode) {
            paint.setAlpha(alpha);
        }
    }

    public int getBrushAlpha() {
        return currentAlpha;
    }

    public void setStrokeWidth(float width) {
        currentStrokeWidth = width;
        paint.setStrokeWidth(width);
        updateHardness();
    }

    public float getStrokeWidth() {
        return currentStrokeWidth;
    }

    public void setHardness(int hardness) {
        currentHardness = hardness;
        updateHardness();
    }

    public int getHardness() {
        return currentHardness;
    }

    private void updateHardness() {
        if (currentHardness >= 100) {
            paint.setMaskFilter(null);
        } else {
            float blurRadius = (currentStrokeWidth / 2f) * (1f - (currentHardness / 100f));
            if (blurRadius > 0) {
                paint.setMaskFilter(new BlurMaskFilter(blurRadius, BlurMaskFilter.Blur.NORMAL));
            } else {
                paint.setMaskFilter(null);
            }
        }
    }

    public Bitmap getBitmap() {
        if (getWidth() <= 0 || getHeight() <= 0) return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
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
        for (Layer l : layers) l.recycle();
        layers.clear();
        
        Layer layer = new Layer(loadedBitmap.getWidth(), loadedBitmap.getHeight(), "Imported Layer");
        layer.canvas.drawBitmap(loadedBitmap, 0, 0, null);
        layers.add(layer);
        currentLayerIndex = 0;
        invalidate();
        captureTimelapseFrame();
    }

    public void setEraserMode(boolean enabled) {
        resetModes();
        isEraserMode = enabled;
        if (enabled) {
            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
            paint.setAlpha(255);
            paint.setMaskFilter(null);
        } else {
            paint.setXfermode(null);
            paint.setColor(currentColor);
            paint.setAlpha(currentAlpha);
            updateHardness();
        }
    }

    public void setFillMode(boolean enabled) {
        resetModes();
        isFillMode = enabled;
    }

    public void setPickerMode(boolean enabled) {
        resetModes();
        isPickerMode = enabled;
    }

    public void setTextMode(boolean enabled) {
        resetModes();
        isTextMode = enabled;
    }

    public void setSelectMode(boolean enabled) {
        resetModes();
        isSelectMode = enabled;
        selectionRect = null;
    }

    public void setMoveMode(boolean enabled) {
        resetModes();
        isMoveMode = enabled;
    }

    public void setShapeType(ShapeType type) {
        resetModes();
        this.currentShape = type;
    }

    private void resetModes() {
        if (isMoveMode) {
            applyMove();
        }
        isEraserMode = false;
        isFillMode = false;
        isPickerMode = false;
        isTextMode = false;
        isSelectMode = false;
        isMoveMode = false;
        isTransformMode = false;
        currentShape = ShapeType.NONE;
        paint.setXfermode(null);
        paint.setColor(currentColor);
        paint.setAlpha(currentAlpha);
        updateHardness();
    }

    public void toggleEraser() {
        setEraserMode(!isEraserMode);
    }

    public void setBackgroundColor(int color) {
        backgroundColor = color;
        invalidate();
        captureTimelapseFrame();
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
