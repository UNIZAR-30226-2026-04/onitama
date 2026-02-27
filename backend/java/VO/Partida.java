package VO;

import JDBC.CartasAccionJDBC;
import JDBC.CartasMovJDBC;
import JDBC.JugadorJDBC;
import JDBC.PartidaJDBC;
import java.sql.SQLException;
import java.util.List;

public class Partida {
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
        this.tablero.cargarTablero(p1, p2);

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
        return fichasJ1;
    }

    public String getPos_Fichas_Eq2(){
        return fichasJ2;
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
            return jdbc.updateEstado(IDPartida, estado) | jdbc.updateMuertesFichas2(IDPartida, muertesJ2) | jdbc.updateMuertesFichas1(IDPartida, muertesJ1) | jdbc.updateTiempo(IDPartida, tiempo) | jdbc.updateGanadorJ2(IDPartida, j2Ganador) | jdbc.updateGanadorJ1(IDPartida, j1Ganador) | jdbc.updatePosFichas1(IDPartida, p1) | jdbc.updatePosFichas2(IDPartida, p2); //| para que se ejecuten todos
        } catch (SQLException e) {
            return false;
        }
    }
}