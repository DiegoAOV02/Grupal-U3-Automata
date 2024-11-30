package com.z_iti_271311_u3_e07;

import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ExtractorDatosImagen {
    private Bitmap imagenOriginal;
    private Context context;
<<<<<<< HEAD
    private DrawingView drawingView;
=======
    private String currentPhotoPath;

>>>>>>> abddab2da0ba69542d0a3dda7d3a0494839b8656
    //Matrices necesarias
    private Mat mFotoOriginal;
    private Mat mFotoGrises;

    //Listas de elementos
<<<<<<< HEAD
    private List<Circle> detectedCircles = new ArrayList<>();
    private List<Line> detectedLines = new ArrayList<>();
=======
    private ArrayList<Estado> estados = new ArrayList<>();
    private Estado estadoInicial = null;
    private ArrayList<Estado> estadosFinales = new ArrayList<>(); // Lista para los estados finales detectados.
    private ArrayList<Transicion> transiciones = new ArrayList<>();

    //Reconocimiento de texto
    TextRecognizer recognizer;
>>>>>>> abddab2da0ba69542d0a3dda7d3a0494839b8656

    public ExtractorDatosImagen(Context context, String currentPhotoPath, Bitmap imagenOriginal, DrawingView drawingView) {
        this.imagenOriginal = imagenOriginal;
        this.currentPhotoPath = currentPhotoPath;
        this.context = context;
        this.drawingView = drawingView;
    }

    public ArrayList<Estado> getEstados() {
        return estados;
    }

    public Estado getEstadoInicial() {
        return estadoInicial;
    }

    public ArrayList<Estado> getEstadosFinales() {
        return estadosFinales;
    }

    public ArrayList<Transicion> getTransiciones() {
        return transiciones;
    }

    public Bitmap getImagenOriginal() {
        return imagenOriginal;
    }

    public void extraerDatos(Bitmap bitmap) {
        // Procesar la imagen para detectar formas
        recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        detectAutomata(bitmap);
    }

    public boolean isAutomata() {
        if (estadoInicial != null && !estadosFinales.isEmpty() && !transiciones.isEmpty()) {
            Toast.makeText(context, "Ingresa la cadena a recorrer y empieza el recorrido", Toast.LENGTH_LONG).show();
            return true;
        } else {
            Toast.makeText(context, "No se detecto como automata, toma la foto otra vez", Toast.LENGTH_LONG).show();
        }
        return false;
    }

    public void limpiarDatos() {
        estados.clear();
        estadosFinales.clear();
        transiciones.clear();
        estadoInicial = null;
    }

    // Método para obtener la orientación de la imagen usando EXIF
    public int getExifOrientation(String imagePath) {
        try {
            ExifInterface exif = new ExifInterface(imagePath);
            return exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
        } catch (IOException e) {
            Log.e("Exif", "Error al obtener orientación EXIF", e);
            return ExifInterface.ORIENTATION_NORMAL; // Valor por defecto
        }
    }

    // Método para rotar la imagen si es necesario
    public Bitmap rotateImageIfNeeded(Bitmap bitmap, int orientation) {
        Matrix matrix = new Matrix();
        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                matrix.postRotate(90);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                matrix.postRotate(180);
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                matrix.postRotate(270);
                break;
            case ExifInterface.ORIENTATION_NORMAL:
            default:
                return bitmap; // No es necesario rotar
        }
        // Crear una nueva imagen rotada
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    private void detectAutomata(Bitmap bitmap) {
        Mat originalMat = new Mat();
        Utils.bitmapToMat(bitmap, originalMat);

        // Convertir la imagen a escala de grises
        Mat grayMat = new Mat();
        Imgproc.cvtColor(originalMat, grayMat, Imgproc.COLOR_RGB2GRAY);

        // Aplicar desenfoque para reducir ruido
        Imgproc.GaussianBlur(grayMat, grayMat, new Size(9, 9), 2);

        // Detectar bordes con Canny
        Mat edges = new Mat();
        Imgproc.Canny(grayMat, edges, 50, 150);

        // Detectar círculos
        Mat circles = new Mat();
        Imgproc.HoughCircles(grayMat, circles, Imgproc.HOUGH_GRADIENT,
                1,          // Resolución inversa del acumulador
                30,         // Distancia mínima entre centros de círculos
                100,        // Umbral superior para Canny (param1)
                50,         // Umbral acumulador para Hough (param2)
                20,         // Radio mínimo
                100         // Radio máximo
        );

<<<<<<< HEAD
        detectedCircles.clear();
        for (int i = 0; i < circles.cols(); i++) {
            double[] data = circles.get(0, i);
            if (data != null) {
                Point center = new Point(data[0], data[1]);
                int radius = (int) Math.round(data[2]);
=======
        releaseMats(blurredMat, kernel, edges);
        detectarEstados(mFotoOriginal, dilatedEdges);
        detectarTransiciones(mFotoOriginal);
>>>>>>> abddab2da0ba69542d0a3dda7d3a0494839b8656

                // Validar distancia mínima entre círculos detectados
                boolean isValid = true;
                for (Circle detected : detectedCircles) {
                    if (Math.sqrt(Math.pow(center.x - detected.center.x, 2) +
                            Math.pow(center.y - detected.center.y, 2)) < 20) {
                        isValid = false;
                        break;
                    }
                }

                if (isValid) {
                    detectedCircles.add(new Circle(center, radius));
                }
            }
        }

        // Dibujar los círculos detectados
        drawAutomata(detectedCircles, detectedLines);
    }

<<<<<<< HEAD

    private void detectarTodosLosCirculos(Mat matriz, Mat dilatedEdges) {
=======
    private void detectarTransiciones(Mat mFotoOriginal) {

    }

    private void detectarEstados(Mat matriz, Mat dilatedEdges) {
>>>>>>> abddab2da0ba69542d0a3dda7d3a0494839b8656
        // Detectar círculos con la Transformada de Hough
        Mat circles = new Mat();
        Imgproc.HoughCircles(
                dilatedEdges,
                circles,
                Imgproc.HOUGH_GRADIENT,
                1.0,
                (double) mFotoGrises.rows() / 8,
                100,  // Umbral superior para Canny
                40,   // Umbral acumulador
                80,   // Radio mínimo
                300   // Radio máximo
        );

        // Recorrerlos para guardar los datos de círculos y analizarlos
        for (int i = 0; i < circles.cols(); i++) {
            double[] data = circles.get(0, i);
            if (data == null) continue;

            Point center = new Point(data[0], data[1]);
            int radius = (int) Math.round(data[2]);

            // Crear un nuevo Estado
            Estado estado = new Estado(center, radius);

            if (!buscarIgual(estado)) {
                estados.add(estado);
                // Reconocer texto en este estado
                reconocerTexto(estado);
            }
        }

        // Identificar el estado inicial y final
        releaseMats(circles);
        detectInitialStates(estados);
        detectFinalStates(estados);
        detectEstadosNormales(estados);
    }

    private void detectEstadosNormales(List<Estado> estados) {
        // Dibujar otros estados en Azul
        for (Estado estado : estados) {
            Imgproc.circle(mFotoOriginal, estado.center, estado.radius, new Scalar(0, 0, 255), 5); // Azul
        }
    }

    private void detectFinalStates(List<Estado> estados) {
        Iterator<Estado> iteratorCircle = estados.iterator();

        while (iteratorCircle.hasNext()) {
            Estado currentEstado = iteratorCircle.next();
            boolean isFinalState = false;

            // 1. Intentar encontrar un círculo externo si el actual es un círculo interno.
            int margin = 10;
            Rect expandedROI = adjustROI(
                    new Rect(
                            (int) (currentEstado.center.x - (currentEstado.radius + margin)),
                            (int) (currentEstado.center.y - (currentEstado.radius + margin)),
                            (int) (currentEstado.radius * 2) + margin,
                            (int) (currentEstado.radius * 2) + margin
                    ),
                    mFotoOriginal.size()
            );

            Mat subMat = new Mat(mFotoGrises, expandedROI);
            Mat candidateCircles = new Mat();

            // Detectar círculos más grandes cercanos.
            Imgproc.HoughCircles(
                    subMat, candidateCircles, Imgproc.HOUGH_GRADIENT, 1,
                    (double) mFotoGrises.rows() / 8, 100, 30,
                    (int) currentEstado.radius + 10, (int) (currentEstado.radius * 2)
            );

            if (candidateCircles.cols() > 0) {
                double[] outerData = candidateCircles.get(0, 0);
                if (outerData != null) {
                    // Procesar el círculo externo detectado.
                    Point outerCenter = new Point(outerData[0] + expandedROI.x, outerData[1] + expandedROI.y);
                    double outerRadius = outerData[2];

                    // Verificar que comparte centro con el círculo interno.
                    if (Math.abs(outerCenter.x - currentEstado.center.x) < 2 &&
                            Math.abs(outerCenter.y - currentEstado.center.y) < 2) {

                        estadosFinales.add(new Estado(outerCenter, (int) outerRadius)); // Agregar a estados finales.
                        isFinalState = true;
                    }
                }
            }

            releaseMats(subMat, candidateCircles);

            // 2. Si no se encontró un círculo externo, asumir que es externo y buscar un círculo interno.
            if (!isFinalState) {
                Rect innerROI = adjustROI(
                        new Rect(
                                (int) (currentEstado.center.x - currentEstado.radius),
                                (int) (currentEstado.center.y - currentEstado.radius),
                                (int) (currentEstado.radius * 2),
                                (int) (currentEstado.radius * 2)
                        ),
                        mFotoOriginal.size()
                );

                subMat = new Mat(mFotoGrises, innerROI);
                candidateCircles = new Mat();

                // Detectar círculos más pequeños dentro del actual.
                Imgproc.HoughCircles(
                        subMat, candidateCircles, Imgproc.HOUGH_GRADIENT, 1,
                        (double) mFotoGrises.rows() / 8, 100, 30,
                        10, (int) currentEstado.radius
                );

                if (candidateCircles.cols() > 0) {
                    double[] innerData = candidateCircles.get(0, 0);
                    if (innerData != null) {
                        // Procesar el círculo interno detectado.
                        Point innerCenter = new Point(innerData[0] + innerROI.x, innerData[1] + innerROI.y);
                        double innerRadius = innerData[2];

                        // Verificar que comparte centro con el círculo externo.
                        if (Math.abs(innerCenter.x - currentEstado.center.x) < 2 &&
                                Math.abs(innerCenter.y - currentEstado.center.y) < 2) {

                            estadosFinales.add(currentEstado); // Agregar a estados finales.
                            isFinalState = true;
                        }
                    }
                }

                releaseMats(subMat, candidateCircles);
            }

            // Si se determinó que es un estado final, eliminar el círculo procesado de la lista original.
            if (isFinalState) {
                Toast.makeText(context, "Hay un estado final", Toast.LENGTH_LONG).show();
                iteratorCircle.remove();
            }
        }

        // Dibujar los estados finales en Rojo
        for (Estado finalEstado : estadosFinales) {
            Imgproc.circle(mFotoOriginal, finalEstado.center, finalEstado.radius, new Scalar(255, 0, 0), 5); // Rojo
        }

    }

    private boolean buscarIgual(Estado estado) {
        for (Estado estadoNew : estados) {
            if (estadoNew.center == estado.center && estadoNew.radius == estado.radius) {
                return true;
            }
        }
        return false;
    }

    private void detectInitialStates(List<Estado> estados) {
        // Detectar el estado inicial como el círculo con mayor grosor
        double maxThickness = 0;

        for (Estado estado : estados) {
            // Crear una máscara para calcular el grosor del borde
            Mat mask = Mat.zeros(mFotoGrises.size(), CvType.CV_8UC1);
            Imgproc.circle(mask, estado.center, estado.radius, new Scalar(255), -1); // Llena el círculo completo

            // Crear un borde para simular el contorno externo
            Mat outerMask = Mat.zeros(mFotoGrises.size(), CvType.CV_8UC1);
            Imgproc.circle(outerMask, estado.center, estado.radius + 2, new Scalar(255), -1);

            // Calcular el grosor aproximado del borde como la diferencia de áreas
            Core.subtract(outerMask, mask, mask);
            int thickness = Core.countNonZero(mask);

            // Comparar el grosor con el máximo encontrado
            if (thickness > maxThickness) {
                maxThickness = thickness;
                estadoInicial = estado;
            }

            // Liberar las máscaras
            releaseMats(mask, outerMask);
        }

        //Eliminar el estado inicial de la lista de circulos para que no pase por otro proceso
        this.estados.remove(estadoInicial);

        // Dibujar el estado inicial en verde
        if (estadoInicial != null) {
            Toast.makeText(context, "Hay un estado inicial", Toast.LENGTH_LONG).show();
            Imgproc.circle(mFotoOriginal, estadoInicial.center, estadoInicial.radius, new Scalar(0, 255, 0), 5); // Verde
        }
    }

    private void reconocerTexto(Estado estado) {
        Rect roi = adjustROI(
                new Rect(
                        (int) (estado.center.x - estado.radius),
                        (int) (estado.center.y - estado.radius),
                        (int) (estado.radius * 2),
                        (int) (estado.radius * 2)
                ),
                mFotoOriginal.size()
        );

        Mat subMat = new Mat(mFotoGrises, roi);
        Bitmap bitmap = Bitmap.createBitmap(subMat.width(), subMat.height(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(subMat, bitmap);

        InputImage image = InputImage.fromBitmap(bitmap, 0);

        recognizer.process(image)
                .addOnSuccessListener(new OnSuccessListener<Text>() {
                    @Override
                    public void onSuccess(Text text) {
                        String detectedText = text.getText();
                        if (!detectedText.isEmpty()) {
                            estado.setNombre(detectedText); // Asignar texto al estado
                            Log.d("OCR_RESULT", "Estado detectado: " + detectedText);
                        } else {
                            estado.setNombre("Texto no detectado");
                            Log.d("OCR_RESULT", "No se detectó texto en el estado.");
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e("OCR_ERROR", "Error al detectar texto en el estado.", e);
                    }
                });

        subMat.release();
    }

    public void guardarMat(Mat matriz, String nombre) {
        Bitmap bitmap = Bitmap.createBitmap(matriz.cols(), matriz.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(matriz, bitmap);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, nombre + ".jpg");
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
            values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES);

            try {
                // Obtén el URI donde guardar la imagen
                Uri uri = context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

                if (uri != null) {
                    try (OutputStream outputStream = context.getContentResolver().openOutputStream(uri)) {
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
                        Log.d("IMAGEN PICTURES", "Imagen guardada en Pictures: " + nombre);
                    } catch (IOException e) {
                        e.printStackTrace();
                        Toast.makeText(context, "Error al guardar la imagen", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(context, "No se pudo crear el URI", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(context, "Error al guardar la imagen", Toast.LENGTH_SHORT).show();
            }
        } else {
            String Ruta = Environment.getExternalStorageDirectory().getAbsolutePath();
            File dir = new File(Ruta);
            File archivoImagen = new File(dir + "/" + nombre + ".jpg");
            try (FileOutputStream out = new FileOutputStream(archivoImagen)) {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out); // Comprimir y guardar el bitmap
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        bitmap.recycle();
    }

    private Rect adjustROI(Rect roi, Size size) {
        int margin = 10; // Expandir ligeramente el ROI
        int x = Math.max(roi.x - margin, 0);
        int y = Math.max(roi.y - margin, 0);
        int width = Math.min(roi.width + 2 * margin, (int) size.width - x);
        int height = Math.min(roi.height + 2 * margin, (int) size.height - y);
        return new Rect(x, y, width, height);
    }

    private void releaseMats(Mat... mats) {
        for (Mat mat : mats) {
            if (mat != null) mat.release();
        }
    }
<<<<<<< HEAD

    // Clase para representar un círculo detectado
    class Circle {
        Point center;
        int radius;

        Circle(Point center, int radius) {
            this.center = center;
            this.radius = radius;
        }
    }
    // Clase auxiliar para líneas
    private static class Line {
        Point start;
        Point end;

        Line(Point start, Point end) {
            this.start = start;
            this.end = end;
        }
    }

    private void drawAutomata(List<Circle> circles, List<Line> lines) {
        drawingView.clear();

        // Dimensiones del DrawingView
        int viewWidth = drawingView.getWidth();
        int viewHeight = drawingView.getHeight();

        // Dimensiones de la imagen original
        int originalWidth = imagenOriginal.getWidth();
        int originalHeight = imagenOriginal.getHeight();

        // Escalas para ajustar las coordenadas
        float scaleX = (float) viewWidth / originalWidth;
        float scaleY = (float) viewHeight / originalHeight;

        // Configurar pinceles
        Paint circlePaint = new Paint();
        circlePaint.setColor(Color.BLUE);
        circlePaint.setStyle(Paint.Style.STROKE);
        circlePaint.setStrokeWidth(5);

        Paint textPaint = new Paint();
        textPaint.setColor(Color.BLACK);
        textPaint.setTextSize(40);

        Paint linePaint = new Paint();
        linePaint.setColor(Color.RED);
        linePaint.setStrokeWidth(5);

        // Dibujar círculos
        for (Circle circle : circles) {
            float adjustedX = (float) circle.center.x * scaleX;
            float adjustedY = (float) circle.center.y * scaleY;
            float adjustedRadius = circle.radius * scaleX;

            Log.d("DRAW_AUTOMATA", "Dibujando círculo en: (" + adjustedX + ", " + adjustedY + ") con radio: " + adjustedRadius);
            drawingView.drawCircle(adjustedX, adjustedY, adjustedRadius, circlePaint);

            // Dibujar texto asociado
            String stateLabel = "S" + circles.indexOf(circle);
            drawingView.drawText(stateLabel, adjustedX - 20, adjustedY + 10, textPaint);
        }

        // Dibujar líneas
        for (Line line : lines) {
            float startX = (float) line.start.x * scaleX;
            float startY = (float) line.start.y * scaleY;
            float endX = (float) line.end.x * scaleX;
            float endY = (float) line.end.y * scaleY;

            Log.d("DRAW_AUTOMATA", "Dibujando línea desde: (" + startX + ", " + startY + ") hasta: (" + endX + ", " + endY + ")");
            drawingView.drawLine(startX, startY, endX, endY, linePaint);
        }
    }



=======
>>>>>>> abddab2da0ba69542d0a3dda7d3a0494839b8656
}
