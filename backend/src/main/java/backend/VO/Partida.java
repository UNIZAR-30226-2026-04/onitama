package backend.VO;

import java.sql.SQLException;
import java.util.List;

import backend.JDBC.CartasAccionJDBC;
import backend.JDBC.CartasMovJDBC;
import backend.JDBC.JugadorJDBC;
import backend.JDBC.PartidaJDBC;
import backend.DAO.PartidaDAO;
import backend.DAO.CartasAccionDAO;
import backend.DAO.CartasMovDAO;
import backend.DAO.JugadorDAO;

public class Partida{
    /** Base de katanas (puntos) al ganar / perder; se ajusta ± (mis fichas − su fichas) en tablero al finalizar. */
    private static final int BASE_PUNTOS_VICTORIA = 30;
    private static final int BASE_PUNTOS_DERROTA = 30;

    private int IDPartida, tiempo, muertesJ1, muertesJ2, turno;
    private String estado, tipo, trampaPosJ1, trampaPosJ2; //Cambiar fichas por su correspondiente clase
    private boolean j1Ganador, j2Ganador, trampaJ1, trampaJ2, eleccionCartaAccionJ1, eleccionCartaAccionJ2, accionActivadaJ2, accionActivadaJ1;
    private Jugador jugador1, jugador2;
    private CartaAccion cartaAccionJugadaJ1, cartaAccionJugadaJ2;
    /** Restricción Dama del Mar / Finisterre: no se mutan cartas; solo validación por turno -> QUITADO POR CIRO, lo veia muy extraño (esa informacion la tenias ya en cartaActiva) */
    private List<CartaAccion> cartasA;
    private List<CartaMov> cartasM;
    private Tablero tablero;
    private PartidaDAO dao;
    private CartasAccionDAO daoAccion;
    private CartasMovDAO daoMov;
    private JugadorDAO daoJugador;

    public Posicion trampaActivada = null;

    public Partida(int IDPartida, String estado, int tiempo, String tipo, String p1, String p2, int m1, int m2, String jugador1, String jugador2, boolean g1, boolean g2, int turno, String trampaPosJ1, String trampaPosJ2) {
        this.IDPartida = IDPartida;
        this.dao = new PartidaJDBC();
        this.daoAccion = new CartasAccionJDBC();
        this.daoMov = new CartasMovJDBC();
        this.daoJugador = new JugadorJDBC();
        cartaAccionJugadaJ1 = null;
        cartaAccionJugadaJ2 = null;
        accionActivadaJ2 = false;
        accionActivadaJ2 = false; //CAMBIAR POR UNA FUNCION QUE DEVUELVA TRUE SI NO HAY NINGUNA CARTA USADA Y FALSE EN CASO CONTRARIO
        boolean esNueva = (p1 == null || p1.isEmpty()) && (p2 == null || p2.isEmpty());
        eleccionCartaAccionJ1 = estado != null && (estado.equals("JUGANDOSE") || estado.equals("FINALIZADA")) && !esNueva; 
        eleccionCartaAccionJ2 = estado != null && (estado.equals("JUGANDOSE") || estado.equals("FINALIZADA")) && !esNueva; 
        this.estado = estado;
        this.tiempo = tiempo;
        this.tipo = tipo;
        this.turno = turno;
        this.muertesJ1 = m1;
        this.trampaPosJ1 = trampaPosJ1;
        this.trampaPosJ2 = trampaPosJ2;
        trampaJ1 = estado != null && (estado.equals("JUGANDOSE") || estado.equals("FINALIZADA")) && !esNueva; 
        trampaJ2 = estado != null && (estado.equals("JUGANDOSE") || estado.equals("FINALIZADA")) && !esNueva;
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
        aplicarTrampaGuardada(this.trampaPosJ1);
        aplicarTrampaGuardada(this.trampaPosJ2);

        try {
            // 1. Buscamos los objetos Jugador
            this.jugador1 = daoJugador.buscarJugador(jugador1);
            this.jugador2 = daoJugador.buscarJugador(jugador2);

        } catch (java.sql.SQLException e) {
            this.jugador1 = null;
            this.jugador2 = null;
        }
    }

    public int eraTrampa(String cartaAc){
        for(CartaAccion ca : cartasA){
            if(ca.getNombre().equals(cartaAc)){
                return ca.eraTrampa();
            }
        }
        return -1;
    }

