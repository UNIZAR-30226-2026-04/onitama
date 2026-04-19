package VO;

import java.sql.SQLException;

import ACCIONES.Accion;
import ACCIONES.Espejo;
import ACCIONES.Revivir;
import ACCIONES.Robar;
import ACCIONES.Sacrificio;
import ACCIONES.SalvarRey;
import ACCIONES.SoloAdelante;
import ACCIONES.SoloAtras;
import JDBC.CartasAccionJDBC;
import DAO.CartasAccionDAO;

public class CartaAccion {
    private String nombre, accion, estado, img;
    private int puntosMin, equipo;
    private CartasAccionDAO dao;
    private Accion accionEjecutable;
    
    public CartaAccion(String nombre, String accion, int puntosMin, String img){
        this.nombre = nombre;
        this.accion = accion;
        this.puntosMin = puntosMin;
        this.img = img;
        this.estado = "VISION"; //Esta en modo vision (Para ver en la tienda)
        this.equipo = -1;
        dao = new CartasAccionJDBC();
        switch (accion) {
            case "ESPEJO":
                accionEjecutable = new Espejo();
                break;
            case "REVIVIR":
                accionEjecutable = new Revivir();
                break;
            case "SALVAR_REY":
                accionEjecutable = new SalvarRey();
                break;
            case "SACRIFICIO":
                accionEjecutable = new Sacrificio();
                break;
            case "SOLO_PARA_ADELANTE":
                accionEjecutable = new SoloAdelante();
                break;
            case "ROBAR":
                accionEjecutable = new Robar();
                break;
            case "SOLO_PARA_ATRAS":
                accionEjecutable = new SoloAtras();
                break;
            default:
        }
    }

    //Constructor para cartas en partida, con estado y equipo
    public CartaAccion(String nombre, String accion, int puntosMin, String img, String estado, int equipo){
        this.nombre = nombre;
        this.accion = accion;
        this.puntosMin = puntosMin;
        this.img = img;
        this.estado = estado;
        this.equipo = equipo;
        dao = new CartasAccionJDBC();
    }

    //Comprueba si la carta puede ser usada (estado USABLE y no usada previamente)
    public boolean puedeUsarse(){
        return "USABLE".equals(estado);
    }

    public void marcarActivada(){
        estado = "ACTIVA";
    }

    public void marcarUsable(){
        estado = "USABLE";
    }

    public void marcarUsada(){
        estado = "USADA";
    }

    public void marcarEsperando(){
        estado = "ESPERANDO";
    }

    public void marcarNoUsable(){
        estado = "NO_USABLE";
    }

    public boolean estaActiva(){
        return estado.equals("ACTIVA");
    }

    public String getImg() {
        return img;
    }

    public void setImg(String img) {
        this.img = img;
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
            return dao.crearCarta(this);
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

    public boolean permiteMovimiento(int x, int y){
        return accionEjecutable.esMovPermitido(x, y);
    }

    public boolean jugarCarta(Partida partida, int x, int y, int equipo, int xOp, int yOp, String nomCarta){
        if (puedeUsarse() &&  equipo == this.equipo && accion.equals("CEGAR")) {
            return true; //Como es un efecto visual, se manejara en el front (solo se mandara el mensaje avisando que se ha jugado)
        }else if (puedeUsarse() && accionEjecutable != null && equipo == this.equipo) {
            if(accionEjecutable.ejecutar(partida, x, y, equipo, xOp, yOp, nomCarta)) {
                marcarActivada();
                return true;
            }
        }
        return false;
    }

    public void deshacerCarta(Partida partida){
        if (accionEjecutable != null) {
            accionEjecutable.deshacer(partida);
        }
    }

    public boolean actualizarBD(){
        try {
            return dao.updatePuntosMin(nombre, puntosMin) | dao.updateAccion(nombre, accion);
        } catch (SQLException e) {
            return false;
        }
    }

    public boolean actualizarDatosPartida(int IDPartida){
        try {
            return dao.updateEstadoEnPartida(IDPartida, nombre, estado) | dao.asignarEquipo(IDPartida, nombre, equipo);
        } catch (SQLException e) {
            return false;
        }
    }

    /*
    //Marca la carta como usada cambiando su estado
    public void marcarComoUsada(){
        this.estado = "USADA";
    }

    //Marca la carta como usada y actualiza en la BD para la partida dada
    public boolean marcarComoUsada(int IDPartida){
        this.estado = "USADA";
        return actualizarDatosPartida(IDPartida);
    }*/
}