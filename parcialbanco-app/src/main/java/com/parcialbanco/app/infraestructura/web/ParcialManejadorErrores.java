package com.parcialbanco.app.infraestructura.web;

import com.parcialbanco.app.dominio.excepcion.ParcialException;
import com.parcialbanco.app.infraestructura.web.ParcialDtos.ErrorRespuesta;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Traduce errores de negocio a respuestas JSON controladas {codigo, mensaje}. */
@RestControllerAdvice
public class ParcialManejadorErrores {

    @ExceptionHandler(ParcialException.class)
    public ResponseEntity<ErrorRespuesta> negocio(ParcialException e) {
        return ResponseEntity.status(e.getEstado()).body(new ErrorRespuesta(e.getCodigo(), e.getMessage()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorRespuesta> cuerpoInvalido(HttpMessageNotReadableException e) {
        return ResponseEntity.badRequest()
                .body(new ErrorRespuesta("CUERPO_INVALIDO", "La peticion no tiene un formato valido (revisa los tipos de los campos)."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorRespuesta> inesperado(Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorRespuesta("ERROR_INTERNO", "Ocurrio un error inesperado. Intenta nuevamente."));
    }
}
