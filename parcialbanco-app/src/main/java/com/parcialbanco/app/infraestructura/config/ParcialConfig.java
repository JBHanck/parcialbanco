package com.parcialbanco.app.infraestructura.config;

import com.parcialbanco.app.aplicacion.caso.ParcialService;
import com.parcialbanco.app.aplicacion.puerto.ParcialCifrador;
import com.parcialbanco.app.aplicacion.puerto.ParcialCuentaRepositorio;
import com.parcialbanco.app.aplicacion.puerto.ParcialSesionRepositorio;
import com.parcialbanco.app.infraestructura.persistencia.ParcialCuentaRepositorioMemoria;
import com.parcialbanco.app.infraestructura.persistencia.ParcialSesionRepositorioMemoria;
import com.parcialbanco.app.infraestructura.seguridad.ParcialCifradorSha;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Composicion de dependencias. Los beans se construyen con instancias directas
 * (sin Clock ni interceptores inyectados), por lo que no puede haber ciclos.
 */
@Configuration
public class ParcialConfig {

    @Bean
    public ParcialCuentaRepositorio parcialCuentaRepositorio() {
        return new ParcialCuentaRepositorioMemoria();
    }

    @Bean
    public ParcialSesionRepositorio parcialSesionRepositorio() {
        return new ParcialSesionRepositorioMemoria();
    }

    @Bean
    public ParcialCifrador parcialCifrador() {
        return new ParcialCifradorSha();
    }

    @Bean
    public ParcialService parcialService(ParcialCuentaRepositorio cuentas,
                                         ParcialSesionRepositorio sesiones,
                                         ParcialCifrador cifrador) {
        return new ParcialService(cuentas, sesiones, cifrador);
    }
}
