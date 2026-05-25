package backend.VO;

import backend.gestor.GestorSkin;

public class Skin {
    private String nombre;
    private int precio;
    private GestorSkin gestor;

    public Skin(String nombre, int precio){
        this.nombre = nombre;
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

    public void setNombre(String nombre){
        this.nombre = nombre;
    }

    public void setPrecio(int precio){
        this.precio = precio;
    }
    
    public boolean actualizarBD(){
        try {
            return gestor.updatePrecio(nombre, precio); //| para que se ejecuten todos
        } catch (Exception e) {
            return false;
        }
    }
}