package com.z_iti_271311_u3_e07;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class DrawingView extends View {

    // Lista para almacenar las formas a dibujar
    private final List<Shape> shapes = new ArrayList<>();

    public DrawingView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    // Método para dibujar un círculo
    public void drawCircle(float cx, float cy, float radius, Paint paint) {
        shapes.add(new Shape(Shape.Type.CIRCLE, cx, cy, radius, paint));
        invalidate(); // Redibujar la vista
    }

    // Método para dibujar una línea
    public void drawLine(float startX, float startY, float endX, float endY, Paint paint) {
        shapes.add(new Shape(Shape.Type.LINE, startX, startY, endX, endY, paint));
        invalidate(); // Redibujar la vista
    }

    // Método para limpiar el lienzo
    public void clear() {
        shapes.clear();
        invalidate(); // Redibujar la vista
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Dibujar todas las formas almacenadas
        for (Shape shape : shapes) {
            if (shape.type == Shape.Type.CIRCLE) {
                canvas.drawCircle(shape.cx, shape.cy, shape.radius, shape.paint);
            } else if (shape.type == Shape.Type.LINE) {
                canvas.drawLine(shape.startX, shape.startY, shape.endX, shape.endY, shape.paint);
            }
        }
    }

    // Clase interna para representar formas (círculos y líneas)
    private static class Shape {
        enum Type { CIRCLE, LINE }

        Type type; // Tipo de la forma
        float cx, cy, radius; // Coordenadas y radio (para círculos)
        float startX, startY, endX, endY; // Coordenadas iniciales y finales (para líneas)
        Paint paint; // Estilo de dibujo

        // Constructor para un círculo
        Shape(Type type, float cx, float cy, float radius, Paint paint) {
            this.type = type;
            this.cx = cx;
            this.cy = cy;
            this.radius = radius;
            this.paint = paint;
        }

        // Constructor para una línea
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
