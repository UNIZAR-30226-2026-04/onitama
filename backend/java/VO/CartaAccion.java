package VO;

import JDBC.CartasAccionJDBC;
import java.sql.SQLException;

public class CartaAccion {
    private String nombre, accion;
    private int puntosMin;
    private CartasAccionJDBC jdbc;
    
    public CartaAccion(String nombre, String accion, int puntosMin){
        this.nombre = nombre;
        this.accion = accion;
        this.puntosMin = puntosMin;
        jdbc = new CartasAccionJDBC();
    }

    public boolean registrarCartaAccion(){
        try {
            return jdbc.crearCarta(this);
        } catch (SQLException e) {
            return false;
        }
    }

    public String getNombre(){
        return nombre;
    }

    public String getAccion(){
        return accion;
    }

    public int getPuntosMin(){
        return puntosMin;
    }

    public void setAccion(String accion){
        this.accion = accion;
    }

    public void setPuntosMin(int puntosMin){
        this.puntosMin = puntosMin;
    }

    public boolean actualizarBD(){
        try {
            return jdbc.updatePuntosMin(nombre, puntosMin) | jdbc.updateAccion(nombre, accion);
        } catch (SQLException e) {
            return false;
        }
    }
}