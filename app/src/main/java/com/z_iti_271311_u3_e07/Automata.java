package com.z_iti_271311_u3_e07;

import java.util.ArrayList;

public class Automata {
    private ArrayList<Transicion> listaTransiciones = new ArrayList<>();
    private Estado estadoInicial = null;
    private ArrayList<Estado> listaEstadosNormales = new ArrayList<>();
    private ArrayList<Estado> listaEstadosFinales = new ArrayList<>();

    public ArrayList<Transicion> getListaTransiciones(ArrayList<Transicion> transiciones) {
        return listaTransiciones;
    }

    public Estado getEstadoInicial() {
        return estadoInicial;
    }

    public void setEstadoInicial(Estado estadoInicial) {
        this.estadoInicial = estadoInicial;
    }

    public ArrayList<Estado> getListaEstadosNormales() {
        return listaEstadosNormales;
    }

    public void setListaEstadosNormales(ArrayList<Estado> listaEstadosNormales) {
        this.listaEstadosNormales = listaEstadosNormales;
    }

    public ArrayList<Estado> getListaEstadosFinales() {
        return listaEstadosFinales;
    }

    public void setListaEstadosFinales(ArrayList<Estado> listaEstadosFinales) {
        this.listaEstadosFinales = listaEstadosFinales;
    }
}
