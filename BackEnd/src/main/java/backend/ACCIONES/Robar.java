package backend.ACCIONES;

import java.util.List;

import backend.VO.CartaMov;
import backend.VO.Partida;

public class Robar extends Accion {
    public Robar() {
        super("ROBAR");
    }

    @Override
    public boolean ejecutar(Partida partida, int x, int y, int equipo, int xOp, int yOp, String nomCarta) {
        System.out.println("Ejecutando acción: " + getNombre());
        List<CartaMov> movimientos = partida.getCartasMovimiento();
        final int equipoOp = (equipo == 1) ? 2 : 1;
        final String estadoOponente = "EQ" + equipoOp;
        final String estadoPropio = "EQ" + equipo;
        final String nombreObjetivo = (nomCarta == null) ? "" : nomCarta.trim();

        CartaMov cartaRobada = null;
        CartaMov primeraDelMazo = null;

        // 1) Buscar de forma determinista la carta objetivo del rival.
        for (CartaMov carta : movimientos) {
            if (carta == null) continue;
            String estado = carta.getEstado();
            String nombre = carta.getNombre();

            if (primeraDelMazo == null && "MAZO".equals(estado)) {
                primeraDelMazo = carta;
            }

            if (estadoOponente.equals(estado) && nombre != null && nombre.trim().equalsIgnoreCase(nombreObjetivo)) {
                cartaRobada = carta;
                break;
            }
        }

        if (cartaRobada == null) {
            return false; // No existe esa carta como carta del rival en este momento.
        }

        // 2) Transferir la robada al ejecutor.
        cartaRobada.setEstado(estadoPropio);
        cartaRobada.actualizarDatosPartida(partida.getIDPartida());

        // 3) Reponer al rival con la primera carta disponible del mazo (si existe).
        if (primeraDelMazo != null && primeraDelMazo != cartaRobada) {
            primeraDelMazo.setEstado(estadoOponente);
            primeraDelMazo.actualizarDatosPartida(partida.getIDPartida());
        }

        return true;
    }

    @Override
    public void deshacer(Partida partida) {}
}