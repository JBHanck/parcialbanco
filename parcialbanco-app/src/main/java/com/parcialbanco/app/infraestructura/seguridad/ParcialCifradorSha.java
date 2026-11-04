package com.parcialbanco.app.infraestructura.seguridad;

import com.parcialbanco.app.aplicacion.puerto.ParcialCifrador;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/** SHA-256 con sal aleatoria por secreto. Formato almacenado: sal:hash (Base64). */
public class ParcialCifradorSha implements ParcialCifrador {

    private final SecureRandom azar = new SecureRandom();

    @Override
    public String cifrar(String secretoPlano) {
        byte[] sal = new byte[16];
        azar.nextBytes(sal);
        return Base64.getEncoder().encodeToString(sal) + ":" + resumen(sal, secretoPlano);
    }

    @Override
    public boolean coincide(String secretoPlano, String secretoCifrado) {
        if (secretoPlano == null || secretoCifrado == null || !secretoCifrado.contains(":")) {
            return false;
        }
        String[] partes = secretoCifrado.split(":", 2);
        byte[] sal = Base64.getDecoder().decode(partes[0]);
        return MessageDigest.isEqual(
                resumen(sal, secretoPlano).getBytes(StandardCharsets.UTF_8),
                partes[1].getBytes(StandardCharsets.UTF_8));
    }

    private static String resumen(byte[] sal, String secreto) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(sal);
            return Base64.getEncoder().encodeToString(md.digest(secreto.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }
}
