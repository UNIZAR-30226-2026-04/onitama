package backend.JDBC;

import java.sql.*;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import backend.VO.Skin;
import java.util.List;
import java.util.ArrayList;

import backend.DAO.SkinDAO;

public final class SkinJDBC implements SkinDAO {

    private final DataSource dataSource;

    public SkinJDBC() {
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
    
    // inserta una nueva skin en la tabla Skin con sus características
    public boolean crearSkin(Skin skin) throws SQLException {
        final String sql = "INSERT INTO Skin (Nombre, Precio) VALUES (?, ?)";

        try (Connection conn = dataSource.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, skin.getNombre());
            ps.setInt(2, skin.getPrecio());
            int filasAfectadas = ps.executeUpdate();
            return filasAfectadas > 0;
            
        } catch (SQLException e) {
            return false; // Si hay una excepción, asumimos que no se creó
        }
    }

    // asocia una skin a un jugador insertando un nuevo registro en la tabla Jugador_Skin.
    public boolean comprarSkin(String nombre, String jugador) throws SQLException {
        final String sql = "INSERT INTO Jugador_Skins (Jugador, Skin) VALUES (?, ?)";

        try (Connection conn = dataSource.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, jugador);
            ps.setString(2, nombre);
            int filasAfectadas = ps.executeUpdate();
            return filasAfectadas > 0;
            
        } catch (SQLException e) {
            return false; // Si hay una excepción, asumimos que no se creó
        }
    }

    // busca skin por nombre, null si no existe
    public Skin buscarSkin(String nombre) throws SQLException {
        final String sql = "SELECT * FROM Skin WHERE Nombre = ?";
        try (Connection conn = dataSource.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nombre);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return montarSkin(rs);
            }
        }
    }

    // actualiza el precio de una skin al buscarla por su nombre
    public boolean updatePrecio(String nombre, int nuevosPrecio) throws SQLException {
        try(Connection c = dataSource.getConnection(); 
            PreparedStatement p = c.prepareStatement("UPDATE Skin SET Precio = ? WHERE Nombre = ?")) { 
            p.setInt(1, nuevosPrecio); 
            p.setString(2, nombre); 
            p.executeUpdate(); 
            return true;
        }catch (SQLException e) {
            return false; // Si hay una excepción, asumimos que no se creó
        }
    }

    // devuelve la lista completa de skins disponibles en la tienda
    public List<Skin> sacarSkinDisp() throws SQLException {
        final String sql = "SELECT * FROM Skin";

        List<Skin> tienda = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
            PreparedStatement p = conn.prepareStatement(sql)) {

            try (ResultSet rs = p.executeQuery()) {
                while (rs.next()) {
                    tienda.add(montarSkin(rs));
                }
            }
        }
        return tienda;
    }

    // devuelve las skins que tiene un jugador concreto
    public List<Skin> sacarSkinJugador(String nombreUS) throws SQLException {
        final String sql = "SELECT s.Nombre, s.Precio FROM Skin s, Jugador_Skins js WHERE js.Skin = s.Nombre AND js.Jugador = ?";

        List<Skin> jugador = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
            PreparedStatement p = conn.prepareStatement(sql)) {
            p.setString(1, nombreUS); 

            try (ResultSet rs = p.executeQuery()) {
                while (rs.next()) {
                    jugador.add(montarSkin(rs));
                }
            }
        }
        return jugador;
    }

    // elimina una skin de la tabla por su nombre
    public void borrar(String nombre) throws SQLException {
        final String sql = "DELETE FROM Skin WHERE Nombre = ?";
        try (Connection conn = dataSource.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nombre);
            ps.executeUpdate();
        }
    }

    //Metodo auxiliar que saca los campod de la BD y crea un objeto de tipo Jugador
    private Skin montarSkin(ResultSet rs) throws SQLException {
        return new Skin(
            rs.getString("Nombre"),
            rs.getInt("Precio")
        );
    }
}
