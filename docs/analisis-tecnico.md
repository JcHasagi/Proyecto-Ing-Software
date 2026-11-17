# Análisis técnico de SimInversión

Revisión del 16 de noviembre de 2026, hecha sobre el código con las cuatro iteraciones terminadas (REQ-001 a REQ-019).

Cubre cuatro preguntas:

1. ¿Está bien estructurado el proyecto?
2. ¿Los patrones de diseño están bien aplicados?
3. ¿Qué tan fiel es el código a lo que se pidió?
4. ¿Qué se podía optimizar, y qué se optimizó en esta revisión?

## Resumen

| Tema | Veredicto |
|---|---|
| Estructura | Buena. Paquetes por funcionalidad con capas controlador → servicio → repositorio. El punto débil es `portafolio`, que concentra dependencias: quedan 4 ciclos entre paquetes (había 6). |
| Patrones | Bien elegidos y sin sobreingeniería. El libro de transacciones inmutable es la mejor decisión del diseño. Se corrigieron tres problemas: servicios que dependían de la capa web, consultas repetidas por fila (N+1) y lecturas duplicadas del libro. |
| Fidelidad a lo pedido | Alta. De 19 requerimientos, 17 cumplen el flujo original completo y 2 lo cumplen con una variación. REQ-018 estaba parcial y se completó en esta revisión. Las 5 decisiones de arquitectura y las 10 reglas de negocio se respetan. |
| Rendimiento | No era un problema, pero había consultas de sobra. El detalle del portafolio pasó de 31 a 17 consultas SQL y el historial de 13 a 3. |
| Pruebas | 99 pruebas automáticas, todas pasan. Cobertura de líneas del 87,0 %. Los cálculos de dinero (costo base, comisión, intereses) están al 100 %. |

Lo más importante que sigue pendiente, en orden:

1. **Las divisas y criptomonedas** estaban fuera de alcance en el documento original. Se agregaron a petición del equipo y hay que reflejarlo en el SRS.

## Cómo se hizo la revisión

- **Código:** lectura completa de `src/main/java` (104 archivos y 5.278 líneas al empezar; 106 y 5.416 al terminar), de las 22 plantillas y de las 5 migraciones.
- **Referencia para la fidelidad:** el catálogo de requerimientos *anterior* a la versión 2.0 y la especificación inicial del proyecto. La versión 2.0 de `requerimientos.md` se escribió para reflejar el código, así que compararla con el código no diría nada. En cada desviación se indica si la versión 2.0 ya la documenta.
- **Rendimiento:** se contaron las sentencias SQL que prepara Hibernate en cada operación, antes y después de los cambios, con un portafolio de 10 posiciones, cuenta de ahorro y un año de reloj (439 transacciones). Se cuentan consultas y no milisegundos porque el tiempo de una sola corrida sobre H2 en memoria no es una medida fiable.
- **Verificación de los cambios:** `mvnw test` (99 pruebas en verde). Las páginas se verificaron con las pruebas de MockMvc. En el navegador solo se recorrió la carga de históricos en dos pasos; el resto de la aplicación no se volvió a recorrer después de los cambios.

## 1. Estructura del proyecto

### Organización

El código está organizado por funcionalidad y no por capa técnica. Cada paquete tiene su controlador, su servicio y su repositorio.

