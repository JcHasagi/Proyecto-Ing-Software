-- Iteración 2: portafolios, órdenes, libro de transacciones e instantáneas diarias.
-- Montos de dinero en DECIMAL(18,4) (RN-10); precios por unidad en DECIMAL(20,8), como precio_historico.

CREATE TABLE portafolio (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    usuario_id       BIGINT        NOT NULL,
    nombre           VARCHAR(80)   NOT NULL,
    saldo_inicial    DECIMAL(18,4) NOT NULL,
    fecha_inicio_sim DATE          NOT NULL,
    fecha_actual_sim DATE          NOT NULL,
    estado           VARCHAR(20)   NOT NULL,
    creado_en        DATETIME(6)   NOT NULL,
    version          BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT fk_portafolio_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id),
    CONSTRAINT ck_portafolio_estado CHECK (estado IN ('EN_CURSO', 'FINALIZADO', 'ARCHIVADO')),
    CONSTRAINT ck_portafolio_reloj CHECK (fecha_actual_sim >= fecha_inicio_sim)
);
CREATE INDEX ix_portafolio_usuario ON portafolio (usuario_id);

-- Portafolio activo: el último que usó el usuario (REQ-002).
ALTER TABLE usuario ADD COLUMN portafolio_activo_id BIGINT;
ALTER TABLE usuario ADD CONSTRAINT fk_usuario_portafolio_activo
    FOREIGN KEY (portafolio_activo_id) REFERENCES portafolio (id);

CREATE TABLE orden (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    portafolio_id        BIGINT       NOT NULL,
    instrumento_id       BIGINT       NOT NULL,
    tipo                 VARCHAR(10)  NOT NULL,
    cantidad             BIGINT       NOT NULL,
    estado               VARCHAR(20)  NOT NULL,
    fecha_emision_sim    DATE         NOT NULL,
    fecha_resolucion_sim DATE,
    motivo               VARCHAR(255),
    creado_en            DATETIME(6)  NOT NULL,
    version              BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT fk_orden_portafolio FOREIGN KEY (portafolio_id) REFERENCES portafolio (id),
    CONSTRAINT fk_orden_instrumento FOREIGN KEY (instrumento_id) REFERENCES instrumento (id),
    CONSTRAINT ck_orden_tipo CHECK (tipo IN ('COMPRA', 'VENTA')),
    CONSTRAINT ck_orden_estado CHECK (estado IN ('PENDIENTE', 'EJECUTADA', 'CANCELADA', 'RECHAZADA')),
    CONSTRAINT ck_orden_cantidad CHECK (cantidad > 0)
);
CREATE INDEX ix_orden_portafolio_estado ON orden (portafolio_id, estado);

-- Libro de transacciones: única fuente del efectivo y las posiciones (RN-06). No se modifica ni se borra.
-- Cada ejecución registra dos filas en el mismo paso: la operación (COMPRA o VENTA) y su COMISION.
CREATE TABLE transaccion (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    portafolio_id    BIGINT        NOT NULL,
    tipo             VARCHAR(20)   NOT NULL,
    orden_id         BIGINT,
    instrumento_id   BIGINT,
    cantidad         BIGINT,
    precio_ejecucion DECIMAL(20,8),
    monto            DECIMAL(18,4) NOT NULL,
    fecha_sim        DATE          NOT NULL,
    creado_en        DATETIME(6)   NOT NULL,
    CONSTRAINT fk_transaccion_portafolio FOREIGN KEY (portafolio_id) REFERENCES portafolio (id),
    CONSTRAINT fk_transaccion_orden FOREIGN KEY (orden_id) REFERENCES orden (id),
    CONSTRAINT fk_transaccion_instrumento FOREIGN KEY (instrumento_id) REFERENCES instrumento (id),
    CONSTRAINT ck_transaccion_tipo CHECK (tipo IN ('DEPOSITO_INICIAL', 'COMPRA', 'VENTA', 'COMISION')),
    CONSTRAINT ck_transaccion_monto CHECK (monto >= 0)
);
CREATE INDEX ix_transaccion_portafolio_fecha ON transaccion (portafolio_id, fecha_sim);

-- Valor del portafolio al cierre de cada día bursátil procesado por el reloj (REQ-009).
CREATE TABLE instantanea_portafolio (
    portafolio_id    BIGINT        NOT NULL,
    fecha_sim        DATE          NOT NULL,
    efectivo         DECIMAL(18,4) NOT NULL,
    valor_posiciones DECIMAL(18,4) NOT NULL,
    valor_total      DECIMAL(18,4) NOT NULL,
    PRIMARY KEY (portafolio_id, fecha_sim),
    CONSTRAINT fk_instantanea_portafolio FOREIGN KEY (portafolio_id) REFERENCES portafolio (id)
);

-- Días bursátiles del mercado (fechas con algún cierre), que recorre el reloj.
CREATE INDEX ix_precio_fecha ON precio_historico (fecha);
