package com.z_iti_271311_u3_e07;

class Automata {
    private final Estado inicial;

    public Automata(Estado inicial) {
        this.inicial = inicial;
    }

    public boolean simularCadena(String cadena) {
        Estado actual = inicial;
        for (char c : cadena.toCharArray()) {
            actual = actual.transicion(c);
            if (actual == null) return false;
        }
        return actual.esFinal;
    }
}

class Transicion {
    char simbolo;
    Estado destino;

    public Transicion(char simbolo, Estado destino) {
        this.simbolo = simbolo;
        this.destino = destino;
    }
}