| Paquete | Archivos | Líneas | Responsabilidad |
|---|---:|---:|---|
| `portafolio` | 12 | 764 | Portafolios, portafolio activo, resumen, valoración y reinicio (REQ-002, 003, 015) |
| `instrumento` | 13 | 702 | Catálogo, detalle y administración del catálogo (REQ-004, 005, 017) |
| `carga` | 9 | 652 | Lectura de CSV y carga de históricos en dos pasos (REQ-018) |
| `orden` | 7 | 464 | Estimación, emisión y cancelación de órdenes (REQ-006, 007, 008) |
| `reloj` | 6 | 433 | Avance del reloj y ejecución de órdenes (REQ-009, 010) |
| `transaccion` | 5 | 417 | El libro: transacciones inmutables y `EstadoDelLibro` |
| `usuario` | 11 | 407 | Registro, autenticación y administrador inicial (REQ-001, 002) |
| `ahorro` | 7 | 319 | Cuenta de ahorro e intereses (REQ-013) |
| `precio` | 7 | 309 | `ProveedorDePrecios` y su implementación sobre la base de datos |
| `escenario` | 5 | 162 | Escenarios históricos (REQ-014) |
| `estadistica` | 3 | 131 | Estadísticas de uso (REQ-019) |
| `rendimiento` | 4 | 129 | P&L y evolución del valor (REQ-011) |
| `historial` | 4 | 127 | Historial de transacciones (REQ-012) |
| `web` | 4 | 120 | Formatos para las vistas y datos comunes a todas las páginas |
| `educacion` | 3 | 117 | Contenido educativo (REQ-016) |
| `comun` | 3 | 89 | Excepción de regla de negocio, reglas de dinero y textos |
| `configuracion` | 2 | 58 | Seguridad y reloj del sistema |

### Lo que está bien

- **Un requerimiento se encuentra por el nombre del paquete.** REQ-013 está en `ahorro`, REQ-014 en `escenario`.
- **Las reglas de negocio están en los servicios**, como exige el proyecto. Los controladores solo convierten la petición, llaman al servicio y redirigen.
- **Las clases son pequeñas.** La más larga es `PortafolioServicio` (318 líneas). Ninguna otra pasa de 240.
- **El esquema lo controla solo Flyway.** Hibernate valida y no crea tablas.
- **Los precios tienen un único punto de acceso** (`ProveedorDePrecios`).

### Lo que se encontró y se corrigió

| Hallazgo | Corrección |
|---|---|
| Cuatro servicios (`OrdenServicio`, `RelojServicio`, `PortafolioServicio` y `AhorroServicio`) importaban `web.Formatos` para armar mensajes. Una capa inferior dependía de la superior. | Los textos de dinero y fecha pasaron a `comun.Textos`. Ningún servicio importa ya nada de `web`. |
| `OperacionInvalidaException` y `ReglasDeOperacion` vivían en `portafolio`, pero las usan otros 8 paquetes. Medio proyecto dependía de `portafolio` solo por eso. | Pasaron al paquete nuevo `comun`, que no depende de nadie. |
| El historial (REQ-012) vivía en `transaccion` y necesitaba a `portafolio`, lo que cerraba un ciclo entre los dos paquetes. | Pasó al paquete `historial`. `transaccion` queda solo con el libro. |
| `EscenarioServicio` y `AdministracionCatalogoServicio` consultaban `PrecioHistoricoRepositorio` directamente. La decisión de arquitectura 1 dice que todo acceso a precios pasa por `ProveedorDePrecios`. | Se agregaron `instrumentosActivosConPrecios` y `coberturaPorInstrumento` a la interfaz. Solo `carga` usa el repositorio, porque es quien escribe la tabla. |
| `AdministracionCatalogoServicio` lanzaba `ResponseStatusException`, un tipo de la capa web. | Ahora lanza `InstrumentoNoEncontradoException`, que ya existía. La respuesta sigue siendo 404. |

### Lo que queda: ciclos entre paquetes

Un ciclo es un par de paquetes que se importan entre sí. Dificulta entender y probar cada uno por separado.

| Ciclo | Causa |
|---|---|
| `portafolio` ↔ `orden` | El resumen del portafolio lista órdenes; `OrdenServicio` necesita `PortafolioServicio` para verificar el dueño. |
| `portafolio` ↔ `ahorro` | El resumen pregunta si hay cuenta de ahorro; `AhorroServicio` necesita `PortafolioServicio`. |
| `portafolio` ↔ `escenario` | `PortafolioServicio.crearDesdeEscenario` usa `Escenario`; `EscenarioControlador` llama a ese método. |
| `orden` ↔ `transaccion` | `Transaccion.operacion(Orden, …)` recibe la orden; `OrdenServicio` usa `EstadoDelLibro`. |

Antes de la revisión había 6 (los cuatro de la tabla, más `portafolio` ↔ `transaccion` y `portafolio` ↔ `web`).

