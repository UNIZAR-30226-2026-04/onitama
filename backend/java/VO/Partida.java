package VO;

import java.sql.SQLException;
import java.util.List;

import JDBC.CartasAccionJDBC;
import JDBC.CartasMovJDBC;
import JDBC.JugadorJDBC;
import JDBC.PartidaJDBC;

public class Partida{
    /** Base de katanas (puntos) al ganar / perder; se ajusta ± (mis fichas − su fichas) en tablero al finalizar. */
    private static final int BASE_PUNTOS_VICTORIA = 30;
    private static final int BASE_PUNTOS_DERROTA = 30;

    private int IDPartida, tiempo, muertesJ1, muertesJ2, turno, turnoAccionJ1, turnoAccionJ2;
    private String estado, tipo; //Cambiar fichas por su correspondiente clase
    private boolean j1Ganador, j2Ganador, trampaJ1, trampaJ2, eleccionCartaAccionJ1, eleccionCartaAccionJ2;
    private Jugador jugador1, jugador2;
    private CartaAccion cartaAccionActivaJ1, cartaAccionActivaJ2;
    private List<CartaAccion> cartasA;
    private List<CartaMov> cartasM;
    private Tablero tablero;
    private PartidaJDBC jdbc;
    private CartasAccionJDBC jdbcAccion;
    private CartasMovJDBC jdbcMov;
    private JugadorJDBC jdbcJugador;

    public Partida(int IDPartida, String estado, int tiempo, String tipo, String p1, String p2, int m1, int m2, String jugador1, String jugador2, boolean g1, boolean g2, int turno) {
        this.IDPartida = IDPartida;
        this.jdbc = new PartidaJDBC();
        this.jdbcAccion = new CartasAccionJDBC();
        this.jdbcMov = new CartasMovJDBC();
        this.jdbcJugador = new JugadorJDBC();
        cartaAccionActivaJ1 = null;
        cartaAccionActivaJ2 = null;
        turnoAccionJ1 = -1;
        turnoAccionJ2 = -1;
        eleccionCartaAccionJ1 = true; // Frontend no implementa selección de carta acción: se salta automáticamente
        eleccionCartaAccionJ2 = true; 
        this.estado = estado;
        this.tiempo = tiempo;
        this.tipo = tipo;
        this.turno = turno;
        this.muertesJ1 = m1;
        trampaJ1 = true; // Frontend no implementa fase de trampas: se salta automáticamente
        trampaJ2 = true;
        this.muertesJ2 = m2;
        this.j1Ganador = g1;
        this.j2Ganador = g2;
        this.tablero = new Tablero(7);
        if ((p1 == null || p1.isEmpty()) && (p2 == null || p2.isEmpty())) {
            this.tablero.cargarTablero();
        } else {
            try {
                this.tablero.cargarTablero(p1, p2);
            } catch (Exception e) {
                this.tablero.cargarTablero();
            }
        }

        try {
            // 1. Buscamos los objetos Jugador
            this.jugador1 = jdbcJugador.buscarJugador(jugador1);
            this.jugador2 = jdbcJugador.buscarJugador(jugador2);

        } catch (java.sql.SQLException e) {
            this.jugador1 = null;
            this.jugador2 = null;
        }
    }

    public int getTurno(){
        return turno;
    }

    public boolean registrarPartida(){
        try {
            turno = 0; //Ciro: Mi idea es que siempre uno de los jugadores (siempre J1 por ejemplo)
            IDPartida = jdbc.registrarPartida(this);
            return IDPartida >= 0; //Se devuelve el id que asigna la base a la partida, si hay algun problema devuelve -1
        } catch (SQLException e) {
            return false;
        }
    }

    public void asignarCartas(){
        try {
            int puntosMin = Math.min(this.jugador1.getPuntos(), this.jugador2.getPuntos());
            java.util.List<CartaAccion> ca = jdbcAccion.asignar4CartasPartida(IDPartida, puntosMin);
            this.cartasA = ca != null ? ca : new java.util.ArrayList<>();
            java.util.List<CartaMov> cm = jdbcMov.asignar7CartasPartida(IDPartida, puntosMin);
            this.cartasM = cm != null ? cm : new java.util.ArrayList<>();
            if (this.cartasA.isEmpty()) {
                System.err.println("asignarCartas: no se obtuvieron cartas de acción para IDPartida=" + IDPartida + " puntosMin=" + puntosMin);
            }
        } catch (Exception e) {
            System.err.println("asignarCartas: error al asignar cartas - " + e.getMessage());
            this.cartasA = new java.util.ArrayList<>();
            this.cartasM = new java.util.ArrayList<>();
        }
    }

