package com.z_iti_271311_u3_e07;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Recorrido {
    private Automata automata;

    public Recorrido(Automata automata) {
        this.automata = automata;
    }

    public List<String> simular(String cadena) {
        List<String> recorrido = new ArrayList<>();
        Estado estadoActual = automata.getEstadoInicial();
        recorrido.add(estadoActual.getNombre());

        for (char c : cadena.toCharArray()) {
            String valor = String.valueOf(c);
            boolean transicionEncontrada = false;

            for (Transicion transicion : automata.getListaTransiciones()) {
                if (transicion.getFrom().equals(estadoActual) && transicion.getValor().equals(valor)) {
                    estadoActual = transicion.getTo();
                    recorrido.add(estadoActual.getNombre());
                    transicionEncontrada = true;
                    break;
                }
            }

            if (!transicionEncontrada) {
                return null;
            }
        }

        // Validar si termina en un estado final
        if (automata.getListaEstadosFinales().contains(estadoActual)) {
            return recorrido;
        }

        return null; // No termina en un estado final
    }
}
