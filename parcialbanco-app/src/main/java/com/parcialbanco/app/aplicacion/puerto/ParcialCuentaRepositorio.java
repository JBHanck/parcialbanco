package com.parcialbanco.app.aplicacion.puerto;

import com.parcialbanco.app.dominio.modelo.ParcialCuenta;

import java.util.Optional;

/** Puerto de salida: persistencia de cuentas. */
public interface ParcialCuentaRepositorio {

    long siguienteId();

    void guardar(ParcialCuenta cuenta);

    Optional<ParcialCuenta> buscarPorId(long id);

    Optional<ParcialCuenta> buscarPorCorreo(String correo);

    /** Reindexa la cuenta cuando el titular cambia su correo. */
    void actualizarIndiceCorreo(String correoAnterior, ParcialCuenta cuenta);
}