    private void aplicarTrampaGuardada(String trampaGuardada) {
        if (trampaGuardada == null || trampaGuardada.trim().isEmpty()) return;
        String[] partes = trampaGuardada.split(",");
        if (partes.length < 2) return;
        try {
            int x = Integer.parseInt(partes[0].trim());
            int y = Integer.parseInt(partes[1].trim());
            int activa = partes.length >= 3 ? Integer.parseInt(partes[2].trim()) : 1;
            if (x < 0 || x >= tablero.getDIM() || y < 0 || y >= tablero.getDIM()) return;
            Posicion p = tablero.getPosicion(x, y);
            p.activarTrampa();
            if (activa == 0) {
                p.desactivarCasilla();
            }
        } catch (NumberFormatException ignored) {
        }
    }

    private void marcarTrampaDisparada(Posicion destino) {
        String pos = destino.getX() + "," + destino.getY();
        try {
            if (trampaPosJ1 != null && trampaPosJ1.startsWith(pos)) {
                trampaPosJ1 = pos + ",0";
                dao.updateTrampaJ1(IDPartida, trampaPosJ1);
            } else if (trampaPosJ2 != null && trampaPosJ2.startsWith(pos)) {
                trampaPosJ2 = pos + ",0";
                dao.updateTrampaJ2(IDPartida, trampaPosJ2);
            }
        } catch (SQLException e) {
            System.err.println("Error al persistir trampa disparada: " + e.getMessage());
        }
    }

    public int getTurno() {
        return turno;
    }

    public void decrementarTurno() {
        turno--;
    }

    public boolean registrarPartida(){
        try {
            turno = 0; //Ciro: Mi idea es que siempre uno de los jugadores (siempre J1 por ejemplo)
            IDPartida = dao.registrarPartida(this);
            return IDPartida >= 0; //Se devuelve el id que asigna la base a la partida, si hay algun problema devuelve -1
        } catch (SQLException e) {
            return false;
        }
    }

    public void asignarCartas(){
        try {
            int puntosMin = Math.min(this.jugador1.getPuntos(), this.jugador2.getPuntos());
            java.util.List<CartaAccion> ca = daoAccion.asignar4CartasPartida(IDPartida, puntosMin);
            this.cartasA = ca != null ? ca : new java.util.ArrayList<>();
            java.util.List<CartaMov> cm = daoMov.asignar7CartasPartida(IDPartida, puntosMin);
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

    public String getTrampaPosJ1() {
        return trampaPosJ1;
    }

    public String getTrampaPosJ2() {
        return trampaPosJ2;
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
            this.cartasM = daoMov.sacarCartasPartida(IDPartida);
            this.cartasA = daoAccion.sacarCartasPartida(IDPartida);
            if (this.cartasM == null) this.cartasM = new java.util.ArrayList<>();
            if (this.cartasA == null){
                this.cartasA = new java.util.ArrayList<>();
            }else{
                for(CartaAccion ca : cartasA){
                    if(ca.estaActiva()){
                        if(ca.getEquipo() == 1){
                            cartaAccionJugadaJ1 = ca;
                            accionActivadaJ1 = true;
                        }else{
                            cartaAccionJugadaJ2 = ca;
                            accionActivadaJ2 = true;
                        }
                    }
                }
            }
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
                ca.marcarUsable();
                cartaEncontrada = true;
                // si la partida se pausa antes de jugarla, se recupera con equipo y estado correctos al reanudar
                ca.actualizarDatosPartida(IDPartida);
            } else if (!ca.getNombre().equals(nomCarta) && ca.getEquipo() == -equipo) {
                // La carta que NO eligió este jugador: se la asignamos al rival
                cartaA = ca;
            }
        }
        int equipoOp = (equipo == 1) ? 2 : 1;
        if (cartaEncontrada && cartaA != null) { //Asignamos la otra carta al oponente
            cartaA.setEquipo(equipoOp);
            cartaA.marcarUsable();
            // lo mismo con la carta que se rechaza
            cartaA.actualizarDatosPartida(IDPartida);
        }
        eleccionCartaAccionJ1 = (equipo == 1) ? true : eleccionCartaAccionJ1;
        eleccionCartaAccionJ2 = (equipo == 2) ? true : eleccionCartaAccionJ2;
        return cartaEncontrada;
    }