    public int getIDPartida(){
        return IDPartida;
    }

    public String getEstado(){
        return estado;
    }

    public int getTiempo(){
        return tiempo;
    }

    public String getTipo(){
        return tipo;
    }

    public String getPos_Fichas_Eq1(){
        //return fichasJ1; //Parche de compilacion
        StringPorReferencia p1 = new StringPorReferencia("");
        StringPorReferencia p2 = new StringPorReferencia("");
        tablero.getPosicionesEquipos(p1, p2);
        return p1.getValor();
    }

    public String getPos_Fichas_Eq2(){
        //return fichasJ2; //Parche de compilacion
        StringPorReferencia p1 = new StringPorReferencia("");
        StringPorReferencia p2 = new StringPorReferencia("");
        tablero.getPosicionesEquipos(p1, p2);
        return p2.getValor();
    }

    public int getFichasMuertas1(){
        return muertesJ1;
    }

    public int getFichasMuertas2(){
        return muertesJ2;
    }

    public boolean isEs_Ganador_J1(){
        return j1Ganador;
    }

    public boolean isEs_Ganador_J2(){
        return j2Ganador;
    }

    public String getJ1(){
        return jugador1.getNombre();
    }

    public String getJ2(){
        return jugador2.getNombre();
    }

    public Tablero getTablero(){
        return tablero;
    }

    public List<CartaAccion> getCartasAccion(){
       return cartasA;
    }

    public List<CartaMov> getCartasMovimiento(){
       return cartasM;
    }

    public void setCartasMovimiento(List<CartaMov> cartasM){
       this.cartasM = cartasM;
    }

    public void setCartasAccion(List<CartaAccion> cartasA){
       this.cartasA = cartasA;
    }

    /** Carga las cartas de movimiento y acción desde la BD (necesario al reanudar una partida pausada). */
    public void cargarCartas() {
        try {
            this.cartasM = jdbcMov.sacarCartasPartida(IDPartida);
            this.cartasA = jdbcAccion.sacarCartasPartida(IDPartida);
            if (this.cartasM == null) this.cartasM = new java.util.ArrayList<>();
            if (this.cartasA == null) this.cartasA = new java.util.ArrayList<>();
        } catch (java.sql.SQLException e) {
            System.err.println("cargarCartas: error al cargar cartas de la BD - " + e.getMessage());
            if (this.cartasM == null) this.cartasM = new java.util.ArrayList<>();
            if (this.cartasA == null) this.cartasA = new java.util.ArrayList<>();
        }
    }

    public Posicion getPosicion(int x, int y){
        return tablero.getPosicion(x, y);
    }

    public void setJ1(Jugador j){
        this.jugador1 = j;
    }

    public void setJ2(Jugador j){
        this.jugador2 = j;
    }

    public boolean setEquipoCartaAccion(String nomCarta, int equipo){
        boolean cartaEncontrada = false;
        CartaAccion cartaA = null;
        for (CartaAccion ca : cartasA) {
            if (ca.getNombre().equals(nomCarta) && ca.getEquipo() == -equipo) { //Comprobamos que la carta es del mazo (equipo -1 o -2) y que el equipo que la quiere coger es el correcto
                ca.setEquipo(equipo);
                ca.setEstado("USABLE");
                cartaEncontrada = true;
            }else if (ca.getNombre().equals(nomCarta)) {
                cartaA = ca; 
            }
        }
        int equipoOp = (equipo == 1) ? 2 : 1;
        if (cartaEncontrada && cartaA != null) { //Asignamos la otra carta al oponente
            cartaA.setEquipo(equipoOp);
            cartaA.setEstado("USABLE");
        }
        eleccionCartaAccionJ1 = (equipo == 1) ? true : eleccionCartaAccionJ1;
        eleccionCartaAccionJ2 = (equipo == 2) ? true : eleccionCartaAccionJ2;
        return cartaEncontrada;
    }

    public void repartirCartas() {
        int i = 0;
        for (CartaAccion ca : cartasA) {
            ca.setEquipo(-(i%2 + 1)); //Sin equipo (el - representa que se le da la opcion de elegirla a ese equipo)
            ca.setEstado("ESPERANDO"); //Esperando a ser seleccionada al inicio
            i++;
            ca.actualizarDatosPartida(IDPartida);    
        }
        i = 0;
        for (CartaMov cm : cartasM) {
            if (i%2 == 0) {
                cm.setEstado("EQ1"); //Que se asigna al equipo 1 (si se juega -> MAZO)
            } else {
                cm.setEstado("EQ2"); //Que se asigna al equipo 2 (si se juega -> MAZO)
            }
            cm.actualizarDatosPartida(IDPartida);
            i++;
            if(i == 4) break; //Asignamos las primeras 4, el resto al mazo
        }
    }

