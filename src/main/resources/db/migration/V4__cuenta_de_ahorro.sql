-- Iteración 3: cuenta de ahorro (REQ-013) y sus movimientos en el libro de transacciones.
-- La cuenta no guarda saldo: se deriva de las transacciones, igual que el efectivo (RN-06).

ALTER TABLE transaccion DROP CONSTRAINT ck_transaccion_tipo;
ALTER TABLE transaccion ADD CONSTRAINT ck_transaccion_tipo CHECK (tipo IN (
    'DEPOSITO_INICIAL', 'COMPRA', 'VENTA', 'COMISION', 'DEPOSITO_AHORRO', 'RETIRO_AHORRO', 'INTERES_AHORRO'));

-- 0..1 por portafolio. La tasa queda fija al abrirla, aunque luego cambie la configuración.
CREATE TABLE cuenta_ahorro (
    portafolio_id               BIGINT       PRIMARY KEY,
    tasa_anual                  DECIMAL(9,6) NOT NULL,
    fecha_apertura_sim          DATE         NOT NULL,
    fecha_ultima_capitalizacion DATE         NOT NULL,
    CONSTRAINT fk_cuenta_ahorro_portafolio FOREIGN KEY (portafolio_id) REFERENCES portafolio (id),
    CONSTRAINT ck_cuenta_ahorro_tasa CHECK (tasa_anual >= 0 AND tasa_anual <= 1)
);

ALTER TABLE instantanea_portafolio ADD COLUMN saldo_ahorro DECIMAL(18,4) NOT NULL DEFAULT 0;
