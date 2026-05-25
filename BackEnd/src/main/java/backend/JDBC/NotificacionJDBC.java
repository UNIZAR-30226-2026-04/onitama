package backend.JDBC;

import java.sql.*;
import javax.sql.DataSource;
import java.util.List;
import java.util.ArrayList;
import backend.VO.Notificacion;

import backend.DAO.NotificacionDAO;

public final class NotificacionJDBC implements NotificacionDAO {

    private final DataSource dataSource;

    public NotificacionJDBC() {
        try {
            String url = "jdbc:postgresql://database:5432/postgres";
            String user = "postgres";
            String password = "postgres";

            org.postgresql.ds.PGSimpleDataSource ds = new org.postgresql.ds.PGSimpleDataSource();
            ds.setURL(url);
            ds.setUser(user);
            ds.setPassword(password);
            this.dataSource = ds;

        } catch (Exception e) {
            throw new RuntimeException("Error al conectar manualmente", e);
        }
    }

    /**
     * Crea una nueva notificación.
     * (devuelve El ID generado o -1 si falló).
     */
    public int crear(Notificacion notif) throws SQLException {
        final String sql = "INSERT INTO Notificaciones (Tipo, Remitente, Destinatario, Estado, Fecha_Expiracion, ID_Partida) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, notif.getTipo());
            ps.setString(2, notif.getRemitente());
            ps.setString(3, notif.getDestinatario());
            ps.setString(4, notif.getEstado());
            if (notif.getFechaExpiracion() != null) {
                ps.setTimestamp(5, notif.getFechaExpiracion());
            } else {
                ps.setNull(5, Types.TIMESTAMP);
            }
            if (notif.getIdPartida() != null) {
                ps.setInt(6, notif.getIdPartida());
            } else {
                ps.setNull(6, Types.INTEGER);
            }

            int filas = ps.executeUpdate();
            if (filas > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
            }
            return -1;
        }
    }

    /**
     * Obtiene las notificaciones pendientes (no expiradas) de un destinatario.
     */
    public List<Notificacion> obtenerPendientes(String nombreUsuario) throws SQLException {
        final String sql = "SELECT ID_Notificacion, Tipo, Remitente, Destinatario, Estado, Fecha_Creacion, Fecha_Expiracion, ID_Partida " +
                "FROM Notificaciones WHERE Destinatario = ? AND Estado = 'PENDIENTE' " +
                "AND (Fecha_Expiracion IS NULL OR Fecha_Expiracion > CURRENT_TIMESTAMP) ORDER BY Fecha_Creacion DESC";

        List<Notificacion> lista = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nombreUsuario);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(montarNotificacion(rs));
                }
            }
        }
        return lista;
    }

    /**
     * Actualiza el estado de una notificación.
     */
    public boolean actualizarEstado(int idNotificacion, String nuevoEstado) throws SQLException {
        final String sql = "UPDATE Notificaciones SET Estado = ? WHERE ID_Notificacion = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nuevoEstado);
            ps.setInt(2, idNotificacion);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Obtiene una notificación por su ID.
     */
    public Notificacion obtenerPorId(int idNotificacion) throws SQLException {
        final String sql = "SELECT ID_Notificacion, Tipo, Remitente, Destinatario, Estado, Fecha_Creacion, Fecha_Expiracion, ID_Partida " +
                "FROM Notificaciones WHERE ID_Notificacion = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idNotificacion);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return montarNotificacion(rs);
                }
            }
        }
        return null;
    }

    public void borrar(int ID) throws SQLException {
        final String sql = "DELETE FROM Notificaciones WHERE ID_Notificacion = ?";
        try (Connection conn = dataSource.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, ID);
            ps.executeUpdate();
        }
    }

    private Notificacion montarNotificacion(ResultSet rs) throws SQLException {
        int id = rs.getInt("ID_Notificacion");
        String tipo = rs.getString("Tipo");
        String remitente = rs.getString("Remitente");
        String destinatario = rs.getString("Destinatario");
        String estado = rs.getString("Estado");
        Timestamp fechaCreacion = rs.getTimestamp("Fecha_Creacion");
        Timestamp fechaExpiracion = rs.getTimestamp("Fecha_Expiracion");
        Integer idPartida;
        int idP = rs.getInt("ID_Partida");
        idPartida = rs.wasNull() ? null : idP;
        return new Notificacion(id, tipo, remitente, destinatario, estado, fechaCreacion, fechaExpiracion, idPartida);
    }
}
