package com.parcialbanco.app.aplicacion.caso;

import com.parcialbanco.app.aplicacion.puerto.ParcialCifrador;
import com.parcialbanco.app.aplicacion.puerto.ParcialCuentaRepositorio;
import com.parcialbanco.app.aplicacion.puerto.ParcialSesionRepositorio;
import com.parcialbanco.app.dominio.excepcion.ParcialException;
import com.parcialbanco.app.dominio.modelo.ParcialCuenta;
import com.parcialbanco.app.dominio.modelo.ParcialMovimiento;
import com.parcialbanco.app.dominio.servicio.ParcialDispensador;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Capa de aplicacion: orquesta los casos de uso sobre los puertos.
 * Las operaciones que modifican saldo son atomicas (synchronized) para que dos
 * retiros simultaneos no puedan saltarse el tope diario.
 */
public class ParcialService {

    public static final long SALDO_INICIAL = 2_000_000L;
    public static final long TOPE_DIARIO = 1_000_000L;
    public static final long RETIRO_MINIMO = 10_000L;

    private static final ZoneId ZONA = ZoneId.of("America/Bogota");
    private static final Pattern CORREO = Pattern.compile("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$");
    private static final Pattern TELEFONO = Pattern.compile("^\\+?\\d{7,15}$");
    private static final Pattern PIN = Pattern.compile("^\\d{4}$");

    private final ParcialCuentaRepositorio cuentas;
    private final ParcialSesionRepositorio sesiones;
    private final ParcialCifrador cifrador;

    public ParcialService(ParcialCuentaRepositorio cuentas, ParcialSesionRepositorio sesiones, ParcialCifrador cifrador) {
        this.cuentas = cuentas;
        this.sesiones = sesiones;
        this.cifrador = cifrador;
    }

    // ---------- Resultados ----------

    public record Sesion(String token, ParcialCuenta cuenta) { }

    public record EstadoSaldo(long saldo, long retiradoHoy, long disponibleHoy, long topeDiario) { }

    public record ResultadoRetiro(long monto, long saldoNuevo, Map<Integer, Integer> desglose,
                                  int totalBilletes, long retiradoHoy, long disponibleHoy) { }

    // ---------- Registro y sesion ----------

    public synchronized Sesion registrar(String nombre, String correo, String telefono, String direccion,
                                         String clave, String pin) {
        exigirTexto(nombre, "nombre", "El nombre del titular es obligatorio.");
        String correoNorm = validarCorreo(correo);
        validarTelefono(telefono);
        exigirTexto(direccion, "direccion", "La direccion es obligatoria.");
        validarClave(clave);
        validarFormatoPin(pin);

        if (cuentas.buscarPorCorreo(correoNorm).isPresent()) {
            throw ParcialException.conflicto("CORREO_DUPLICADO", "Ya existe una cuenta registrada con ese correo electronico.");
        }
        ParcialCuenta cuenta = new ParcialCuenta(cuentas.siguienteId(), nombre.trim(), correoNorm, telefono.trim(),
                direccion.trim(), cifrador.cifrar(clave), cifrador.cifrar(pin), SALDO_INICIAL);
        cuentas.guardar(cuenta);
        return new Sesion(sesiones.abrir(cuenta.getId()), cuenta);
    }

    public Sesion iniciarSesion(String correo, String clave) {
        String correoNorm = correo == null ? "" : correo.trim().toLowerCase(Locale.ROOT);
        ParcialCuenta cuenta = cuentas.buscarPorCorreo(correoNorm).orElse(null);
        if (cuenta == null || clave == null || !cifrador.coincide(clave, cuenta.getClaveHash())) {
            throw ParcialException.noAutorizado("CREDENCIALES_INVALIDAS", "Correo o clave de acceso incorrectos.");
        }
        return new Sesion(sesiones.abrir(cuenta.getId()), cuenta);
    }

    public void cerrarSesion(String token) {
        sesiones.cerrar(token);
    }

    public ParcialCuenta autenticar(String token) {
        if (token == null || token.isBlank()) {
            throw ParcialException.noAutorizado("SIN_SESION", "Debes iniciar sesion para realizar esta operacion.");
        }
        return sesiones.cuentaDe(token)
                .flatMap(cuentas::buscarPorId)
                .orElseThrow(() -> ParcialException.noAutorizado("SESION_INVALIDA", "Tu sesion no es valida o ha expirado."));
    }

    // ---------- Consultas ----------

    public EstadoSaldo consultarSaldo(ParcialCuenta cuenta) {
        synchronized (cuenta) {
            long retirado = cuenta.retiradoEn(hoy());
            return new EstadoSaldo(cuenta.getSaldo(), retirado, Math.max(0, TOPE_DIARIO - retirado), TOPE_DIARIO);
        }
    }

    public List<ParcialMovimiento> listarMovimientos(ParcialCuenta cuenta) {
        synchronized (cuenta) {
            return cuenta.getMovimientos().stream()
                    .sorted((a, b) -> Long.compare(b.id(), a.id()))
                    .toList();
        }
    }

    // ---------- Operaciones ----------

    public long depositar(ParcialCuenta cuenta, long monto) {
        if (monto <= 0) {
            throw ParcialException.validacion("MONTO_INVALIDO", "El monto a depositar debe ser mayor a cero.");
        }
        if (monto > 50_000_000L) {
            throw ParcialException.reglaNegocio("DEPOSITO_EXCEDE_LIMITE",
                    "El deposito supera el maximo de $50.000.000 por operacion.");
        }
        synchronized (cuenta) {
            cuenta.depositar(monto, ahora());
            return cuenta.getSaldo();
        }
    }

