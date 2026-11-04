package com.parcialbanco.app.infraestructura.web;

import com.parcialbanco.app.dominio.modelo.ParcialCuenta;
import com.parcialbanco.app.dominio.modelo.ParcialMovimiento;

import java.time.LocalDateTime;

/** Contratos JSON de la API REST. */
public final class ParcialDtos {

    private ParcialDtos() {
    }

    // ---- Peticiones ----
    public record RegistroPeticion(String nombre, String correo, String telefono, String direccion,
                                   String clave, String pin) { }

    public record LoginPeticion(String correo, String clave) { }

    public record DepositoPeticion(Long monto) { }

    public record DatosPeticion(String correo, String telefono, String direccion) { }

    public record ClavePeticion(String claveActual, String claveNueva) { }

    public record RetiroPeticion(Long monto, String pin) { }

    // ---- Respuestas ----
    public record PerfilRespuesta(long id, String nombre, String correo, String telefono, String direccion) {
        public static PerfilRespuesta de(ParcialCuenta c) {
            return new PerfilRespuesta(c.getId(), c.getNombre(), c.getCorreo(), c.getTelefono(), c.getDireccion());
        }
    }

    public record SesionRespuesta(String token, PerfilRespuesta perfil) { }

    public record MovimientoRespuesta(long id, String tipo, long monto, long saldoPosterior,
                                      LocalDateTime fecha, String detalle) {
        public static MovimientoRespuesta de(ParcialMovimiento m) {
            return new MovimientoRespuesta(m.id(), m.tipo().name(), m.monto(), m.saldoPosterior(), m.fecha(), m.detalle());
        }
    }

    public record ErrorRespuesta(String codigo, String mensaje) { }
}
