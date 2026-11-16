-- Iteración 4: escenarios históricos (REQ-014) y contenido educativo (REQ-016).

-- Un escenario fija el período del reloj: empieza en fecha_inicio (o el siguiente día bursátil)
-- y el portafolio queda finalizado al llegar a fecha_fin (decisión del 2026-11-16).
CREATE TABLE escenario (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre              VARCHAR(80)  NOT NULL,
    descripcion         VARCHAR(600) NOT NULL,
    fecha_inicio        DATE         NOT NULL,
    fecha_fin           DATE         NOT NULL,
    orden_visualizacion INT          NOT NULL,
    CONSTRAINT ck_escenario_fechas CHECK (fecha_fin > fecha_inicio)
);

ALTER TABLE portafolio ADD COLUMN escenario_id BIGINT;
ALTER TABLE portafolio ADD CONSTRAINT fk_portafolio_escenario FOREIGN KEY (escenario_id) REFERENCES escenario (id);

INSERT INTO escenario (nombre, descripcion, fecha_inicio, fecha_fin, orden_visualizacion) VALUES
    ('Crisis financiera de 2008',
     'Empieza dos semanas antes de la quiebra de Lehman Brothers y termina en el punto más bajo del mercado, en marzo de 2009. Las bolsas cayeron con fuerza y la volatilidad fue extrema. En esas fechas todavía no hay datos de las criptomonedas del catálogo.',
     '2008-09-02', '2009-03-09', 1),
    ('Mercado alcista de 2013',
     'Un año de subidas casi constantes: el S&P 500 ganó cerca de 30 %. Sirve para comparar invertir con dejar el dinero en la cuenta de ahorro. Todavía no hay datos de criptomonedas.',
     '2013-01-02', '2013-12-31', 2),
    ('Caída por COVID-19 (2020)',
     'Incluye la caída más rápida de la historia reciente, en marzo de 2020, y la recuperación de los meses siguientes. Pon a prueba qué haces cuando el portafolio cae en pocas semanas.',
     '2020-02-03', '2020-08-31', 3),
    ('Mercado bajista de 2022',
     'Un año de subidas de tasas de interés: cayeron las acciones tecnológicas, los bonos de largo plazo y las criptomonedas. Muestra que diversificar no siempre evita las pérdidas.',
     '2022-01-03', '2022-12-30', 4);

-- Artículos educativos. Los párrafos del contenido se separan con una línea en blanco.
-- No contienen recomendaciones de inversión.
CREATE TABLE articulo (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    titulo              VARCHAR(120)  NOT NULL,
    resumen             VARCHAR(300)  NOT NULL,
    contenido           VARCHAR(8000) NOT NULL,
    orden_visualizacion INT           NOT NULL
);

INSERT INTO articulo (titulo, resumen, contenido, orden_visualizacion) VALUES
('Qué es una acción',
 'Una acción es una parte pequeña de una empresa. Su precio sube y baja según lo que el mercado espera de ella.',
 'Cuando una empresa divide su propiedad en muchas partes iguales, cada parte se llama acción. Quien compra una acción se vuelve dueño de una fracción muy pequeña de la empresa.

El precio de una acción cambia todos los días que la bolsa abre. Sube cuando más personas quieren comprarla que venderla y baja en el caso contrario. Detrás de esas decisiones están las expectativas sobre las ganancias futuras de la empresa, las noticias y el ánimo general del mercado.

Ganar o perder con una acción depende de la diferencia entre el precio al que se compra y el precio al que se vende. Mientras no se venda, esa diferencia es solo una ganancia o pérdida en papel.

En este simulador las acciones son de empresas de Estados Unidos y sus precios están en dólares. Los precios están ajustados por divisiones de acciones (splits), pero no incluyen dividendos.',
 1),
('Qué es un ETF',
 'Un ETF es un fondo que reúne muchos activos y se compra y vende en la bolsa como si fuera una sola acción.',
 'ETF significa Exchange Traded Fund, es decir, fondo cotizado en bolsa. Es una canasta de activos (acciones, bonos u oro, por ejemplo) que se negocia en la bolsa igual que una acción.

Muchos ETF siguen un índice. SPY, por ejemplo, replica el S&P 500, que agrupa a unas 500 de las empresas más grandes de Estados Unidos. Al comprar una unidad de SPY se obtiene, en un solo paso, una pequeña exposición a todas esas empresas.

Otros ETF se enfocan en un sector o en otro tipo de activo: QQQ sigue a grandes empresas tecnológicas, GLD sigue el precio del oro y TLT invierte en bonos del Tesoro de Estados Unidos a largo plazo.

Como un ETF contiene muchos activos, su precio suele moverse menos que el de una sola empresa. Eso no significa que no pueda perder valor: si cae todo el mercado que sigue, el ETF también cae.',
 2),
