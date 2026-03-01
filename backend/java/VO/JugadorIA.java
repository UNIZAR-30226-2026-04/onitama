package VO;

import java.util.List;
import java.util.ArrayList;

public class JugadorIA {
    public enum NivelDificultad {
        FACIL,
        MEDIO,
        DIFICIL
    }

    private NivelDificultad nivel;
    private String algoritmo;
    private int equipo; //Equipo que juega la IA (1 o 2)

    //Clase para representar una jugada completa
    public static class Jugada {
        public boolean esAccion;        //true = carta de accion, false = carta de movimiento
        public String cartaMovimiento;  //Nombre de la carta de movimiento usada (si esAccion=false)
        public int origenX, origenY;    //Posicion de la ficha que se mueve (si esAccion=false)
        public int destinoX, destinoY;  //Posicion destino (si esAccion=false)
        public String cartaAccion;      //Nombre de la carta de accion usada (si esAccion=true)
        public String tipoAccion;       //Tipo de accion (ESPEJO, REVIVIR...)

        //Constructor para jugada de movimiento
        public Jugada(String cartaMov, int ox, int oy, int dx, int dy) {
            this.esAccion = false;
            this.cartaMovimiento = cartaMov;
            this.origenX = ox;
            this.origenY = oy;
            this.destinoX = dx;
            this.destinoY = dy;
            this.cartaAccion = null;
            this.tipoAccion = null;
        }

        //Constructor para jugada de accion
        public Jugada(String cartaAccion, String tipoAccion) {
            this.esAccion = true;
            this.cartaAccion = cartaAccion;
            this.tipoAccion = tipoAccion;
            this.cartaMovimiento = null;
            this.origenX = -1;
            this.origenY = -1;
            this.destinoX = -1;
            this.destinoY = -1;
        }

        public String toString() {
            if (esAccion) {
                return "Accion:" + cartaAccion + " (" + tipoAccion + ")";
            } else {
                return "Carta:" + cartaMovimiento + " (" + origenX + "," + origenY + ")->(" + destinoX + "," + destinoY + ")";
            }
        }
    }

    //Estado interno ligero para la simulacion de minimax (no modifica el tablero real)
    private static class Estado {
        int[][] tablero; //0=vacio, 1=peon_eq1, 2=peon_eq2, 3=rey_eq1, 4=rey_eq2
        int DIM;
        int centroX; //Columna del trono
        List<CartaMov> cartasEq1;
        List<CartaMov> cartasEq2;
        List<CartaMov> cartasMazo;
        List<CartaAccion> accionesEq1;
        List<CartaAccion> accionesEq2;

        Estado clonar() {
            Estado copia = new Estado();
            copia.DIM = this.DIM;
            copia.centroX = this.centroX;
            copia.tablero = new int[DIM][DIM];
            for (int i = 0; i < DIM; i++) {
                for (int j = 0; j < DIM; j++) {
                    copia.tablero[i][j] = this.tablero[i][j];
                }
            }
            copia.cartasEq1 = new ArrayList<>(this.cartasEq1);
            copia.cartasEq2 = new ArrayList<>(this.cartasEq2);
            copia.cartasMazo = new ArrayList<>(this.cartasMazo);
            copia.accionesEq1 = new ArrayList<>(this.accionesEq1);
            copia.accionesEq2 = new ArrayList<>(this.accionesEq2);
            return copia;
        }
    }

    public JugadorIA(NivelDificultad nivel, int equipo) {
        this.nivel = nivel;
        this.algoritmo = "minimax_alfabeta";
        this.equipo = equipo;
    }

    //Devuelve la profundidad de busqueda segun el nivel de dificultad
    private int getProfundidad() {
        switch (nivel) {
            case FACIL:    return 2;
            case MEDIO:    return 4;
            case DIFICIL:  return 6;
            default:       return 2;
        }
    }
    //Selecciona la mejor carta de movimiento a usar (null si la mejor jugada es una accion)
    public CartaMov seleccionarCarta(Partida partida) {
        Jugada mejor = calcularMejorMovimiento(partida);
        if (mejor == null || mejor.esAccion) return null;

        //Buscar la carta de movimiento correspondiente
        List<CartaMov> cartas = partida.getCartasMovimiento();
        for (CartaMov cm : cartas) {
            if (cm.getNombre().equals(mejor.cartaMovimiento)) {
                return cm;
            }
        }
        return null;
    }