    public void repartirCartas() {
        int i = 0;
        for (CartaAccion ca : cartasA) {
            ca.setEquipo(-(i%2 + 1)); //Sin equipo (el - representa que se le da la opcion de elegirla a ese equipo)
            ca.marcarEsperando(); //Esperando a ser seleccionada al inicio
            i++;
            ca.actualizarDatosPartida(IDPartida);    
        }
        i = 0;
        for (CartaMov cm : cartasM) {
            cm.marcarEquipo(i%2 + 1);
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
            return dao.updateTurno(IDPartida, turno) | dao.updateEstado(IDPartida, estado) | dao.updateMuertesFichas2(IDPartida, muertesJ2) | dao.updateMuertesFichas1(IDPartida, muertesJ1) | dao.updateTiempo(IDPartida, tiempo) | dao.updateGanadorJ2(IDPartida, j2Ganador) | dao.updateGanadorJ1(IDPartida, j1Ganador) | dao.updatePosFichas1(IDPartida, p1.getValor()) | dao.updatePosFichas2(IDPartida, p2.getValor()); //| para que se ejecuten todos
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
            return dao.updateEstado(IDPartida, estado);
        } catch (SQLException e) {
            return false;
        }
    }

    // Cancela la partida sin ganador (timer expirado en partida pública)
    public boolean cancelarPartida() {
        this.estado = "CANCELADA";
        try {
            return dao.updateEstado(IDPartida, estado);
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
            return dao.updateEstado(IDPartida, estado);
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
            if(tipo.equals("PRIVADA")){
                jugador1.registrarPartida(0, 0, true);
            }else{
                jugador1.registrarPartida(10, puntosKatanasSegunTablero(true, 1), true);
            }
        }
        if (j2Ganador && jugador2 != null) {
            if(tipo.equals("PRIVADA")){
                jugador2.registrarPartida(0, 0, true);
            }else{
                jugador2.registrarPartida(10, puntosKatanasSegunTablero(true, 2), true);
            }
        }
        if (!j1Ganador && jugador1 != null) {
            if(tipo.equals("PRIVADA")){
                jugador1.registrarPartida(0, 0, false);
            }else{
                jugador1.registrarPartida(10, puntosKatanasSegunTablero(false, 1), false);
            }
        }
        if (!j2Ganador && jugador2 != null) {
            if(tipo.equals("PRIVADA")){
                jugador2.registrarPartida(0, 0, false);
            }else{
                jugador2.registrarPartida(10, puntosKatanasSegunTablero(false, 2), false);
            }
        }
        return actualizarBD();
    }

    //-1 -> error
    // 0 -> nada
    // 1 -> todas las trampas puestas
    public int setTrampa(int equipo, int fila, int columna){
        Posicion p = tablero.getPosicion(columna, fila);
        
        boolean valida = false;
        if (p != null && p.ocupado() == -1 && !p.esTrampa()) {
            int dim = tablero.getDIM();
            if (equipo == 1 && fila > 0 && fila <= 2) valida = true;
            else if (equipo == 2 && fila >= dim - 3 && fila <= dim - 2) valida = true;
        }
        
        if (!valida) return -1; //Error: posicion no valida
        
        if (equipo == 1 && !trampaJ1) {
            p.activarTrampa();
            trampaJ1 = true;
            trampaPosJ1 = columna + "," + fila + ",1";
             try {
                // único update, ocurre una sola vez
                dao.updateTrampaJ1(IDPartida, trampaPosJ1);
            } catch (SQLException e) {
                System.err.println("Error al persistir trampa J1: " + e.getMessage());
            }
        } else if(equipo == 2 && !trampaJ2){
            p.activarTrampa();
            trampaJ2 = true;
            trampaPosJ2 = columna + "," + fila + ",1";
             try {
                dao.updateTrampaJ2(IDPartida, trampaPosJ2);
            } catch (SQLException e) {
                System.err.println("Error al persistir trampa J2: " + e.getMessage());
            }
        } else{
            return -1; //Error: ya puso su trampa o la posicion no es valida
        }

        if(trampaJ1 && trampaJ2){
            return 1;
        }else{
            return 0;
        }
    }

