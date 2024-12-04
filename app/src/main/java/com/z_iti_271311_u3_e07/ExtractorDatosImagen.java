package com.z_iti_271311_u3_e07;

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

import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
//import com.googlecode.tesseract.android.TessBaseAPI;

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
    private Context context;
    private String currentPhotoPath;
//    TessBaseAPI tessBaseAPI;
    //Reconocimiento de texto
    TextRecognizer recognizer;

    //Matrices necesarias
    private Mat mFotoOriginal;
    private Mat mFotoGrises;
    final String[] detectedText = new String[1];

    //Listas de elementos
    private ArrayList<Estado> estados = new ArrayList<>();
    private Estado estadoInicial = null;
    private ArrayList<Estado> estadosFinales = new ArrayList<>(); // Lista para los estados finales detectados.
    private ArrayList<Transicion> transiciones = new ArrayList<>();

    public ExtractorDatosImagen(Context context, String currentPhotoPath, Bitmap imagenOriginal) {
        this.imagenOriginal = imagenOriginal;
        this.currentPhotoPath = currentPhotoPath;
        this.context = context;
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
        //Inicializar tessbase
//        tessBaseAPI = new TessBaseAPI();
//        tessBaseAPI.init(context.getFilesDir() + "/tesseract/", "eng");
        recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        detectAutomata(bitmap);
    }

    public boolean isAutomata() {
        if (estadoInicial != null && !estadosFinales.isEmpty() /*&& !transiciones.isEmpty()*/) {
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

        detectarEstados(mFotoOriginal.clone(), dilatedEdges);
        detectarTransiciones(mFotoOriginal.clone());

        releaseMats(mFotoOriginal, mFotoGrises, dilatedEdges);
    }

    private void procesarBitmapReconocer(Estado estado, TextRecognitionCallback callback) {
        Rect roi = getROI(
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
// Crear instancia de ReconocerTexto
        ReconocerTexto reconocerTexto = new ReconocerTexto();

        // Llamar al método de reconocimiento de texto
        reconocerTexto.reconocerTexto(context, bitmap, new ReconocerTexto.TextRecognitionCallback() {
            @Override
            public void onTextRecognized(String texto) {
                if (!texto.isEmpty()) {
                    // Texto reconocido no está vacío
                    guardarDato(estado, texto);
                } else {
                    // No se reconoció texto
                    guardarDato(estado, "Estado");
                }
                Log.d("OCR_RESULT_BITMAP", "Texto detectado: " + texto);
//                callback.onTextRecognized(estado);
            }
        });
        subMat.release();
    }

    private void guardarDato(Estado estado, String texto) {
        estado.setNombre(texto);
        Log.d("GUARDAR DATO:", "Estado: " + estado + " Nombre: " + texto);
    }

    private void detectarTransiciones(Mat mFotoOriginal) {
        // Aplicar desenfoque para reducir ruido y destacar bordes
        Mat blurredMat = new Mat();
        Imgproc.GaussianBlur(mFotoOriginal, blurredMat, new Size(9, 9), 2);

        // Detectar bordes con Canny
        Mat edges = new Mat();
        Imgproc.Canny(blurredMat, edges, 50, 150);

        // Dilatar bordes para reforzar contornos
        Mat dilatedEdges = new Mat();
        Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(5, 5));
        Imgproc.dilate(edges, dilatedEdges, kernel);

        // Detección de líneas rectas usando HoughLinesP
        Mat lines = new Mat();
        Imgproc.HoughLinesP(dilatedEdges, lines, 1, Math.PI / 180, 50, 120, 10);
        for (int i = 0; i < lines.rows(); i++) {
            double[] puntos = lines.get(i, 0);
            Point p1 = new Point(puntos[0], puntos[1]);
            Point p2 = new Point(puntos[2], puntos[3]);
            // Dibuja las líneas rectas detectadas
            Imgproc.line(mFotoOriginal, p1, p2, new Scalar(255, 0, 0), 2);
        }

        // Detección de líneas curvas (bordes de flechas circulares o de retorno)
        MatOfPoint2f approxCurve = new MatOfPoint2f();
        List<MatOfPoint> contornos = new ArrayList<>();
        Mat jerarquia = new Mat();
        Imgproc.findContours(edges, contornos, jerarquia, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);
        for (MatOfPoint contorno : contornos) {
            MatOfPoint2f contorno2f = new MatOfPoint2f(contorno.toArray());
            double perimetro = Imgproc.arcLength(contorno2f, true);
            Imgproc.approxPolyDP(contorno2f, approxCurve, 0.02 * perimetro, true);

            // Filtro para curvas cerradas (ej., flechas de retorno)
            if (Imgproc.contourArea(contorno) > 100 && approxCurve.total() > 4) {
                Rect rect = Imgproc.boundingRect(new MatOfPoint(approxCurve.toArray()));
                Imgproc.rectangle(mFotoOriginal, rect.tl(), rect.br(), new Scalar(0, 255, 0), 2);
            }
            contorno2f.release();
        }

        // Liberar memoria de matrices para evitar fugas
        guardarMat(mFotoOriginal, "lineas");

        approxCurve.release();
        for (MatOfPoint contorno : contornos) {
            contorno.release();
        }
        releaseMats(mFotoOriginal, blurredMat, edges, dilatedEdges, lines, jerarquia);
    }

    private void detectarEstados(Mat matriz, Mat dilatedEdges) {
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
                100,   // Radio mínimo
                500   // Radio máximo
        );

        //Recorrerlos para guardar los datos de circulos y analizarlos
        for (int i = 0; i < circles.cols(); i++) {
            double[] data = circles.get(0, i);
            if (data == null) continue;

            Point center = new Point(data[0], data[1]);
            int radius = (int) Math.round(data[2]);

            Estado estado = new Estado(center, radius);

            if (!buscarIgual(estado)) {
                procesarBitmapReconocer(estado, new TextRecognitionCallback() {
                    @Override
                    public void onTextRecognized(Estado estadoReconocido) {
                        Log.d("OCR_RESULT_GET", "Texto detectado: " + estadoReconocido.getNombre());
                        estados.add(estadoReconocido);
                    }
                });
            }
        }

        // Identificar el estado inicial y final
        detectInitialStates(matriz, estados);
        detectFinalStates(matriz, estados);
        detectEstadosNormales(matriz, estados);

        guardarMat(matriz, "imagen_resultante");
        releaseMats(circles, matriz);
    }

    private void detectEstadosNormales(Mat mfoto, List<Estado> estados) {
        // Dibujar otros estados en Azul
        for (Estado estado : estados) {
            Imgproc.circle(mfoto, estado.center, estado.radius, new Scalar(0, 0, 255), 5); // Azul
        }
    }

    private void detectFinalStates(Mat mfoto, List<Estado> estados) {
        Iterator<Estado> iteratorCircle = estados.iterator();

        while (iteratorCircle.hasNext()) {
            Estado currentEstado = iteratorCircle.next();
            boolean isFinalState = false;

            //Intentar encontrar un círculo externo si el actual es un círculo interno.
            int margin = 50;
            Rect expandedROI = getROI(
                    new Rect(
                            (int) (currentEstado.center.x - currentEstado.radius - margin),
                            (int) (currentEstado.center.y - currentEstado.radius - margin),
                            (int) (currentEstado.radius * 2) + (margin * 2),
                            (int) (currentEstado.radius * 2) + (margin * 2)
                    ),
                    mfoto.size()
            );

            Mat subMat = new Mat(mFotoGrises, expandedROI);

            // Aplicar desenfoque para reducir ruido y destacar bordes
            Mat blurredMat = new Mat();
            Imgproc.GaussianBlur(subMat, blurredMat, new Size(9, 9), 2);

            // Detectar bordes con Canny
            Mat edges = new Mat();
            Imgproc.Canny(blurredMat, edges, 50, 150);

            // Dilatar bordes para reforzar contornos
            Mat dilatedEdges = new Mat();
            Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(5, 5));
            Imgproc.dilate(edges, dilatedEdges, kernel);

            Mat candidateCircles = new Mat();
            // Detectar círculos cercanos.
            Imgproc.HoughCircles(
                    dilatedEdges, candidateCircles, Imgproc.HOUGH_GRADIENT, 1,
                    (double) mFotoGrises.rows() / 8, 100, 30,
                    (int) currentEstado.radius * 2, (int) currentEstado.radius * 3
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

            releaseMats(subMat, candidateCircles, blurredMat, edges, dilatedEdges);

            // Si no se encontró un círculo externo, asumir que es externo y buscar un círculo interno.
            if (!isFinalState) {
                Rect innerROI = getROI(
                        new Rect(
                                (int) (currentEstado.center.x - currentEstado.radius),
                                (int) (currentEstado.center.y - currentEstado.radius),
                                (int) (currentEstado.radius * 2),
                                (int) (currentEstado.radius * 2)
                        ),
                        mfoto.size()
                );

                subMat = new Mat(mFotoGrises, innerROI);

                // Aplicar desenfoque para reducir ruido y destacar bordes
                blurredMat = new Mat();
                Imgproc.GaussianBlur(subMat, blurredMat, new Size(9, 9), 2);

                // Detectar bordes con Canny
                edges = new Mat();
                Imgproc.Canny(blurredMat, edges, 50, 150);

                // Dilatar bordes para reforzar contornos
                dilatedEdges = new Mat();
                kernel = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(5, 5));
                Imgproc.dilate(edges, dilatedEdges, kernel);
                candidateCircles = new Mat();

                // Detectar círculos más pequeños dentro del actual.
                Imgproc.HoughCircles(
                        subMat, candidateCircles, Imgproc.HOUGH_GRADIENT, 1,
                        (double) mFotoGrises.rows() / 8, 100, 30,
                        10, (int) currentEstado.radius - 2
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

                releaseMats(subMat, candidateCircles, blurredMat, edges, dilatedEdges);
            }

            // Si se determinó que es un estado final, eliminar el círculo procesado de la lista original.
            if (isFinalState) {
                Toast.makeText(context, "Hay un estado final", Toast.LENGTH_LONG).show();
                iteratorCircle.remove();
            }
        }

        // Dibujar los estados finales en Rojo
        for (Estado finalEstado : estadosFinales) {
            Imgproc.circle(mfoto, finalEstado.center, finalEstado.radius, new Scalar(255, 0, 0), 5); // Rojo
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

    private void detectInitialStates(Mat mfoto, List<Estado> estados) {
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
            Imgproc.circle(mfoto, estadoInicial.center, estadoInicial.radius, new Scalar(0, 255, 0), 5); // Verde
        }
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

    private Rect getROI(Rect roi, Size size) {
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

    // Interfaz callback
    public interface TextRecognitionCallback {
        void onTextRecognized(Estado estado);
    }

    // Callback para manejar texto reconocido
    interface OCRCallback {
        void onTextRecognized(String text);

        void onError(Exception e);
    }
}