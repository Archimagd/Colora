package alex.kaghktsyan.colora;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ComposeShader;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class ColorPickerView extends View {
    private Paint huePaint;
    private Paint satValPaint;
    private Paint alphaPaint;
    private Paint previewPaint;
    private Paint selectorPaint;

    private float[] hsv = {0, 1, 1};
    private int alpha = 255;
    private OnColorChangedListener listener;

    private RectF hueRect = new RectF();
    private RectF satValRect = new RectF();
    private RectF alphaRect = new RectF();
    private RectF previewRect = new RectF();

    private Shader hueShader;

    public interface OnColorChangedListener {
        void onColorChanged(int color);
    }

    public ColorPickerView(Context context) {
        super(context);
        init();
    }

    public ColorPickerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        huePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        satValPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        alphaPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        previewPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        selectorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        selectorPaint.setStyle(Paint.Style.STROKE);
        selectorPaint.setStrokeWidth(5);
        selectorPaint.setColor(Color.WHITE);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = (int) (width * 0.9f); // Increased height to accommodate alpha bar
        setMeasuredDimension(width, height);
    }

    public void setOnColorChangedListener(OnColorChangedListener listener) {
        this.listener = listener;
    }

    public void setColor(int color) {
        alpha = Color.alpha(color);
        Color.colorToHSV(color, hsv);
        invalidate();
    }

    public int getColor() {
        return Color.HSVToColor(alpha, hsv);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        float padding = 30;
        float barHeight = 50;
        float previewWidth = w * 0.25f;

        float topAreaHeight = h - (barHeight * 2) - (padding * 5);

        previewRect.set(padding, padding, previewWidth, topAreaHeight);
        satValRect.set(previewWidth + padding * 2, padding, w - padding, topAreaHeight);
        
        hueRect.set(padding, topAreaHeight + padding * 2, w - padding, topAreaHeight + padding * 2 + barHeight);
        alphaRect.set(padding, topAreaHeight + padding * 3 + barHeight, w - padding, topAreaHeight + padding * 3 + barHeight * 2);

        int[] colors = new int[7];
        for (int i = 0; i < 7; i++) {
            colors[i] = Color.HSVToColor(new float[]{i * 60f, 1, 1});
        }
        hueShader = new LinearGradient(hueRect.left, 0, hueRect.right, 0, colors, null, Shader.TileMode.CLAMP);
        huePaint.setShader(hueShader);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // Draw preview
        previewPaint.setColor(getColor());
        canvas.drawRoundRect(previewRect, 10, 10, previewPaint);

        // Draw Saturation/Value picker
        Shader valShader = new LinearGradient(0, satValRect.top, 0, satValRect.bottom, Color.WHITE, Color.BLACK, Shader.TileMode.CLAMP);
        int hueColor = Color.HSVToColor(new float[]{hsv[0], 1, 1});
        Shader satShader = new LinearGradient(satValRect.left, 0, satValRect.right, 0, Color.WHITE, hueColor, Shader.TileMode.CLAMP);
        ComposeShader composeShader = new ComposeShader(valShader, satShader, PorterDuff.Mode.MULTIPLY);
        satValPaint.setShader(composeShader);
        canvas.drawRoundRect(satValRect, 10, 10, satValPaint);

        // Draw Hue bar
        canvas.drawRoundRect(hueRect, hueRect.height() / 2, hueRect.height() / 2, huePaint);

        // Draw Alpha bar
        int opaqueColor = Color.HSVToColor(255, hsv);
        int transparentColor = Color.HSVToColor(0, hsv);
        Shader alphaShader = new LinearGradient(alphaRect.left, 0, alphaRect.right, 0, transparentColor, opaqueColor, Shader.TileMode.CLAMP);
        alphaPaint.setShader(alphaShader);
        canvas.drawRoundRect(alphaRect, alphaRect.height() / 2, alphaRect.height() / 2, alphaPaint);

        // Draw Sat/Val selector
        float selectorX = satValRect.left + hsv[1] * satValRect.width();
        float selectorY = satValRect.top + (1 - hsv[2]) * satValRect.height();
        selectorPaint.setColor(hsv[2] > 0.5 ? Color.BLACK : Color.WHITE);
        canvas.drawCircle(selectorX, selectorY, 15, selectorPaint);

        // Draw Hue selector
        float hueX = hueRect.left + (hsv[0] / 360f) * hueRect.width();
        selectorPaint.setColor(Color.WHITE);
        canvas.drawCircle(hueX, hueRect.centerY(), hueRect.height() / 2 + 5, selectorPaint);

        // Draw Alpha selector
        float alphaX = alphaRect.left + (alpha / 255f) * alphaRect.width();
        selectorPaint.setColor(Color.WHITE);
        canvas.drawCircle(alphaX, alphaRect.centerY(), alphaRect.height() / 2 + 5, selectorPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_MOVE) {
            if (hueRect.contains(x, y) || (y > hueRect.top - 20 && y < hueRect.bottom + 20)) {
                hsv[0] = (x - hueRect.left) / hueRect.width() * 360f;
                hsv[0] = Math.max(0, Math.min(360, hsv[0]));
            } else if (alphaRect.contains(x, y) || (y > alphaRect.top - 20 && y < alphaRect.bottom + 20)) {
                alpha = (int) ((x - alphaRect.left) / alphaRect.width() * 255);
                alpha = Math.max(0, Math.min(255, alpha));
            } else if (satValRect.contains(x, y) || (y < hueRect.top - 40)) {
                hsv[1] = (x - satValRect.left) / satValRect.width();
                hsv[2] = 1 - (y - satValRect.top) / satValRect.height();
                hsv[1] = Math.max(0, Math.min(1, hsv[1]));
                hsv[2] = Math.max(0, Math.min(1, hsv[2]));
            }

            if (listener != null) {
                listener.onColorChanged(getColor());
            }
            invalidate();
            return true;
        }
        return true;
    }
}