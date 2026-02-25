package VO;

import JDBC.CartasAccionJDBC;
import java.sql.SQLException;

public class CartaAccion {
    private String nombre, accion, estado;
    private int puntosMin, equipo;
    private CartasAccionJDBC jdbc;
    
    public CartaAccion(String nombre, String accion, int puntosMin){
        this.nombre = nombre;
        this.accion = accion;
        this.puntosMin = puntosMin;
        this.estado = "VISION"; //Esta en modo vision (Para ver en la tienda)
        this.equipo = -1;
        jdbc = new CartasAccionJDBC();
    }

    //Constructor para cartas en partida, con estado y equipo
    public CartaAccion(String nombre, String accion, int puntosMin, String estado, int equipo){
        this.nombre = nombre;
        this.accion = accion;
        this.puntosMin = puntosMin;
        this.estado = estado;
        this.equipo = equipo;
        jdbc = new CartasAccionJDBC();
    }

    public String getEstado() {
        return estado;
    }

    public int getEquipo() {
        return equipo;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public void setEquipo(int equipo) {
        this.equipo = equipo;
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

    public boolean actualizarDatosPartida(int IDPartida){
        try {
            return jdbc.updateEstadoEnPartida(IDPartida, nombre, estado) | jdbc.asignarEquipo(IDPartida, nombre, equipo);
        } catch (SQLException e) {
            return false;
        }
    }
}