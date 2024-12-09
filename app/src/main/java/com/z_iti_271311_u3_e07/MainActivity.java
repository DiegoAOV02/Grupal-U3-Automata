package com.z_iti_271311_u3_e07;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import org.opencv.core.Rect;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
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

    ExtractorDatosImagen extractorDatosImagen;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Inicializar OpenCV
        if (!OpenCVLoader.initDebug()) {
            Log.e("OpenCV", "Error al cargar OpenCV");
        }

        setContentView(R.layout.activity_main);

        imageView = findViewById(R.id.imageView);
        drawingView = findViewById(R.id.drawingView);
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
                //openCamera();
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
                        getApplicationContext().getPackageName() + ".fileprovider", // Asegúrate de que coincide con el authority definido
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
            try {
                Bitmap bitmap = BitmapFactory.decodeFile(currentPhotoPath);

                if (bitmap != null) {
                    extractorDatosImagen = new ExtractorDatosImagen(getApplicationContext(), currentPhotoPath, bitmap);

                    // Obtener la orientación de la imagen desde los metadatos EXIF
                    int orientation = extractorDatosImagen.getExifOrientation(currentPhotoPath);

                    // Rotar la imagen si es necesario
                    Bitmap rotatedBitmap = extractorDatosImagen.rotateImageIfNeeded(bitmap, orientation);

                    // Mostrar la imagen capturada en el ImageView
                    imageView.setImageBitmap(rotatedBitmap);

                    // Procesar los datos del autómata
                    extractorDatosImagen.extraerDatos(extractorDatosImagen.getImagenOriginal());

                    if (extractorDatosImagen.isAutomata()) {
                        // Crear el autómata con los datos detectados
                        Automata automata = new Automata();
                        automata.setEstadoInicial(extractorDatosImagen.getEstadoInicial());
                        automata.setListaEstadosFinales(extractorDatosImagen.getEstadosFinales());
                        automata.setListaEstadosNormales(extractorDatosImagen.getEstados());
                        automata.setListaTransiciones(extractorDatosImagen.getTransiciones());

                        // Dibujar automáticamente en el DrawingView
                        Toast.makeText(getApplicationContext(), "Termino la extraccion", Toast.LENGTH_LONG).show();
                        drawAutomata(automata, rotatedBitmap);
                        Log.d("AUTOMATA", "SE DIBUJO EL AUTOMATA");
                    } else {
                        Toast.makeText(this, "La imagen no contiene un autómata válido.", Toast.LENGTH_SHORT).show();
                    }
                }
            } catch (Exception e) {
                Log.e("Camera", "Error al procesar la imagen", e);
                Toast.makeText(getApplicationContext(), "Error al procesar la imagen", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void drawAutomata(Automata automata, Bitmap imagen) {
        drawingView.clear();
        Mat mFotoOriginal = new Mat();
        Utils.bitmapToMat(imagen, mFotoOriginal);

        // Dibujar estados
        Estado estadoInicial = automata.getEstadoInicial();

        drawingView.drawState(estadoInicial, true, false, mFotoOriginal);

        if (!automata.getListaEstadosFinales().isEmpty()) {
            for (Estado estadoFinal : automata.getListaEstadosFinales()) {
                drawingView.drawState(estadoFinal, false, true, mFotoOriginal);
            }
        } else {
            Log.d("DIBUJO", "No hay estados finales");
        }

        if (!automata.getListaEstadosNormales().isEmpty()) {
            for (Estado estadoNormal : automata.getListaEstadosNormales()) {
                drawingView.drawState(estadoNormal, false, false, mFotoOriginal);
            }
        } else {
            Log.d("DIBUJO", "No hay estados normales");
        }

        // Dibujar transiciones
        if (!automata.getListaTransiciones().isEmpty()) {
            for (Transicion transicion : automata.getListaTransiciones()) {
                Estado from = transicion.getFrom();
                Estado to = transicion.getTo();
                drawingView.drawTransition(
                        (float) from.getCenter().x + from.getRadius(),
                        (float) from.getCenter().y,
                        (float) to.getCenter().x - to.getRadius(),
                        (float) to.getCenter().y,
                        transicion.getValor()
                );
            }
        } else {
            Log.d("DIBUJO", "No hay transiciones");
        }
    }

    private void simulateAutomata(String input) {
        // Simulando el autómata
        boolean result = input.matches("[01]+"); // Solo acepta cadenas con 0s y 1s
        tvResult.setText(result ? "Cadena válida" : "Cadena no válida");
    }
}