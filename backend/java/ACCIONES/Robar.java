package ACCIONES;

import java.util.List;

import VO.CartaMov;
import VO.Partida;

public class Robar extends Accion {
    public Robar() {
        super("ROBAR");
    }

    @Override
    public boolean ejecutar(Partida partida, int x, int y, int equipo, int xOp, int yOp, String nomCarta) {
        System.out.println("Ejecutando acción: " + getNombre());
        List<CartaMov> movimientos = partida.getCartasMovimiento();
        boolean encontrada = false;
        for (CartaMov carta : movimientos) {
            boolean esOponente = (carta.getEstado().equals("EQ1") && equipo == 2) || (carta.getEstado().equals("EQ2") && equipo == 1);
            if (esOponente && carta.getNombre().equals(nomCarta)) {
                carta.setEstado("EQ"+equipo);
                encontrada = true;
            }
            if (carta.getEstado().equals("MAZO")) {
                if (encontrada) {
                    int equipoOp = (equipo == 1) ? 2 : 1;
                    carta.setEstado("EQ"+equipoOp);
                }
                break;
            }
        }
        return encontrada;
    }

    @Override
    public void deshacer(Partida partida) {}
}