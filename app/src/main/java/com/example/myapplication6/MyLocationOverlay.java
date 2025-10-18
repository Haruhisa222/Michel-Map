package com.example.myapplication6;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class MyLocationOverlay extends View {
    private float x = -1, y = -1;
    private Paint paintBlue;
    private Paint paintWhite;

    // Javaからnewする時用
    public MyLocationOverlay(Context context) {
        super(context);
        init();
    }

    // XMLからinflateする時用（必須）
    public MyLocationOverlay(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        // 青丸用
        paintBlue = new Paint();
        paintBlue.setColor(0xFF0000FF); // 青色
        paintBlue.setAntiAlias(true);

        // 白枠用
        paintWhite = new Paint();
        paintWhite.setColor(0xFFFFFFFF); // 白
        paintWhite.setAntiAlias(true);
        paintWhite.setStyle(Paint.Style.STROKE); // 枠線だけ
        paintWhite.setStrokeWidth(6f);           // 枠の太さ
    }

    public void setLocation(float x, float y) {
        this.x = x;
        this.y = y;
        invalidate(); // 再描画
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (x >= 0 && y >= 0) {
            // 白い円の枠（青丸と同じ大きさ）
            canvas.drawCircle(x, y, 20, paintWhite);

            // 青丸（中身）
            canvas.drawCircle(x, y, 20, paintBlue);
        }
    }
}
