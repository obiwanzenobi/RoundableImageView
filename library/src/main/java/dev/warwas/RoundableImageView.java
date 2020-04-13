/*
 * Copyright (c) 2020. Wojciech Warwas.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.warwas;

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
import android.util.AttributeSet;

import java.util.Arrays;

import androidx.annotation.ColorInt;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.content.ContextCompat;

/**
 * A roundable ImageView.
 * It can take common corners radius, for each individual corner or just be a circle.
 * There is also an option to draw border.
 */

public class RoundableImageView extends AppCompatImageView {

    public static final int CORNER_RADIUS_COUNT = 8;
    public static final float BORDER_ALPHA_MARGIN = 0.3f;
    private final PorterDuffXfermode mLayerXfer = new PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP);
    public float borderWidth;
    private Path clipPath, borderPath;
    private Paint borderPaint, clipPaint, layerPaint, maskPaint;
    private boolean isCircle, hasBorder;
    private float commonRoundedCornersRadius;
    private float topLeftCornerRadius, topRightCornerRadius, bottomRightCornerRaius, bottomLeftCornerRadius;
    private Drawable background;
    private boolean hasSizeCHanged;
    private float[] corners;

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

    /**
     * Sets the @radius of all corners.
     * It will overwrite individual corners settings if they were set before that call.
     *
     * @param radius The new common radius of all corners in the image.
     */
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
        this.commonRoundedCornersRadius = 0;
        invalidate();
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

    public void setTopLeftCornerRadius(float topLeftCornerRadius) {
        this.topLeftCornerRadius = topLeftCornerRadius;
        postInvalidate();
    }

    public void setTopRightCornerRadius(float topRightCornerRadius) {
        this.topRightCornerRadius = topRightCornerRadius;
        postInvalidate();
    }

    public void setBottomRightCornerRadius(float bottomRightCornerRaius) {
        this.bottomRightCornerRaius = bottomRightCornerRaius;
        postInvalidate();
    }

    public void setBottomLeftCornerRadius(float bottomLeftCornerRadius) {
        this.bottomLeftCornerRadius = bottomLeftCornerRadius;
        postInvalidate();
    }

    protected void updateLayerPaint(Paint layerPaint) {
        layerPaint.setXfermode(getBackground() != null ? mLayerXfer : null);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (isInEditMode()) {
            // basic clip that is used only to provide android studio preview (draw/save layer is not shown in preview)
            canvas.clipPath(getClipPath());
        }

        canvas.drawPath(getClipPath(), maskPaint);

        canvas.saveLayer(null, clipPaint, Canvas.ALL_SAVE_FLAG);

        if (background != null) {
            background.setBounds(0, 0, getWidth(), getHeight());
            background.draw(canvas);
        }

        super.onDraw(canvas);

        if (hasBorder) {
            canvas.drawPath(getBorderPath(), borderPaint);
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        hasSizeCHanged = w != oldw || h != oldh;
        super.onSizeChanged(w, h, oldw, oldh);
    }

    @Override
    public void setBackgroundColor(int color) {
        background = new ColorDrawable(color);
        invalidate();
    }

    @Override
    public void setBackground(Drawable background) {
        this.background = background;
        invalidate();
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

        clipPath = new Path();
        borderPath = new Path();
        createCornerRadius();
        setLayerType(LAYER_TYPE_SOFTWARE, maskPaint);
    }

    private void initMaskPaints() {

        maskPaint = new Paint();
        clipPaint = new Paint();
        layerPaint = new Paint();

        maskPaint.setAntiAlias(true);
        clipPaint.setAntiAlias(true);
        layerPaint.setAntiAlias(true);

        clipPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP));
        updateLayerPaint(layerPaint);
    }

    private void initBorderPaint() {
        borderPaint = new Paint();

        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setAntiAlias(true);
        borderPaint.setFilterBitmap(true);
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
        RectF rectF = new RectF(0, 0, this.getWidth(), this.getHeight());
        borderPath.addRoundRect(rectF, corners, Path.Direction.CW);
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

            Arrays.fill(corners, commonRoundedCornersRadius);

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
}
