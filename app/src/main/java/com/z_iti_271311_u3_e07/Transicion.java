package com.z_iti_271311_u3_e07;

public class Transicion {
    private Estado to;
    private Estado from;
    private String valor;

    public Transicion(Estado from, Estado to, String valor) {
        this.from = from;
        this.to = to;
        this.valor = valor;
    }

    public Estado getFrom() {
        return from;
    }

    public void setFrom(Estado from) {
        this.from = from;
    }

    public Estado getTo() {
        return to;
    }

    public void setTo(Estado to) {
        this.to = to;
    }

    public String getValor() {
        return valor;
    }

    public void setValor(String valor) {
        this.valor = valor;
    }
}
