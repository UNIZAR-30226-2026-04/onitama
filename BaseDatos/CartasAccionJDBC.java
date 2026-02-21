import java.sql.*;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.util.List;
import java.util.ArrayList;

public final class CartasAccionJDBC implements movimientoDAO {

    private final DataSource dataSource;

    public CartasAccionJDBC() {
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
    
    public boolean crearCarta(CartaAccion movimiento) throws SQLException {
        final String sql = "INSERT INTO Cartas_Accion (Nombre, Accion, Puntos_min) VALUES (?, ?, ?)";
        
        try (Connection conn = dataSource.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, movimiento.getNombre());
            ps.setString(2, movimiento.getAccion());
            ps.setInt(3, movimiento.getPuntosMin());
            int filasAfectadas = ps.executeUpdate();
            return filasAfectadas > 0;
            
        } catch (SQLException e) {
            return false; // Si hay una excepción, asumimos que no se creó
        }
    }

    public CartaAccion buscarAccion(String nombre) throws SQLException {
        final String sql = "SELECT * FROM Cartas_Accion WHERE Nombre = ?";
        try (Connection conn = dataSource.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nombre);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return montarAccion(rs);
            }
        }
    }

    public boolean updateAccion(String nombre, int nuevaAccion) throws SQLException {
        try(Connection c = dataSource.getConnection(); 
            PreparedStatement p = c.prepareStatement("UPDATE Cartas_Accion SET Accion = ? WHERE Nombre = ?")) { 
            p.setString(1, nuevaAccion); 
            p.setString(2, nombre); 
            p.executeUpdate(); 
            return true;
        }catch (SQLException e) {
            return false; // Si hay una excepción, asumimos que no se creó
        }
    }

    public boolean updatePuntosMin(String nombre, int nuevoPunt) throws SQLException {
        try(Connection c = dataSource.getConnection(); 
            PreparedStatement p = c.prepareStatement("UPDATE Cartas_Accion SET Puntos_min = ? WHERE Nombre = ?")) { 
            p.setInt(1, nuevoPunt); 
            p.setString(2, nombre); 
            p.executeUpdate(); 
            return true;
        }catch (SQLException e) {
            return false; // Si hay una excepción, asumimos que no se creó
        }
    }

    public List<CartaAccion> sacarCartas() throws SQLException {
        final String sql = "SELECT * FROM Cartas_Accion";

        List<CartaAccion> cartas = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
            PreparedStatement p = conn.prepareStatement(sql)) {

            try (ResultSet rs = p.executeQuery()) {
                while (rs.next()) {
                    cartas.add(montarAccion(rs));
                }
            }
        }
        return cartas;
    }

    public void borrar(String nombre) throws SQLException {
        final String sql = "DELETE FROM Cartas_Accion WHERE Nombre = ?";
        try (Connection conn = dataSource.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nombre);
            ps.executeUpdate();
        }
    }

    //Metodo auxiliar que saca los campod de la BD y crea un objeto de tipo movimiento
    private CartaAccion montarAccion(ResultSet rs) throws SQLException {
        return new CartaAccion(
            rs.getString("Nombre"),
            rs.getString("Accion"),
            rs.getInt("Puntos_min")
        );
    }
}
