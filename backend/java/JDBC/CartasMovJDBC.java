package JDBC;

import java.sql.*;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

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
    
    public List<CartaMov> sacarCartasPartida(int IDPartida) throws SQLException {
        final String sql = "SELECT c.Nombre, c.Accion, c.Puntos_min FROM Cartas_Accion c, Partida_Cartas_Mov p WHERE c.Nombre = ID_Carta_Mov AND ID_Partida = ?";

        List<CartaMov> cartas = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
            PreparedStatement p = conn.prepareStatement(sql)) {
            p.setInt(2, IDPartida); 

            try (ResultSet rs = p.executeQuery()) {
                while (rs.next()) {
                    cartas.add(montarAccion(rs));
                }
            }
        }
        return cartas;
    }

    public boolean updateEstadoEnPartida(int IDPartida, String nombreCarta, String estado) throws SQLException {
        try(Connection c = dataSource.getConnection(); 
            PreparedStatement p = c.prepareStatement("UPDATE Partida_Cartas_Mov SET Estado = ? WHERE ID_Carta_Mov = ? AND ID_Partida = ?")) { 
            p.setString(1, estado); 
            p.setString(2, nombreCarta); 
            p.setInt(2, IDPartida); 
            p.executeUpdate(); 
            return true;
        }catch (SQLException e) {
            return false; // Si hay una excepción, asumimos que no se creó
        }
    }

    public boolean asignar8CartasPartida(int IDPartida) {
        List<CartaMov> disponibles = sacarCartas(); //IMPORTANTE: Tiene que haber mas de 7 cartas para que funcione
        Collections.shuffle(disponibles);
        for(int i = 0; i<8; i++){
            if(!asignarCartaPartida(IDPartida, disponibles.get(i).getNombre())){
                return false;
            }
        }
        return true;
    }

    public boolean asignarCartaPartida(int IDPartida, String nombreCarta) throws SQLException {
        final String sql = "INSERT INTO Partida_Cartas_Mov (ID_Partida, ID_Carta_Mov, Estado) VALUES (?, ?, ?)";
        
        try (Connection conn = dataSource.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, IDPartida);
            ps.setString(2, nombreCarta);
            ps.setString(3, "Mazo");
            int filasAfectadas = ps.executeUpdate();
            return filasAfectadas > 0;
            
        } catch (SQLException e) {
            return false; // Si hay una excepción, asumimos que no se creó
        }
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
