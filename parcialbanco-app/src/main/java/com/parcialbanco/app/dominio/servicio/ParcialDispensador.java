package com.parcialbanco.app.dominio.servicio;

import com.parcialbanco.app.dominio.excepcion.ParcialException;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Algoritmo de dispensacion: calcula la MENOR cantidad de billetes posible usando
 * exclusivamente denominaciones de 100.000, 50.000, 20.000 y 10.000 COP.
 *
 * Se resuelve con programacion dinamica (cambio minimo), de modo que la minimalidad
 * esta garantizada sin depender de que el sistema de denominaciones sea "canonico".
 * Complejidad O(n * d), con n = monto / 10.000 y d = numero de denominaciones.
 */
public final class ParcialDispensador {

    /** Denominaciones fisicas disponibles, de mayor a menor. */
    public static final int[] DENOMINACIONES = {100_000, 50_000, 20_000, 10_000};

    private static final int UNIDAD = 10_000;

    private ParcialDispensador() {
    }

    /**
     * @return mapa denominacion -> cantidad (solo las denominaciones usadas), de mayor a menor.
     * @throws ParcialException si el monto no se puede fraccionar exactamente.
     */
    public static Map<Integer, Integer> calcular(long monto) {
        if (monto <= 0 || monto % UNIDAD != 0) {
            throw ParcialException.reglaNegocio("MONTO_NO_FRACCIONABLE",
                    "El monto solicitado no puede entregarse de forma exacta: el cajero solo dispone de billetes de "
                            + "$100.000, $50.000, $20.000 y $10.000, por lo que el valor debe ser multiplo de $10.000.");
        }
        int unidades = (int) (monto / UNIDAD);
        int[] valores = Arrays.stream(DENOMINACIONES).map(d -> d / UNIDAD).toArray();

        int inf = Integer.MAX_VALUE / 2;
        int[] minBilletes = new int[unidades + 1];
        int[] ultimo = new int[unidades + 1];
        Arrays.fill(minBilletes, inf);
        minBilletes[0] = 0;

        for (int u = 1; u <= unidades; u++) {
            for (int i = 0; i < valores.length; i++) { // de mayor a menor: desempata a favor del billete mas alto
                int v = valores[i];
                if (v <= u && minBilletes[u - v] + 1 < minBilletes[u]) {
                    minBilletes[u] = minBilletes[u - v] + 1;
                    ultimo[u] = i;
                }
            }
        }

        if (minBilletes[unidades] >= inf) {
            throw ParcialException.reglaNegocio("MONTO_NO_FRACCIONABLE",
                    "No es posible entregar ese monto con las denominaciones disponibles.");
        }

        int[] cantidades = new int[valores.length];
        for (int u = unidades; u > 0; u -= valores[ultimo[u]]) {
            cantidades[ultimo[u]]++;
        }

        Map<Integer, Integer> desglose = new LinkedHashMap<>();
        for (int i = 0; i < DENOMINACIONES.length; i++) {
            if (cantidades[i] > 0) {
                desglose.put(DENOMINACIONES[i], cantidades[i]);
            }
        }
        return desglose;
    }
}
