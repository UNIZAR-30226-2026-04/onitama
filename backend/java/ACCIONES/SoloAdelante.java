package ACCIONES;

import java.util.ArrayList;
import java.util.List;
import VO.Partida;
import VO.CartaMov;
import VO.Posicion;

public class SoloAdelante extends Accion {
    List<CartaMov> movimientosSinModificar;

    public SoloAdelante() {
        movimientosSinModificar = new ArrayList<>();
        super("SOLO_PARA_ADELANTE");
    }

    @Override
    public boolean ejecutar(Partida partida, int x, int y, int equipo, int xOp, int yOp, String nomCarta) {
        System.out.println("Ejecutando acción: " + getNombre());
        movimientosSinModificar = partida.getCartasMovimiento();
        List<CartaMov> movimientosNuevos = new ArrayList<>();
        for (CartaMov carta : movimientosSinModificar) {
            List<Posicion> movimientosFiltrados = new ArrayList<>();
            for (Posicion pos : carta.getListaMovimientos()) {
                if (pos.getY() >= 0) { // Solo permite movimientos hacia adelante (Y positivo)
                    movimientosFiltrados.add(pos);
                }
            }
            carta.setListaMovimientos(movimientosFiltrados);
            movimientosNuevos.add(carta);
        }
        partida.setCartasMovimiento(movimientosNuevos);
        return true;
    }

    @Override
    public void deshacer(Partida partida) {
        System.out.println("Deshaciendo acción: " + getNombre());
        partida.setCartasMovimiento(movimientosSinModificar);
    }
}