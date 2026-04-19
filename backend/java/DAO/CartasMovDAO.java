package DAO;

import VO.CartaMov;
import java.sql.SQLException;
import java.util.List;

public interface CartasMovDAO {
    boolean crearCarta(CartaMov movimiento) throws SQLException;
    CartaMov buscarMovimiento(String nombre) throws SQLException;
    boolean updateMovimientos(String nombre, String nuevosMov) throws SQLException;
    boolean updatePuntosMin(String nombre, int puntos) throws SQLException;
    boolean updateImg(String nombre, String img) throws SQLException;
    void borrar(String nombre) throws SQLException;
    List<CartaMov> sacarCartas() throws SQLException;
    List<CartaMov> sacarCartasPartida(int IDPartida) throws SQLException;
    boolean updateEstadoEnPartida(int IDPartida, String nombreCarta, String estado) throws SQLException;
    List<CartaMov> asignar7CartasPartida(int IDPartida, int puntosMin) throws SQLException;
    boolean asignarCartaPartida(int IDPartida, String nombreCarta) throws SQLException;
}