package backend.ACCIONES;

import backend.VO.Partida;

public class SoloAdelante extends Accion {

    public SoloAdelante() {
        super("SOLO_PARA_ADELANTE");
    }

    @Override
    public boolean ejecutar(Partida partida, int x, int y, int equipo, int xOp, int yOp, String nomCarta) {
        System.out.println("Ejecutando acción: " + getNombre());
        // La restricción se aplica en Partida.moverFicha (normDy) sin mutar cartas de movimiento.
        return true;
    }

    @Override
    public void deshacer(Partida partida) {
        // Sin mutación en ejecutar.
    }

    @Override
    public boolean esMovPermitido(int x, int y){
        return y>=0;
    }

    @Override
    public boolean esTipoRestriccion(){
        return true;
    }
}