    // 0 -> Accion realizada con exito
   // 1 -> equipo 1 gana
   // 2 -> equipo 2 gana
   // -1 -> error
    public int jugarAccion(String nomCartaAcc, int x, int y, int equipo, int xOp, int yOp, String nomCarta) {
        boolean cartaEncontrada = false;
        CartaAccion cartaA = null;
        if (equipo - 1 == turno % 2 && ((cartaAccionJugadaJ1 == null && equipo == 1) || (cartaAccionJugadaJ2 == null && equipo == 2))) { // Turno correcto y aún no hay otra acción activa de ese equipo
            for (CartaAccion ca : cartasA) {
                if (ca.getNombre().equals(nomCartaAcc)) {
                    if (ca.jugarCarta(this, x, y, equipo, xOp, yOp, nomCarta)) {
                        // Atrapasueños (ROBAR) mantiene el turno del ejecutor para que pueda mover con 3 cartas.
                        if (!"ROBAR".equals(ca.getAccion())) {
                            turno++; //Cambiamos de turno (tambien lo utilizamos para saber cuantas rondas se han jugado)
                        }
                        cartaEncontrada = true;
                        cartaAccionJugadaJ1 = (equipo == 1) ? ca : cartaAccionJugadaJ1;
                        cartaAccionJugadaJ2 = (equipo == 2) ? ca : cartaAccionJugadaJ2;
                        if (equipo == 1) {
                            accionActivadaJ1 = true;
                        } else {
                            accionActivadaJ2 = true;
                        }
                        ca.actualizarDatosPartida(IDPartida);
                    } else {
                        cartaEncontrada = false;
                    }
                }else if (ca.getEquipo() == equipo) {
                    cartaA = ca; 
                }
            }
        }

        if (cartaEncontrada && cartaA != null) {
            cartaA.marcarNoUsable(); //Marcamos la otra carta como usada porque solo se puede usar una carta de accion por partida y equipo
            cartaA.actualizarDatosPartida(IDPartida);
        }

        //Busqueda para ver si se ha quedado sin movimientos
        if((accionActivadaJ1 && cartaAccionJugadaJ1 != null && cartaAccionJugadaJ1.esTipoRestriccion()) || (accionActivadaJ2 && cartaAccionJugadaJ2 != null && cartaAccionJugadaJ2.esTipoRestriccion())){
            System.err.println("BUSCANDO MOVIMIENTOS...");
            boolean hayMovimiento = false;
            if(cartaAccionJugadaJ1 == null && equipo == 2){
                hayMovimiento = true; //El enemigo puede jugar su carta de acccion para librarse de la accion
            }else if(cartaAccionJugadaJ2 == null && equipo == 1){
                hayMovimiento = true;
            }else{
                for(CartaMov cm : cartasM){
                    int equipoEnemigo = (equipo == 1) ? 2 : 1;
                    if(cm.perteneceAlEquipo(equipoEnemigo)){
                        hayMovimiento = tablero.existeMovimiento(cm.getListaMovimientos(), equipoEnemigo, cartaAccionJugadaJ1, cartaAccionJugadaJ2);
                        if(hayMovimiento){
                            System.err.println("TIENE MOVIMIENTOS CON " + cm.getNombre());
                            break;
                        }else{
                            return equipo; //Si el enemigo no tiene movimientos -> hemos ganado
                        }
                    }
                }
            }
        }

        return (cartaEncontrada) ? 0 : -1;
    }

