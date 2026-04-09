package ACCIONES;

import java.util.ArrayList;
import java.util.List;
import VO.Partida;
import VO.CartaMov;
import VO.Posicion;

public class Espejo extends Accion {
    List<CartaMov> movimientosSinModificar;

    public Espejo() {
        movimientosSinModificar = new ArrayList<>();
        super("ESPEJO");
    }

    @Override
    public boolean ejecutar(Partida partida, int x, int y, int equipo, int xOp, int yOp, String nomCarta) {
        System.out.println("Ejecutando acción: " + getNombre());
        movimientosSinModificar = partida.getCartasMovimiento();
        List<CartaMov> movimientosNuevos = new ArrayList<>();
        for (CartaMov carta : movimientosSinModificar) {
            List<Posicion> movimientosEspejo = new ArrayList<>();
            for (Posicion pos : carta.getListaMovimientos()) {
                Posicion posEspejo = new Posicion(-pos.getX(), pos.getY(), null);
                movimientosEspejo.add(pos);
            }
            carta.setListaMovimientos(movimientosEspejo);
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