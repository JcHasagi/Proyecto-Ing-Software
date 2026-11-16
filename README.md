# SimInversión

Plataforma web para practicar inversión con datos históricos reales y dinero ficticio. Es el proyecto semestral de Ingeniería de Software (Universidad Industrial de Santander).

Es una herramienta educativa: no maneja dinero real, no se conecta con bancos ni comisionistas y no da recomendaciones de inversión.

## Qué hace

- **Portafolios ficticios.** Cada portafolio tiene un saldo inicial en USD y su propio reloj de simulación, que solo avanza cuando el usuario lo pide y nunca retrocede.
- **Órdenes con ejecución diferida.** Una orden de compra o venta queda pendiente y se ejecuta al cierre del siguiente día bursátil. Así nadie opera con el precio que ya vio.
- **Libro de transacciones inmutable.** El efectivo y las posiciones no se guardan: se calculan a partir de las transacciones.
- **Comisiones y costo base.** Comisión del 0,3 % con mínimo de US$ 5; costo base por promedio ponderado.
- **Rendimiento.** Ganancia realizada y no realizada por separado, y gráfico de la evolución del portafolio.
- **Cuenta de ahorro simulada**, con interés del 4 % nominal anual, para comparar ahorrar con invertir.
- **Escenarios históricos:** crisis de 2008, mercado alcista de 2013, caída por COVID-19 de 2020 y mercado bajista de 2022.
- **Catálogo de 23 instrumentos:** 12 acciones, 4 ETF, 4 divisas y 3 criptomonedas, con precios diarios desde 2005.
- **Contenido educativo** sobre los conceptos básicos.
- **Administración:** catálogo de instrumentos, carga de históricos desde CSV y estadísticas de uso.

## Tecnología

- Java 21 y Spring Boot 4.1 (Web MVC, Security, Data JPA, Validation, Thymeleaf)
- Flyway para el esquema de la base de datos
- MySQL 8.4; las pruebas usan H2 en modo MySQL
- Maven Wrapper (no hace falta instalar Maven)

## Requisitos

- JDK 21
- MySQL 8.4, solo si se quiere ejecutar con base de datos persistente

## Cómo ejecutarlo

Los comandos están escritos para PowerShell en Windows. En Linux o macOS se usa `./mvnw` en lugar de `.\mvnw.cmd`.

### Pruebas

```powershell
.\mvnw.cmd test
```

Ejecuta todas las pruebas (98 al 16 de noviembre de 2026) sobre H2 en memoria. No requiere MySQL. El informe de cobertura queda en `target/site/jacoco/index.html`.

### Demostración con H2 en memoria

Es la forma más rápida de ver la aplicación. Los datos se pierden al detenerla.

```powershell
.\mvnw.cmd spring-boot:test-run "-Dspring-boot.run.arguments=--siminversion.historico.directorio=datos/historico --siminversion.admin.correo=admin@siminversion.test --siminversion.admin.contrasena=ELIGE-UNA-CLAVE"
```

Cambia `ELIGE-UNA-CLAVE` por una clave de 8 caracteres o más. Luego abre <http://localhost:8080>.

- El argumento `historico.directorio` carga los precios de `datos/historico/` al arrancar. Sin él, la aplicación empieza sin precios.
- Los dos argumentos `admin` crean la cuenta de administrador. El registro público solo crea inversionistas.

### Con MySQL

La aplicación todavía no se ha probado contra MySQL: hasta ahora todo se ha ejecutado con H2.

1. Crea la base de datos y un usuario:

   ```sql
   CREATE DATABASE siminversion CHARACTER SET utf8mb4;
   CREATE USER 'siminversion'@'localhost' IDENTIFIED BY 'ELIGE-UNA-CLAVE';
   GRANT ALL PRIVILEGES ON siminversion.* TO 'siminversion'@'localhost';
   ```

2. Define las variables de entorno:

   | Variable | Obligatoria | Para qué sirve |
   |---|---|---|
   | `SIMINVERSION_DB_CONTRASENA` | Sí | Clave del usuario de MySQL |
   | `SIMINVERSION_DB_USUARIO` | No | Usuario de MySQL (por defecto, `siminversion`) |
   | `SIMINVERSION_DB_URL` | No | URL JDBC (por defecto, `localhost:3306/siminversion`) |
   | `SIMINVERSION_ADMIN_CORREO` | No | Correo del primer administrador |
   | `SIMINVERSION_ADMIN_CONTRASENA` | No | Clave del primer administrador |
   | `SIMINVERSION_HISTORICO_DIR` | No | Carpeta con los CSV iniciales (por defecto, `datos/historico`) |

3. Arranca la aplicación:

   ```powershell
   .\mvnw.cmd spring-boot:run
   ```

Flyway crea las tablas al arrancar. El administrador se crea solo si se definen sus dos variables y el correo no existe.

## Estructura

El código está en `src/main/java/co/edu/uis/siminversion`, organizado por funcionalidad. Cada paquete tiene su controlador, su servicio y su repositorio.

| Paquete | Contenido |
|---|---|
| `usuario` | Registro y autenticación |
| `instrumento` | Catálogo, detalle y administración del catálogo |
| `precio` | Acceso a los precios históricos |
| `carga` | Carga de históricos desde CSV |
| `portafolio` | Portafolios, resumen y reinicio |
| `orden` | Emisión y cancelación de órdenes |
| `transaccion` | El libro de transacciones |
| `reloj` | Avance del reloj y ejecución de órdenes |
| `ahorro` | Cuenta de ahorro |
| `rendimiento` | Ganancias, pérdidas y evolución |
| `historial` | Historial de transacciones |
| `escenario` | Escenarios históricos |
| `educacion` | Contenido educativo |
| `estadistica` | Estadísticas de uso |
| `comun`, `web`, `configuracion` | Reglas de dinero, formatos y seguridad |

Las plantillas están en `src/main/resources/templates` y las migraciones de la base de datos en `src/main/resources/db/migration`.

## Datos históricos

`datos/historico/` trae un CSV por instrumento, descargado de Yahoo Finance y ajustado por splits. El origen y el tratamiento están en [datos/historico/FUENTES.md](datos/historico/FUENTES.md).

Para volver a descargarlos (requiere internet):

```powershell
python scripts/descargar_historicos.py
```

Los datos se incluyen solo con fines educativos.

## Documentación

- [docs/analisis-tecnico.md](docs/analisis-tecnico.md): estructura, patrones de diseño, fidelidad a los requerimientos, mediciones y pendientes.
- [datos/historico/FUENTES.md](datos/historico/FUENTES.md): origen y tratamiento de los datos históricos.

## Estado

Las cuatro iteraciones del plan están implementadas (REQ-001 a REQ-019). Falta probar la aplicación contra MySQL y hacer la demostración sin conexión a internet.
