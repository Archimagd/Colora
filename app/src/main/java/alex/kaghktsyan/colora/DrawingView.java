package alex.kaghktsyan.colora;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class DrawingView extends View {
    private Paint paint;
    private Path currentPath;
    private Bitmap bitmap;
    private Canvas bitmapCanvas;
    private float lastX, lastY;

    private List<PathWithPaint> paths = new ArrayList<>();
    private List<PathWithPaint> undonePaths = new ArrayList<>();


    private static class PathWithPaint {
        Path path;
        Paint paint;

        PathWithPaint(Path path, Paint paint) {
            this.path = new Path(path);
            this.paint = new Paint(paint);
        }
    }

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
        if (bitmap != null) {
            bitmap.recycle();
        }
        bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        bitmapCanvas = new Canvas(bitmap);
        bitmapCanvas.drawColor(Color.WHITE);
        redrawAllPaths();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawBitmap(bitmap, 0, 0, null);
        if (!currentPath.isEmpty()) {
            canvas.drawPath(currentPath, paint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                currentPath.moveTo(x, y);
                lastX = x;
                lastY = y;
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
                paths.add(new PathWithPaint(currentPath, paint));
                undonePaths.clear();
                bitmapCanvas.drawPath(currentPath, paint);
                currentPath.reset();
                invalidate();
                break;
        }
        return true;
    }

    private void redrawAllPaths() {
        bitmap.eraseColor(Color.WHITE);
        for (PathWithPaint pwp : paths) {
            bitmapCanvas.drawPath(pwp.path, pwp.paint);
        }
        invalidate();
    }

    public void clearCanvas() {
        paths.clear();
        undonePaths.clear();
        currentPath.reset();
        bitmap.eraseColor(Color.WHITE);
        invalidate();
    }

    public void undo() {
        if (!paths.isEmpty()) {
            undonePaths.add(paths.remove(paths.size() - 1));
            redrawAllPaths();
        }
    }

    public void redo() {
        if (!undonePaths.isEmpty()) {
            paths.add(undonePaths.remove(undonePaths.size() - 1));
            redrawAllPaths();
        }
    }

    public boolean canUndo() {
        return !paths.isEmpty();
    }

    public boolean canRedo() {
        return !undonePaths.isEmpty();
    }

    public void setColor(int color) {
        paint.setColor(color);
    }

    public void setStrokeWidth(float width) {
        paint.setStrokeWidth(width);
    }

    public int getColor() {
        return paint.getColor();
    }

    public float getStrokeWidth() {
        return paint.getStrokeWidth();
    }

    public Bitmap getBitmap() {
        return Bitmap.createBitmap(bitmap);
    }

    public void setEraserMode(boolean isEraser) {
        if (isEraser) {
            paint.setColor(Color.WHITE);
        }
    }
}