La causa común es que `PortafolioServicio` hace dos trabajos: modifica portafolios (crear, activar, reiniciar) y arma la vista de resumen, que necesita órdenes, ahorro y escenario. Separar el resumen en un servicio de consulta propio rompería los tres primeros ciclos. No se hizo en esta revisión porque toca a todos los servicios que hoy le piden el portafolio a `PortafolioServicio`, y el beneficio no justifica el riesgo a esta altura del semestre.

## 2. Patrones de diseño

| Patrón | Dónde | Evaluación |
|---|---|---|
| Capas con paquetes por funcionalidad | Todo el proyecto | Bien aplicado. Ver los ciclos de la sección 1. |
| Libro inmutable (cercano a Event Sourcing) | `Transaccion` (`@Immutable`) y `EstadoDelLibro` | **La mejor decisión del diseño.** El efectivo, las posiciones y el ahorro se recalculan recorriendo las transacciones. `EstadoDelLibro` es el único lugar que aplica el efecto de una transacción, y lo usan tanto las consultas como el reloj. Costo: cada lectura recorre todo el libro (ver sección 4). |
| Puerto y adaptador | `ProveedorDePrecios` → `ProveedorHistoricoBD` | Bien aplicado. Con una sola implementación es un puerto más que una estrategia; se justifica por la decisión de arquitectura 1. `precioCierre` y `siguienteDiaBursatil` solo los usan las pruebas: el reloj lee las series completas con `SeriesDePrecios`. |
| Máquina de estados con enum | `EstadoOrden`, `EstadoPortafolio` | Bien en `Orden`: `resolver()` impide salir de PENDIENTE dos veces. En `Portafolio`, `finalizar()` y `archivar()` no validan el estado anterior; hoy lo valida el servicio. |
| Políticas como funciones puras | `ReglasDeOperacion`, `CalculadoraDeIntereses` | Bien aplicado. Se prueban sin base de datos y tienen cobertura del 100 %. |
| Métodos de fábrica | `Transaccion.depositoInicial / operacion / comision / movimientoDeAhorro` | Bien aplicado. No se puede crear una transacción a medias. |
| Builder | `ResumenAvance.Constructor` | Bien aplicado: acumula durante el avance y arma el resumen al final. |
| Bloqueo optimista | `@Version` en `Portafolio`, `obtenerParaModificar` | Bien aplicado, pero **sin prueba automática** de dos operaciones simultáneas. |
| Unidad de trabajo | `@Transactional`; todo el avance del reloj es una transacción | Bien aplicado. Ahora tiene prueba (`AtomicidadDelAvanceTest`). |
| Caché por operación | `SeriesDePrecios` | Bien aplicado. Evita una consulta por instrumento y por día. |
| Post/Redirect/Get | Todos los formularios | Bien aplicado. |
| Vista compuesta | Fragmentos de `plantilla.html` | Bien aplicado. |
| Objetos de formulario y modelos de vista | `FormularioPortafolio`, `EstimacionOrden`, `ResumenPortafolio`… | Aceptable. Algunos modelos de vista llevan entidades (`ResumenPortafolio` lleva `Portafolio` y `Orden`). No causa errores porque no quedan asociaciones sin cargar, pero las plantillas ven las entidades. |

### Problemas de diseño que quedan

| Problema | Dónde | Recomendación |
|---|---|---|
| El mismo `try / catch (OperacionInvalidaException)` con mensaje flash se repite 14 veces. | Controladores | Aceptable: cada uno redirige a un sitio distinto. Un manejador común obligaría a codificar el destino en la excepción. |
| `OrdenServicio.estimar` valida compra y venta en un solo método de unas 70 líneas. | `orden` | Separar en `validarCompra` y `validarVenta`. No se hizo porque el orden de las validaciones define qué mensaje ve el usuario. |
| Cada verificación de dueño hace dos consultas: el usuario por correo y luego el portafolio. | `PortafolioServicio.obtener` | Unirlas en una consulta. Ahorra una consulta por operación. |
| El historial filtra en memoria y no pagina. | `HistorialServicio` | Con cuenta de ahorro el libro crece una fila por día. Paginar o filtrar en SQL si los portafolios pasan de unos pocos años. |

