package ACCIONES;

import VO.Ficha;
import VO.Partida;

public class Sacrificio extends Accion {

    public Sacrificio() {
        super("SACRIFICIO");
    }

    @Override
    public boolean ejecutar(Partida partida, int x, int y, int equipo, int xOp, int yOp, String nomCarta) {
        System.out.println("Ejecutando acción: " + getNombre());
        Ficha mia = partida.getPosicion(x, y).getFicha();
        Ficha oponente = partida.getPosicion(xOp, yOp).getFicha();
        if (mia != null && oponente != null && mia.getEquipo() == equipo && oponente.getEquipo() != equipo && !mia.isRey() && !oponente.isRey()) {
            return !mia.matar() && !oponente.matar();
        }
        return false;
    }

    @Override
    public void deshacer(Partida partida) {}
}