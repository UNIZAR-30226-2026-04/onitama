package backend.VO;

import backend.ACCIONES.Accion;
import backend.ACCIONES.Espejo;
import backend.ACCIONES.Revivir;
import backend.ACCIONES.Robar;
import backend.ACCIONES.Sacrificio;
import backend.ACCIONES.SalvarRey;
import backend.ACCIONES.SoloAdelante;
import backend.ACCIONES.SoloAtras;
import backend.gestor.GestorCartasAccion;

public class CartaAccion {
    private String nombre, accion, estado;
    private int puntosMin, equipo;
    private GestorCartasAccion gestor;
    private Accion accionEjecutable;
    
    public CartaAccion(String nombre, String accion, int puntosMin){
        this.nombre = nombre;
        this.accion = accion;
        this.puntosMin = puntosMin;
        this.estado = "VISION"; //Esta en modo vision (Para ver en la tienda)
        this.equipo = -1;
        gestor = new GestorCartasAccion();

        //Generamos la accion segun el String
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
    public CartaAccion(String nombre, String accion, int puntosMin, String estado, int equipo){
        this.nombre = nombre;
        this.accion = accion;
        this.puntosMin = puntosMin;
        this.estado = estado;
        this.equipo = equipo;
        gestor = new GestorCartasAccion();
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

    //Devuelve 1 si el rey que se movio con la accion murio por caer en una trampa y 2 si paso lo mismo pero con un peon. 0 si no paso nada
    public int eraTrampa(){
        if(accionEjecutable instanceof SalvarRey){
            SalvarRey acc1 = (SalvarRey)accionEjecutable;
            if(acc1.reyMuerto()){
                return 1;
            }
        }else if(accionEjecutable instanceof Revivir){
            Revivir acc2 = (Revivir)accionEjecutable;
            if(acc2.peonMuerto()){
                return 2;
            }
        }
        return 0;
    }

    //Devuelve true si la carta restringe movimientos
    public boolean esTipoRestriccion(){
        return accionEjecutable != null && accionEjecutable.esTipoRestriccion();
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
            return gestor.crearCarta(this);
        } catch (Exception e) {
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

    //Mira si la accion permite un movimiento dado
    public boolean permiteMovimiento(int x, int y){
        if (accionEjecutable == null) {
            return true;
        }
        return accionEjecutable.esMovPermitido(x, y);
    }

    //Juega la accion en la carta si se puede usar
    public boolean jugarCarta(Partida partida, int x, int y, int equipo, int xOp, int yOp, String nomCarta){
        if (puedeUsarse() &&  equipo == this.equipo && accion.equals("CEGAR")) {
            marcarActivada();
            return true; //Como es un efecto visual, se manejara en el front (solo se mandara el mensaje avisando que se ha jugado)
        }else if (puedeUsarse() && accionEjecutable != null && equipo == this.equipo) {
            if(accionEjecutable.ejecutar(partida, x, y, equipo, xOp, yOp, nomCarta)) {
                marcarActivada();
                return true;
            }
        }
        return false;
    }

    //Deshace la accion de la carta
    public void deshacerCarta(Partida partida){
        if (accionEjecutable != null) {
            accionEjecutable.deshacer(partida);
        }
    }

    //Actualiza la informacion de la carta en la BD
    public boolean actualizarBD(){
        try {
            return gestor.updatePuntosMin(nombre, puntosMin) | gestor.updateAccion(nombre, accion);
        } catch (Exception e) {
            return false;
        }
    }

    //Actualiza la informacion de la carta en la partida
    public boolean actualizarDatosPartida(int IDPartida){
        try {
            return gestor.updateEstadoEnPartida(IDPartida, nombre, estado) | gestor.asignarEquipo(IDPartida, nombre, equipo);
        } catch (Exception e) {
            return false;
        }
    }
}