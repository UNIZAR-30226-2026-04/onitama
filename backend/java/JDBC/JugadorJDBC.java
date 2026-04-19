package JDBC;

import java.sql.*;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.util.List;
import java.util.ArrayList;
import VO.Jugador;

import DAO.JugadorDAO;

public final class JugadorJDBC implements JugadorDAO {

    private final DataSource dataSource;

    public JugadorJDBC() {
        try {
            String url = "jdbc:postgresql://localhost:5432/postgres"; 
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
    
    public boolean registrarse(Jugador jugador) throws SQLException {
        final String sql = "INSERT INTO Jugador (Correo, Nombre_US, Contrasena_Hash, Puntos, Cores, Partidas_Ganadas, Partidas_Jugadas, avatar_id, skin_activa) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = dataSource.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, jugador.getCorreo());
            ps.setString(2, jugador.getNombre());
            ps.setString(3, jugador.getContrasenya());
            ps.setInt(4, jugador.getPuntos());
            ps.setInt(5, jugador.getCores());
            ps.setInt(6, jugador.getPartidasGanadas());
            ps.setInt(7, jugador.getPartidasJugadas());
            // NUEVO PARA AVATAR Y  SKIN
            ps.setString(8, jugador.getAvatarId());
            ps.setString(9, jugador.getSkinActiva());
            int filasAfectadas = ps.executeUpdate();
            return filasAfectadas > 0;
            
        } catch (SQLException e) {
            e.printStackTrace(); // ESTO TE DIRÁ EL ERROR REAL
            return false; // Si hay una excepción, asumimos que no se creó
        }
    }

    public Jugador buscarJugador(String nombreUS) throws SQLException {
        // añadido avatar y skin
        final String sql = "SELECT Correo, Nombre_US, Contrasena_Hash, Puntos, Cores, Partidas_Ganadas, Partidas_Jugadas, avatar_id, skin_activa FROM Jugador WHERE Nombre_US = ?";
        try (Connection conn = dataSource.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nombreUS);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return montarJugador(rs);
            }
        }
    }

    public List<Jugador> buscarJugadoresPorRaiz(String nombreUS) throws SQLException {
        final String sql = "SELECT * FROM Jugador WHERE Nombre_US LIKE ?";
        List<Jugador> lista = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, nombreUS + "%");
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(montarJugador(rs));
                }
            }
        }
        return lista; 
    }

    public boolean updatePuntos(String nombreUS, int nuevosPuntos) throws SQLException {
        try(Connection c = dataSource.getConnection(); 
            PreparedStatement p = c.prepareStatement("UPDATE Jugador SET Puntos = ? WHERE Nombre_US = ?")) { 
            p.setInt(1, nuevosPuntos); 
            p.setString(2, nombreUS); 
            p.executeUpdate(); 
            return true;
        }catch (SQLException e) {
            return false; // Si hay una excepción, asumimos que no se creó
        }
    }

    public boolean updateContrasenya(String nombreUS, String psswd) throws SQLException {
        try(Connection c = dataSource.getConnection(); 
            PreparedStatement p = c.prepareStatement("UPDATE Jugador SET Contrasena_Hash = ? WHERE Nombre_US = ?")) { 
            p.setString(1, psswd); 
            p.setString(2, nombreUS); 
            p.executeUpdate(); 
            return true;
        }catch (SQLException e) {
            return false; // Si hay una excepción, asumimos que no se creó
        }
    }

    public boolean updateCorreo(String nombreUS, String nuevoCorreo) throws SQLException {
        try(Connection c = dataSource.getConnection(); 
            PreparedStatement p = c.prepareStatement("UPDATE Jugador SET Correo = ? WHERE Nombre_US = ?")) { 
            p.setString(1, nuevoCorreo); 
            p.setString(2, nombreUS); 
            p.executeUpdate(); 
            return true;
        }catch (SQLException e) {
            return false; // Si hay una excepción, asumimos que no se creó
        }
    }

    public boolean updateCores(String nombreUS, int nuevosCores) throws SQLException {
        try(Connection c = dataSource.getConnection(); 
            PreparedStatement p = c.prepareStatement("UPDATE Jugador SET Cores = ? WHERE Nombre_US = ?")) { 
            p.setInt(1, nuevosCores); 
            p.setString(2, nombreUS); 
            p.executeUpdate(); 
            return true;
        }catch (SQLException e) {
            return false; // Si hay una excepción, asumimos que no se creó
        }
    }

    public boolean updatePartidasGanadas(String nombreUS, int nuevasPartidasGanadas) throws SQLException {
        try(Connection c = dataSource.getConnection(); 
            PreparedStatement p = c.prepareStatement("UPDATE Jugador SET Partidas_Ganadas = ? WHERE Nombre_US = ?")) { 
            p.setInt(1, nuevasPartidasGanadas); 
            p.setString(2, nombreUS); 
            p.executeUpdate(); 
            return true;
        }catch (SQLException e) {
            return false; // Si hay una excepción, asumimos que no se creó
        }
    }

    public boolean updatePartidasJugadas(String nombreUS, int nuevasPartidasJugadas) throws SQLException {
        try(Connection c = dataSource.getConnection(); 
            PreparedStatement p = c.prepareStatement("UPDATE Jugador SET Partidas_Jugadas = ? WHERE Nombre_US = ?")) { 
            p.setInt(1, nuevasPartidasJugadas); 
            p.setString(2, nombreUS); 
            p.executeUpdate(); 
            return true;
        }catch (SQLException e) {
            return false; // Si hay una excepción, asumimos que no se creó
        }
    }

    // nuevos updates para avtar y skin
    public boolean updateAvatar(String nombreUS, String nuevoAvatarId) throws SQLException {
        try(Connection c = dataSource.getConnection();
            PreparedStatement p = c.prepareStatement("UPDATE Jugador SET avatar_id = ? WHERE Nombre_US = ?")) {
            p.setString(1, nuevoAvatarId);
            p.setString(2, nombreUS);
            p.executeUpdate();
            return true;
        }catch (SQLException e) {
            return false; // Si hay una excepción, asumimos que no se creó
        }
    }

    public boolean updateSkinActiva(String nombreUS, String nuevaSkin) throws SQLException {
        try(Connection c = dataSource.getConnection();
             PreparedStatement p = c.prepareStatement("UPDATE Jugador SET skin_activa = ? WHERE Nombre_US = ?")) {
            p.setString(1, nuevaSkin);
            p.setString(2, nombreUS);
            p.executeUpdate();
            return true;
        }catch (SQLException e) {
            return false; // Si hay una excepción, asumimos que no se actualizó
        }
    }

    /**
     * Inserta una amistad aceptada. Las solicitudes se gestionan en Notificaciones.
     * Mantiene Jugador_1 < Jugador_2 para consistencia.
     */
    public boolean insertarAmistad(String jugadorA, String jugadorB) throws SQLException {
        if (jugadorA.equals(jugadorB)) return false;
        String j1 = jugadorA.compareTo(jugadorB) < 0 ? jugadorA : jugadorB;
        String j2 = jugadorA.compareTo(jugadorB) < 0 ? jugadorB : jugadorA;

        final String sql = "INSERT INTO Amistades (Jugador_1, Jugador_2) VALUES (?, ?) ON CONFLICT (Jugador_1, Jugador_2) DO NOTHING";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, j1);
            ps.setString(2, j2);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean sonAmigos(String jugadorA, String jugadorB) throws SQLException {
        if (jugadorA.equals(jugadorB)) return false;
        String j1 = jugadorA.compareTo(jugadorB) < 0 ? jugadorA : jugadorB;
        String j2 = jugadorA.compareTo(jugadorB) < 0 ? jugadorB : jugadorA;
        final String sql = "SELECT 1 FROM Amistades WHERE Jugador_1 = ? AND Jugador_2 = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, j1);
            ps.setString(2, j2);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public List<Jugador> sacarAmigos(String nombreUS) throws SQLException {
        // añadido avatar y skin
        final String sql = "SELECT DISTINCT j.Correo, j.Nombre_US, j.Contrasena_Hash, j.Puntos, j.Cores, j.Partidas_Ganadas, j.Partidas_Jugadas, j.avatar_id, j.skin_activa FROM Jugador j, Amistades a WHERE (j.Nombre_US = a.Jugador_1 AND a.Jugador_2 = ?) OR (j.Nombre_US = a.Jugador_2 AND a.Jugador_1 = ?)";

        List<Jugador> amigos = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
            PreparedStatement p = conn.prepareStatement(sql)) {
            p.setString(1, nombreUS);
            p.setString(2, nombreUS);

            try (ResultSet rs = p.executeQuery()) {
                while (rs.next()) {
                    amigos.add(montarJugador(rs));
                }
            }
        }
        return amigos;
    }

    public List<Jugador> sacarJugadoresDisp() throws SQLException {
        final String sql = "SELECT * FROM Jugador";

        List<Jugador> jugadores = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
            PreparedStatement p = conn.prepareStatement(sql)) {

            try (ResultSet rs = p.executeQuery()) {
                while (rs.next()) {
                    jugadores.add(montarJugador(rs));
                }
            }
        }
        return jugadores;
    }

    public boolean borrarAmigo(String miNombre, String nombreAmigo) throws SQLException {
        if (miNombre.equals(nombreAmigo)) return false;
        String j1 = miNombre.compareTo(nombreAmigo) < 0 ? miNombre : nombreAmigo;
        String j2 = miNombre.compareTo(nombreAmigo) < 0 ? nombreAmigo : miNombre;
        final String sql = "DELETE FROM Amistades WHERE Jugador_1 = ? AND Jugador_2 = ?";
        try (Connection conn = dataSource.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, j1);
            ps.setString(2, j2);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            return false;
        }
    }

    public boolean desbloquearSkin(String miNombre, String skin) throws SQLException {
        final String sql = "INSERT INTO Jugador_Skins (Jugador, Skin) VALUES (?, ?)";
        
        try (Connection conn = dataSource.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, miNombre);
            ps.setString(2, skin);
            int filasAfectadas = ps.executeUpdate();
            return filasAfectadas > 0;
            
        } catch (SQLException e) {
            return false; // Si hay una excepción, asumimos que no se creó
        }
    }

    public void borrar(String nombreUS) throws SQLException {
        final String sql = "DELETE FROM Jugador WHERE Nombre_US = ?";
        try (Connection conn = dataSource.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nombreUS);
            ps.executeUpdate();
        }
    }

    public Jugador buscarJugadorPorCorreo(String correo) throws SQLException {
        final String sql = "SELECT Correo, Nombre_US, Contrasena_Hash, Puntos, Cores, Partidas_Ganadas, Partidas_Jugadas, avatar_id, skin_activa FROM Jugador WHERE Correo = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, correo);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return montarJugador(rs);
            }
        }
    }

    //Metodo auxiliar que saca los campos de la BD y crea un objeto de tipo Jugador
    private Jugador montarJugador(ResultSet rs) throws SQLException {
        return new Jugador(
            rs.getString("Correo"),
            rs.getString("Nombre_US"),
            rs.getString("Contrasena_Hash"),
            rs.getInt("Puntos"),
            rs.getInt("Cores"),
            rs.getInt("Partidas_Ganadas"),
            rs.getInt("Partidas_Jugadas"),
            // añadido avatar y skin
            rs.getString("avatar_id"),
            rs.getString("skin_activa")
        );
    }
}
