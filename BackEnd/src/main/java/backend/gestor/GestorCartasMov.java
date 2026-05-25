package backend.gestor;

import java.sql.SQLException;
import java.util.List;

import backend.JDBC.CartasMovJDBC;
import backend.JDBC.PartidaJDBC;
import backend.VO.CartaMov;
import backend.VO.Partida;

public class GestorCartasMov {

    private final CartasMovJDBC cartasMovJdbc;
    private final PartidaJDBC partidaJdbc;

    public GestorCartasMov() {
        this.cartasMovJdbc = new CartasMovJDBC();
        this.partidaJdbc = new PartidaJDBC();
    }

    public boolean crearCarta(CartaMov movimiento) throws SQLException {
        if (cartasMovJdbc.buscarMovimiento(movimiento.getNombre()) != null) {
            return false;
        }
        return cartasMovJdbc.crearCarta(movimiento);
    }

    public CartaMov buscarMovimiento(String nombre) throws SQLException {
        return cartasMovJdbc.buscarMovimiento(nombre);
    }

    public boolean updateMovimientos(String nombre, String nuevosMov) throws SQLException {
        return cartasMovJdbc.updateMovimientos(nombre, nuevosMov);
    }

    public boolean updatePuntosMin(String nombre, int puntos) throws SQLException {
        return cartasMovJdbc.updatePuntosMin(nombre, puntos);
    }

    public boolean updateImg(String nombre, String img) throws SQLException {
        return cartasMovJdbc.updateImg(nombre, img);
    }

    public void borrar(String nombre) throws SQLException {
        cartasMovJdbc.borrar(nombre);
    }

    public List<CartaMov> sacarCartas() throws SQLException {
        return cartasMovJdbc.sacarCartas();
    }

    public List<CartaMov> sacarCartasPartida(int idPartida) throws SQLException {
        return cartasMovJdbc.sacarCartasPartida(idPartida);
    }

    public boolean updateEstadoEnPartida(int idPartida, String nombreCarta, String estado) throws SQLException {
        return cartasMovJdbc.updateEstadoEnPartida(idPartida, nombreCarta, estado);
    }

    public List<CartaMov> asignar7CartasPartida(int idPartida, int puntosMin) throws SQLException {
        Partida partida = partidaJdbc.buscarPorId(idPartida);
        if (partida == null) {
            return List.of();
        }
        return cartasMovJdbc.asignar7CartasPartida(idPartida, puntosMin);
    }

    public boolean asignarCartaPartida(int idPartida, String nombreCarta) throws SQLException {
        return cartasMovJdbc.asignarCartaPartida(idPartida, nombreCarta);
    }
}