### Lo que no se usa, y está bien que no se use

Observer, State de GoF con una clase por estado, Abstract Factory y Singletons propios. El proyecto no los necesita.

## 3. Fidelidad a los requerimientos

### Requerimientos funcionales

"Original" es el catálogo anterior a la versión 2.0.

| Requerimiento | Estado | Observación |
|---|---|---|
| REQ-001 Registro | Cumple | La política de contraseñas es provisional (8 caracteres a 72 bytes). |
| REQ-002 Inicio y cierre de sesión | Cumple | Mensaje genérico de error, portafolio activo al entrar, invitación a crear uno. |
| REQ-003 Creación de portafolio | Cumple | Agrega la fecha inicial, que el original dejaba sin definir. |
| REQ-004 Catálogo | Cumple con variación | Funciona también sin portafolio activo (muestra precios hasta hoy). El original exigía portafolio activo. |
| REQ-005 Detalle e histórico | Cumple con variación | El usuario elige entre 5 rangos fijos, no un rango libre de fechas. Por eso el flujo alternativo "rango con fechas posteriores" no puede ocurrir. La versión 2.0 ya describe los rangos fijos, pero conserva ese flujo alternativo. |
| REQ-006 Orden de compra | Cumple | Además descuenta las compras pendientes al validar el efectivo. |
| REQ-007 Orden de venta | Cumple | Además descuenta las ventas pendientes al validar la posición. |
| REQ-008 Cancelación | Cumple | |
| REQ-009 Avance del reloj | Cumple | Orden correcto en cada día: órdenes, intereses, instantánea. |
| REQ-010 Ejecución | Cumple | Segunda validación al ejecutar; operación y comisión en el mismo paso. |
| REQ-011 Valoración y rendimiento | Cumple | Separa P&L realizado y no realizado. |
| REQ-012 Historial | Cumple | |
| REQ-013 Cuenta de ahorro | Cumple | La tasa (4 % nominal anual) se definió después; el original la dejaba abierta. |
| REQ-014 Escenarios | Cumple | Agrega el fin del escenario, que el original dejaba sin definir. |
| REQ-015 Reinicio | Cumple | Archiva sin borrar. |
| REQ-016 Contenido educativo | Cumple | 8 artículos, con aviso de que no son recomendaciones. |
| REQ-017 Administración del catálogo | Cumple | |
| REQ-018 Carga masiva | Cumple | Estaba parcial al empezar la revisión. Ver abajo. |
| REQ-019 Estadísticas | Cumple | Solo totales, sin datos personales. |

**REQ-018, la desviación que se cerró.** El original pedía este flujo: validar el archivo, mostrar un resumen, esperar la confirmación del administrador y entonces guardar. Si había huecos, el administrador podía continuar o cancelar. Al empezar la revisión, la implementación validaba y guardaba en un solo paso. Ahora `CargaHistoricoServicio.validar` arma el resumen sin guardar nada y `confirmar` guarda cuando el administrador acepta; también puede cancelar. Si entre la validación y la confirmación alguien carga una de esas fechas, se omite sin sobrescribirla. `requerimientos.md` pasó a la versión 2.1 con este flujo. La carga inicial que hace la aplicación al arrancar sigue sin confirmación, porque no hay un administrador presente.

### Reglas de negocio

| Regla | Estado | Observación |
|---|---|---|
| RN-01 Saldo inicial | Cumple con cambio | Los montos son los originales divididos entre 1.000 y están en USD (10.000 por defecto, entre 1.000 y 100.000). Documentado en la versión 2.0. |
| RN-02 Sin venta en corto | Cumple | Se valida al emitir y de nuevo al ejecutar. |
| RN-03 Ejecución diferida | Cumple | Al cierre del siguiente día bursátil del instrumento. |
| RN-04 Comisión | Cumple con cambio | 0,3 % con mínimo de 5 USD (original: 5.000). |
| RN-05 Costo base | Cumple | Promedio ponderado, sin comisiones. |
| RN-06 Derivado del libro | Cumple | Nada se materializa salvo las instantáneas diarias. |
| RN-07 Cantidades enteras | Cumple | |
| RN-08 Días bursátiles | Cumple | El calendario es por instrumento; el original no lo precisaba. |
| RN-09 El reloj no retrocede | Cumple | Lo garantiza la entidad, no solo el servicio. |
| RN-10 Decimal fijo | Cumple con cambio | El dinero se redondea a 2 decimales. Los precios usan 8 decimales porque 1 COP ≈ 0,00025 USD no cabe en menos. |

