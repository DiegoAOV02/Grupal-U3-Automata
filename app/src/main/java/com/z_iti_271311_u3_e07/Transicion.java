package com.z_iti_271311_u3_e07;

public class Transicion {
<<<<<<< HEAD
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
=======
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
>>>>>>> abddab2da0ba69542d0a3dda7d3a0494839b8656
}
