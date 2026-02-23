package VO;

import JDBC.SkinJDBC;
import java.sql.SQLException;

public class Skin {
    private String nombre, colorTablero, colorAliado, colorEnemigo;
    private int precio;
    private SkinJDBC jdbc;

    public Skin(String nombre, String colorTablero, String colorAliado, String colorEnemigo, int precio){
        this.nombre = nombre;
        this.colorTablero = colorTablero;
        this.colorAliado = colorAliado;
        this.colorEnemigo = colorEnemigo;
        this.precio = precio;
        jdbc = new SkinJDBC();
    }

    public boolean registrarSkin(){
        try {
            return jdbc.crearSkin(this);
        } catch (SQLException e) {
            return false;
        }
    }

    public String getNombre(){
        return nombre;
    }

    public int getPrecio(){
        return precio;
    }
    
    public String getTablero(){
        return colorTablero;
    }
    
    public String getAliadas(){
        return colorAliado;
    }
    
    public String getEnemigas(){
        return colorEnemigo;
    }

    public void setNombre(String nombre){
        this.nombre = nombre;
    }

    public void setPrecio(int precio){
        this.precio = precio;
    }
    
    public void setTablero(String colorTablero){
        this.colorTablero = colorTablero;
    }
    
    public void setAliadas(String colorAliado){
        this.colorAliado = colorAliado;
    }
    
    public void setEnemigas(String colorEnemigo){
        this.colorEnemigo = colorEnemigo;
    }

    public boolean actualizarBD(){
        try {
            return jdbc.updatePrecio(nombre, precio) | jdbc.updateTablero(nombre, colorTablero) | jdbc.updateAliadas(nombre, colorAliado) | jdbc.updateEnemigas(nombre, colorEnemigo); //| para que se ejecuten todos
        } catch (SQLException e) {
            return false;
        }
    }
}