('Diversificación',
 'Diversificar es repartir el dinero entre activos distintos para que un solo resultado no defina todo el portafolio.',
 'La idea se resume en un refrán: no poner todos los huevos en la misma canasta. Si todo el dinero está en una sola empresa, una mala noticia de esa empresa afecta todo el portafolio.

Al repartir el dinero entre varias acciones, sectores y tipos de activo, las caídas de unos pueden compensarse con el comportamiento de otros. El resultado total tiende a ser más estable.

La diversificación funciona mejor cuando los activos no se mueven igual. Dos bancos suelen reaccionar parecido ante la misma noticia; un banco y un fondo de bonos, no necesariamente.

Diversificar reduce el riesgo propio de cada empresa, pero no elimina el riesgo de que todo el mercado caiga al mismo tiempo, como ocurrió en 2008 o en marzo de 2020. Puedes comprobarlo con los escenarios históricos del simulador.',
 3),
('Riesgo',
 'El riesgo es la posibilidad de que el resultado de una inversión sea distinto del esperado, incluida la pérdida de dinero.',
 'Toda inversión tiene riesgo. En términos sencillos, es la incertidumbre sobre cuánto valdrá mañana lo que tienes hoy. Una cuenta de ahorro con tasa fija tiene poco riesgo; una criptomoneda puede ganar o perder la mitad de su valor en pocos meses.

En general, los activos que pueden dar mayores ganancias también pueden dar mayores pérdidas. No existe una ganancia alta garantizada.

Hay riesgos de distinto origen: el de una empresa en particular, el del mercado completo, el de las tasas de interés o el del tipo de cambio cuando se compran divisas.

Una forma práctica de entender tu tolerancia al riesgo es preguntarte cuánto podrías ver caer tu portafolio sin cambiar de plan. En el simulador puedes vivir esas caídas sin perder dinero real.',
 4),
('Volatilidad',
 'La volatilidad mide qué tanto y qué tan rápido cambia el precio de un activo.',
 'Un activo es volátil cuando su precio sube y baja con fuerza en poco tiempo. En el gráfico se ve como una línea con picos y caídas pronunciadas; un activo poco volátil dibuja una línea más suave.

Las criptomonedas suelen ser mucho más volátiles que las acciones grandes, y las acciones más volátiles que los bonos o las divisas principales. Compara en el catálogo el gráfico de BTCUSD con el de EURUSD en el mismo rango.

La volatilidad no indica si un activo va a subir o a bajar; indica qué tan amplios pueden ser los movimientos. Por eso se usa como una medida de riesgo.

En periodos de crisis la volatilidad aumenta en casi todo el mercado al mismo tiempo. El escenario de la caída de 2020 es un buen ejemplo.',
 5),
('Interés compuesto',
 'El interés compuesto es ganar intereses sobre los intereses que ya se ganaron.',
 'Con interés simple, siempre se gana sobre el monto inicial. Con interés compuesto, los intereses se suman al saldo y desde ese momento también generan intereses.

Por ejemplo, 1.000 dólares al 4 % anual se convierten en 1.040 al cabo de un año. El segundo año el 4 % se calcula sobre 1.040, no sobre 1.000, y así sucesivamente. Con el tiempo, la diferencia frente al interés simple crece cada vez más.

La cuenta de ahorro del simulador paga una tasa anual fija y suma los intereses en cada día que avanzas el reloj. Como los intereses de un día se suman al saldo, al día siguiente también ganan intereses.

El factor más importante del interés compuesto es el tiempo. Avanza el reloj varios años con dinero en la cuenta de ahorro y observa cómo crece el saldo.',
 6),
('Órdenes y comisiones en el simulador',
 'Las órdenes no se ejecutan al precio que ves: quedan pendientes y se ejecutan al cierre del siguiente día bursátil.',
 'Cuando compras o vendes, creas una orden. La orden queda pendiente porque el simulador no permite operar con un precio que ya conoces: eso sería como saber el futuro.

Al avanzar el reloj, cada orden pendiente se ejecuta al precio de cierre del siguiente día en que su instrumento cotiza. Por eso el precio final puede ser distinto del estimado. Si para entonces no te alcanza el efectivo o ya no tienes las unidades que querías vender, la orden se rechaza y verás el motivo.

Cada ejecución paga una comisión del 0,3 % del monto operado, con un mínimo de 5 dólares. En una compra la comisión sale de tu efectivo; en una venta se descuenta de lo que recibes. Hacer muchas operaciones pequeñas puede costar más en comisiones de lo que ganas.

Mientras una orden siga pendiente, puedes cancelarla desde la página de tu portafolio.',
 7),
('Ganancia realizada y no realizada',
 'La ganancia realizada viene de lo que ya vendiste; la no realizada, de lo que todavía tienes.',
 'Si compras una acción a 100 dólares y hoy vale 120, tienes una ganancia de 20, pero solo en papel: se llama ganancia no realizada, porque cambiará si el precio cambia mañana.

Cuando vendes, la diferencia entre el precio de venta y lo que te costó cada unidad pasa a ser ganancia realizada. Ese resultado ya no cambia.

Para saber lo que te costó cada unidad, el simulador usa el costo promedio ponderado: si compras 10 unidades a 100 y otras 10 a 120, tu costo base es 110 por unidad. Al vender una parte, el costo base de las unidades que quedan no cambia.

En la pestaña Rendimiento de tu portafolio verás ambas ganancias por separado, junto con las comisiones pagadas y los intereses de la cuenta de ahorro.',
 8);
