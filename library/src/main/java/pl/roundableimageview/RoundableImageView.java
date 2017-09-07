package pl.roundableimageview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.ColorInt;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;

public class RoundableImageView extends AppCompatImageView {

    public static final int CORNER_RADIUS_COUNT = 8;
    public static final int CORNER_START_RATIO = 4;
    public static final int DRAWING_START_POINT_RATIO = CORNER_START_RATIO / 2;

    public static final float BORDER_ALPHA_MARGIN = 0.3f;

    private Path clipPath, borderPath;
    private Paint borderPaint, clipPaint, layerPaint, maskPaint;
    private boolean isCircle, hasBorder;
    private float commonRoundedCornersRadius;
    private float topLeftCornerRadius, topRightCornerRadius, bottomRightCornerRaius, bottomLeftCornerRadius;
    private Drawable background;
    private boolean hasSizeCHanged;
    private float[] corners;

    private final PorterDuffXfermode mLayerXfer = new PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP);
    public float borderWidth;

    public RoundableImageView(Context context) {
        super(context);
        init(null);
    }

    public RoundableImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public RoundableImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    private void init(AttributeSet attr) {
        TypedArray a = getContext().obtainStyledAttributes(attr, R.styleable.RoundableImageView);

        try {
            isCircle = a.getBoolean(R.styleable.RoundableImageView_isCircle, false);
            commonRoundedCornersRadius = a.getDimension(R.styleable.RoundableImageView_cornerRadius_common, 0);
            topLeftCornerRadius = a.getDimension(R.styleable.RoundableImageView_cornerRadius_top_left, 0);
            topRightCornerRadius = a.getDimension(R.styleable.RoundableImageView_cornerRadius_top_right, 0);
            bottomRightCornerRaius = a.getDimension(R.styleable.RoundableImageView_cornerRadius_bottom_right, 0);
            bottomLeftCornerRadius = a.getDimension(R.styleable.RoundableImageView_cornerRadius_bottom_left, 0);

            initBorderPaint();
            borderWidth = a.getDimension(R.styleable.RoundableImageView_borderWidth, 0f);
            this.hasBorder = borderWidth > 0.0f;
            this.borderPaint.setStrokeWidth(borderWidth);

            this.borderPaint.setColor(a.getColor(R.styleable.RoundableImageView_borderColor, Color.WHITE));

            background = a.getDrawable(R.styleable.RoundableImageView_android_background);

            initMaskPaints();

        } finally {
            a.recycle();
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) {
            setLayerType(LAYER_TYPE_SOFTWARE, null); // to allow clipPath working with rounded corners
        }

        clipPath = new Path();
        borderPath = new Path();
        createCornerRadius();
    }

    private void initMaskPaints() {

        maskPaint = new Paint();
        clipPaint = new Paint();
        layerPaint = new Paint();

        maskPaint.setAntiAlias(true);
        clipPaint.setAntiAlias(true);
        layerPaint.setAntiAlias(true);

        clipPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        setLayerType(LAYER_TYPE_HARDWARE, layerPaint); // switching to software will cause graphical bugs
        updateLayerPaint(layerPaint);
    }

    @Override
    public void setWillNotCacheDrawing(boolean willNotCacheDrawing) {
        if (getLayerType() == LAYER_TYPE_NONE) {
            super.setWillNotCacheDrawing(willNotCacheDrawing);
        }
    }

    private void initBorderPaint() {
        borderPaint = new Paint();

        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setAntiAlias(true);
        borderPaint.setFilterBitmap(true);
    }

    protected void updateLayerPaint(Paint layerPaint) {
        layerPaint.setXfermode(getBackground() != null ? mLayerXfer : null);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawPath(getClipPath(), maskPaint);

        int saveCount = canvas.saveLayer(null, clipPaint, Canvas.HAS_ALPHA_LAYER_SAVE_FLAG);

        if (background != null) {
            background.setBounds(0, 0, getWidth(), getHeight());
            background.draw(canvas);
        }

        super.onDraw(canvas);

        if (hasBorder) {
            canvas.drawPath(getBorderPath(), borderPaint);
        }

        canvas.restoreToCount(saveCount);

    }

    private Path getBorderPath() {
        if (!borderPath.isEmpty() && !hasSizeCHanged) {
            return borderPath;
        }

        hasSizeCHanged = false;
        borderPath.reset();

        if (isCircle) {
            float lowerDimenValue = getLowerDimenValue() - (borderPaint.getStrokeWidth());
            float value = (lowerDimenValue / 2.0f) + BORDER_ALPHA_MARGIN;
            borderPath.addCircle(this.getWidth() / 2.0f, this.getHeight() / 2.0f, value, Path.Direction.CW);

        } else {
            drawRoundedBorderPath();
        }

        return borderPath;
    }

    private void drawRoundedBorderPath() {
        float w = getWidth();
        float h = getHeight();

        RectF rectF = new RectF(0, 0, this.getWidth(), this.getHeight());
        borderPath.addRoundRect(rectF, corners, Path.Direction.CW);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        hasSizeCHanged = w != oldw || h != oldh;
        super.onSizeChanged(w, h, oldw, oldh);
    }

    private Path getClipPath() {

        if (!clipPath.isEmpty() && !hasSizeCHanged) {
            return clipPath;
        }

        clipPath.reset();

        if (isCircle) {

            float lowerDimenValue = getLowerDimenValue();
            clipPath.addCircle(this.getWidth() / 2.0f, this.getHeight() / 2.0f, lowerDimenValue / 2.0f, Path.Direction.CW);

        } else {
            RectF rectF = new RectF(0, 0, this.getWidth(), this.getHeight());

            clipPath.addRoundRect(rectF, corners, Path.Direction.CW);

        }

        return clipPath;
    }

    private float getLowerDimenValue() {
        int highestHorizontalPadding = Math.max(getPaddingLeft(), getPaddingRight());
        int highestVerticalPadding = Math.max(getPaddingTop(), getPaddingBottom());

        int w = this.getWidth() - (2 * highestHorizontalPadding);
        int h = this.getHeight() - (2 * highestVerticalPadding);

        return (float) Math.min(w, h);
    }

    private void createCornerRadius() {

        corners = new float[CORNER_RADIUS_COUNT];

        if (commonRoundedCornersRadius > 0) {

            for (int i = 0; i < corners.length; i++) {
                corners[i] = commonRoundedCornersRadius;
            }

            topLeftCornerRadius = topRightCornerRadius = bottomLeftCornerRadius =
                    bottomRightCornerRaius = commonRoundedCornersRadius;

        } else {
            corners[0] = topLeftCornerRadius;
            corners[1] = topLeftCornerRadius;
            corners[2] = topRightCornerRadius;
            corners[3] = topRightCornerRadius;
            corners[4] = bottomRightCornerRaius;
            corners[5] = bottomRightCornerRaius;
            corners[6] = bottomLeftCornerRadius;
            corners[7] = bottomLeftCornerRadius;
        }

    }

    public void setBorderWidth(float borderWidth) {
        this.hasBorder = borderWidth > 0.0f;
        this.borderPaint.setStrokeWidth(borderWidth);
        invalidate();
    }

    public void setBorderColor(@ColorInt int color) {
        this.borderPaint.setColor(color);
        invalidate();
    }

    @Override
    public void setBackgroundColor(int color) {
        background = new ColorDrawable(color);
        invalidate();
    }

    @Override
    public void setBackgroundResource(int resid) {
        background = ContextCompat.getDrawable(getContext(), resid);
        invalidate();
    }

    @Override
    public void setBackgroundDrawable(Drawable background) {
        this.background = background;
        invalidate();

        if (layerPaint != null) {
            updateLayerPaint(layerPaint);
        }
    }

    @Override
    public void setBackground(Drawable background) {
        this.background = background;
        invalidate();
    }

    public void setCommonRoundedCornersRadius(float radius) {
        commonRoundedCornersRadius = radius;
        createCornerRadius();
        invalidate();
    }

    public float getCornerRadius() {
        return commonRoundedCornersRadius;
    }

    public float[] getCornersRadiusArray() {
        return corners;
    }

    public void setCornersRadiusArray(float[] corners) {
        this.corners = corners;
        invalidate();
    }
}
