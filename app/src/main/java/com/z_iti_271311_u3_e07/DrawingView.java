package com.z_iti_271311_u3_e07;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

public class DrawingView extends View {
    private final List<Shape> shapes = new ArrayList<>();
    private Paint statePaint;
    private Paint finalStatePaint;
    private Paint initialStatePaint;
    private Paint textPaint;
    private Paint arrowPaint;

    public DrawingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initPaints();
    }

    // Inicializar los estilos de los elementos del autómata
    private void initPaints() {
        // Estado normal (azul)
        statePaint = new Paint();
        statePaint.setStyle(Paint.Style.STROKE);
        statePaint.setStrokeWidth(5);
        statePaint.setColor(Color.BLUE);

        // Estado final (rojo)
        finalStatePaint = new Paint();
        finalStatePaint.setStyle(Paint.Style.STROKE);
        finalStatePaint.setStrokeWidth(5);
        finalStatePaint.setColor(Color.RED);

        // Estado inicial (verde)
        initialStatePaint = new Paint();
        initialStatePaint.setStyle(Paint.Style.STROKE);
        initialStatePaint.setStrokeWidth(8);
        initialStatePaint.setColor(Color.GREEN);

        // Texto
        textPaint = new Paint();
        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setTextSize(30);
        textPaint.setColor(Color.BLACK);

        // Flechas
        arrowPaint = new Paint();
        arrowPaint.setStyle(Paint.Style.STROKE);
        arrowPaint.setStrokeWidth(5);
        arrowPaint.setColor(Color.BLACK);
    }

    public void drawState(Estado estado, boolean isInitial, boolean isFinal, Mat mFotoOriginal) {
        // Dibujar el círculo
        if (isInitial) {
            drawCircle((float) estado.getCenter().x / 6, (float) estado.getCenter().y / 6, (float) estado.getRadius() / 8, initialStatePaint);
        } else if (isFinal) {
            drawCircle((float) estado.getCenter().x / 6, (float) estado.getCenter().y / 6, (float) estado.getRadius() / 8, finalStatePaint);
            drawCircle((float) estado.getCenter().x / 6, (float) estado.getCenter().y / 6, (float) (estado.getRadius() / 8) - 10, finalStatePaint); // Círculo interno para estado final
        } else {
            drawCircle((float) estado.getCenter().x / 6, (float) estado.getCenter().y / 6, (float) estado.getRadius() / 8, statePaint);
        }

        // Extraer el nombre del estado
        Rect roi = ExtractorDatosImagen.getROI(
                new Rect(
                        (int) (estado.center.x - estado.radius),
                        (int) (estado.center.y - estado.radius),
                        (int) (estado.radius * 2),
                        (int) (estado.radius * 2)
                ),
                mFotoOriginal.size()
        );

        Mat mFotoGrises = new Mat();
        Imgproc.cvtColor(mFotoOriginal, mFotoGrises, Imgproc.COLOR_RGB2GRAY);

        Mat subMat = new Mat(mFotoGrises, roi);
        Bitmap bitmap = Bitmap.createBitmap(subMat.width(), subMat.height(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(subMat, bitmap);

        // Llamar al método de reconocimiento de texto
        InputImage imagen = InputImage.fromBitmap(bitmap, 0);

        TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        recognizer.process(imagen)
                .addOnSuccessListener(new OnSuccessListener<Text>() {
                    @Override
                    public void onSuccess(Text result) {
                        String textoCompleto = result.getText();
                        if (textoCompleto.isEmpty()){
                            drawText("Estado", (float) (estado.getCenter().x / 6) - 15, (float) (estado.getCenter().y / 6) + 10, textPaint);
                        }else{
                            drawText(textoCompleto, (float) (estado.getCenter().x / 6) - 15, (float) (estado.getCenter().y / 6) + 10, textPaint);
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // En caso de error, devolver cadena vacía
                        drawText("Estado", (float) (estado.getCenter().x / 6) - 15, (float) (estado.getCenter().y / 6) + 10, textPaint);
                    }
                });
    }

    public void drawTransition(float startX, float startY, float endX, float endY, String value) {
        // Dibujar línea
        drawLine(startX, startY, endX, endY, arrowPaint);

        // Dibujar flecha (línea con punta)
        float arrowSize = 20;
        float angle = (float) Math.atan2(endY - startY, endX - startX);
        float arrowX1 = endX - arrowSize * (float) Math.cos(angle - Math.PI / 6);
        float arrowY1 = endY - arrowSize * (float) Math.sin(angle - Math.PI / 6);
        float arrowX2 = endX - arrowSize * (float) Math.cos(angle + Math.PI / 6);
        float arrowY2 = endY - arrowSize * (float) Math.sin(angle + Math.PI / 6);
        drawLine(endX, endY, arrowX1, arrowY1, arrowPaint);
        drawLine(endX, endY, arrowX2, arrowY2, arrowPaint);

        // Dibujar valor de la transición
        float midX = (startX + endX) / 2;
        float midY = (startY + endY) / 2;
        drawText(value, midX, midY, textPaint);
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

    // Métodos reutilizables para agregar formas
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
