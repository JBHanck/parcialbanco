package com.parcialbanco.app;

import com.parcialbanco.app.dominio.excepcion.ParcialException;
import com.parcialbanco.app.dominio.servicio.ParcialDispensador;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ParcialDispensadorTest {

    @Test
    void entregaMenorCantidadDeBilletes() {
        assertEquals(Map.of(100_000, 1, 50_000, 1, 20_000, 1, 10_000, 1), ParcialDispensador.calcular(180_000));
        assertEquals(Map.of(100_000, 2), ParcialDispensador.calcular(200_000));
        assertEquals(Map.of(10_000, 1), ParcialDispensador.calcular(10_000));
        assertEquals(Map.of(50_000, 1, 10_000, 1), ParcialDispensador.calcular(60_000));
        assertEquals(Map.of(20_000, 2), ParcialDispensador.calcular(40_000));
        assertEquals(Map.of(100_000, 10), ParcialDispensador.calcular(1_000_000));
    }

    @Test
    void sumaSiempreIgualAlMontoYEsMinima() {
        for (long monto = 10_000; monto <= 1_000_000; monto += 10_000) {
            Map<Integer, Integer> d = ParcialDispensador.calcular(monto);
            long suma = d.entrySet().stream().mapToLong(e -> (long) e.getKey() * e.getValue()).sum();
            assertEquals(monto, suma);
            // el algoritmo voraz es optimo para 10-5-2-1 (en unidades de 10.000): comparamos contra el.
            int voraz = 0;
            long resto = monto;
            for (int den : ParcialDispensador.DENOMINACIONES) {
                voraz += (int) (resto / den);
                resto %= den;
            }
            assertEquals(voraz, d.values().stream().mapToInt(Integer::intValue).sum(), "monto " + monto);
        }
    }

    @Test
    void rechazaMontosNoFraccionables() {
        ParcialException e = assertThrows(ParcialException.class, () -> ParcialDispensador.calcular(15_000));
        assertEquals("MONTO_NO_FRACCIONABLE", e.getCodigo());
        assertThrows(ParcialException.class, () -> ParcialDispensador.calcular(10_500));
    }
}