    //Metodo principal: calcula el mejor movimiento para la IA en la partida actual
    public Jugada calcularMejorMovimiento(Partida partida) {
        Estado estado = crearEstado(partida);
        int profundidad = getProfundidad();

        List<Jugada> movimientos = generarMovimientos(estado, equipo);
        //Caso de seguiridad
        if (movimientos.isEmpty()) {
            return null; //No hay movimientos posibles
        }
        //Inicializamos la mejor jugada y el mejor valor
        Jugada mejorJugada = null;
        int mejorValor = Integer.MIN_VALUE;
        //Recorremos todos los movimientos posibles
        for (Jugada jugada : movimientos) {
            Estado copia = estado.clonar();
            aplicarJugada(copia, jugada, equipo);
            //Calculamos el valor de la jugada
            int valor = minimax(copia, profundidad - 1, Integer.MIN_VALUE, Integer.MAX_VALUE, false);
            //Si el valor es mejor que el mejor valor actual, actualizamos
            if (valor > mejorValor) {
                mejorValor = valor;
                mejorJugada = jugada;
            }
        }
        return mejorJugada;
    }

    //Evalua el tablero actual de la partida desde la perspectiva de la IA
    public int evaluarTablero(Partida partida) {
        Estado estado = crearEstado(partida);
        return evaluar(estado);
    }

    //Minimax con poda alfa-beta
    private int minimax(Estado estado, int profundidad, int alpha, int beta, boolean maximizando) {
        //Condicion de parada con profundidad 0 o estado terminal
        int victoria = comprobarVictoria(estado);
        if (victoria != 0) {
            //Victoria detectada: valor muy alto/bajo segun quien gana
            return (victoria == equipo) ? 100000 + profundidad : -100000 - profundidad;
        }
        if (profundidad == 0) {
            return evaluar(estado);
        }

        int equipoActual = maximizando ? equipo : equipoRival();
        List<Jugada> movimientos = generarMovimientos(estado, equipoActual);
        if (movimientos.isEmpty()) {
            return evaluar(estado); //Sin movimientos, evaluar posicion actual
        }
        //Si es el turno de la IA (maximiza)
        if (maximizando) {
            int maxEval = Integer.MIN_VALUE;
            for (Jugada jugada : movimientos) {
                Estado copia = estado.clonar();
                aplicarJugada(copia, jugada, equipoActual);
                int eval = minimax(copia, profundidad - 1, alpha, beta, false);
                maxEval = Math.max(maxEval, eval);
                alpha = Math.max(alpha, eval);
                if (beta <= alpha) break; //Poda beta
            }
            return maxEval;
        //Si es el turno del jugador (minimiza)
        } else {
            int minEval = Integer.MAX_VALUE;
            for (Jugada jugada : movimientos) {
                Estado copia = estado.clonar();
                aplicarJugada(copia, jugada, equipoActual);
                int eval = minimax(copia, profundidad - 1, alpha, beta, true);
                minEval = Math.min(minEval, eval);
                beta = Math.min(beta, eval);
                if (beta <= alpha) break; //Poda alfa
            }
            return minEval;
        }
    }

