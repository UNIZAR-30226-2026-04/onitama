package VO;

import JDBC.CartasAccionJDBC;
import JDBC.CartasMovJDBC;
import JDBC.JugadorJDBC;
import JDBC.PartidaJDBC;
import java.sql.SQLException;
import java.util.List;

public class Partida extends Subject {
    private int IDPartida, tiempo, muertesJ1, muertesJ2;
    private String estado, tipo; //Cambiar fichas por su correspondiente clase
    private boolean j1Ganador, j2Ganador;
    private Jugador jugador1, jugador2;
    private List<CartaAccion> cartasA;
    private List<CartaMov> cartasM;
    private Tablero tablero;
    private PartidaJDBC jdbc;
    private CartasAccionJDBC jdbcAccion;
    private CartasMovJDBC jdbcMov;
    private JugadorJDBC jdbcJugador;

    public Partida(int IDPartida, String estado, int tiempo, String tipo, String p1, String p2, int m1, int m2, String jugador1, String jugador2, boolean g1, boolean g2) {
        this.IDPartida = IDPartida;
        this.jdbc = new PartidaJDBC();
        this.jdbcAccion = new CartasAccionJDBC();
        this.jdbcMov = new CartasMovJDBC();
        this.jdbcJugador = new JugadorJDBC();
        this.estado = estado;
        this.tiempo = tiempo;
        this.tipo = tipo;
        this.muertesJ1 = m1;
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

    public boolean registrarPartida(){
        try {
            IDPartida = jdbc.registrarPartida(this);
            return IDPartida >= 0; //Se devuelve el id que asigna la base a la partida, si hay algun problema devuelve -1
        } catch (SQLException e) {
            return false;
        }
    }

    public void asignarCartas(){
        try {
            int puntosMin = Math.min(this.jugador1.getPuntos(), this.jugador2.getPuntos());
            this.cartasA = jdbcAccion.asignar4CartasPartida(IDPartida, puntosMin);
            this.cartasM = jdbcMov.asignar7CartasPartida(IDPartida, puntosMin);
        } catch (java.sql.SQLException e) {
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

    public void setJ1(Jugador j){
        this.jugador1 = j;
    }

    public void setJ2(Jugador j){
        this.jugador2 = j;
    }

    public void repartirCartas() {
        int i = 0;
        for (CartaAccion ca : cartasA) {
            ca.setEquipo((i%2)+1);
            ca.setEstado("USABLE"); //Que se puede jugar (si se juega -> USADA)
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
            return jdbc.updateEstado(IDPartida, estado) | jdbc.updateMuertesFichas2(IDPartida, muertesJ2) | jdbc.updateMuertesFichas1(IDPartida, muertesJ1) | jdbc.updateTiempo(IDPartida, tiempo) | jdbc.updateGanadorJ2(IDPartida, j2Ganador) | jdbc.updateGanadorJ1(IDPartida, j1Ganador) | jdbc.updatePosFichas1(IDPartida, p1.getValor()) | jdbc.updatePosFichas2(IDPartida, p2.getValor()); //| para que se ejecuten todos
        } catch (SQLException e) {
            return false;
        }
    }

    //Inicia la partida: cambia estado a Jugandose, asigna las cartas y las reparte
    public boolean iniciarPartida(){
        this.estado = "Jugandose";
        this.tiempo = 0;
        this.tablero.cargarTablero();
        asignarCartas();
        repartirCartas();
        return actualizarBD();
    }

    //Pausa la partida cambiando su estado
    public boolean pausarPartida(){
        if (!"Jugandose".equals(estado)) {
            return false; //Solo se puede pausar una partida en curso
        }
        this.estado = "Pausada";
        try {
            return jdbc.updateEstado(IDPartida, estado);
        } catch (SQLException e) {
            return false;
        }
    }

    //Reanuda la partida desde el estado de pausa
    public boolean reanudarPartida(){
        if (!"Pausada".equals(estado)) {
            return false; //Solo se puede reanudar una partida pausada
        }
        this.estado = "Jugandose";
        try {
            return jdbc.updateEstado(IDPartida, estado);
        } catch (SQLException e) {
            return false;
        }
    }

    //Un jugador abandona la partida, el otro gana automaticamente
    public boolean abandonarPartida(int equipoQueAbandona){
        this.estado = "Abandonada";
        if (equipoQueAbandona == 1) {
            this.j2Ganador = true;
            this.j1Ganador = false;
        } else {
            this.j1Ganador = true;
            this.j2Ganador = false;
        }
        return actualizarBD();
    }

    //Finaliza la partida y actualiza las estadisticas de los jugadores
    public boolean finalizarPartida(){
        this.estado = "Finalizada";
        //Actualizar estadisticas de los jugadores
        if (j1Ganador && jugador1 != null) {
            jugador1.registrarVictoria(10, 50); //Cores y puntos por ganar
            jugador1.actualizarBD();
        }
        if (j2Ganador && jugador2 != null) {
            jugador2.registrarVictoria(10, 50);
            jugador2.actualizarBD();
        }
        //El perdedor solo incrementa partidas jugadas
        if (!j1Ganador && jugador1 != null) {
            jugador1.incrementarPartidasJugadas();
            jugador1.actualizarBD();
        }
        if (!j2Ganador && jugador2 != null) {
            jugador2.incrementarPartidasJugadas();
            jugador2.actualizarBD();
        }
        return actualizarBD();
    }

    /* REVISAR (Ciro): Creo que seria mejor comprobar la victoria al realizar el movimiento
    //Comprueba si hay condicion de victoria:
    //1. Rey capturado (no hay rey en el tablero)
    //2. Rey en trono enemigo
    //Devuelve 0 si no hay victoria, 1 si gana equipo 1, 2 si gana equipo 2
    public int verificarVictoria(){
        boolean reyJ1Vivo = false;
        boolean reyJ2Vivo = false;
        int DIM = tablero.getDIM();

        for (int i = 0; i < DIM; i++) {
            for (int j = 0; j < DIM; j++) {
                Posicion pos = tablero.getPosicion(j, i);
                Ficha ficha = pos.getFicha();
                if (ficha != null && ficha.getVivo()) {
                    if (ficha.isRey() && ficha.getEquipo() == 1) {
                        reyJ1Vivo = true;
                        //Victoria por trono: rey del equipo 1 llega al trono del equipo 2
                        if (pos == tablero.trono2) {
                            j1Ganador = true;
                            return 1;
                        }
                    }
                    if (ficha.isRey() && ficha.getEquipo() == 2) {
                        reyJ2Vivo = true;
                        //Victoria por trono: rey del equipo 2 llega al trono del equipo 1
                        if (pos == tablero.trono1) {
                            j2Ganador = true;
                            return 2;
                        }
                    }
                }
            }
        }
        

        //Victoria por captura: si un rey esta muerto, gana el otro equipo
        if (!reyJ1Vivo) {
            j2Ganador = true;
            return 2;
        }
        if (!reyJ2Vivo) {
            j1Ganador = true;
            return 1;
        }

        return 0; //No hay victoria todavia
    }
    */

   //FALTA EL TEMA DE LOS TURNOS REVISAR
    public boolean moverFicha(int equipo, Posicion origen, Posicion destino, CartaMov carta) {
        Ficha fOrigen = origen.getFicha();
        Ficha fDestino = destino.getFicha();
        List<Posicion> movimientosValidos = carta.getListaMovimientos();
        Posicion movimientoARealizar = new Posicion(destino.getX() - origen.getX(), destino.getY() - origen.getY(), null);
        //Por si acaso comprobamos que el movimiento existe
        if (fOrigen == null || fOrigen.getEquipo() != equipo || !movimientosValidos.contains(movimientoARealizar) || destino.getX()>=7 || destino.getY()>=7 || destino.getX()<0 || destino.getY()<0) {
            return false; //Movimiento no valido segun la carta
        }
        //Posibilidad de que se requiera modificaciones
        if (fDestino != null || fDestino.getEquipo() != equipo) {
            if (fDestino.matar()) { //Si se mata al rey, se acaba la partida
                //Posible implementacion de patron observer para notificar victoria al matar al rey
                if (equipo == 1) {
                    j1Ganador = true;
                    nootify(); //Notificar a los observers que el equipo 1 ha ganado (Esta implementado en la clase Subject)
                } else {
                    j2Ganador = true;
                    nootify(); //Notificar a los observers que el equipo 2 ha ganado
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
        rotarCartas(carta.getNombre(), equipo);
        return true; //Movimiento realizado con exito
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

    //FALTA EL TEMA DE LOS TURNOS REVISAR
    public boolean jugarAccion(CartaAccion carta) {
        //Yo esperaria a tener hecha una partida normal para implementar las cartas de accion, 
        //porque pueden ser muy variadas y no se me ocurre una implementacion general que sirva 
        //para todas, habria que ir viendo carta por carta
        return false;
    }
}