    public boolean actualizarBD(){
        StringPorReferencia p1 = new StringPorReferencia("");
        StringPorReferencia p2 = new StringPorReferencia("");
        tablero.getPosicionesEquipos(p1, p2);
        try {
            return jdbc.updateTurno(IDPartida, turno) | jdbc.updateEstado(IDPartida, estado) | jdbc.updateMuertesFichas2(IDPartida, muertesJ2) | jdbc.updateMuertesFichas1(IDPartida, muertesJ1) | jdbc.updateTiempo(IDPartida, tiempo) | jdbc.updateGanadorJ2(IDPartida, j2Ganador) | jdbc.updateGanadorJ1(IDPartida, j1Ganador) | jdbc.updatePosFichas1(IDPartida, p1.getValor()) | jdbc.updatePosFichas2(IDPartida, p2.getValor()); //| para que se ejecuten todos
        } catch (SQLException e) {
            return false;
        }
    }

    //Inicia la partida: cambia estado a JUGANDOSE, asigna las cartas y las reparte
    public boolean iniciarPartida(){
        this.estado = "JUGANDOSE";
        this.tiempo = 0;
        this.tablero.cargarTablero();
        asignarCartas();
        repartirCartas();
        return actualizarBD();
    }

    //Pausa la partida cambiando su estado
    public boolean pausarPartida(){
        if (!"JUGANDOSE".equalsIgnoreCase(estado)) {
            return false; //Solo se puede pausar una partida en curso
        }
        this.estado = "PAUSADA";
        try {
            return jdbc.updateEstado(IDPartida, estado);
        } catch (SQLException e) {
            return false;
        }
    }

    //Reanuda la partida desde el estado de pausa
    public boolean reanudarPartida(){
        if (!"PAUSADA".equalsIgnoreCase(estado)) {
            return false; //Solo se puede reanudar una partida PAUSADA
        }
        this.estado = "JUGANDOSE";
        try {
            return jdbc.updateEstado(IDPartida, estado);
        } catch (SQLException e) {
            return false;
        }
    }

    public Posicion getRey(int equipo) {
        return tablero.getRey(equipo);
    }

    //Un jugador abandona la partida, el otro gana automaticamente
    public boolean abandonarPartida(int equipoQueAbandona){
        this.estado = "FINALIZADA";
        if (equipoQueAbandona == 1) {
            this.j2Ganador = true;
            this.j1Ganador = false;
        } else {
            this.j1Ganador = true;
            this.j2Ganador = false;
        }
        return finalizarPartida();
    }

    /**
     * Katanas según merecimiento: base ± (mis piezas en tablero − piezas rivales).
     * Victoria: {@code BASE_VICTORIA + diff}. Derrota: {@code -BASE_DERROTA + diff}.
     * {@code diff} = fichas de mi equipo − fichas del otro (al finalizar).
     */
    private int puntosKatanasSegunTablero(boolean victoria, int miEquipo) {
        int f1 = tablero.contarFichasEquipo(1);
        int f2 = tablero.contarFichasEquipo(2);
        int diff = (miEquipo == 1) ? (f1 - f2) : (f2 - f1);
        if (victoria) {
            return BASE_PUNTOS_VICTORIA + diff;
        }
        return -BASE_PUNTOS_DERROTA + diff;
    }

    //Finaliza la partida y actualiza las estadisticas de los jugadores
    public boolean finalizarPartida(){
        this.estado = "FINALIZADA";
        // Cores: +10 victoria, 0 derrota (sin cambio por tablero)
        if (j1Ganador && jugador1 != null) {
            jugador1.registrarPartida(10, puntosKatanasSegunTablero(true, 1), true);
        }
        if (j2Ganador && jugador2 != null) {
            jugador2.registrarPartida(10, puntosKatanasSegunTablero(true, 2), true);
        }
        if (!j1Ganador && jugador1 != null) {
            jugador1.registrarPartida(0, puntosKatanasSegunTablero(false, 1), false);
        }
        if (!j2Ganador && jugador2 != null) {
            jugador2.registrarPartida(0, puntosKatanasSegunTablero(false, 2), false);
        }
        return actualizarBD();
    }

