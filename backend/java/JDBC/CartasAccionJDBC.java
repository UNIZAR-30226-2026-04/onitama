package JDBC;

import java.sql.*;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

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
    
    public List<CartaAccion> sacarCartasPartida(int IDPartida) throws SQLException {
        final String sql = "SELECT c.Nombre, c.Accion, c.Puntos_min FROM Cartas_Accion c, Partida_Cartas_Accion p WHERE c.Nombre = ID_Carta_Accion AND ID_Partida = ?";

        List<CartaAccion> cartas = new ArrayList<>();

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
            PreparedStatement p = c.prepareStatement("UPDATE Partida_Cartas_Accion SET Estado = ? WHERE ID_Carta_Accion = ? AND ID_Partida = ?")) { 
            p.setString(1, estado); 
            p.setString(2, nombreCarta); 
            p.setInt(2, IDPartida); 
            p.executeUpdate(); 
            return true;
        }catch (SQLException e) {
            return false; // Si hay una excepción, asumimos que no se creó
        }
    }

    public boolean asignar4CartasPartida(int IDPartida, int puntosMin) {
        List<CartaAccion> disponibles = sacarCartas();

        //Eliminamos de la lista las cartas que sobrepasen el minimo IMPORTANTE: Poner al menos 4 cartas que no necesiten un minimos de puntos para jugarse
        for(CartaAccion carta : disponibles){
            if(carta.getPuntosMin() > puntosMin){
                disponibles.remove(carta);
            }
        }

        Collections.shuffle(disponibles);
        for(int i = 0; i<4; i++){
            if(!asignarCartaPartida(IDPartida, disponibles.get(i).getNombre())){
                return false;
            }
        }
        return true;
    }

    public boolean asignarCartaPartida(int IDPartida, String nombreCarta) throws SQLException {
        final String sql = "INSERT INTO Partida_Cartas_Accion (ID_Partida, ID_Carta_Accion, Estado) VALUES (?, ?, ?)";
        
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
