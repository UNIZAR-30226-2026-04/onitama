package ACCIONES;

import VO.Ficha;
import VO.Partida;

public class SalvarRey extends Accion {

    public SalvarRey() {
        super("SALVAR_REY");
    }

    @Override
    public boolean ejecutar(Partida partida, int x, int y, int equipo, int xOp, int yOp, String nomCarta) {
        System.out.println("Ejecutando acción: " + getNombre());
        boolean posicionValida = (equipo == 2 && y <= 4) || (equipo == 1 && y >= 4);
        if (posicionValida && partida.getPosicion(x, y).setFicha(new Ficha(true, equipo)) == 0) {
            partida.getRey(equipo).setFicha(null);
            return true;
        }
        return false;
    }

    @Override
    public void deshacer(Partida partida) {}
}