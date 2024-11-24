package com.z_iti_271311_u3_e07;

import static org.opencv.imgproc.Imgproc.getStructuringElement;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import org.opencv.android.OpenCVLoader;
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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final int CAMERA_REQUEST_CODE = 100;
    private static final int PERMISSION_REQUEST_CODE = 200;

    private ImageView imageView; // Mostrar imagen original
    private DrawingView drawingView; // Lienzo para dibujo
    private EditText inputString; // Para ingresar cadenas
    private TextView tvResult; // Result ado de simulación
    private String currentPhotoPath;
    private Uri photoURI;

    //Matrices necesarias
    private Mat mFotoOriginal;
    private Mat mFotoGrises;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Inicializar OpenCV
        if (!OpenCVLoader.initDebug()) {
            Log.e("OpenCV", "Error al cargar OpenCV");
        }

        setContentView(R.layout.activity_main);

        imageView = findViewById(R.id.imageView);
        drawingView = findViewById(R.id.drawView);
        inputString = findViewById(R.id.inputString);
        tvResult = findViewById(R.id.tvResult);

        Button btnCapture = findViewById(R.id.btnCapture);
        Button btnSimulate = findViewById(R.id.btnSimulate);

        // Verificar y solicitar permisos al iniciar
        if (!checkAndRequestPermissions()) {
            Toast.makeText(this, "Concede los permisos necesarios para continuar.", Toast.LENGTH_SHORT).show();
        }

        // Capturar imagen
        btnCapture.setOnClickListener(v -> {
            if (checkAndRequestPermissions()) {
                openCamera();
            }
        });

        // Simular cadena en el autómata
        btnSimulate.setOnClickListener(v -> {
            String input = inputString.getText().toString();
            if (currentPhotoPath != null && !input.isEmpty()) {
                simulateAutomata(input);
            } else {
                Toast.makeText(this, "Captura una imagen y escribe una cadena.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Solicitar permisos dinámicos
    private boolean checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // Para Android 14 y superiores, solo necesitamos permiso de cámara
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.CAMERA},
                        PERMISSION_REQUEST_CODE);
                return false;
            }
            return true;
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Para Android 10 y superiores
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.CAMERA},
                        PERMISSION_REQUEST_CODE);
                return false;
            }
            return true;
        } else {
            // Para versiones anteriores a Android 10
            String[] permissions = {
                    Manifest.permission.CAMERA,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE
            };

            List<String> listPermissionsNeeded = new ArrayList<>();
            for (String permission : permissions) {
                if (ContextCompat.checkSelfPermission(this, permission)
                        != PackageManager.PERMISSION_GRANTED) {
                    listPermissionsNeeded.add(permission);
                }
            }

            if (!listPermissionsNeeded.isEmpty()) {
                ActivityCompat.requestPermissions(this,
                        listPermissionsNeeded.toArray(new String[0]),
                        PERMISSION_REQUEST_CODE);
                return false;
            }
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (allGranted) {
                // Si todos los permisos fueron concedidos, proceder con la operación
                openCamera();
            } else {
                Toast.makeText(this,
                        "Se requieren permisos para usar la cámara",
                        Toast.LENGTH_SHORT).show();

                // Mostrar diálogo explicativo si es necesario
                if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
                    // Aquí podrías mostrar un diálogo explicando por qué necesitas los permisos
                    new AlertDialog.Builder(this)
                            .setTitle("Permisos necesarios")
                            .setMessage("Esta aplicación necesita acceso a la cámara para funcionar correctamente.")
                            .setPositiveButton("Configuración", (dialog, which) -> {
                                // Abrir configuración de la app
                                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                Uri uri = Uri.fromParts("package", getPackageName(), null);
                                intent.setData(uri);
                                startActivity(intent);
                            })
                            .setNegativeButton("Cancelar", null)
                            .show();
                }
            }
        }
    }

    public void guardarMat(Mat matriz, String nombre) {
        Bitmap bitmap = Bitmap.createBitmap(matriz.cols(), matriz.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(matriz, bitmap);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // Código original para Android 14+
            //Toast.makeText(this, "Version 14", Toast.LENGTH_SHORT).show();
            File directorio = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
            File archivoImagen = new File(directorio, nombre + ".jpg");
            try (FileOutputStream out = new FileOutputStream(archivoImagen)) {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
                Toast.makeText(this, archivoImagen.getAbsolutePath(), Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Error al guardar la imagen", Toast.LENGTH_SHORT).show();
            }
        } else {
            //Toast.makeText(getApplicationContext(), "Versiones anteriores", Toast.LENGTH_LONG).show();
            // Código para Android 13 y versiones anteriores
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, nombre + ".jpg");
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Para Android 10 (Q) y superior pero menor a 14
                values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES);
            }

            Uri imageUri = this.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            if (imageUri != null) {
                try (OutputStream out = this.getContentResolver().openOutputStream(imageUri)) {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(this, "Error al guardar la imagen", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void openCamera() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Verificar si hay una aplicación de cámara disponible
        if (cameraIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Log.e("Camera", "Error al crear archivo", ex);
                Toast.makeText(this, "Error al crear archivo de imagen", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                photoURI = FileProvider.getUriForFile(
                        this,
                        "com.z_iti_271311_u3_e07.fileprovider", // Asegúrate de que coincide con el authority definido
                        photoFile
                );
                cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                cameraIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivityForResult(cameraIntent, CAMERA_REQUEST_CODE);
            } catch (IllegalArgumentException e) {
                Log.e("Camera", "Error con FileProvider", e);
                Toast.makeText(this, "Error al configurar la cámara", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "No se encontró una aplicación de cámara", Toast.LENGTH_SHORT).show();
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Crear archivo temporal en el directorio de la app
            File storageDir = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "AutomatasApp");
            if (!storageDir.exists()) {
                storageDir.mkdirs();
            }
            File tempFile = File.createTempFile(imageFileName, ".jpg", storageDir);
            currentPhotoPath = tempFile.getAbsolutePath();

            // También crear entrada en MediaStore para acceso público
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, imageFileName + ".jpg");
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
            values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/AutomatasApp");
            values.put(MediaStore.Images.Media.IS_PENDING, 1);

            Uri mediaStoreUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            if (mediaStoreUri != null) {
                // Guardar el URI para actualizar IS_PENDING después
                photoURI = mediaStoreUri;
            }

            return tempFile;
        } else {
            // Para versiones anteriores a Android 10
            File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            File imageFile = File.createTempFile(imageFileName, ".jpg", storageDir);
            currentPhotoPath = imageFile.getAbsolutePath();
            return imageFile;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CAMERA_REQUEST_CODE && resultCode == RESULT_OK) {
            // Procesar la imagen
            try {
                Bitmap bitmap = BitmapFactory.decodeFile(currentPhotoPath);

                // Obtener la orientación de la imagen desde los metadatos EXIF
                int orientation = getExifOrientation(currentPhotoPath);

                // Rotar la imagen si es necesario
                Bitmap rotatedBitmap = rotateImageIfNeeded(bitmap, orientation);

                // Procesar la imagen para detectar formas
                detectAutomata(rotatedBitmap);

//                detectCircles(rotatedBitmap);

                // Mostrar la imagen capturada (ahora con la orientación correcta)
                imageView.setImageBitmap(rotatedBitmap);
            } catch (Exception e) {
                Log.e("Camera", "Error al procesar la imagen", e);
            }
        }
    }

    // Método para obtener la orientación de la imagen usando EXIF
    private int getExifOrientation(String imagePath) {
        try {
            ExifInterface exif = new ExifInterface(imagePath);
            return exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
        } catch (IOException e) {
            Log.e("Exif", "Error al obtener orientación EXIF", e);
            return ExifInterface.ORIENTATION_NORMAL; // Valor por defecto
        }
    }

    // Método para rotar la imagen si es necesario
    private Bitmap rotateImageIfNeeded(Bitmap bitmap, int orientation) {
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

        // Listas para guardar detalles de los círculos detectados
        List<Circle> detectedCircles = new ArrayList<>();

        // Procesar los círculos detectados
        if (circles.cols() > 0) {
            for (int i = 0; i < circles.cols(); i++) {
                double[] circleParams = circles.get(0, i);
                if (circleParams == null) continue;

                Point center = new Point(circleParams[0], circleParams[1]);
                int radius = (int) Math.round(circleParams[2]);

                // Añadir a la lista de círculos detectados
                detectedCircles.add(new Circle(center, radius));

                // Dibujar el círculo detectado
                Imgproc.circle(mFotoOriginal, center, radius, new Scalar(0, 255, 0), 3);
                Imgproc.circle(mFotoOriginal, center, 3, new Scalar(255, 0, 0), 3); // Centro
            }
        }

        // Detectar círculos grandes con contornos como respaldo
        detectLargeCirclesUsingContours(dilatedEdges, detectedCircles);

        // Identificar el estado inicial y final
        detectInitialAndFinalStates(detectedCircles);

        // Guardar la imagen procesada
        guardarMat(mFotoOriginal, "automata_estados_detectados");

        // Liberar recursos
        blurredMat.release();
        edges.release();
        dilatedEdges.release();
        circles.release();
        liberarRecursos();
    }

    // Detectar el estado inicial y final
    private void detectInitialAndFinalStates(List<Circle> circles) {
        Circle initialState = null;
        Circle finalState = null;

        // Identificar el estado inicial como el círculo con mayor grosor o área
        double maxRadius = 0;
        for (Circle circle : circles) {
            if (circle.radius > maxRadius) {
                maxRadius = circle.radius;
                initialState = circle;
            }
        }

        // Identificar el estado final como un círculo con un contorno adicional
        for (Circle circle : circles) {
            for (Circle other : circles) {
                if (circle != other) {
                    double distance = Math.sqrt(Math.pow(circle.center.x - other.center.x, 2)
                            + Math.pow(circle.center.y - other.center.y, 2));
                    if (distance < Math.abs(circle.radius - other.radius) && circle.radius < other.radius) {
                        finalState = other;
                        break;
                    }
                }
            }
        }

        // Dibujar el estado inicial en azul y el estado final en rojo
        if (initialState != null) {
            Imgproc.circle(mFotoOriginal, initialState.center, initialState.radius, new Scalar(255, 0, 0), 5); // Azul
        }
        if (finalState != null) {
            Imgproc.circle(mFotoOriginal, finalState.center, finalState.radius, new Scalar(0, 0, 255), 5); // Rojo
        }
    }

    // Detectar círculos grandes usando contornos
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

    // Clase para representar un círculo detectado
    class Circle {
        Point center;
        int radius;

        Circle(Point center, int radius) {
            this.center = center;
            this.radius = radius;
        }
    }

    private void liberarRecurso(Mat recurso) {
        recurso.release();
    }

    private void liberarRecursos() {
        if (mFotoOriginal != null) mFotoOriginal.release();
        if (mFotoGrises != null) mFotoGrises.release();
    }

    private void simulateAutomata(String input) {
        // Simulando el autómata
        boolean result = input.matches("[01]+"); // Solo acepta cadenas con 0s y 1s
        tvResult.setText(result ? "Cadena válida" : "Cadena no válida");
    }
}