### Requerimientos no funcionales

| Requerimiento | Estado | Evidencia |
|---|---|---|
| RNF-01 Rendimiento | Cumple | Medido en iteraciones anteriores con el histórico real sobre H2: detalle del portafolio en 0,15 s (límite 2 s) y gráfico de 5 años entre 0,15 y 0,7 s (límite 3 s). Desde esta revisión, `PresupuestoDeConsultasTest` limita las consultas por operación. En MySQL 8.4, con un portafolio de un año: detalle en 0,12 s, gráfico de 5 años en 0,14 s y avance de un año en 0,57 s. |
| RNF-02 Seguridad | Cumple | bcrypt con costo 12, sesión de 30 minutos. Prueba nueva: una sesión cerrada no se puede reutilizar. |
| RNF-03 Aislamiento | Cumple | `AccesoCruzadoTest`. Un portafolio ajeno responde 404. |
| RNF-04 Atomicidad | Cumple | Prueba nueva: `AtomicidadDelAvanceTest` provoca un fallo a mitad del avance y comprueba que no queda nada. |
| RNF-05 Precisión decimal | Cumple | No hay ningún `double` ni `float` en el código de la aplicación. |
| RNF-06 Usabilidad | Cumple | Los mensajes de los flujos alternativos dicen qué pasó y qué hacer. |
| RNF-07 Sin conexión | Cumple | El equipo hizo la demostración con la red desconectada el 16 de noviembre de 2026. Además, `SinRecursosExternosTest` comprueba que ninguna plantilla, hoja de estilos o script pide recursos a otro servidor. |
| RNF-08 Pruebas | Cumple | El informe de cobertura que pide el criterio ahora existe (JaCoCo). |

### Decisiones de arquitectura y criterios de aceptación

| Decisión de arquitectura | Estado |
|---|---|
| 1. Datos precargados; precios solo por `ProveedorDePrecios` | Cumple, tras la corrección de la sección 1 |
| 2. Reloj por portafolio | Cumple |
| 3. La transacción es la única fuente de verdad | Cumple |
| 4. Ejecución diferida | Cumple |
| 5. Dinero en decimal fijo | Cumple |

| Criterio de aceptación | Estado |
|---|---|
| 1. Registro → portafolio → compra → avance → operación reflejada | Cumple, con prueba |
| 2. Efectivo recalculado igual al mostrado, con 50 operaciones o más | Cumple, con prueba |
| 3. Ninguna orden se ejecuta al precio visto al emitirla | Cumple, con prueba |
| 4. Acceso cruzado entre usuarios rechazado | Cumple, con prueba |
| 5. Demostración completa sin red | Cumple; la hizo el equipo el 16 de noviembre de 2026 |

### Cambios de alcance frente al original

Estos cambios los pidió el equipo durante el desarrollo. La versión 2.0 de `requerimientos.md` los documenta; el SRS y el diagrama de clases todavía no.

- **Divisas y criptomonedas como tipos de instrumento.** El original decía: "Instrumentos distintos de acciones y ETFs" fuera de alcance. Es el cambio más grande y conviene avisarlo al profesor.
- **USD como moneda de referencia**, con los montos de RN-01 y RN-04 divididos entre 1.000.
- **Precios con 8 decimales.**

## 4. Optimización

### Mediciones

Sentencias SQL que prepara Hibernate por operación, con el portafolio de prueba descrito arriba.

