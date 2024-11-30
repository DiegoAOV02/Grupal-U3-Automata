package com.z_iti_271311_u3_e07;

import org.opencv.core.Mat;
import org.opencv.core.Point;

public class Estado {
    private String nombre;
    Point center;
    int radius;
    String tipo;

    public Estado(Point center, int radius) {
        this.center = center;
        this.radius = radius;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public Point getCenter() {
        return center;
    }

    public void setCenter(Point center) {
        this.center = center;
    }

    public int getRadius() {
        return radius;
    }

    public void setRadius(int radius) {
        this.radius = radius;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }
}
