package com.parcialbanco.app.aplicacion.puerto;

import java.util.Optional;

/** Puerto de salida: sesiones autenticadas (token opaco -> id de cuenta). */
public interface ParcialSesionRepositorio {

    String abrir(long cuentaId);

    Optional<Long> cuentaDe(String token);

    void cerrar(String token);
}
