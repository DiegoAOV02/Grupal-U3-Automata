package com.z_iti_271311_u3_e07;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class DrawingView extends View {
    private final List<Shape> shapes = new ArrayList<>();

    public DrawingView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void drawCircle(float cx, float cy, float radius, Paint paint) {
        shapes.add(new Shape(Shape.Type.CIRCLE, cx, cy, radius, paint));
        invalidate();
    }

    public void drawText(String text, float x, float y, Paint paint) {
        shapes.add(new Shape(Shape.Type.TEXT, text, x, y, paint));
        invalidate();
    }

    public void drawLine(float startX, float startY, float endX, float endY, Paint paint) {
        shapes.add(new Shape(Shape.Type.LINE, startX, startY, endX, endY, paint));
        invalidate();
    }

    public void clear() {
        shapes.clear();
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        for (Shape shape : shapes) {
            switch (shape.type) {
                case CIRCLE:
                    canvas.drawCircle(shape.cx, shape.cy, shape.radius, shape.paint);
                    break;
                case TEXT:
                    canvas.drawText(shape.text, shape.startX, shape.startY, shape.paint);
                    break;
                case LINE:
                    canvas.drawLine(shape.startX, shape.startY, shape.endX, shape.endY, shape.paint);
                    break;
            }
        }
    }

    private static class Shape {
        enum Type { CIRCLE, TEXT, LINE }

        Type type;
        float cx, cy, radius;
        float startX, startY, endX, endY;
        String text;
        Paint paint;

        // Constructor para círculos
        Shape(Type type, float cx, float cy, float radius, Paint paint) {
            this.type = type;
            this.cx = cx;
            this.cy = cy;
            this.radius = radius;
            this.paint = paint;
        }

        // Constructor para texto
        Shape(Type type, String text, float x, float y, Paint paint) {
            this.type = type;
            this.text = text;
            this.startX = x;
            this.startY = y;
            this.paint = paint;
        }

        // Constructor para líneas
        Shape(Type type, float startX, float startY, float endX, float endY, Paint paint) {
            this.type = type;
            this.startX = startX;
            this.startY = startY;
            this.endX = endX;
            this.endY = endY;
            this.paint = paint;
        }
    }
}
