package com.parcialbanco.app.aplicacion.puerto;

/** Puerto de salida: proteccion de secretos (clave y PIN). */
public interface ParcialCifrador {

    String cifrar(String secretoPlano);

    boolean coincide(String secretoPlano, String secretoCifrado);
}
