package VO;

import java.util.List;
import java.util.ArrayList;
import JDBC.CartasAccionJDBC;
import JDBC.CartasMovJDBC;

public class Partida {
    private int IDPartida;
    private Jugador jugador1, jugador2;
    private List<CartaAccion> cartasA;
    private List<CartaMov> cartasM;
    private CartasAccionJDBC jdbcAccion;
    private CartasMovJDBC jdbcMov;

    public Partida(int IDPartida, Jugador jugador1, Jugador jugador2) {
        this.IDPartida = IDPartida;
        this.jugador1 = jugador1;
        this.jugador2 = jugador2;
        this.jdbcAccion = new CartasAccionJDBC();
        this.jdbcMov = new CartasMovJDBC();

        try {
            // Intentamos asignar las cartas desde la BD
            this.cartasA = jdbcAccion.asignar4CartasPartida(IDPartida, Math.min(jugador1.getPuntos(), jugador2.getPuntos()));
            this.cartasM = jdbcMov.asignar8CartasPartida(IDPartida);
        } catch (java.sql.SQLException e) {
            // Si hay error de SQL, inicializamos las listas vacías para evitar NullPointerException
            System.err.println("Error al asignar cartas en el constructor de Partida: " + e.getMessage());
            this.cartasA = new java.util.ArrayList<>();
            this.cartasM = new java.util.ArrayList<>();
        }
    }

    public int getIDPartida(){
        return IDPartida;
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
}