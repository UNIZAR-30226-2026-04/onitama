package backend.VO;

import backend.gestor.GestorSkin;

public class Skin {
    private String nombre, colorTablero, colorAliado, colorEnemigo;
    private int precio;
    private GestorSkin gestor;

    public Skin(String nombre, String colorTablero, String colorAliado, String colorEnemigo, int precio){
        this.nombre = nombre;
        this.colorTablero = colorTablero;
        this.colorAliado = colorAliado;
        this.colorEnemigo = colorEnemigo;
        this.precio = precio;
        gestor = new GestorSkin();
    }

    public boolean registrarSkin(){
        try {
            return gestor.crearSkin(this);
        } catch (Exception e) {
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
            return gestor.updatePrecio(nombre, precio) | gestor.updateTablero(nombre, colorTablero) | gestor.updateAliadas(nombre, colorAliado) | gestor.updateEnemigas(nombre, colorEnemigo); //| para que se ejecuten todos
        } catch (Exception e) {
            return false;
        }
    }
}