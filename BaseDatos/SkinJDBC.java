import java.sql.*;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.util.List;
import java.util.ArrayList;

public final class SkinJDBC implements SkinDAO {

    private final DataSource dataSource;

    public SkinJDBC() {
        try {
            Context initialContext = new InitialContext();
            // "java:comp/env" es el entorno de nombres específico de esta aplicación
            Context envContext = (Context) initialContext.lookup("java:comp/env");
            // Busca la referencia definida en context.xml / web.xml
            this.dataSource = (DataSource) envContext.lookup("jdbc/MiDataSource");
            
        } catch (NamingException e) {
            System.err.println("ERROR FATAL: No se pudo obtener el recurso JNDI 'jdbc/MiDataSource'.");
            e.printStackTrace();
            throw new RuntimeException("Fallo al inicializar la conexión con la BD.", e);
        }
    }
    
    public boolean crearSkin(Skin skin) throws SQLException {
        final String sql = "INSERT INTO Skin (Nombre, Precio, Color_tablero, Color_Fichas_Aliadas, Color_Fichas_Enemigas) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = dataSource.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, skin.getNombre());
            ps.setInt(2, skin.getPrecio());
            ps.setString(3, skin.getTablero());
            ps.setString(4, skin.getAliadas());
            ps.setString(5, skin.getEnemigas());
            int filasAfectadas = ps.executeUpdate();
            return filasAfectadas > 0;
            
        } catch (SQLException e) {
            return false; // Si hay una excepción, asumimos que no se creó
        }
    }

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

    public boolean updateTablero(String nombre, String nuevoTablero) throws SQLException {
        try(Connection c = dataSource.getConnection(); 
            PreparedStatement p = c.prepareStatement("UPDATE Skin SET Color_tablero = ? WHERE Nombre = ?")) { 
            p.setString(1, nuevoTablero); 
            p.setString(2, nombre); 
            p.executeUpdate(); 
            return true;
        }catch (SQLException e) {
            return false; // Si hay una excepción, asumimos que no se creó
        }
    }

    public boolean updateAliadas(String nombre, int nuevaAliada) throws SQLException {
        try(Connection c = dataSource.getConnection(); 
            PreparedStatement p = c.prepareStatement("UPDATE Skin SET Color_Fichas_Aliadas = ? WHERE Nombre = ?")) { 
            p.setString(1, nuevaAliada); 
            p.setString(2, nombre); 
            p.executeUpdate(); 
            return true;
        }catch (SQLException e) {
            return false; // Si hay una excepción, asumimos que no se creó
        }
    }

    public boolean updateEnemigas(String nombre, int nuevaEnemiga) throws SQLException {
        try(Connection c = dataSource.getConnection(); 
            PreparedStatement p = c.prepareStatement("UPDATE Skin SET Color_Fichas_Enemigas = ? WHERE Nombre = ?")) { 
            p.setString(1, nuevaEnemiga); 
            p.setString(2, nombre); 
            p.executeUpdate(); 
            return true;
        }catch (SQLException e) {
            return false; // Si hay una excepción, asumimos que no se creó
        }
    }

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

    public List<Skin> sacarSkinJugador(String nombreUS) throws SQLException {
        final String sql = "SELECT s.Nombre, s.Precio, s.Color_tablero, s.Color_Fichas_Aliadas, s.Color_Fichas_Enemigas FROM Skin s, Jugador_Skins js WHERE js.Skin = s.Nombre AND js.Jugador = ?";

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
            rs.getInt("Precio"),
            rs.getString("Color_tablero"),
            rs.getString("Color_Fichas_Aliadas"),
            rs.getString("Color_Fichas_Enemigas")
        );
    }
}
