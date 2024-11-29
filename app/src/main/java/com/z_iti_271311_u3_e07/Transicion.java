package com.automatas;

public class Transicion {
    private Estado to;
    private Estado from;
    private String valor;

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
