package com.parcialbanco.app.infraestructura.persistencia;

import com.parcialbanco.app.aplicacion.puerto.ParcialCuentaRepositorio;
import com.parcialbanco.app.dominio.modelo.ParcialCuenta;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/** Adaptador de persistencia en memoria (suficiente para el parcial; sustituible por JPA sin tocar el nucleo). */
public class ParcialCuentaRepositorioMemoria implements ParcialCuentaRepositorio {

    private final Map<Long, ParcialCuenta> porId = new ConcurrentHashMap<>();
    private final Map<String, Long> idPorCorreo = new ConcurrentHashMap<>();
    private final AtomicLong secuencia = new AtomicLong(1000);

    @Override
    public long siguienteId() {
        return secuencia.incrementAndGet();
    }

    @Override
    public void guardar(ParcialCuenta cuenta) {
        porId.put(cuenta.getId(), cuenta);
        idPorCorreo.put(cuenta.getCorreo(), cuenta.getId());
    }

    @Override
    public Optional<ParcialCuenta> buscarPorId(long id) {
        return Optional.ofNullable(porId.get(id));
    }

    @Override
    public Optional<ParcialCuenta> buscarPorCorreo(String correo) {
        Long id = idPorCorreo.get(correo);
        return id == null ? Optional.empty() : buscarPorId(id);
    }

    @Override
    public void actualizarIndiceCorreo(String correoAnterior, ParcialCuenta cuenta) {
        if (correoAnterior != null && !correoAnterior.equals(cuenta.getCorreo())) {
            idPorCorreo.remove(correoAnterior);
        }
        idPorCorreo.put(cuenta.getCorreo(), cuenta.getId());
    }
}
