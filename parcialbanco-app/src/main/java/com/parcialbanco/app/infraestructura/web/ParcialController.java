package com.parcialbanco.app.infraestructura.web;

import com.parcialbanco.app.aplicacion.caso.ParcialService;
import com.parcialbanco.app.dominio.excepcion.ParcialException;
import com.parcialbanco.app.dominio.modelo.ParcialCuenta;
import com.parcialbanco.app.infraestructura.web.ParcialDtos.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Adaptador de entrada REST. Toda ruta protegida exige el header Authorization: Bearer {token}. */
@RestController
@RequestMapping("/api/parcial")
public class ParcialController {

    private final ParcialService service;

    public ParcialController(ParcialService service) {
        this.service = service;
    }

    // ---------- Publico ----------

    @PostMapping("/registro")
    @ResponseStatus(HttpStatus.CREATED)
    public SesionRespuesta registro(@RequestBody RegistroPeticion p) {
        var s = service.registrar(p.nombre(), p.correo(), p.telefono(), p.direccion(), p.clave(), p.pin());
        return new SesionRespuesta(s.token(), PerfilRespuesta.de(s.cuenta()));
    }

    @PostMapping("/login")
    public SesionRespuesta login(@RequestBody LoginPeticion p) {
        var s = service.iniciarSesion(p.correo(), p.clave());
        return new SesionRespuesta(s.token(), PerfilRespuesta.de(s.cuenta()));
    }

    // ---------- Protegido ----------

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@RequestHeader(value = "Authorization", required = false) String auth) {
        service.cerrarSesion(token(auth));
    }

    @GetMapping("/cuenta/perfil")
    public PerfilRespuesta perfil(@RequestHeader(value = "Authorization", required = false) String auth) {
        return PerfilRespuesta.de(service.autenticar(token(auth)));
    }

    @GetMapping("/cuenta/saldo")
    public ParcialService.EstadoSaldo saldo(@RequestHeader(value = "Authorization", required = false) String auth) {
        return service.consultarSaldo(service.autenticar(token(auth)));
    }

    @PostMapping("/cuenta/deposito")
    public ParcialService.EstadoSaldo deposito(@RequestHeader(value = "Authorization", required = false) String auth,
                                               @RequestBody DepositoPeticion p) {
        ParcialCuenta cuenta = service.autenticar(token(auth));
        if (p == null || p.monto() == null) {
            throw ParcialException.validacion("MONTO_INVALIDO", "Debes indicar el monto a depositar.");
        }
        service.depositar(cuenta, p.monto());
        return service.consultarSaldo(cuenta);
    }

    @PutMapping("/cuenta/datos")
    public PerfilRespuesta datos(@RequestHeader(value = "Authorization", required = false) String auth,
                                 @RequestBody DatosPeticion p) {
        ParcialCuenta cuenta = service.autenticar(token(auth));
        return PerfilRespuesta.de(service.actualizarDatos(cuenta, p.correo(), p.telefono(), p.direccion()));
    }

    @PutMapping("/cuenta/clave")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clave(@RequestHeader(value = "Authorization", required = false) String auth,
                      @RequestBody ClavePeticion p) {
        service.cambiarClave(service.autenticar(token(auth)), p.claveActual(), p.claveNueva());
    }

    @PostMapping("/cuenta/retiro")
    public ParcialService.ResultadoRetiro retiro(@RequestHeader(value = "Authorization", required = false) String auth,
                                                 @RequestBody RetiroPeticion p) {
        ParcialCuenta cuenta = service.autenticar(token(auth));
        if (p == null || p.monto() == null) {
            throw ParcialException.validacion("MONTO_INVALIDO", "Debes indicar el monto a retirar.");
        }
        return service.retirar(cuenta, p.monto(), p.pin());
    }

    @GetMapping("/cuenta/movimientos")
    public List<MovimientoRespuesta> movimientos(@RequestHeader(value = "Authorization", required = false) String auth) {
        return service.listarMovimientos(service.autenticar(token(auth))).stream()
                .map(MovimientoRespuesta::de).toList();
    }

    private static String token(String auth) {
        return auth != null && auth.startsWith("Bearer ") ? auth.substring(7).trim() : null;
    }
}
