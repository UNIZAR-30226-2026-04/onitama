package backend.ACCIONES;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import backend.VO.CartaMov;
import backend.VO.Partida;
import backend.VO.Posicion;

public class Espejo extends Accion {
    private Map<CartaMov, List<Posicion>> movimientosOriginales = new HashMap<>();

    public Espejo() {
        super("ESPEJO");
    }

    @Override
    public boolean ejecutar(Partida partida, int x, int y, int equipo, int xOp, int yOp, String nomCarta) {
        System.out.println("Ejecutando acción: " + getNombre());
        List<CartaMov> cartas = partida.getCartasMovimiento();
        movimientosOriginales.clear();

        for (CartaMov carta : cartas) {
            List<Posicion> posOriginales = new ArrayList<>(carta.getListaMovimientos());
            movimientosOriginales.put(carta, posOriginales);

            List<Posicion> posEspejo = new ArrayList<>();
            for (Posicion pos : posOriginales) {
                posEspejo.add(new Posicion(-pos.getX(), pos.getY(), null));
            }
            carta.setListaMovimientos(posEspejo);
        }
        return true;
    }

    @Override
    public void deshacer(Partida partida) {
        System.out.println("Deshaciendo acción: " + getNombre());
        for (Map.Entry<CartaMov, List<Posicion>> entry : movimientosOriginales.entrySet()) {
            entry.getKey().setListaMovimientos(entry.getValue());
        }
        movimientosOriginales.clear();
    }
}