package backend.ACCIONES;

import backend.VO.Ficha;
import backend.VO.Partida;

public class Sacrificio extends Accion {

    public Sacrificio() {
        super("SACRIFICIO");
    }

    @Override
    public boolean ejecutar(Partida partida, int x, int y, int equipo, int xOp, int yOp, String nomCarta) {
        System.out.println("Ejecutando acción: " + getNombre());
        var posMia = partida.getPosicion(x, y);
        var posOponente = partida.getPosicion(xOp, yOp);
        Ficha mia = posMia.getFicha();
        Ficha oponente = posOponente.getFicha();
        if (mia != null && oponente != null && mia.getEquipo() == equipo && oponente.getEquipo() != equipo && !mia.isRey() && !oponente.isRey()) {
            // Sacrificio/Requiem: ambas fichas deben desaparecer físicamente del tablero
            // para que el estado persistido en BD no las recupere al reanudar.
            mia.matar();
            oponente.matar();
            posMia.setFicha(null);
            posOponente.setFicha(null);
            return true;
        }
        return false;
    }

    @Override
    public void deshacer(Partida partida) {}
}