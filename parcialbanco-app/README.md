# ParcialBanco

Parcial final de Análisis y Diseño de Sistemas (UPB). API REST en Spring Boot 3 con arquitectura limpia y frontend de una sola página.

## Ejecutar

```bash
mvn clean spring-boot:run
```

Abrir <http://localhost:8081>. Para probar sin la interfaz, ver el contrato de la API en `DOCUMENTACION.md`.

## Estructura

```
src/main/java/com/parcialbanco/app
├── ParcialBancoApplication.java      # única clase con main
├── dominio                           # núcleo: sin Spring
│   ├── modelo        (ParcialCuenta, ParcialMovimiento)
│   ├── servicio      (ParcialDispensador)
│   └── excepcion     (ParcialException)
├── aplicacion
│   ├── caso          (ParcialService)
│   └── puerto        (repositorios y cifrador)
└── infraestructura
    ├── web           (ParcialController, DTOs, manejador de errores)
    ├── persistencia  (repositorios en memoria)
    ├── seguridad     (ParcialCifradorSha)
    └── config        (ParcialConfig)
src/main/resources/static/index.html   # frontend
```

La documentación académica (elicitación, historias de usuario, diagramas, Git) está en `DOCUMENTACION.md`.
