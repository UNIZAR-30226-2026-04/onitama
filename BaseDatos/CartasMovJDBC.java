import java.sql.*;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.util.List;
import java.util.ArrayList;

public final class CartasMovJDBC implements movimientoDAO {

    private final DataSource dataSource;

    public CartasMovJDBC() {
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
    
    public boolean crearCarta(CartaMov movimiento) throws SQLException {
        final String sql = "INSERT INTO Cartas_Mov (Nombre, Movimientos) VALUES (?, ?)";
        
        try (Connection conn = dataSource.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, movimiento.getNombre());
            ps.setString(2, movimiento.getMovimientos());
            int filasAfectadas = ps.executeUpdate();
            return filasAfectadas > 0;
            
        } catch (SQLException e) {
            return false; // Si hay una excepción, asumimos que no se creó
        }
    }

    public CartaMov buscarMovimiento(String nombre) throws SQLException {
        final String sql = "SELECT * FROM Cartas_Mov WHERE Nombre = ?";
        try (Connection conn = dataSource.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nombre);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return montarMovimiento(rs);
            }
        }
    }

    public boolean updateMovimientos(String nombre, int nuevosMov) throws SQLException {
        try(Connection c = dataSource.getConnection(); 
            PreparedStatement p = c.prepareStatement("UPDATE Cartas_Mov SET Movimientos = ? WHERE Nombre = ?")) { 
            p.setString(1, nuevosMov); 
            p.setString(2, nombre); 
            p.executeUpdate(); 
            return true;
        }catch (SQLException e) {
            return false; // Si hay una excepción, asumimos que no se creó
        }
    }

    public List<CartaMov> sacarCartas() throws SQLException {
        final String sql = "SELECT * FROM Cartas_Mov";

        List<CartaMov> cartas = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
            PreparedStatement p = conn.prepareStatement(sql)) {

            try (ResultSet rs = p.executeQuery()) {
                while (rs.next()) {
                    cartas.add(montarMovimiento(rs));
                }
            }
        }
        return cartas;
    }

    public void borrar(String nombre) throws SQLException {
        final String sql = "DELETE FROM Cartas_Mov WHERE Nombre = ?";
        try (Connection conn = dataSource.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nombre);
            ps.executeUpdate();
        }
    }

    //Metodo auxiliar que saca los campod de la BD y crea un objeto de tipo movimiento
    private CartaMov montarMovimiento(ResultSet rs) throws SQLException {
        return new CartaMov(
            rs.getString("Nombre"),
            rs.getString("Movimientos")
        );
    }
}