    public ParcialCuenta actualizarDatos(ParcialCuenta cuenta, String correo, String telefono, String direccion) {
        String correoNorm = validarCorreo(correo);
        validarTelefono(telefono);
        exigirTexto(direccion, "direccion", "La direccion es obligatoria.");
        synchronized (this) {
            cuentas.buscarPorCorreo(correoNorm)
                    .filter(otra -> otra.getId() != cuenta.getId())
                    .ifPresent(otra -> {
                        throw ParcialException.conflicto("CORREO_DUPLICADO", "Ese correo ya pertenece a otra cuenta.");
                    });
            String anterior = cuenta.getCorreo();
            cuenta.actualizarDatos(correoNorm, telefono.trim(), direccion.trim());
            cuentas.actualizarIndiceCorreo(anterior, cuenta);
        }
        return cuenta;
    }

    public void cambiarClave(ParcialCuenta cuenta, String claveActual, String claveNueva) {
        if (claveActual == null || !cifrador.coincide(claveActual, cuenta.getClaveHash())) {
            throw ParcialException.noAutorizado("CLAVE_ACTUAL_INCORRECTA", "La clave actual no es correcta.");
        }
        validarClave(claveNueva);
        if (claveNueva.equals(claveActual)) {
            throw ParcialException.validacion("CLAVE_REPETIDA", "La nueva clave debe ser distinta de la actual.");
        }
        cuenta.cambiarClaveHash(cifrador.cifrar(claveNueva));
    }

    /**
     * Retiro en cajero. Orden de validaciones (la primera que falle explica el motivo exacto):
     * formato PIN, PIN correcto, monto minimo, fraccionamiento, saldo y tope diario.
     */
    public ResultadoRetiro retirar(ParcialCuenta cuenta, long monto, String pin) {
        validarFormatoPin(pin);
        if (!cifrador.coincide(pin, cuenta.getPinHash())) {
            throw ParcialException.noAutorizado("PIN_INCORRECTO", "El PIN de seguridad es incorrecto. El retiro no fue procesado.");
        }
        if (monto < RETIRO_MINIMO) {
            throw ParcialException.reglaNegocio("MONTO_MINIMO",
                    "El monto minimo de retiro es de $10.000 COP. Solicitaste " + formato(monto) + ".");
        }
        Map<Integer, Integer> desglose = ParcialDispensador.calcular(monto);

        synchronized (cuenta) {
            if (monto > cuenta.getSaldo()) {
                throw ParcialException.reglaNegocio("SALDO_INSUFICIENTE",
                        "Saldo insuficiente: tu saldo es " + formato(cuenta.getSaldo()) + " y solicitaste " + formato(monto) + ".");
            }
            long yaRetirado = cuenta.retiradoEn(hoy());
            if (yaRetirado + monto > TOPE_DIARIO) {
                long disponible = Math.max(0, TOPE_DIARIO - yaRetirado);
                throw ParcialException.reglaNegocio("TOPE_DIARIO_EXCEDIDO",
                        "Se supera el tope diario de $1.000.000 COP. Hoy ya retiraste " + formato(yaRetirado)
                                + " y te quedan disponibles " + formato(disponible) + ".");
            }
            int billetes = desglose.values().stream().mapToInt(Integer::intValue).sum();
            cuenta.retirar(monto, ahora(), "Retiro en cajero (" + billetes + " billetes)");
            long retiradoHoy = yaRetirado + monto;
            return new ResultadoRetiro(monto, cuenta.getSaldo(), desglose, billetes, retiradoHoy,
                    TOPE_DIARIO - retiradoHoy);
        }
    }

    // ---------- Utilidades ----------

    protected LocalDateTime ahora() {
        return LocalDateTime.now(ZONA);
    }

    private LocalDate hoy() {
        return ahora().toLocalDate();
    }

    private static String formato(long valor) {
        return "$" + String.format(Locale.GERMANY, "%,d", valor) + " COP";
    }

    private static void exigirTexto(String valor, String campo, String mensaje) {
        if (valor == null || valor.isBlank()) {
            throw ParcialException.validacion("CAMPO_OBLIGATORIO_" + campo.toUpperCase(Locale.ROOT), mensaje);
        }
    }

    private static String validarCorreo(String correo) {
        if (correo == null || !CORREO.matcher(correo.trim()).matches()) {
            throw ParcialException.validacion("CORREO_INVALIDO", "El correo electronico no tiene un formato valido.");
        }
        return correo.trim().toLowerCase(Locale.ROOT);
    }

    private static void validarTelefono(String telefono) {
        if (telefono == null || !TELEFONO.matcher(telefono.trim()).matches()) {
            throw ParcialException.validacion("TELEFONO_INVALIDO", "El telefono debe tener entre 7 y 15 digitos.");
        }
    }

    private static void validarClave(String clave) {
        if (clave == null || clave.length() < 6) {
            throw ParcialException.validacion("CLAVE_DEBIL", "La clave de acceso debe tener al menos 6 caracteres.");
        }
    }

    private static void validarFormatoPin(String pin) {
        if (pin == null || !PIN.matcher(pin).matches()) {
            throw ParcialException.validacion("PIN_FORMATO", "El PIN debe tener exactamente 4 digitos numericos.");
        }
    }
}
