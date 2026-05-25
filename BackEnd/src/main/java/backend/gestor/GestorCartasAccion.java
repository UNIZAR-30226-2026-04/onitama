package backend.gestor;

import java.sql.SQLException;
import java.util.List;

import backend.JDBC.CartasAccionJDBC;
import backend.JDBC.PartidaJDBC;
import backend.VO.CartaAccion;
import backend.VO.Partida;

public class GestorCartasAccion {

    private final CartasAccionJDBC cartasAccionJdbc;
    private final PartidaJDBC partidaJdbc;

    public GestorCartasAccion() {
        this.cartasAccionJdbc = new CartasAccionJDBC();
        this.partidaJdbc = new PartidaJDBC();
    }

    public boolean crearCarta(CartaAccion accion) throws SQLException {
        if (cartasAccionJdbc.buscarAccion(accion.getNombre()) != null) {
            return false;
        }
        return cartasAccionJdbc.crearCarta(accion);
    }

    public CartaAccion buscarAccion(String nombre) throws SQLException {
        return cartasAccionJdbc.buscarAccion(nombre);
    }

    public boolean updateAccion(String nombre, String nuevaAccion) throws SQLException {
        return cartasAccionJdbc.updateAccion(nombre, nuevaAccion);
    }

    public boolean updatePuntosMin(String nombre, int nuevoPunt) throws SQLException {
        return cartasAccionJdbc.updatePuntosMin(nombre, nuevoPunt);
    }

    public void borrar(String nombre) throws SQLException {
        cartasAccionJdbc.borrar(nombre);
    }

    public List<CartaAccion> sacarCartas() throws SQLException {
        return cartasAccionJdbc.sacarCartas();
    }

    public List<CartaAccion> sacarCartasPartida(int idPartida) throws SQLException {
        return cartasAccionJdbc.sacarCartasPartida(idPartida);
    }

    public boolean asignarEquipo(int idPartida, String nombreCarta, int equipo) throws SQLException {
        if (equipo != 1 && equipo != 2) {
            return false;
        }
        return cartasAccionJdbc.asignarEquipo(idPartida, nombreCarta, equipo);
    }

    public boolean updateEstadoEnPartida(int idPartida, String nombreCarta, String estado) throws SQLException {
        return cartasAccionJdbc.updateEstadoEnPartida(idPartida, nombreCarta, estado);
    }

    public List<CartaAccion> asignar4CartasPartida(int idPartida, int puntosMin) throws SQLException {
        Partida partida = partidaJdbc.buscarPorId(idPartida);
        if (partida == null) {
            return List.of(); // La partida no existe
        }
        return cartasAccionJdbc.asignar4CartasPartida(idPartida, puntosMin);
    }

    public boolean asignarCartaPartida(int idPartida, String nombreCarta) throws SQLException {
        return cartasAccionJdbc.asignarCartaPartida(idPartida, nombreCarta);
    }
}