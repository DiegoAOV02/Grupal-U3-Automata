package com.z_iti_271311_u3_e07;

import android.content.Context;
import android.graphics.Bitmap;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

public class ReconocerTexto {
    private String textoReconocido = "";

    // Getter para textoReconocido
    public String getTextoReconocido() {
        return textoReconocido;
    }

    // Setter para textoReconocido
    public void setTextoReconocido(String texto) {
        this.textoReconocido = texto;
    }

    // Método para reconocer texto desde un Bitmap
    public void reconocerTexto(Context context, Bitmap bitmap, TextRecognitionCallback callback) {
        try {
            // Convertir Bitmap a InputImage
            InputImage imagen = InputImage.fromBitmap(bitmap, 0);

            TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

            recognizer.process(imagen)
                    .addOnSuccessListener(new OnSuccessListener<Text>() {
                        @Override
                        public void onSuccess(Text result) {
                            // Obtener todo el texto reconocido
                            String textoCompleto = result.getText();

                            // Establecer el texto reconocido
                            setTextoReconocido(textoCompleto);

                            // Llamar al callback con el texto reconocido
                            callback.onTextRecognized(textoCompleto);
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            // En caso de error, devolver cadena vacía
                            setTextoReconocido("");

                            // Llamar al callback con cadena vacía
                            callback.onTextRecognized("");
                        }
                    });
        } catch (Exception e) {
            e.printStackTrace();

            // En caso de error al procesar el Bitmap, devolver cadena vacía
            setTextoReconocido("");

            // Llamar al callback con cadena vacía
            callback.onTextRecognized("");
        }
    }

    // Interfaz de callback para manejar el resultado del reconocimiento
    public interface TextRecognitionCallback {
        void onTextRecognized(String texto);
    }
}
