package com.z_iti_271311_u3_e07;

public class Transicion {
    private String to;
    private String from;
    private int valor;

    public Transicion(String from, String to, int valor) {
        this.from = from;
        this.to = to;
        this.valor = valor;
    }

    public String getTo() {
        return to;
    }

    public String getFrom() {
        return from;
    }

    public int getValor() {
        return valor;
    }
}
