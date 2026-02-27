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
    private Paint previewPaint;
    private Paint selectorPaint;

    private float[] hsv = {0, 1, 1};
    private OnColorChangedListener listener;

    private RectF hueRect = new RectF();
    private RectF satValRect = new RectF();
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
        previewPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        selectorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        selectorPaint.setStyle(Paint.Style.STROKE);
        selectorPaint.setStrokeWidth(5);
        selectorPaint.setColor(Color.WHITE);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = (int) (width * 0.7f);
        setMeasuredDimension(width, height);
    }

    public void setOnColorChangedListener(OnColorChangedListener listener) {
        this.listener = listener;
    }

    public void setColor(int color) {
        Color.colorToHSV(color, hsv);
        invalidate();
    }

    public int getColor() {
        return Color.HSVToColor(hsv);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        float padding = 30;
        float hueHeight = 50;
        float previewWidth = w * 0.25f;

        previewRect.set(padding, padding, previewWidth, h - hueHeight - padding * 3);
        satValRect.set(previewWidth + padding * 2, padding, w - padding, h - hueHeight - padding * 3);
        hueRect.set(padding, h - hueHeight - padding, w - padding, h - padding);

        int[] colors = new int[7];
        for (int i = 0; i < 7; i++) {
            colors[i] = Color.HSVToColor(new float[]{i * 60f, 1, 1});
        }
        hueShader = new LinearGradient(hueRect.left, 0, hueRect.right, 0, colors, null, Shader.TileMode.CLAMP);
        huePaint.setShader(hueShader);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        previewPaint.setColor(Color.HSVToColor(hsv));
        canvas.drawRoundRect(previewRect, 10, 10, previewPaint);

        Shader valShader = new LinearGradient(0, satValRect.top, 0, satValRect.bottom, Color.WHITE, Color.BLACK, Shader.TileMode.CLAMP);
        int hueColor = Color.HSVToColor(new float[]{hsv[0], 1, 1});
        Shader satShader = new LinearGradient(satValRect.left, 0, satValRect.right, 0, Color.WHITE, hueColor, Shader.TileMode.CLAMP);
        ComposeShader composeShader = new ComposeShader(valShader, satShader, PorterDuff.Mode.MULTIPLY);
        satValPaint.setShader(composeShader);
        canvas.drawRoundRect(satValRect, 10, 10, satValPaint);

        canvas.drawRoundRect(hueRect, hueRect.height() / 2, hueRect.height() / 2, huePaint);

        float selectorX = satValRect.left + hsv[1] * satValRect.width();
        float selectorY = satValRect.top + (1 - hsv[2]) * satValRect.height();
        selectorPaint.setColor(hsv[2] > 0.5 ? Color.BLACK : Color.WHITE);
        canvas.drawCircle(selectorX, selectorY, 15, selectorPaint);

        float hueX = hueRect.left + (hsv[0] / 360f) * hueRect.width();
        selectorPaint.setColor(Color.WHITE);
        canvas.drawCircle(hueX, hueRect.centerY(), hueRect.height() / 2 + 5, selectorPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_MOVE) {
            if (y > hueRect.top - 30) {
                hsv[0] = (x - hueRect.left) / hueRect.width() * 360f;
                hsv[0] = Math.max(0, Math.min(360, hsv[0]));
            } else if (x > satValRect.left - 20) {
                hsv[1] = (x - satValRect.left) / satValRect.width();
                hsv[2] = 1 - (y - satValRect.top) / satValRect.height();
                hsv[1] = Math.max(0, Math.min(1, hsv[1]));
                hsv[2] = Math.max(0, Math.min(1, hsv[2]));
            }

            if (listener != null) {
                listener.onColorChanged(Color.HSVToColor(hsv));
            }
            invalidate();
            return true;
        }
        return true;
    }
}