package ACCIONES;

import VO.Ficha;
import VO.Partida;

public class Revivir extends Accion {

    public Revivir() {
        super("REVIVIR");
    }

    @Override
    public boolean ejecutar(Partida partida, int x, int y, int equipo, int xOp, int yOp, String nomCarta) {
        System.out.println("Ejecutando acción: " + getNombre());
        boolean posicionValida = (equipo == 2 && y >= 3) || (equipo == 1 && y <= 3);
        return posicionValida && partida.getPosicion(x, y).setFicha(new Ficha(false, equipo)) == 0;
    }

    @Override
    public void deshacer(Partida partida) {}
}