    //-1 -> error
    // 0 -> nada
    // 1 -> todas las trampas puestas
    public int setTrampa(int equipo, int fila, int columna){
        Posicion p = tablero.getPosicion(fila, columna);
        if (equipo == 1 && !trampaJ1 && p!= null) {
            p.activarTrampa();
            trampaJ1 = true;
        } else if(!trampaJ2 && p!= null){
            p.activarTrampa();
            trampaJ2 = true;
        } else{
            return -1; //Error: ya puso su trampa o la posicion no es valida
        }

        if(trampaJ1 && trampaJ2){
            return 1;
        }else{
            return 0;
        }
    }

    //true -> exito en la accion
    //false -> error (carta no del equipo, carta ya usada ...)
    public boolean jugarAccion(String nomCartaAcc, int x, int y, int equipo, int xOp, int yOp, String nomCarta) {
        boolean cartaEncontrada = false;
        CartaAccion cartaA = null;
        if(equipo - 1 == turno % 2 && (cartaAccionActivaJ1 ==null && equipo == 1) || (cartaAccionActivaJ2 ==null && equipo == 2)) { //Comprobamos que el equipo que juega es el correcto segun el turno
            for (CartaAccion ca : cartasA) {
                if (ca.getNombre().equals(nomCartaAcc)) {
                    if (ca.jugarCarta(this, x, y, equipo, xOp, yOp, nomCarta)) {
                        turno++; //Cambiamos de turno (tambien lo utilizamos para saber cuantas rondas se han jugado)
                        cartaEncontrada = true;
                        cartaAccionActivaJ1 = (equipo == 1) ? ca : null;
                        cartaAccionActivaJ2 = (equipo == 2) ? ca : null;
                        turnoAccionJ1 = (equipo == 1) ? turno : turnoAccionJ1;
                        turnoAccionJ2 = (equipo == 2) ? turno : turnoAccionJ2;
                    } else {
                        cartaEncontrada = false;
                    }
                }else if (ca.getEquipo() == equipo) {
                    cartaA = ca; 
                }
            }
        }
        if (cartaEncontrada && cartaA != null) {
            cartaA.setEstado("USADA"); //Marcamos la otra carta como usada porque solo se puede usar una carta de accion por partida y equipo
        }
        return cartaEncontrada;
    }

