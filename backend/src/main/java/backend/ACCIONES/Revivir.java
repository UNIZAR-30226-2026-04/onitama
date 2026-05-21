package backend.ACCIONES;

import backend.VO.Ficha;
import backend.VO.Partida;

public class Revivir extends Accion {

    private boolean eraTrampa;

    public Revivir() {
        super("REVIVIR");
    }

    @Override
    public boolean ejecutar(Partida partida, int x, int y, int equipo, int xOp, int yOp, String nomCarta) {
        System.out.println("Ejecutando acción: " + getNombre());
        boolean posicionValida = (equipo == 2 && y >= 3) || (equipo == 1 && y <= 3);
        if(posicionValida && partida.getPosicion(x, y).estaActiva() && partida.getPosicion(x, y).setFicha(new Ficha(false, equipo)) == 0){
            if(partida.getPosicion(x, y).esTrampa()){
                eraTrampa = true;
                partida.getPosicion(x, y).desactivarCasilla();
                partida.getPosicion(x, y).setFicha(null);
            }
            return true;
        }
        return false;
    }

    @Override
    public void deshacer(Partida partida) {}

    public boolean peonMuerto(){
        return eraTrampa;
    }
}
