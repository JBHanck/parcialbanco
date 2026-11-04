package com.parcialbanco.app.dominio.modelo;

import java.time.LocalDateTime;

/** Registro inmutable de una operacion sobre la cuenta. Montos en pesos colombianos enteros. */
public record ParcialMovimiento(
        long id,
        Tipo tipo,
        long monto,
        long saldoPosterior,
        LocalDateTime fecha,
        String detalle) {

    public enum Tipo {
        DEPOSITO,
        RETIRO
    }
}