    //Genera todas las jugadas posibles para un equipo en el estado dado, osea comprobacion y añadicion de jugadas
    //Por turno solo se puede jugar UNA carta: o de movimiento o de accion
    private List<Jugada> generarMovimientos(Estado estado, int equipoActual) {
        List<Jugada> jugadas = new ArrayList<>();

        //OPCION 1: Jugar una carta de movimiento (mover una ficha)
        List<CartaMov> cartasDisponibles = (equipoActual == 1) ? estado.cartasEq1 : estado.cartasEq2;
        for (CartaMov carta : cartasDisponibles) {
            List<Posicion> movimientos = carta.getListaMovimientos();
            for (int y = 0; y < estado.DIM; y++) {
                for (int x = 0; x < estado.DIM; x++) {
                    if (!esFichaDeEquipo(estado.tablero[y][x], equipoActual)) continue;
                    for (Posicion mov : movimientos) {
                        int dx = mov.getX();
                        int dy = mov.getY();

                        //El equipo 2 tiene los movimientos invertidos (perspectiva opuesta)
                        if (equipoActual == 2) {
                            dx = -dx;
                            dy = -dy;
                        }

                        int nx = x + dx;
                        int ny = y + dy;

                        //Comprobar que la casilla destino es valida y no tiene ficha aliada
                        if (nx >= 0 && nx < estado.DIM && ny >= 0 && ny < estado.DIM) {
                            if (!esFichaDeEquipo(estado.tablero[ny][nx], equipoActual)) {
                                jugadas.add(new Jugada(carta.getNombre(), x, y, nx, ny));
                            }
                        }
                    }
                }
            }
        }

        //Lo mismo pero para las cartas de accion
        List<CartaAccion> accionesDisponibles = (equipoActual == 1) ? estado.accionesEq1 : estado.accionesEq2;
        for (CartaAccion ca : accionesDisponibles) {
            if (!ca.puedeUsarse()) continue;
            String tipo = ca.getAccion();
            //Solo generar jugadas de accion que tengan efecto real en el estado
            switch (tipo) {
                case "REVIVIR":
                    //Solo si el trono propio esta vacio (hay donde revivir)
                    int tronoY = (equipoActual == 1) ? 0 : estado.DIM - 1;
                    if (estado.tablero[tronoY][estado.centroX] == 0) {
                        jugadas.add(new Jugada(ca.getNombre(), tipo));
                    }
                    break;

                case "SACRIFICIO":
                    //Solo si tenemos peones y el rival tiene fichas
                    if (tienePeones(estado, equipoActual) && tieneFichas(estado, equipoActual == 1 ? 2 : 1)) {
                        jugadas.add(new Jugada(ca.getNombre(), tipo));
                    }
                    break;
                //Cartas que no tienen condiciones especiales, se pueden jugar siempre
                case "ESPEJO":
                case "SALVAR_REY":
                case "SOLO_PARA_ADELANTE":
                case "SOLO_PARA_ATRAS":
                case "VENGANZA":
                case "ROBAR":
                case "CEGAR":
                    //Estas acciones siempre se pueden jugar si estan disponibles
                    jugadas.add(new Jugada(ca.getNombre(), tipo));
                    break;
            }
        }

        return jugadas;
    }

    //Aplica una jugada al estado (modifica el estado directamente, usar sobre copia)
    private void aplicarJugada(Estado estado, Jugada jugada, int equipoActual) {
        if (jugada.esAccion) {
            //Aplicar efecto de la carta de accion sobre el estado
            aplicarAccion(estado, jugada, equipoActual);
        } else {
            //Mover la ficha
            estado.tablero[jugada.destinoY][jugada.destinoX] = estado.tablero[jugada.origenY][jugada.origenX];
            estado.tablero[jugada.origenY][jugada.origenX] = 0; //Vaciar origen

            //Rotar la carta usada: la carta usada va al mazo, se saca una del mazo
            List<CartaMov> cartasEquipo = (equipoActual == 1) ? estado.cartasEq1 : estado.cartasEq2;
            CartaMov usada = null;
            for (int i = 0; i < cartasEquipo.size(); i++) {
                if (cartasEquipo.get(i).getNombre().equals(jugada.cartaMovimiento)) {
                    usada = cartasEquipo.remove(i);
                    break;
                }
            }
            if (usada != null && !estado.cartasMazo.isEmpty()) {
                CartaMov nuevaDelMazo = estado.cartasMazo.remove(0);
                cartasEquipo.add(nuevaDelMazo);
                estado.cartasMazo.add(usada);
            }
        }
    }

    //Aplica el efecto de una carta de accion al estado de simulacion
    private void aplicarAccion(Estado estado, Jugada jugada, int equipoActual) {
        switch (jugada.tipoAccion) {
            case "REVIVIR":
                //Colocar un peon en el trono propio
                int tronoY = (equipoActual == 1) ? 0 : estado.DIM - 1;
                int peon = (equipoActual == 1) ? 1 : 2;
                estado.tablero[tronoY][estado.centroX] = peon;
                break;

            case "SACRIFICIO":
                //Sacrificar un peon propio para eliminar un peon rival
                //Simulacion: quitar el primer peon propio y el primer peon rival encontrados
                int peonPropio = (equipoActual == 1) ? 1 : 2;
                int peonRival = (equipoActual == 1) ? 2 : 1;
                boolean sacPropio = false, sacRival = false;
                for (int y = 0; y < estado.DIM && (!sacPropio || !sacRival); y++) {
                    for (int x = 0; x < estado.DIM && (!sacPropio || !sacRival); x++) {
                        if (!sacPropio && estado.tablero[y][x] == peonPropio) {
                            estado.tablero[y][x] = 0;
                            sacPropio = true;
                        } else if (!sacRival && estado.tablero[y][x] == peonRival) {
                            estado.tablero[y][x] = 0;
                            sacRival = true;
                        }
                    }
                }
                break;

            //Las demas acciones tienen efecto sobre las reglas del turno, no sobre el tablero
            //Tal vez podriamos simularlo con un bonus/malus en la evaluacion
            default:
                break;
        }

        //Quitar la carta de accion de la lista del estado (NO llamar marcarComoUsada
        //porque modificaria el objeto real compartido entre ramas del minimax)
        List<CartaAccion> acciones = (equipoActual == 1) ? estado.accionesEq1 : estado.accionesEq2;
        for (int i = 0; i < acciones.size(); i++) {
            if (acciones.get(i).getNombre().equals(jugada.cartaAccion)) {
                acciones.remove(i);
                break;
            }
        }
    }

