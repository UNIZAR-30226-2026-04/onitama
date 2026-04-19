package DAO;

import VO.CartaAccion;
import java.sql.SQLException;
import java.util.List;

public interface CartasAccionDAO {
    boolean crearCarta(CartaAccion accion) throws SQLException;
    CartaAccion buscarAccion(String nombre) throws SQLException;
    boolean updateAccion(String nombre, String nuevaAccion) throws SQLException;
    boolean updatePuntosMin(String nombre, int nuevoPunt) throws SQLException;
    void borrar(String nombre) throws SQLException;
    List<CartaAccion> sacarCartas() throws SQLException;
    List<CartaAccion> sacarCartasPartida(int IDPartida) throws SQLException;
    boolean asignarEquipo(int IDPartida, String nombreCarta, int equipo) throws SQLException;
    boolean updateEstadoEnPartida(int IDPartida, String nombreCarta, String estado) throws SQLException;
    List<CartaAccion> asignar4CartasPartida(int IDPartida, int puntosMin) throws SQLException;
    boolean asignarCartaPartida(int IDPartida, String nombreCarta) throws SQLException;
}