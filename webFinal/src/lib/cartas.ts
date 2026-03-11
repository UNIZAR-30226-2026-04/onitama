/**
 * Definición de las cartas de movimiento del juego Onitama.
 * Fuente: BaseDatos/construir.sql
 *
 * Convención de coordenadas (dc, df):
 *  - dc = delta de columna  (+  = derecha,  − = izquierda)
 *  - df = delta de fila en sentido del jugador actual
 *        (+ = avanzar hacia el oponente,  − = retroceder)
 *
 * Traducción al tablero:
 *  - Equipo 2 (jugador, abajo, avanza hacia fila 0):
 *      nueva_fila = fila − df
 *      nueva_col  = col  + dc
 *  - Equipo 1 (oponente, arriba, avanza hacia fila 6):
 *      nueva_fila = fila + df       (dirección invertida)
 *      nueva_col  = col  − dc       (espejo horizontal)
 *
 * NOTA para el servidor: el Java Tablero.java usa deltas brutos sin inversión.
 * Al integrar con el servidor habrá que acordar si se normaliza aquí o allí.
 */

/** Emoji provisional para cada carta hasta disponer de imágenes reales */
export interface CartaMovDef {
  nombre: string;
  emoji: string;
  /** Movimientos como (dc, df) según el jugador activo */
  movimientos: { dc: number; df: number }[];
}

/**
 * Catálogo completo de cartas de movimiento (16 cartas).
 * Los valores dc/df proceden directamente del SQL (x,y → dc,df).
 */
export const TODAS_LAS_CARTAS: CartaMovDef[] = [
  { nombre: "Tigre",    emoji: "🐯", movimientos: [{ dc:  0, df:-1 }, { dc:  0, df: 2 }] },
  { nombre: "Dragon",   emoji: "🐉", movimientos: [{ dc: -2, df: 1 }, { dc: -1, df:-1 }, { dc: 1, df: 1 }, { dc: 2, df: 1 }] },
  { nombre: "Rana",     emoji: "🐸", movimientos: [{ dc: -1, df: 1 }, { dc:  1, df:-1 }, { dc:-2, df: 0 }] },
  { nombre: "Conejo",   emoji: "🐰", movimientos: [{ dc:  1, df: 1 }, { dc: -1, df: 1 }, { dc: 2, df: 0 }] },
  { nombre: "Cangrejo", emoji: "🦀", movimientos: [{ dc: -2, df: 0 }, { dc:  2, df: 0 }, { dc: 0, df: 1 }] },
  { nombre: "Elefante", emoji: "🐘", movimientos: [{ dc:  1, df: 1 }, { dc: -1, df: 1 }, { dc: 1, df: 0 }, { dc:-1, df: 0 }] },
  { nombre: "Ganso",    emoji: "🦆", movimientos: [{ dc: -1, df: 1 }, { dc: -1, df: 0 }, { dc: 1, df: 0 }, { dc: 1, df:-1 }] },
  { nombre: "Gallo",    emoji: "🐓", movimientos: [{ dc:  1, df: 0 }, { dc: -1, df: 0 }, { dc:-1, df:-1 }, { dc: 1, df: 1 }] },
  { nombre: "Mono",     emoji: "🐒", movimientos: [{ dc:  1, df: 1 }, { dc: -1, df:-1 }, { dc:-1, df: 1 }, { dc: 1, df:-1 }] },
  { nombre: "Mantis",   emoji: "🦗", movimientos: [{ dc:  0, df:-1 }, { dc: -1, df:-1 }, { dc: 1, df: 1 }] },
  { nombre: "Caballo",  emoji: "🐴", movimientos: [{ dc:  0, df: 1 }, { dc:  0, df:-1 }, { dc:-1, df: 0 }] },
  { nombre: "Buey",     emoji: "🐂", movimientos: [{ dc:  0, df: 1 }, { dc:  0, df:-1 }, { dc: 1, df: 0 }] },
  { nombre: "Grulla",   emoji: "🦢", movimientos: [{ dc:  1, df:-1 }, { dc: -1, df:-1 }, { dc: 0, df: 1 }] },
  { nombre: "Oso",      emoji: "🐻", movimientos: [{ dc:  1, df: 0 }, { dc: -1, df: 0 }, { dc: 0, df: 1 }] },
  { nombre: "Aguila",   emoji: "🦅", movimientos: [{ dc: -1, df: 0 }, { dc: -1, df: 1 }, { dc:-1, df:-1 }] },
  { nombre: "Cobra",    emoji: "🐍", movimientos: [{ dc: -1, df: 0 }, { dc:  1, df: 1 }, { dc: 1, df:-1 }] },
];

/**
 * Devuelve la ruta de la imagen para una carta.
 * Los archivos en public usan el nombre del animal (Trigre.png para Tigre).
 */
export function getImagenCarta(nombre: string): string {
  const mapa: Record<string, string> = {
    Tigre: "Trigre", Dragon: "Dragon", Rana: "Rana", Conejo: "Conejo",
    Cangrejo: "Cangrejo", Elefante: "Elefante", Ganso: "Ganso", Gallo: "Gallo",
    Mono: "Mono", Mantis: "Mantis", Caballo: "Caballo", Buey: "Buey",
    Grulla: "Grulla", Oso: "Oso", Aguila: "Aguila", Cobra: "Cobra",
  };
  const archivo = mapa[nombre] ?? nombre;
  return `/${archivo}.png`;
}

/**
 * Devuelve n cartas elegidas aleatoriamente del catálogo completo.
 * Se usa al crear una partida local (mock) sin servidor.
 */
export function seleccionarCartasAleatorias(n: number): CartaMovDef[] {
  const copia = [...TODAS_LAS_CARTAS];
  for (let i = copia.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    [copia[i], copia[j]] = [copia[j], copia[i]];
  }
  return copia.slice(0, n);
}
