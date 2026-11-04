package com.parcialbanco.app.dominio.excepcion;

/**
 * Error de negocio controlado. Lleva un codigo estable (para el frontend),
 * un mensaje legible para el usuario y un estado HTTP sugerido.
 * El dominio no depende de Spring: el estado es un simple entero.
 */
public class ParcialException extends RuntimeException {

    private final String codigo;
    private final int estado;

    public ParcialException(String codigo, String mensaje, int estado) {
        super(mensaje);
        this.codigo = codigo;
        this.estado = estado;
    }

    public static ParcialException validacion(String codigo, String mensaje) {
        return new ParcialException(codigo, mensaje, 400);
    }

    public static ParcialException noAutorizado(String codigo, String mensaje) {
        return new ParcialException(codigo, mensaje, 401);
    }

    public static ParcialException reglaNegocio(String codigo, String mensaje) {
        return new ParcialException(codigo, mensaje, 422);
    }

    public static ParcialException conflicto(String codigo, String mensaje) {
        return new ParcialException(codigo, mensaje, 409);
    }

    public String getCodigo() {
        return codigo;
    }

    public int getEstado() {
        return estado;
    }
}
