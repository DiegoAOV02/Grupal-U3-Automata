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
        // Obtener la orientación de la imagen desde los metadatos EXIF
        int orientation = getExifOrientation(currentPhotoPath);

        // Rotar la imagen si es necesario
        Bitmap rotatedBitmap = rotateImageIfNeeded(bitmap, orientation);

        // Procesar la imagen para detectar formas
        detectAutomata(rotatedBitmap);
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

        if (circles.cols() > 0) {
            drawDetectedCircles(circles, mFotoOriginal, dilatedEdges);
        }

        guardarMat(mFotoOriginal, "imagen_resultante");
        releaseMats(mFotoOriginal, mFotoGrises, circles);
    }

    private void drawDetectedCircles(Mat circles, Mat matriz, Mat dilatedEdges) {
        for (int i = 0; i < circles.cols(); i++) {
            double[] data = circles.get(0, i);
            if (data == null) continue;

            Point center = new Point(data[0], data[1]);
            int radius = (int) Math.round(data[2]);

            // Añadir a la lista de círculos detectados
            detectedCircles.add(new Circle(center, radius));

            Imgproc.circle(matriz, center, (int) radius, new Scalar(0, 255, 0), 2);
        }

        // Detectar círculos grandes con contornos como respaldo
        detectLargeCirclesUsingContours(dilatedEdges, detectedCircles);
        // Identificar el estado inicial y final
        detectInitialAndFinalStates(detectedCircles);
    }

    public void guardarMat(Mat matriz, String nombre) {
        Bitmap bitmap = Bitmap.createBitmap(matriz.cols(), matriz.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(matriz, bitmap);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            Toast.makeText(context, "version 14", Toast.LENGTH_LONG).show();
            File directorio = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
            File archivoImagen = new File(directorio, nombre + ".jpg");

            try (FileOutputStream out = new FileOutputStream(archivoImagen)) {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
                Toast.makeText(context, archivoImagen.getAbsolutePath(), Toast.LENGTH_SHORT).show();

            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(context, "Error al guardar la imagen", Toast.LENGTH_SHORT).show();
            }
        } else {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, nombre + ".jpg");
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Para Android 10 (Q) y superior pero menor a 14
                values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES);
            }

            Uri imageUri = context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            if (imageUri != null) {
                try (OutputStream out = context.getContentResolver().openOutputStream(imageUri)) {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(context, "Error al guardar la imagen", Toast.LENGTH_SHORT).show();
                }
            }
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

    private void detectInitialAndFinalStates(List<Circle> circles) {
        Circle initialState = null;
        Circle finalState = null;
        List<Circle> detectedFinalStates = new ArrayList<>(); // Para soportar múltiples estados finales.

        // Detectar el estado inicial como el círculo con mayor densidad de contorno (mayor radio)
        double maxRadius = 0;
        Iterator<Circle> iteratorCircle = circles.iterator();
        while (iteratorCircle.hasNext()) {
            Circle circle = iteratorCircle.next();
            if (circle.radius > maxRadius) {
                maxRadius = circle.radius;
                initialState = circle;
                //Eliminar este circulo de la lista
                iteratorCircle.remove();
            }
        }

        // Detectar estados finales como círculos concéntricos
        while (iteratorCircle.hasNext()) {
            Circle outerCircle = iteratorCircle.next();
            Rect roi = adjustROI(new Rect((int) (outerCircle.center.x - outerCircle.radius), (int) (outerCircle.center.y - outerCircle.radius), (int) outerCircle.radius * 2, (int) outerCircle.radius * 2), mFotoOriginal.size());
            Mat subMat = new Mat(mFotoGrises, roi);

            Mat innerCircles = new Mat();
            Imgproc.HoughCircles(subMat, innerCircles, Imgproc.HOUGH_GRADIENT, 1, (double) mFotoGrises.rows() / 8, 100, 30, 30, (int) outerCircle.radius);

            if (innerCircles.cols() > 0) {
                //Solo para el primer circulo que encuentre, ya que solo ocupamos un circulo despues del circulo externo
                double[] innerData = innerCircles.get(0, 1);
                if (innerData == null) continue;
                Point innerCenter = new Point(innerData[0] + roi.x, innerData[1] + roi.y);
                Imgproc.circle(mFotoOriginal, innerCenter, (int) innerData[2], new Scalar(255, 0, 0), 2);

                if (!detectedFinalStates.contains(outerCircle)) {
                    detectedFinalStates.add(outerCircle);
                    iteratorCircle.remove();
                }
            }
            releaseMats(subMat, innerCircles);
        }

        // Dibujar el estado inicial en verde
        if (initialState != null) {
            Imgproc.circle(mFotoOriginal, initialState.center, initialState.radius, new Scalar(0, 255, 0), 5); // Verde
            Imgproc.putText(mFotoOriginal, "Inicial", new Point(initialState.center.x - 20, initialState.center.y - 20),
                    Imgproc.FONT_HERSHEY_SIMPLEX, 1, new Scalar(0, 255, 0), 2);
        }

        // Dibujar los estados finales en Rojo
        for (Circle finalCircle : detectedFinalStates) {
            Imgproc.circle(mFotoOriginal, finalCircle.center, finalCircle.radius, new Scalar(255, 0, 0), 5); // Rojo
            Imgproc.putText(mFotoOriginal, "Final", new Point(finalCircle.center.x - 20, finalCircle.center.y - 20),
                    Imgproc.FONT_HERSHEY_SIMPLEX, 1, new Scalar(255, 0, 0), 2);
        }

        // Dibujar otros estados en Azul
        for (Circle circle : circles) {
            if (!circle.equals(initialState) && !detectedFinalStates.contains(circle)) {
                Imgproc.circle(mFotoOriginal, circle.center, circle.radius, new Scalar(0, 0, 255), 5); // Azul
                Imgproc.putText(mFotoOriginal, "Estado", new Point(circle.center.x - 20, circle.center.y - 20),
                        Imgproc.FONT_HERSHEY_SIMPLEX, 1, new Scalar(0, 0, 255), 2);
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
