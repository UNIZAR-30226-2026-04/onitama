package VO;

import JDBC.JugadorJDBC;
import java.util.List;
import java.util.ArrayList;
import java.sql.SQLException;

//Faltan las skins
public class Jugador {
    private String nombre, password, correo;
    private int puntos;
    private List<Jugador> amigos;
    private JugadorJDBC jdbc;
    
    //Constructor necesario para la BD
    public Jugador(String correo, String nombre, String password, int puntos){
        this.correo = correo;
        this.nombre = nombre;
        this.password = password; //Faltaria hashear con BCryp
        this.puntos = puntos;
        amigos = new ArrayList<>();
        jdbc = new JugadorJDBC();
    }

    public boolean registrarse(){
        try {
            return jdbc.registrarse(this);
        } catch (SQLException e) {
            return false;
        }
    }

    public void setNombre(String nombre){
        this.nombre = nombre;
    }

    public void setCorreo(String correo){
        this.correo = correo;
    }

    public void setContrasenya(String password){
        this.password = password;
    }

    public void setPuntos(int puntos){
        this.puntos = puntos;
    }

    public String getCorreo(){
        return correo;
    }
     
    public String getNombre(){
        return nombre;
    }
    
    public String getContrasenya(){
        return password;
    }
    
    public int getPuntos(){
        return puntos;
    }

    public boolean actualizarBD(){
        try {
            return jdbc.updateContrasenya(nombre, password) | jdbc.updateCorreo(nombre, correo) | jdbc.updatePuntos(nombre, puntos); //| para que se ejecuten todos
        } catch (SQLException e) {
            return false;
        }
    }

    public void cargarAmigos(){
        try {
            amigos = jdbc.sacarAmigos(nombre);
        } catch (SQLException e) {
        }
    }

    public List<Jugador> getAmigos(){
        return amigos;
    }

    public boolean anyadirAmigo(Jugador amigo){
        amigos.add(amigo); //Añadimos en la lista para evitar tener que estar cargando de la BD
        try {
            return jdbc.anyadirAmigo(nombre, amigo.getNombre());
        } catch (SQLException e) {
            return false;
        }
    }
    
    public boolean borrarAmigo(Jugador amigo){
        amigos.remove(amigo); //Borramos de la lista para evitar tener que estar cargando de la BD
        try {
            return jdbc.borrarAmigo(nombre, amigo.getNombre());
        } catch (SQLException e) {
            return false;
        }
    }
}