package com.automatas;

import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
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
    private String currentPhotoPath;
    private Context context;

    //Matrices necesarias
    private Mat mFotoOriginal;
    private Mat mFotoGrises;

    //Listas de elementos
    List<Circle> detectedCircles = new ArrayList<>();

    public ExtractorDatosImagen(Context context, String currentPhotoPath, Bitmap imagenOriginal) {
        this.imagenOriginal = imagenOriginal;
        this.currentPhotoPath = currentPhotoPath;
        this.context = context;
    }

    public Bitmap getImagenOriginal() {
        return imagenOriginal;
    }

    public void extraerDatos(Bitmap bitmap){
        // Procesar la imagen para detectar formas
        detectAutomata(bitmap);
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
        // Convertir el bitmap a Mat
        mFotoOriginal = new Mat();
        Utils.bitmapToMat(bitmap, mFotoOriginal);

        // Convertir la imagen a escala de grises
        mFotoGrises = new Mat();
        Imgproc.cvtColor(mFotoOriginal, mFotoGrises, Imgproc.COLOR_RGB2GRAY);

        // Aplicar desenfoque para reducir ruido y destacar bordes
        Mat blurredMat = new Mat();
        Imgproc.GaussianBlur(mFotoGrises, blurredMat, new Size(9, 9), 2);

        // Detectar bordes con Canny
        Mat edges = new Mat();
        Imgproc.Canny(blurredMat, edges, 50, 150);

        // Dilatar bordes para reforzar contornos
        Mat dilatedEdges = new Mat();
        Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(5, 5));
        Imgproc.dilate(edges, dilatedEdges, kernel);

        releaseMats(blurredMat, kernel, edges);
        detectarTodosLosCirculos(mFotoOriginal, dilatedEdges);

        guardarMat(mFotoOriginal, "imagen_resultante");
        releaseMats(mFotoOriginal, mFotoGrises, dilatedEdges);
    }

    private void detectarTodosLosCirculos(Mat matriz, Mat dilatedEdges) {
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

        //Recorrerlos para guardar los datos de circulos y analizarlos
        for (int i = 0; i < circles.cols(); i++) {
            double[] data = circles.get(0, i);
            if (data == null) continue;

            Point center = new Point(data[0], data[1]);
            int radius = (int) Math.round(data[2]);

            // Añadir a la lista de círculos detectados
            Circle circle = new Circle(center, radius);
            if (!buscarIgual(circle)){
                detectedCircles.add(circle);
                Imgproc.circle(matriz, center, (int) radius, new Scalar(0, 255, 0), 2);
            }
        }

        guardarMat(matriz, "circulos_iniciales");

        // Identificar el estado inicial y final
        releaseMats(circles);
        //detectLargeCirclesUsingContours(dilatedEdges, detectedCircles);
        detectInitialStates(detectedCircles);
        detectFinalStates(detectedCircles);
    }

    private void detectFinalStates(List<Circle> circles) {
        List<Circle> detectedFinalStates = new ArrayList<>(); // Lista para los estados finales detectados.
        Iterator<Circle> iteratorCircle = circles.iterator();

        while (iteratorCircle.hasNext()) {
            Circle currentCircle = iteratorCircle.next();
            boolean isFinalState = false;

            // 1. Intentar encontrar un círculo externo si el actual es un círculo interno.
            Rect expandedROI = adjustROI(
                    new Rect(
                            (int) (currentCircle.center.x - currentCircle.radius),
                            (int) (currentCircle.center.y - currentCircle.radius),
                            (int) (currentCircle.radius * 2),
                            (int) (currentCircle.radius * 2)
                    ),
                    mFotoOriginal.size()
            );

            Mat subMat = new Mat(mFotoGrises, expandedROI);
            guardarMat(subMat, "candidatosfinales");
            Mat candidateCircles = new Mat();

            // Detectar círculos más grandes cercanos.
            Imgproc.HoughCircles(
                    subMat, candidateCircles, Imgproc.HOUGH_GRADIENT, 1,
                    (double) mFotoGrises.rows() / 8, 100, 30,
                    (int) currentCircle.radius + 10, (int) (currentCircle.radius * 2)
            );

            if (candidateCircles.cols() > 0) {
                double[] outerData = candidateCircles.get(0, 0);
                if (outerData != null) {
                    // Procesar el círculo externo detectado.
                    Point outerCenter = new Point(outerData[0] + expandedROI.x, outerData[1] + expandedROI.y);
                    double outerRadius = outerData[2];

                    // Verificar que comparte centro con el círculo interno.
                    if (Math.abs(outerCenter.x - currentCircle.center.x) < 2 &&
                            Math.abs(outerCenter.y - currentCircle.center.y) < 2) {

                        // Dibujar ambos círculos.
                        //Imgproc.circle(mFotoOriginal, currentCircle.center, (int) currentCircle.radius, new Scalar(255, 0, 0), 2);
                        //Imgproc.circle(mFotoOriginal, outerCenter, (int) outerRadius, new Scalar(0, 255, 0), 2);

                        detectedFinalStates.add(new Circle(outerCenter, (int) outerRadius)); // Agregar a estados finales.
                        isFinalState = true;
                    }
                }
            }

            releaseMats(subMat, candidateCircles);

            // 2. Si no se encontró un círculo externo, asumir que es externo y buscar un círculo interno.
            if (!isFinalState) {
                Rect innerROI = adjustROI(
                        new Rect(
                                (int) (currentCircle.center.x - currentCircle.radius),
                                (int) (currentCircle.center.y - currentCircle.radius),
                                (int) (currentCircle.radius * 2),
                                (int) (currentCircle.radius * 2)
                        ),
                        mFotoOriginal.size()
                );

                subMat = new Mat(mFotoGrises, innerROI);
                candidateCircles = new Mat();

                // Detectar círculos más pequeños dentro del actual.
                Imgproc.HoughCircles(
                        subMat, candidateCircles, Imgproc.HOUGH_GRADIENT, 1,
                        (double) mFotoGrises.rows() / 8, 100, 30,
                        10, (int) currentCircle.radius
                );

                if (candidateCircles.cols() > 0) {
                    double[] innerData = candidateCircles.get(0, 0);
                    if (innerData != null) {
                        // Procesar el círculo interno detectado.
                        Point innerCenter = new Point(innerData[0] + innerROI.x, innerData[1] + innerROI.y);
                        double innerRadius = innerData[2];

                        // Verificar que comparte centro con el círculo externo.
                        if (Math.abs(innerCenter.x - currentCircle.center.x) < 2 &&
                                Math.abs(innerCenter.y - currentCircle.center.y) < 2) {

                            // Dibujar ambos círculos.
                            //Imgproc.circle(mFotoOriginal, currentCircle.center, (int) currentCircle.radius, new Scalar(0, 255, 0), 2);
                            //Imgproc.circle(mFotoOriginal, innerCenter, (int) innerRadius, new Scalar(255, 0, 0), 2);

                            detectedFinalStates.add(currentCircle); // Agregar a estados finales.
                            isFinalState = true;
                        }
                    }
                }

                releaseMats(subMat, candidateCircles);
            }

            // 3. Si se determinó que es un estado final, eliminar el círculo procesado de la lista original.
            if (isFinalState) {
                Toast.makeText(context, "Hay un estado final", Toast.LENGTH_LONG).show();
                iteratorCircle.remove();
            }
        }

        // Dibujar los estados finales en Rojo
        for (Circle finalCircle : detectedFinalStates) {
            Imgproc.circle(mFotoOriginal, finalCircle.center, finalCircle.radius, new Scalar(255, 0, 0), 5); // Rojo
            Imgproc.putText(mFotoOriginal, "Final", new Point(finalCircle.center.x - 20, finalCircle.center.y - 20),
                    Imgproc.FONT_HERSHEY_SIMPLEX, 1, new Scalar(255, 0, 0), 2);
        }
    }


    private void detectLargeCirclesUsingContours(Mat edges, List<Circle> circles) {
        List<MatOfPoint> contours = new ArrayList<>();
        Imgproc.findContours(edges, contours, new Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        for (MatOfPoint contour : contours) {
            MatOfPoint2f contour2f = new MatOfPoint2f(contour.toArray());
            double perimeter = Imgproc.arcLength(contour2f, true);
            double area = Imgproc.contourArea(contour);

            // Filtrar por área y circularidad
            if (area > 10000 && area < 200000) { // Detectar círculos grandes
                double circularity = (4 * Math.PI * area) / (perimeter * perimeter);
                if (circularity > 0.7) { // Confirmar forma circular
                    Rect boundingBox = Imgproc.boundingRect(contour);
                    Point center = new Point(
                            boundingBox.x + boundingBox.width / 2.0,
                            boundingBox.y + boundingBox.height / 2.0
                    );
                    int radius = (int) (boundingBox.width / 2.0);

                    // Añadir a la lista de círculos
                    circles.add(new Circle(center, radius));

                    // Dibujar el círculo detectado
                    Imgproc.circle(mFotoOriginal, center, radius, new Scalar(0, 255, 255), 3);
                }
            }
            contour.release();
        }
    }

    private boolean buscarIgual(Circle circle) {
        for (Circle circleNew : detectedCircles){
            if (circleNew.center == circle.center && circleNew.radius == circle.radius){
                return true;
            }
        }
        return false;
    }

    private void detectInitialStates(List<Circle> circles) {
        Circle initialState = null;

        // Detectar el estado inicial como el círculo con mayor grosor
        double maxThickness = 0;

        for (Circle circle : circles) {
            // Crear una máscara para calcular el grosor del borde
            Mat mask = Mat.zeros(mFotoGrises.size(), CvType.CV_8UC1);
            Imgproc.circle(mask, circle.center, circle.radius, new Scalar(255), -1); // Llena el círculo completo

            // Crear un borde para simular el contorno externo
            Mat outerMask = Mat.zeros(mFotoGrises.size(), CvType.CV_8UC1);
            Imgproc.circle(outerMask, circle.center, circle.radius + 2, new Scalar(255), -1);

            // Calcular el grosor aproximado del borde como la diferencia de áreas
            Core.subtract(outerMask, mask, mask);
            int thickness = Core.countNonZero(mask);

            // Comparar el grosor con el máximo encontrado
            if (thickness > maxThickness) {
                maxThickness = thickness;
                initialState = circle;
            }

            // Liberar las máscaras
            releaseMats(mask, outerMask);
        }

        //Eliminar el estado inicial de la lista de circulos para que no pase por otro proceso
        detectedCircles.remove(initialState);

        // Dibujar el estado inicial en verde
        if (initialState != null) {
            Toast.makeText(context, "Hay un estado inicial", Toast.LENGTH_LONG).show();
            Imgproc.circle(mFotoOriginal, initialState.center, initialState.radius, new Scalar(0, 255, 0), 5); // Verde
            Imgproc.putText(mFotoOriginal, "Inicial", new Point(initialState.center.x - 20, initialState.center.y - 20),
                    Imgproc.FONT_HERSHEY_SIMPLEX, 1, new Scalar(0, 255, 0), 2);
        }

        /*

        // Dibujar otros estados en Azul
        for (Circle circle : circles) {
            if (!circle.equals(initialState) && !detectedFinalStates.contains(circle)) {
                Imgproc.circle(mFotoOriginal, circle.center, circle.radius, new Scalar(0, 0, 255), 5); // Azul
                Imgproc.putText(mFotoOriginal, "Estado", new Point(circle.center.x - 20, circle.center.y - 20),
                        Imgproc.FONT_HERSHEY_SIMPLEX, 1, new Scalar(0, 0, 255), 2);
            }
        }*/
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
                        Toast.makeText(context, "Imagen guardada en Pictures", Toast.LENGTH_SHORT).show();
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
    }

    private Rect adjustROI(Rect roi, Size size) {
        int x = Math.max(roi.x, 0);
        int y = Math.max(roi.y, 0);
        int width = Math.min(roi.width, (int) size.width - x);
        int height = Math.min(roi.height, (int) size.height - y);
        return new Rect(x, y, width, height);
    }

    private void releaseMats(Mat... mats) {
        for (Mat mat : mats) {
            if (mat != null) mat.release();
        }
    }

    // Clase para representar un círculo detectado
    class Circle {
        Point center;
        int radius;

        Circle(Point center, int radius) {
            this.center = center;
            this.radius = radius;
        }
    }
}
