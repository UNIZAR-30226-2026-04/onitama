package VO;

public class Ficha {
    private final boolean rey;
    private boolean vivo;
    private final int equipo;

    public Ficha(boolean R, int E){
        rey = R;
        vivo = true;
        equipo = E;
    }

//Devuelve true si mata al rey
    public boolean setPosicion(Posicion P){
        return P.setFicha(this)==3;
    }

//Devuelve si era el rey para saber si acabar la partida
    public boolean matar(){
        vivo = false;
        return rey;
    }

    public boolean getVivo(){
        return vivo;
    }

    public int getEquipo(){
        return equipo;
    }
}