    //Comprueba si el equipo tiene peones vivos
    private boolean tienePeones(Estado estado, int eq) {
        int peon = (eq == 1) ? 1 : 2;
        for (int y = 0; y < estado.DIM; y++) {
            for (int x = 0; x < estado.DIM; x++) {
                if (estado.tablero[y][x] == peon) return true;
            }
        }
        return false;
    }

    //Comprueba si el equipo tiene alguna ficha viva
    private boolean tieneFichas(Estado estado, int eq) {
        for (int y = 0; y < estado.DIM; y++) {
            for (int x = 0; x < estado.DIM; x++) {
                if (esFichaDeEquipo(estado.tablero[y][x], eq)) return true;
            }
        }
        return false;
    }

    //Funcion de evaluacion del tablero desde la perspectiva de la IA
    private int evaluar(Estado estado) {
        int puntuacion = 0;
        int fichasPropias = 0;
        int fichasRival = 0;
        int reyPropioX = -1, reyPropioY = -1;
        int reyRivalX = -1, reyRivalY = -1;
        int rival = equipoRival();

        //Recorrer el tablero
        for (int y = 0; y < estado.DIM; y++) {
            for (int x = 0; x < estado.DIM; x++) {
                int celda = estado.tablero[y][x];
                if (celda == 0) continue;

                if (esFichaDeEquipo(celda, equipo)) {
                    if (celda == 3 || celda == 4) { //Rey propio
                        reyPropioX = x;
                        reyPropioY = y;
                        puntuacion += 1000; //Rey vivo = muy valioso
                    } else {
                        fichasPropias++;
                        puntuacion += 100; //Cada peon vivo vale 100

                        //Bonus por control del centro
                        int distCentro = Math.abs(x - estado.centroX) + Math.abs(y - estado.DIM / 2);
                        puntuacion += (estado.DIM - distCentro) * 5;
                    }
                } else {
                    if (celda == 3 || celda == 4) { //Rey rival
                        reyRivalX = x;
                        reyRivalY = y;
                        puntuacion -= 1000;
                    } else {
                        fichasRival++;
                        puntuacion -= 100;
                    }
                }
            }
        }

        //Victoria por trono: bonus por acercar el rey al trono enemigo
        if (reyPropioX >= 0) {
            int tronoEnemigoY = (equipo == 1) ? estado.DIM - 1 : 0;
            int distTrono = Math.abs(reyPropioX - estado.centroX) + Math.abs(reyPropioY - tronoEnemigoY);
            puntuacion += (estado.DIM * 2 - distTrono) * 30; //Cuanto mas cerca del trono, mejor
        }

        //Penalizar si el rey rival esta cerca de nuestro trono
        if (reyRivalX >= 0) {
            int nuestroTronoY = (equipo == 1) ? 0 : estado.DIM - 1;
            int distRivalTrono = Math.abs(reyRivalX - estado.centroX) + Math.abs(reyRivalY - nuestroTronoY);
            puntuacion -= (estado.DIM * 2 - distRivalTrono) * 30;
        }

        //Valorar calidad de las cartas en mano (mas movimientos posibles = mejor)
        List<CartaMov> misCartas = (equipo == 1) ? estado.cartasEq1 : estado.cartasEq2;
        for (CartaMov carta : misCartas) {
            puntuacion += carta.getListaMovimientos().size() * 15;
        }
        List<CartaMov> cartasRival = (equipo == 1) ? estado.cartasEq2 : estado.cartasEq1;
        for (CartaMov carta : cartasRival) {
            puntuacion -= carta.getListaMovimientos().size() * 15;
        }

        //Valorar cartas de accion disponibles
        List<CartaAccion> misAcciones = (equipo == 1) ? estado.accionesEq1 : estado.accionesEq2;
        for (CartaAccion ca : misAcciones) {
            if (ca.puedeUsarse()) {
                puntuacion += 40; //Tener una carta de accion usable es ventaja
            }
        }

        return puntuacion;
    }

