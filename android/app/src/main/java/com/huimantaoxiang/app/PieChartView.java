package com.huimantaoxiang.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class PieChartView extends View {
    private List<PieSlice> slices = new ArrayList<>();
    private Paint paint;
    private RectF rectF;
    private int centerX, centerY, radius;

    // 颜色数组
    private static final int[] COLORS = {
            Color.parseColor("#FF6B6B"),  // 红色
            Color.parseColor("#FFD93D"),  // 黄色
            Color.parseColor("#FF8B3D"),  // 橙色
            Color.parseColor("#FF6B9D"),  // 粉色
            Color.parseColor("#FFB6D9"),  // 浅粉
            Color.parseColor("#4ECDC4"),  // 青色
            Color.parseColor("#6BCB77"),  // 绿色
            Color.parseColor("#4D96FF"),  // 蓝色
            Color.parseColor("#C9B1FF"),  // 紫色
    };

    public PieChartView(Context context) {
        super(context);
        init();
    }

    public PieChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public PieChartView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.FILL);
        rectF = new RectF();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        centerX = w / 2;
        centerY = h / 2;
        radius = Math.min(w, h) / 2 - 20;
        rectF.set(centerX - radius, centerY - radius, centerX + radius, centerY + radius);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float startAngle = -90f; // 从顶部开始

        for (int i = 0; i < slices.size(); i++) {
            PieSlice slice = slices.get(i);
            paint.setColor(slice.color);
            canvas.drawArc(rectF, startAngle, slice.sweepAngle, true, paint);
            startAngle += slice.sweepAngle;
        }

        // 绘制中心圆，创建环形效果
        paint.setColor(Color.WHITE);
        canvas.drawCircle(centerX, centerY, radius * 0.5f, paint);
    }

    public void setData(List<PieSlice> slices) {
        this.slices = slices;
        invalidate();
    }

    public static class PieSlice {
        public String label;
        public float value;
        public float percentage;
        public int color;
        public float sweepAngle;

        public PieSlice(String label, float value, int color) {
            this.label = label;
            this.value = value;
            this.color = color;
        }
    }
}
