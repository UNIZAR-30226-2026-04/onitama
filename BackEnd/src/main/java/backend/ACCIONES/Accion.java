package backend.ACCIONES;

import backend.VO.Partida;

public abstract class Accion {
    private final String nombre;

    public Accion(String nombre) {
        this.nombre = nombre;
    }

    public String getNombre() {
        return nombre;
    }

    public abstract boolean ejecutar(Partida partida, int x, int y, int equipo, int xOp, int yOp, String nomCarta);

    public abstract void deshacer(Partida partida);

    public boolean esMovPermitido(int x, int y){
        return true;
    }

    public boolean esTipoRestriccion(){
        return false;
    }
}