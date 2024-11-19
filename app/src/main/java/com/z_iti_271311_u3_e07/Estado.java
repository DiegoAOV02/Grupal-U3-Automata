package com.z_iti_271311_u3_e07;

import java.util.ArrayList;
import java.util.List;

class Estado {
    String nombre;
    boolean esFinal;
    List<Transicion> transiciones = new ArrayList<>();

    public Estado(String nombre, boolean esFinal) {
        this.nombre = nombre;
        this.esFinal = esFinal;
    }

    public void agregarTransicion(char simbolo, Estado destino) {
        transiciones.add(new Transicion(simbolo, destino));
    }

    public Estado transicion(char simbolo) {
        for (Transicion t : transiciones) {
            if (t.simbolo == simbolo) return t.destino;
        }
        return null;
    }
}