    //Comprueba victoria: 0=nadie, 1=gana equipo 1, 2=gana equipo 2
    private int comprobarVictoria(Estado estado) {
        boolean reyJ1Vivo = false;
        boolean reyJ2Vivo = false;

        for (int y = 0; y < estado.DIM; y++) {
            for (int x = 0; x < estado.DIM; x++) {
                int celda = estado.tablero[y][x];
                if (celda == 3) { //Rey equipo 1
                    reyJ1Vivo = true;
                    //Victoria por trono: rey del eq1 en trono del eq2
                    if (y == estado.DIM - 1 && x == estado.centroX) return 1;
                }
                if (celda == 4) { //Rey equipo 2
                    reyJ2Vivo = true;
                    //Victoria por trono: rey del eq2 en trono del eq1
                    if (y == 0 && x == estado.centroX) return 2;
                }
            }
        }

        if (!reyJ1Vivo) return 2;
        if (!reyJ2Vivo) return 1;
        return 0;
    }

    //Crea un Estado ligero a partir de la Partida real
    private Estado crearEstado(Partida partida) {
        Estado estado = new Estado();
        Tablero tab = partida.getTablero();
        estado.DIM = tab.getDIM();
        estado.centroX = estado.DIM / 2;
        estado.tablero = new int[estado.DIM][estado.DIM];

        //Convertir el tablero de Posicion/Ficha a int[][]
        for (int y = 0; y < estado.DIM; y++) {
            for (int x = 0; x < estado.DIM; x++) {
                Posicion pos = tab.getPosicion(x, y);
                Ficha ficha = pos.getFicha();
                if (ficha == null || !ficha.getVivo()) {
                    estado.tablero[y][x] = 0;
                } else if (ficha.isRey() && ficha.getEquipo() == 1) {
                    estado.tablero[y][x] = 3; //Rey equipo 1
                } else if (ficha.isRey() && ficha.getEquipo() == 2) {
                    estado.tablero[y][x] = 4; //Rey equipo 2
                } else if (ficha.getEquipo() == 1) {
                    estado.tablero[y][x] = 1; //Peon equipo 1
                } else {
                    estado.tablero[y][x] = 2; //Peon equipo 2
                }
            }
        }

        //Separar las cartas de movimiento por equipo y mazo
        estado.cartasEq1 = new ArrayList<>();
        estado.cartasEq2 = new ArrayList<>();
        estado.cartasMazo = new ArrayList<>();

        List<CartaMov> todasCartas = partida.getCartasMovimiento();
        if (todasCartas != null) {
            for (CartaMov cm : todasCartas) {
                String est = cm.getEstado();
                if ("EQ1".equals(est)) {
                    estado.cartasEq1.add(cm);
                } else if ("EQ2".equals(est)) {
                    estado.cartasEq2.add(cm);
                } else {
                    estado.cartasMazo.add(cm);
                }
            }
        }

        //Separar las cartas de accion por equipo
        estado.accionesEq1 = new ArrayList<>();
        estado.accionesEq2 = new ArrayList<>();

        List<CartaAccion> todasAcciones = partida.getCartasAccion();
        if (todasAcciones != null) {
            for (CartaAccion ca : todasAcciones) {
                if (ca.getEquipo() == 1) {
                    estado.accionesEq1.add(ca);
                } else if (ca.getEquipo() == 2) {
                    estado.accionesEq2.add(ca);
                }
            }
        }

        return estado;
    }

    //Devuelve true si la celda contiene una ficha del equipo dado
    private boolean esFichaDeEquipo(int celda, int eq) {
        if (eq == 1) return celda == 1 || celda == 3;
        if (eq == 2) return celda == 2 || celda == 4;
        return false;
    }

    //Devuelve el equipo rival
    private int equipoRival() {
        return (equipo == 1) ? 2 : 1;
    }

    public NivelDificultad getNivel() {
        return nivel;
    }

    public void setNivel(NivelDificultad nivel) {
        this.nivel = nivel;
    }

    public String getAlgoritmo() {
        return algoritmo;
    }

    public int getEquipo() {
        return equipo;
    }
}
