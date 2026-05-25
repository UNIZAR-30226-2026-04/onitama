package backend.ACCIONES;

import backend.VO.Ficha;
import backend.VO.Partida;

public class SalvarRey extends Accion {

    private boolean eraTrampa = false;

    public SalvarRey() {
        super("SALVAR_REY");
    }

    @Override
    public boolean ejecutar(Partida partida, int x, int y, int equipo, int xOp, int yOp, String nomCarta) {
        System.out.println("Ejecutando acción: " + getNombre());
        boolean posicionValida = (equipo == 2 && y >= 3) || (equipo == 1 && y <= 3);
        if (posicionValida && partida.getPosicion(x, y).estaActiva() && partida.getPosicion(x, y).setFicha(new Ficha(true, equipo)) == 0) {
            if(partida.getPosicion(x, y).esTrampa()){
                eraTrampa = true;
                partida.getPosicion(x, y).desactivarCasilla();
            }
            partida.getRey(equipo).setFicha(null);
            return true;
        }
        return false;
    }

    @Override
    public void deshacer(Partida partida) {}

    public boolean reyMuerto(){
        return eraTrampa;
    }
}