   // 0 -> Movimiento realizado con exito
   // 1 -> equipo 1 gana
   // 2 -> equipo 2 gana
   // -1 -> carta no existente en la partida
   // -2 -> movimiento no valido
    public int moverFicha(int equipo, Posicion origen, Posicion destino, String cartaNom) {
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
            // ... validaciones iniciales
            boolean movExiste = false;
            for(Posicion mov : movimientosValidos){
                movExiste = movimientoARealizar.getX() == mov.getX() && movimientoARealizar.getY() == mov.getY();
                if (movExiste){
                    break;
                }
            }
            if (movExiste) {
                //Nos da igual si la accion activada es nuestra, sigue afectandonos si el rival no ha movido
                if((accionActivadaJ2 && cartaAccionJugadaJ2 != null && !cartaAccionJugadaJ2.permiteMovimiento(normDx, normDy)) || (accionActivadaJ1 && cartaAccionJugadaJ1 != null && !cartaAccionJugadaJ1.permiteMovimiento(normDx, normDy))){
                    movExiste = false;
                    return -2;
                }
            }
            //Por si acaso comprobamos que el movimiento existe y que se hayan puesto las trampas
            if ((equipo - 1 != turno % 2) || !trampaJ1 || !trampaJ2 || !eleccionCartaAccionJ1 || !eleccionCartaAccionJ2 || fOrigen == null || fOrigen.getEquipo() != equipo || !destino.estaActiva() || !movExiste || destino.getX()>=7 || destino.getY()>=7 || destino.getX()<0 || destino.getY()<0) {
                System.out.println("ERROR AL MOVER");
                return -2; //Movimiento no valido
            }
            
            // Si el destino tiene una trampa que ya NO ESTA activa, no se puede pisar (?) Wait, that means inactive traps act as walls. Actually we should just allow passing but do nothing, or it's a wall? Let's just keep the original constraint, which actually blocked it. Wait, the user said it freezes, but maybe we should allow moving ONLY if it's NOT an inactive trap. So we change it back to original check.

            boolean esTrampa = destino.esTrampa() && destino.estaActiva();

            //Logica de casillas trampa
            if(esTrampa){
                destino.desactivarCasilla();
                marcarTrampaDisparada(destino);
                boolean reyMatado = origen.matar(); // Mata la ficha que haya en la casilla trampa
                this.trampaActivada = destino; // Guardamos para que el servidor lo lea
                if (reyMatado && equipo == 1) {
                    j2Ganador = true;
                    j1Ganador = false;
                    return 2; //Gana el equipo 2 por trampa
                } else if (reyMatado) {
                    j1Ganador = true;
                    j2Ganador = false;
                    return 1; //Gana el equipo 1 por trampa
                } else {
                    if (equipo == 1) { muertesJ1++; } else { muertesJ2++; }
                    origen.setFicha(null);
                    destino.setFicha(null); // La ficha desaparece
                }
            } else {
                //Posibilidad de normal captura
                if (fDestino != null && fDestino.getEquipo() != equipo) {
                    if (fDestino.matar()) { //Si se mata al rey, se acaba la partida
                        //Posible implementacion de patron observer para notificar victoria al matar al rey
                        if (equipo == 1) {
                            j1Ganador = true;
                            muertesJ2++;
                            return 1;
                        } else {
                            j2Ganador = true;
                            muertesJ1++;
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
            }

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
            //Solo se desactivara a accion cuando el rival mueva (osea que podria afectarte si el rival juega justo despues su carta de accion)
            if (accionActivadaJ1 && cartaAccionJugadaJ1 != null && turno%2 + 1 == 2) {
                CartaAccion accionJ1 = cartaAccionJugadaJ1;
                accionJ1.deshacerCarta(this);
                accionJ1.marcarUsada();
                cartaAccionJugadaJ1 = null;
                accionActivadaJ1 = false;
            }
            if (accionActivadaJ2 && cartaAccionJugadaJ2 != null && turno%2 + 1 == 1) {
                CartaAccion accionJ2 = cartaAccionJugadaJ2;
                accionJ2.deshacerCarta(this);
                accionJ2.marcarUsada();
                cartaAccionJugadaJ2 = null;
                accionActivadaJ2 = false;
            }
            turno++; //Cambiamos de turno (tambien lo utilizamos para saber cuantas rondas se han jugado) 
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
            if (cm.getNombre().equals(cartaUsada) && (cm.perteneceAlEquipo(1) || cm.perteneceAlEquipo(2))) {
                usada = cm;
                break;
            }
        }

        if (usada == null) {
            return false; //Carta no encontrada o no pertenece al equipo
        }

        //Buscar una carta del mazo para darle al equipo
        for (CartaMov cm : cartasM) {
            if (cm.estaMazo()) {
                nuevaDelMazo = cm;
                break;
            }
        }

        if (nuevaDelMazo == null) {
            return false; //No hay cartas en el mazo
        }

        //La carta usada pasa al mazo
        usada.marcarMazo();
        usada.actualizarDatosPartida(IDPartida);

        //La carta del mazo pasa al equipo
        nuevaDelMazo.marcarEquipo(equipo);
        nuevaDelMazo.actualizarDatosPartida(IDPartida);

        return true;
    }
}