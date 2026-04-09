package VO;

import java.sql.SQLException;

import ACCIONES.Accion;
import JDBC.CartasAccionJDBC;

public class CartaAccion {
    private String nombre, accion, estado, img;
    private int puntosMin, equipo;
    private CartasAccionJDBC jdbc;
    private Accion accionEjecutable;
    
    public CartaAccion(String nombre, String accion, int puntosMin, String img){
        this.nombre = nombre;
        this.accion = accion;
        this.puntosMin = puntosMin;
        this.img = img;
        this.estado = "VISION"; //Esta en modo vision (Para ver en la tienda)
        this.equipo = -1;
        jdbc = new CartasAccionJDBC();
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
            case "VENGANZA":
                accionEjecutable = new Venganza();
                break;
            case "ROBAR":
                accionEjecutable = new Robar();
                break;
            case "CEGAR":
                accionEjecutable = new Cegar();
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
        jdbc = new CartasAccionJDBC();
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

    public boolean jugarCarta(Partida partida, int x, int y, int equipo, int xOp, int yOp, String nomCarta){
        if (accionEjecutable != null && equipo == this.equipo) {
            if(accionEjecutable.ejecutar(partida, x, y, equipo, xOp, yOp, nomCarta)) {
                estado = "USADA";
                return true;
            }
        }
        return false;
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

    //Comprueba si la carta puede ser usada (estado USABLE y no usada previamente)
    public boolean puedeUsarse(){
        return "USABLE".equals(estado);
    }

    //Marca la carta como usada cambiando su estado
    public void marcarComoUsada(){
        this.estado = "USADA";
    }

    //Marca la carta como usada y actualiza en la BD para la partida dada
    public boolean marcarComoUsada(int IDPartida){
        this.estado = "USADA";
        return actualizarDatosPartida(IDPartida);
    }

    //Ejecuta la accion de la carta sobre el tablero de la partida
    //Devuelve true si la accion se ejecuto correctamente
    public boolean ejecutarAccion(Tablero tablero, int equipoQueUsa){
        if (!puedeUsarse()) {
            return false; //No se puede usar una carta que ya fue usada o no esta disponible
        }

        switch (accion) {
            case "ESPEJO":
                //Refleja los movimientos del oponente igual iria al turno
                marcarComoUsada();
                return true;

            case "REVIVIR":
                //Revive un peon muerto (meter en el trono propio si esta vacio)
                Posicion trono = (equipoQueUsa == 1) ? tablero.trono1 : tablero.trono2;
                if (trono.ocupado() == -1) {
                    trono.setFicha(new Ficha(false, equipoQueUsa));
                    marcarComoUsada();
                    return true;
                }
                return false; //Trono ocupado, no se puede revivir

            case "SALVAR_REY":
                //Salva al rey de una captura igual iria al turno
                marcarComoUsada();
                return true;

            case "SACRIFICIO":
                //Se sacrifica un peon tuyo para matar a otro del enemigo
                //La seleccion de peones se haría externamente, aqui solo se marca
                marcarComoUsada();
                return true;

            case "SOLO_PARA_ADELANTE":
                //El oponente solo puede mover hacia adelante este turno
                marcarComoUsada();
                return true;

            case "VENGANZA":
                //Si te matan al rey antes de 5 min, empate 
                marcarComoUsada();
                return true;

            case "ROBAR":
                //Roba una carta de movimiento al enemigo
                marcarComoUsada();
                return true;

            case "CEGAR":
                //El oponente no ve el tablero durante un turno
                marcarComoUsada();
                return true;

            case "SOLO_PARA_ATRAS":
                //El oponente solo puede mover hacia atras este turno
                marcarComoUsada();
                return true;

            default:
                return false; //Accion no reconocida
        }
    }
}