| Operación | Antes | Después |
|---|---:|---:|
| Detalle del portafolio | 31 | 17 |
| Rendimiento | 32 | 17 |
| Historial de transacciones | 13 | 3 |
| Cuenta de ahorro | 20 | 8 |
| Estimación de una orden | 19 | 9 |
| Avanzar el reloj un día | 50 | 40 |
| Avanzar el reloj 30 días | 70 | 60 |
| Avanzar el reloj un año | 417 | 407 |
| Barra superior (en cada página) | 4 | 4 |
| Catálogo (23 instrumentos) | 24 | 24 |
| Lista de portafolios | 4 | 4 |

### Qué se cambió

| Cambio | Efecto |
|---|---|
| Las consultas del libro y de las órdenes traen el instrumento en la misma sentencia (`@EntityGraph`). | Antes había una consulta adicional por cada instrumento distinto. Es lo que más redujo. |
| El resumen toma el efectivo y el saldo de ahorro del recorrido del libro que ya hace. | 4 sumas SQL menos por página. |
| El rendimiento recorre el libro una sola vez (`resumenConLibro`). | Antes lo leía dos veces. |
| La estimación de una orden reutiliza la lista de órdenes pendientes. | Antes la consultaba dos veces. |
| El reloj pregunta si queda algún día bursátil, en vez de traer la lista de días hasta el final del histórico. | Con un portafolio en 2008 esa lista tenía miles de fechas. |
| El detalle de un instrumento busca el portafolio activo una vez. | Antes, dos. |
| La vista inicial pregunta si el usuario tiene portafolios, en vez de listarlos con su efectivo. | Menos consultas al entrar. |

### Limpieza

- Código sin uso eliminado: `Transaccion.efectoEnEfectivo`, el signo de posición de `TipoTransaccion` y `InstrumentoRepositorio.findBySimboloAndActivoTrue`.
- `comoMonto` estaba duplicado en dos controladores; ahora está en `web.Entradas`.
- El mensaje "El portafolio está finalizado…" estaba armado en tres servicios; ahora lo arma `Portafolio.exigirEnCurso`.

Ninguno de estos cambios altera lo que ve el usuario.

### Qué se evaluó y no se cambió

| Posible mejora | Por qué no se hizo |
|---|---|
| Insertar en lote los intereses diarios. Bajaría el avance de un año de 407 a unas 45 sentencias. | Obligaría a escribir en la tabla `transaccion` por dos caminos (JPA y JDBC). El libro es la tabla más delicada del sistema y el avance de un año ya tarda menos de un segundo en H2. Vale la pena solo si en MySQL resulta lento. |
| Una sola consulta para el último precio de todos los instrumentos del catálogo. | No se midió y no es seguro que gane: las 23 consultas actuales van por índice y su número está acotado por el tamaño del catálogo. |
| Guardar el estado del libro para no recorrerlo completo en cada lectura. | RN-06 lo permite si existe un recálculo desde cero, pero agrega un caché que hay que mantener coherente. Con libros de cientos o pocos miles de filas no hace falta. |
| Una sola consulta para cargar cada serie de precios al avanzar el reloj (hoy son dos por instrumento). | Ahorra una consulta por instrumento y exige otro método en `ProveedorDePrecios`. Ganancia pequeña. |

## 5. Pruebas

99 pruebas en 24 clases. Todas pasan.

### Cobertura (JaCoCo)

Líneas: 87,0 % (1.438 de 1.653). Ramas: 77,2 %.

| Clases de cálculo (RNF-08) | Líneas | Ramas |
|---|---:|---:|
| `EstadoDelLibro` (costo base, P&L realizado) | 100 % | 100 % |
| `ReglasDeOperacion` (comisión, redondeo) | 100 % | 100 % |
| `CalculadoraDeIntereses` | 100 % | 100 % |
| `RendimientoServicio` | 100 % | 75 % |
| `RelojServicio` | 93 % | 88 % |
| `OrdenServicio` | 94 % | 90 % |
| `PortafolioServicio` | 93 % | 81 % |

Los paquetes con menos cobertura son `escenario` (62 %) y `portafolio` (73 %). Lo que no se cubre son sobre todo controladores y accesores.

### Pruebas agregadas en esta revisión

