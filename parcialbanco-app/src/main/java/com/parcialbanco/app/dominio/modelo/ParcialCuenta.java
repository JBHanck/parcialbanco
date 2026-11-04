package com.parcialbanco.app.dominio.modelo;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Entidad de dominio: cuenta del titular. Concentra las invariantes de saldo
 * y el control acumulado diario de retiros.
 */
public class ParcialCuenta {

    private final long id;
    private String nombre;
    private String correo;
    private String telefono;
    private String direccion;
    private String claveHash;
    private final String pinHash;
    private long saldo;
    private final List<ParcialMovimiento> movimientos = new ArrayList<>();
    private long secuenciaMovimientos = 0;

    public ParcialCuenta(long id, String nombre, String correo, String telefono, String direccion,
                         String claveHash, String pinHash, long saldoInicial) {
        this.id = id;
        this.nombre = nombre;
        this.correo = correo;
        this.telefono = telefono;
        this.direccion = direccion;
        this.claveHash = claveHash;
        this.pinHash = pinHash;
        this.saldo = saldoInicial;
    }

    public void depositar(long monto, java.time.LocalDateTime ahora) {
        saldo += monto;
        registrar(ParcialMovimiento.Tipo.DEPOSITO, monto, ahora, "Deposito de fondos");
    }

    public void retirar(long monto, java.time.LocalDateTime ahora, String detalle) {
        saldo -= monto;
        registrar(ParcialMovimiento.Tipo.RETIRO, monto, ahora, detalle);
    }

    /** Suma de los retiros efectuados en la jornada indicada. */
    public long retiradoEn(LocalDate dia) {
        return movimientos.stream()
                .filter(m -> m.tipo() == ParcialMovimiento.Tipo.RETIRO)
                .filter(m -> m.fecha().toLocalDate().equals(dia))
                .mapToLong(ParcialMovimiento::monto)
                .sum();
    }

    private void registrar(ParcialMovimiento.Tipo tipo, long monto, java.time.LocalDateTime ahora, String detalle) {
        movimientos.add(new ParcialMovimiento(++secuenciaMovimientos, tipo, monto, saldo, ahora, detalle));
    }

    public void actualizarDatos(String correo, String telefono, String direccion) {
        this.correo = correo;
        this.telefono = telefono;
        this.direccion = direccion;
    }

    public void cambiarClaveHash(String nuevoHash) {
        this.claveHash = nuevoHash;
    }

    public List<ParcialMovimiento> getMovimientos() {
        return Collections.unmodifiableList(movimientos);
    }

    public long getId() { return id; }
    public String getNombre() { return nombre; }
    public String getCorreo() { return correo; }
    public String getTelefono() { return telefono; }
    public String getDireccion() { return direccion; }
    public String getClaveHash() { return claveHash; }
    public String getPinHash() { return pinHash; }
    public long getSaldo() { return saldo; }
}
