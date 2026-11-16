-- Catálogo inicial (sin precios: el histórico se carga con REQ-018).
-- Acciones y ETFs con historia anterior a 2008 para que sirvan en los escenarios.
-- Divisas y criptomonedas cotizan como precio en USD de 1 unidad (EURUSD = USD por 1 EUR).

INSERT INTO instrumento (simbolo, nombre, tipo, sector, activo) VALUES
    ('AAPL',   'Apple Inc.',                            'ACCION',       'Tecnología',             TRUE),
    ('MSFT',   'Microsoft Corporation',                 'ACCION',       'Tecnología',             TRUE),
    ('AMZN',   'Amazon.com Inc.',                       'ACCION',       'Consumo discrecional',   TRUE),
    ('GOOGL',  'Alphabet Inc. (clase A)',               'ACCION',       'Comunicaciones',         TRUE),
    ('JPM',    'JPMorgan Chase & Co.',                  'ACCION',       'Financiero',             TRUE),
    ('BAC',    'Bank of America Corporation',           'ACCION',       'Financiero',             TRUE),
    ('KO',     'The Coca-Cola Company',                 'ACCION',       'Consumo básico',         TRUE),
    ('PG',     'The Procter & Gamble Company',          'ACCION',       'Consumo básico',         TRUE),
    ('JNJ',    'Johnson & Johnson',                     'ACCION',       'Salud',                  TRUE),
    ('XOM',    'Exxon Mobil Corporation',               'ACCION',       'Energía',                TRUE),
    ('WMT',    'Walmart Inc.',                          'ACCION',       'Consumo básico',         TRUE),
    ('DIS',    'The Walt Disney Company',               'ACCION',       'Comunicaciones',         TRUE),
    ('SPY',    'SPDR S&P 500 ETF Trust',                'ETF',          'Renta variable EE. UU.', TRUE),
    ('QQQ',    'Invesco QQQ Trust',                     'ETF',          'Tecnología (Nasdaq-100)', TRUE),
    ('GLD',    'SPDR Gold Shares',                      'ETF',          'Materias primas (oro)',  TRUE),
    ('TLT',    'iShares 20+ Year Treasury Bond ETF',    'ETF',          'Renta fija (bonos EE. UU.)', TRUE),
    ('EURUSD', 'Euro',                                  'DIVISA',       NULL,                     TRUE),
    ('GBPUSD', 'Libra esterlina',                       'DIVISA',       NULL,                     TRUE),
    ('JPYUSD', 'Yen japonés',                           'DIVISA',       NULL,                     TRUE),
    ('COPUSD', 'Peso colombiano',                       'DIVISA',       NULL,                     TRUE),
    ('BTCUSD', 'Bitcoin',                               'CRIPTOMONEDA', NULL,                     TRUE),
    ('ETHUSD', 'Ethereum',                              'CRIPTOMONEDA', NULL,                     TRUE),
    ('LTCUSD', 'Litecoin',                              'CRIPTOMONEDA', NULL,                     TRUE);