| Prueba | Qué verifica |
|---|---|
| `AtomicidadDelAvanceTest` | RNF-04. Provoca un fallo al final del avance, cuando la orden ya se ejecutó, y comprueba que no quedan transacciones, que la orden sigue pendiente y que el reloj no se movió. |
| `SeguridadYNavegacionWebTest.unaSesionCerradaNoSePuedeReutilizar` | RNF-02. |
| `PresupuestoDeConsultasTest` | RNF-01. Falla si una operación vuelve a hacer una consulta por fila. |
| `CargaEnDosPasosTest` | REQ-018. Validar no guarda nada; confirmar guarda; cancelar descarta; un archivo inválido se rechaza sin pedir confirmación. |

### Huecos que quedan

- **Concurrencia.** No hay prueba de dos operaciones simultáneas sobre el mismo portafolio (el bloqueo optimista).
- **MySQL.** Las pruebas automáticas corren sobre H2. Contra MySQL 8.4 se hizo una verificación manual el 16 de noviembre de 2026: migraciones, carga de los 23 históricos y el flujo completo, sin errores.
- **Precios futuros en la web.** Está probado en el proveedor de precios, pero no hay una prueba de la página del instrumento con un portafolio activo en una fecha pasada.

## 6. Seguridad

| Aspecto | Estado |
|---|---|
| Contraseñas | bcrypt con costo 12. |
| Sesión | Expira a los 30 minutos y se invalida al cerrar sesión (con prueba). |
| CSRF | Activo. Los 21 formularios usan `th:action`, que agrega el token. |
| Inyección SQL | Todas las consultas son parametrizadas. La búsqueda del catálogo escapa los comodines de `LIKE`. |
| XSS | Ninguna plantilla usa `th:utext`. Los datos para los gráficos pasan por `th:inline="javascript"`, que los escapa. |
| Control de acceso | Toda búsqueda de portafolio incluye al dueño. Las rutas `/admin/**` se bloquean en el servidor. |
| Recursos externos | Ninguno: sin CDN ni fuentes remotas. |

Lo que falta:

- **Límite de intentos de inicio de sesión.** Hoy se puede probar contraseñas sin restricción.
- **Política de contraseñas definitiva.** La actual solo exige longitud.
- **Cookie de sesión con `Secure`** si la aplicación se despliega con HTTPS.

## 7. Pendientes, por prioridad

| # | Pendiente | Por qué importa |
|---|---|---|
| 1 | Pasar al SRS y al PDF de UML los cambios de `docs/cambios-para-el-srs.md`. | El texto, el modelo de datos y el diagrama de clases actualizados ya están ahí; falta copiarlos a los documentos entregables. |
| 2 | Definir la política de contraseñas y limitar los intentos de inicio de sesión. | Seguridad. |
| 3 | Agregar una prueba de concurrencia del bloqueo optimista. | Es el único mecanismo importante sin prueba. |
| 4 | Separar el resumen del portafolio en un servicio de consulta. | Rompe tres de los cuatro ciclos entre paquetes. |
| 5 | Paginar el historial o filtrarlo en SQL. | Solo si los portafolios crecen a varios años con cuenta de ahorro. |

### Preparación del repositorio

Hecho en esta revisión:

- `README.md` con la descripción, los requisitos y los comandos para ejecutar.
- `.claude/launch.json` quedó fuera del repositorio (`.gitignore`): contiene la contraseña de un administrador de prueba para la vista previa local.
- Las notas internas de trabajo quedaron fuera del repositorio.

Por decidir antes de publicar:

- **Público o privado.** Los archivos de `datos/historico/` vienen de Yahoo Finance; conviene revisar sus condiciones de uso antes de publicarlos en un repositorio público. `scripts/descargar_historicos.py` permite regenerarlos, pero `DatosHistoricosTest` y la demostración sin red los necesitan.
- **Licencia.** El proyecto no tiene archivo `LICENSE`.

## Cómo reproducir

```powershell
.\mvnw.cmd test
```

Ejecuta las 99 pruebas y deja el informe de cobertura en `target/site/jacoco/index.html`.

Los límites de consultas por operación están en `src/test/java/co/edu/uis/siminversion/PresupuestoDeConsultasTest.java`. La columna "Antes" de la sección 4 se obtuvo con esa misma prueba sobre el código anterior a los cambios.
