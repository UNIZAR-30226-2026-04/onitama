package JDBC;

import java.sql.*;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.util.List;
import java.util.ArrayList;
import VO.Jugador;

public final class JugadorJDBC{

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
        final String sql = "INSERT INTO Jugador (Correo, Nombre_US, Contrasena_Hash, Puntos) VALUES (?, ?, ?, ?)";
        
        try (Connection conn = dataSource.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, jugador.getCorreo());
            ps.setString(2, jugador.getNombre());
            ps.setString(3, jugador.getContrasenya());
            ps.setInt(4, jugador.getPuntos());
            int filasAfectadas = ps.executeUpdate();
            return filasAfectadas > 0;
            
        } catch (SQLException e) {
            return false; // Si hay una excepción, asumimos que no se creó
        }
    }

    public Jugador buscarJugador(String nombreUS) throws SQLException {
        final String sql = "SELECT Correo, Nombre_US, Contrasena_Hash, Puntos FROM Jugador WHERE Nombre_US = ?";
        try (Connection conn = dataSource.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nombreUS);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return montarJugador(rs);
            }
        }
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
            PreparedStatement p = c.prepareStatement("UPDATE Jugador SET Puntos = ? WHERE Nombre_US = ?")) { 
            p.setString(1, nuevoCorreo); 
            p.setString(2, nombreUS); 
            p.executeUpdate(); 
            return true;
        }catch (SQLException e) {
            return false; // Si hay una excepción, asumimos que no se creó
        }
    }

    public List<Jugador> sacarAmigos(String nombreUS) throws SQLException {
        final String sql = "SELECT DISTINCT j.Correo, j.Nombre_US, j.Contrasena_Hash, j.Puntos FROM Jugador j, Amistades a WHERE (j.Nombre_US = a.Jugador_1 AND a.Jugador_2 = ?) OR (j.Nombre_US = a.Jugador_2 AND a.Jugador_1 = ?)";

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

    public boolean anyadirAmigo(String miNombre, String nombreAmigo) throws SQLException {
        final String sql = "INSERT INTO Amistades (Jugador_1, Jugador_2) VALUES (?, ?)";
        
        try (Connection conn = dataSource.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, miNombre);
            ps.setString(2, nombreAmigo);
            int filasAfectadas = ps.executeUpdate();
            return filasAfectadas > 0;
            
        } catch (SQLException e) {
            return false; // Si hay una excepción, asumimos que no se creó
        }
    }

    public boolean borrarAmigo(String miNombre, String nombreAmigo) throws SQLException {
        final String sql = "DELETE FROM Amistades WHERE (Jugador_1 = ? AND Jugador_2 = ?) OR (Jugador_2 = ? AND Jugador_1 = ?)";
        try (Connection conn = dataSource.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, miNombre);
            ps.setString(2, nombreAmigo);
            ps.setString(3, miNombre);
            ps.setString(4, nombreAmigo);
            ps.executeUpdate();
            return true;
            
        } catch (SQLException e) {
            return false; // Si hay una excepción, asumimos que no se creó
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

    //Metodo auxiliar que saca los campod de la BD y crea un objeto de tipo Jugador
    private Jugador montarJugador(ResultSet rs) throws SQLException {
        return new Jugador(
            rs.getString("Correo"),
            rs.getString("Nombre_US"),
            rs.getString("Contrasena_Hash"),
            rs.getInt("Puntos")
        );
    }
}
