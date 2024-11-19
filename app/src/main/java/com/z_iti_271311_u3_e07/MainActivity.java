package com.z_iti_271311_u3_e07;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int CAMERA_REQUEST = 100;
    private ImageView imageView;
    private EditText inputString;
    private TextView tvResult;
    private Bitmap capturedImage;
    private Automata automata;
    private Uri imageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageView = findViewById(R.id.imageView);
        Button btnCapture = findViewById(R.id.btnCapture);
        inputString = findViewById(R.id.inputString);
        Button btnSimulate = findViewById(R.id.btnSimulate);
        tvResult = findViewById(R.id.tvResult);

        // Inicializar OpenCV
        if (!OpenCVLoader.initLocal()) {
            Log.e("OpenCV", "No se pudo cargar OpenCV.");
        } else {
            Log.d("OpenCV", "OpenCV cargado correctamente.");
        }

        btnCapture.setOnClickListener(v -> openCamera());
        btnSimulate.setOnClickListener(v -> simulateString());
    }

    private void openCamera() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (cameraIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(cameraIntent, CAMERA_REQUEST);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CAMERA_REQUEST && resultCode == RESULT_OK) {
            try {
                if (data != null && data.getExtras() != null) {
                    capturedImage = (Bitmap) data.getExtras().get("data");

                    if (!isImageValid(capturedImage)) {
                        Toast.makeText(this, "La imagen es demasiado oscura o no válida. Intente nuevamente.", Toast.LENGTH_LONG).show();
                        return;
                    }

                    imageView.setImageBitmap(capturedImage);
                    processAutomataImage(capturedImage);
                }
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Error al procesar la imagen: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    private boolean isImageValid(Bitmap bitmap) {
        Mat mat = new Mat();
        org.opencv.android.Utils.bitmapToMat(bitmap, mat);

        // Convertir a escala de grises
        Mat gray = new Mat();
        Imgproc.cvtColor(mat, gray, Imgproc.COLOR_BGR2GRAY);

        // Calcular el brillo promedio
        Scalar mean = org.opencv.core.Core.mean(gray);
        return mean.val[0] >= 30; // Si el brillo promedio es menor que 30, la imagen es demasiado oscura
    }

    private void processAutomataImage(Bitmap bitmap) {
        Mat mat = new Mat();
        org.opencv.android.Utils.bitmapToMat(bitmap, mat);

        // Redimensionar la imagen si es demasiado grande
        if (mat.width() > 1080 || mat.height() > 1080) {
            Mat resized = new Mat();
            Imgproc.resize(mat, resized, new Size(1080, 1080));
            mat = resized;
        }

        // Convertir a escala de grises
        Mat gray = new Mat();
        Imgproc.cvtColor(mat, gray, Imgproc.COLOR_BGR2GRAY);

        // Aplicar umbralización adaptativa
        Mat threshold = new Mat();
        Imgproc.adaptiveThreshold(gray, threshold, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY_INV, 11, 2);

        // Encontrar contornos
        List<MatOfPoint> contours = new ArrayList<>();
        try {
            Imgproc.findContours(threshold, contours, new Mat(), Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);
        } catch (Exception e) {
            Log.e("OpenCV", "Error en findContours: " + e.getMessage());
            Toast.makeText(this, "No se pudo procesar la imagen debido a un error interno.", Toast.LENGTH_LONG).show();
            return;
        }

        if (contours.isEmpty()) {
            Toast.makeText(this, "No se detectó un autómata válido", Toast.LENGTH_SHORT).show();
            return;
        }

        // Dibujar contornos detectados para depuración
        Mat drawing = Mat.zeros(threshold.size(), CvType.CV_8UC3);
        for (int i = 0; i < contours.size(); i++) {
            Imgproc.drawContours(drawing, contours, i, new Scalar(0, 255, 0), 2);
        }

        // Convertir de vuelta a Bitmap y mostrarlo
        Bitmap resultBitmap = Bitmap.createBitmap(drawing.cols(), drawing.rows(), Bitmap.Config.ARGB_8888);
        org.opencv.android.Utils.matToBitmap(drawing, resultBitmap);
        imageView.setImageBitmap(resultBitmap);

        // Llama al OCR después del preprocesamiento
        extractTextWithOCR(bitmap);
    }


    private void extractTextWithOCR(Bitmap bitmap) {
        InputImage image = InputImage.fromBitmap(bitmap, 0);
        TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

        recognizer.process(image)
                .addOnSuccessListener(this::configureAutomata)
                .addOnFailureListener(e -> Log.e("OCR", "Error al procesar la imagen", e));
    }

    private void configureAutomata(Text visionText) {
        List<Estado> estados = new ArrayList<>();
        Estado inicial = null;

        for (Text.TextBlock block : visionText.getTextBlocks()) {
            String text = block.getText();
            Log.d("OCR", "Texto detectado: " + text);

            // Detectar estados (por ejemplo, s0, s1, etc.)
            if (text.matches("s\\d+")) {
                Estado estado = new Estado(text, text.contains("1")); // Suponemos que "1" en el texto indica estado final
                estados.add(estado);

                // Determinar si es inicial (basado en posición u otros criterios)
                if (text.equals("s0")) {
                    inicial = estado;
                }
            }
        }

        if (estados.size() >= 2 && inicial != null) {
            automata = new Automata(inicial);
            for (Estado estado : estados) {
                // Aquí puedes añadir transiciones (ejemplo básico)
                estado.agregarTransicion('0', inicial); // Transiciones ficticias
                estado.agregarTransicion('1', estados.get(1));
            }
            tvResult.setText("Autómata configurado con éxito.");
        } else {
            tvResult.setText("No se pudo configurar el autómata.");
        }
    }

    private void simulateString() {
        if (automata == null) {
            Toast.makeText(this, "Primero capture un autómata", Toast.LENGTH_SHORT).show();
            return;
        }

        String input = inputString.getText().toString();
        boolean accepted = automata.simularCadena(input);

        tvResult.setText(accepted ? "Cadena aceptada" : "Cadena no aceptada");
    }
}