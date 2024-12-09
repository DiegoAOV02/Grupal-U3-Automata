package com.z_iti_271311_u3_e07;

import androidx.annotation.NonNull;

import org.opencv.core.Point;

import java.util.ArrayList;
import java.util.List;

public class Transicion {
    private Estado to;
    private Estado from;
    private String valor = "0";
    private Point puntoInicio;
    private Point puntoFin;

    public Transicion(Estado from, Estado to) {
        this.from = from;
        this.to = to;
    }

    public Transicion(Point puntoInicio, Point puntoFin) {
        this.puntoInicio = puntoInicio;
        this.puntoFin = puntoFin;
    }

    public Transicion(Estado from, Estado to, Point puntoInicio, Point puntoFin) {
        this.from = from;
        this.to = to;
        this.puntoInicio = puntoInicio;
        this.puntoFin = puntoFin;
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

    public Point getPuntoInicio() {
        return puntoInicio;
    }

    public void setPuntoInicio(Point puntoInicio) {
        this.puntoInicio = puntoInicio;
    }

    public Point getPuntoFin() {
        return puntoFin;
    }

    public void setPuntoFin(Point puntoFin) {
        this.puntoFin = puntoFin;
    }

    @NonNull
    @Override
    public String toString() {
        return "De " + from.getNombre() + " a " + to.getNombre() + " con valor " + valor;
    }
}
