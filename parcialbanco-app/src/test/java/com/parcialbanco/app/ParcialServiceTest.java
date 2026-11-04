package com.parcialbanco.app;

import com.parcialbanco.app.aplicacion.caso.ParcialService;
import com.parcialbanco.app.dominio.excepcion.ParcialException;
import com.parcialbanco.app.dominio.modelo.ParcialCuenta;
import com.parcialbanco.app.infraestructura.persistencia.ParcialCuentaRepositorioMemoria;
import com.parcialbanco.app.infraestructura.persistencia.ParcialSesionRepositorioMemoria;
import com.parcialbanco.app.infraestructura.seguridad.ParcialCifradorSha;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ParcialServiceTest {

    private ParcialService service;
    private ParcialCuenta cuenta;

    @BeforeEach
    void preparar() {
        service = new ParcialService(new ParcialCuentaRepositorioMemoria(), new ParcialSesionRepositorioMemoria(),
                new ParcialCifradorSha());
        cuenta = service.registrar("Ana Prueba", "ana@correo.com", "3001234567", "Calle 1 # 2-3", "secreta1", "1234").cuenta();
    }

    private String codigo(Runnable r) {
        return assertThrows(ParcialException.class, r::run).getCodigo();
    }

    @Test
    void registroAsignaSaldoInicial() {
        assertEquals(2_000_000L, service.consultarSaldo(cuenta).saldo());
    }

    @Test
    void retiroExitosoDescuentaSaldoYDevuelveDesglose() {
        var r = service.retirar(cuenta, 180_000, "1234");
        assertEquals(1_820_000L, r.saldoNuevo());
        assertEquals(4, r.totalBilletes());
        assertEquals(820_000L, r.disponibleHoy());
    }

    @Test
    void pinIncorrectoOMalFormadoRechaza() {
        assertEquals("PIN_INCORRECTO", codigo(() -> service.retirar(cuenta, 50_000, "9999")));
        assertEquals("PIN_FORMATO", codigo(() -> service.retirar(cuenta, 50_000, "12")));
        assertEquals("PIN_FORMATO", codigo(() -> service.retirar(cuenta, 50_000, "12345")));
        assertEquals("PIN_FORMATO", codigo(() -> service.retirar(cuenta, 50_000, "12a4")));
        assertEquals(2_000_000L, service.consultarSaldo(cuenta).saldo());
    }

    @Test
    void montoMinimoYFraccionamiento() {
        assertEquals("MONTO_MINIMO", codigo(() -> service.retirar(cuenta, 9_999, "1234")));
        assertEquals("MONTO_NO_FRACCIONABLE", codigo(() -> service.retirar(cuenta, 15_000, "1234")));
    }

    @Test
    void topeDiarioAcumulado() {
        service.retirar(cuenta, 600_000, "1234");
        service.retirar(cuenta, 400_000, "1234"); // llega exactamente a 1.000.000: permitido
        assertEquals("TOPE_DIARIO_EXCEDIDO", codigo(() -> service.retirar(cuenta, 10_000, "1234")));
        assertEquals(1_000_000L, service.consultarSaldo(cuenta).saldo());
    }

    @Test
    void topeDiarioRechazaSiLaSumaSupera() {
        service.retirar(cuenta, 950_000, "1234");
        assertEquals("TOPE_DIARIO_EXCEDIDO", codigo(() -> service.retirar(cuenta, 60_000, "1234")));
        service.retirar(cuenta, 50_000, "1234");
    }

    @Test
    void depositoIncrementaSaldoYValidaMonto() {
        service.depositar(cuenta, 500_000);
        assertEquals(2_500_000L, service.consultarSaldo(cuenta).saldo());
        assertEquals("MONTO_INVALIDO", codigo(() -> service.depositar(cuenta, 0)));
        assertEquals("MONTO_INVALIDO", codigo(() -> service.depositar(cuenta, -5)));
    }

    @Test
    void cambioDeClaveYLogin() {
        assertEquals("CLAVE_ACTUAL_INCORRECTA", codigo(() -> service.cambiarClave(cuenta, "mala", "nuevaClave1")));
        service.cambiarClave(cuenta, "secreta1", "nuevaClave1");
        assertEquals("CREDENCIALES_INVALIDAS", codigo(() -> service.iniciarSesion("ana@correo.com", "secreta1")));
        assertNotNull(service.iniciarSesion("ana@correo.com", "nuevaClave1").token());
    }

    @Test
    void actualizacionDeDatosYCorreoDuplicado() {
        service.registrar("Beto", "beto@correo.com", "3007654321", "Cra 5", "secreta2", "4321");
        assertEquals("CORREO_DUPLICADO", codigo(() -> service.actualizarDatos(cuenta, "beto@correo.com", "3001234567", "X")));
        service.actualizarDatos(cuenta, "ana.nueva@correo.com", "3110000000", "Nueva direccion 9");
        assertEquals("ana.nueva@correo.com", cuenta.getCorreo());
        assertNotNull(service.iniciarSesion("ana.nueva@correo.com", "secreta1"));
    }

    @Test
    void rutasProtegidasExigenSesion() {
        assertEquals("SIN_SESION", codigo(() -> service.autenticar(null)));
        assertEquals("SESION_INVALIDA", codigo(() -> service.autenticar("token-falso")));
    }
}
