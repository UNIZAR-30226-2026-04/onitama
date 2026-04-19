package DAO;

import VO.Notificacion;
import java.sql.SQLException;
import java.util.List;

public interface NotificacionDAO {
    int crear(Notificacion notif) throws SQLException;
    Notificacion obtenerPorId(int idNotificacion) throws SQLException;
    void borrar(int ID) throws SQLException;
    List<Notificacion> obtenerPendientes(String nombreUsuario) throws SQLException;
    boolean actualizarEstado(int idNotificacion, String nuevoEstado) throws SQLException;
}