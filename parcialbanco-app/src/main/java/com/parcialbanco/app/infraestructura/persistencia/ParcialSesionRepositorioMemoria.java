package com.parcialbanco.app.infraestructura.persistencia;

import com.parcialbanco.app.aplicacion.puerto.ParcialSesionRepositorio;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Sesiones en memoria con token opaco (UUID). */
public class ParcialSesionRepositorioMemoria implements ParcialSesionRepositorio {

    private final Map<String, Long> sesiones = new ConcurrentHashMap<>();

    @Override
    public String abrir(long cuentaId) {
        String token = UUID.randomUUID().toString();
        sesiones.put(token, cuentaId);
        return token;
    }

    @Override
    public Optional<Long> cuentaDe(String token) {
        return Optional.ofNullable(sesiones.get(token));
    }

    @Override
    public void cerrar(String token) {
        if (token != null) {
            sesiones.remove(token);
        }
    }
}