   // 0 -> Movimiento realizado con exito
   // 1 -> equipo 1 gana
   // 2 -> equipo 2 gana
   // -1 -> carta no existente en la partida
   // -2 -> movimiento no valido
    public int moverFicha(int equipo, Posicion origen, Posicion destino, String cartaNom, Posicion Trampa) {
        Ficha fOrigen = origen.getFicha();
        Ficha fDestino = destino.getFicha();
        CartaMov carta = null;

        for (CartaMov c : cartasM){
            if(c.getNombre().trim().equalsIgnoreCase(cartaNom)){
                System.out.println(c.getNombre() + " - " + cartaNom);
                carta = c;
            }
        }

        if(carta != null){
            List<Posicion> movimientosValidos = carta.getListaMovimientos();

            // La BD almacena movimientos como (dc, df) relativos al jugador,
            // donde df+ = avanzar hacia el oponente.
            // Los deltas brutos del tablero deben normalizarse según el equipo:
            //   Equipo 1 (arriba, avanza hacia fila 6): delta_x_norm = -delta_x, delta_y_norm = delta_y
            //   Equipo 2 (abajo, avanza hacia fila 0): delta_x_norm = delta_x,  delta_y_norm = -delta_y
            int rawDx = destino.getX() - origen.getX();
            int rawDy = destino.getY() - origen.getY();
            int normDx = (equipo == 1) ? -rawDx : rawDx;
            int normDy = (equipo == 1) ?  rawDy : -rawDy;

            Posicion movimientoARealizar = new Posicion(normDx, normDy, null);
            boolean movExiste = false;
            for(Posicion mov : movimientosValidos){
                movExiste = movimientoARealizar.getX() == mov.getX() && movimientoARealizar.getY() == mov.getY();
                if (movExiste){
                    break;
                }
            }
            //Por si acaso comprobamos que el movimiento existe y que se hayan puesto las trampas, aunque el controlador no deberia dejar llegar aqui si no se han puesto las trampas o si el movimiento no es valido
            if ((equipo - 1 != turno % 2) || !trampaJ1 || !trampaJ2 || !eleccionCartaAccionJ1 || !eleccionCartaAccionJ2 || fOrigen == null || fOrigen.getEquipo() != equipo || !movExiste || !destino.estaActiva() || destino.getX()>=7 || destino.getY()>=7 || destino.getX()<0 || destino.getY()<0) {
                return -2; //Movimiento no valido
            }

            //Logica de casillas trampa
            if(destino.esTrampa()){
                destino.desactivarCasilla();
                boolean reyMatado = origen.matar(); // Mata la ficha que haya en la casilla trampa
                Trampa = destino; //Devolvemos la posicion de la trampa para que el controlador pueda notificar a los jugadores
                if (reyMatado && equipo == 1) {
                    j2Ganador = true;
                    j1Ganador = false;
                    //finalizarPartida();
                    return 2; //Gana el equipo 2 por trampa
                } else if (reyMatado) {
                    j1Ganador = true;
                    j2Ganador = false;
                    //finalizarPartida();
                    return 1; //Gana el equipo 1 por trampa
                }
            }

            //Posibilidad de que se requiera modificaciones
            else if (fDestino != null && fDestino.getEquipo() != equipo) {
                if (fDestino.matar()) { //Si se mata al rey, se acaba la partida
                    //Posible implementacion de patron observer para notificar victoria al matar al rey
                    if (equipo == 1) {
                        j1Ganador = true;
                        muertesJ2++;
                        //finalizarPartida();
                        return 1;
                    } else {
                        j2Ganador = true;
                        muertesJ1++;
                        //finalizarPartida();
                        return 2;
                    }
                }
                if (equipo == 1) {
                    muertesJ2++;
                } else {
                    muertesJ1++;
                }
            }
            destino.setFicha(fOrigen);
            origen.setFicha(null);

            // Verificar victoria por trono: el rey llega al trono enemigo
            if (fOrigen.isRey()) {
                tablero.setRey(destino.getX(), destino.getY(), equipo);
                Posicion tronoEnemigo = (equipo == 1) ? tablero.trono2 : tablero.trono1;
                if (destino == tronoEnemigo) {
                    if (equipo == 1) { 
                        j1Ganador = true; 
                        //finalizarPartida();
                        return 1; 
                    }else { 
                        j2Ganador = true; 
                        //finalizarPartida();
                        return 2; 
                    }
                }
            }

            rotarCartas(carta.getNombre(), equipo);
            turno++; //Cambiamos de turno (tambien lo utilizamos para saber cuantas rondas se han jugado)
            if (turnoAccionJ1 != -1 && turno > turnoAccionJ1 && equipo - 1 != (turno-1)%2) {
                cartaAccionActivaJ1.deshacerCarta(this);
                turnoAccionJ1 = -1;
            }
            if (turnoAccionJ2 != -1 && turno > turnoAccionJ2 && equipo - 1 != (turno-1)%2) { //Deshacemos la accion solo si ha movido el rival despues de activar la accion
                cartaAccionActivaJ2.deshacerCarta(this);
                turnoAccionJ2 = -1;
            }
            return 0; //Movimiento realizado con exito
        }else{
            return -1;
        }
    }

    //REVISAR (Ciro): Creo que igual seria mejor actualizar la base al abandonar, parar, o terminar la partida
    //Rota las cartas de movimiento segun la mecanica de Onitama:
    //La carta usada por un equipo pasa al mazo y se le asigna una del mazo
    public boolean rotarCartas(String cartaUsada, int equipo){
        CartaMov usada = null;
        CartaMov nuevaDelMazo = null;

        //Buscar la carta usada
        for (CartaMov cm : cartasM) {
            if (cm.getNombre().equals(cartaUsada) && 
                ((equipo == 1 && "EQ1".equals(cm.getEstado())) || (equipo == 2 && "EQ2".equals(cm.getEstado())))) {
                usada = cm;
                break;
            }
        }

        if (usada == null) {
            return false; //Carta no encontrada o no pertenece al equipo
        }

        //Buscar una carta del mazo para darle al equipo
        for (CartaMov cm : cartasM) {
            if ("MAZO".equals(cm.getEstado())) {
                nuevaDelMazo = cm;
                break;
            }
        }

        if (nuevaDelMazo == null) {
            return false; //No hay cartas en el mazo
        }

        //La carta usada pasa al mazo
        usada.setEstado("MAZO");
        usada.actualizarDatosPartida(IDPartida);

        //La carta del mazo pasa al equipo
        nuevaDelMazo.setEstado(equipo == 1 ? "EQ1" : "EQ2");
        nuevaDelMazo.actualizarDatosPartida(IDPartida);

        return true;
    }
}