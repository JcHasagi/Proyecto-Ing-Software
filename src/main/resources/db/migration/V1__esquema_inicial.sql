-- Iteración 1: usuarios, catálogo de instrumentos e histórico de precios.
-- Debe correr igual en MySQL 8 y en H2 (modo MySQL), que usan las pruebas.

CREATE TABLE usuario (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    correo          VARCHAR(254) NOT NULL,
    nombre          VARCHAR(100) NOT NULL,
    hash_contrasena VARCHAR(100) NOT NULL,
    rol             VARCHAR(20)  NOT NULL,
    activo          BOOLEAN      NOT NULL DEFAULT TRUE,
    fecha_registro  DATETIME(6)  NOT NULL,
    CONSTRAINT uq_usuario_correo UNIQUE (correo),
    CONSTRAINT ck_usuario_rol CHECK (rol IN ('INVERSIONISTA', 'ADMINISTRADOR'))
);

CREATE TABLE instrumento (
    id      BIGINT AUTO_INCREMENT PRIMARY KEY,
    simbolo VARCHAR(20)  NOT NULL,
    nombre  VARCHAR(120) NOT NULL,
    tipo    VARCHAR(20)  NOT NULL,
    sector  VARCHAR(80),
    activo  BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_instrumento_simbolo UNIQUE (simbolo),
    CONSTRAINT ck_instrumento_tipo CHECK (tipo IN ('ACCION', 'ETF', 'DIVISA', 'CRIPTOMONEDA'))
);

-- Precio por unidad en la moneda de referencia (USD). Usa 8 decimales y no DECIMAL(18,4):
-- 1 COP vale unos 0,00025 USD y con 4 decimales quedaría en 0,0003.
-- El volumen admite NULL porque las divisas no tienen volumen centralizado.
CREATE TABLE precio_historico (
    instrumento_id BIGINT        NOT NULL,
    fecha          DATE          NOT NULL,
    apertura       DECIMAL(20,8) NOT NULL,
    maximo         DECIMAL(20,8) NOT NULL,
    minimo         DECIMAL(20,8) NOT NULL,
    cierre         DECIMAL(20,8) NOT NULL,
    volumen        BIGINT,
    PRIMARY KEY (instrumento_id, fecha),
    CONSTRAINT fk_precio_instrumento FOREIGN KEY (instrumento_id) REFERENCES instrumento (id)
);
